// Copyright 2008 and onwards Matthew Burkhart.
//
// This program is free software; you can redistribute it and/or modify it under
// the terms of the GNU General Public License as published by the Free Software
// Foundation; version 3 of the License.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
// FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
// details.

package android.com.abb;

import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import junit.framework.Assert;


/** Class to abstract the details of extracting and accessing files from the ABB
 * content zip file in the same was files on disk are accessed. TODO: This class
 * is not thread-safe. Extracted files are not cached. */
public class Content {
  public static void initialize(Resources resources) {
    mResources = resources;
    prepare();
  }

  private static void safePrepare() {
    if (!(new File(kTempContentPath)).exists()) {
      prepare();
    }
  }

  private static void prepare() {
    try {
      InputStream content_input_stream =
          mResources.openRawResource(R.raw.content_package);
      writeStreamToFile(content_input_stream, kTempContentPath);
      content_input_stream.close();
    } catch (IOException ex) {
      Assert.fail("Content::initialize. " +
                  "Failed extracting content package: " + ex.toString());
    }
  }

  public static void cleanup() {
    safeDelete(kTempFilePath);
    safeDelete(kTempContentPath);
  }

  protected static void safeDelete(String file_path) {
    File file = new File(file_path);
    if (file.exists()) {
      file.delete();
    }
  }

  public static boolean exists(Uri uri) {
    Log.d("Content::exists", uri.toString());

    safePrepare();

    // Handle "file://" scheme.
    if (uri.getScheme().equals("file")) {
      return (new File(uri.getPath())).exists();
    }

    // Handle "content://" scheme.
    else if (uri.getScheme().equals("content")) {
      String entry_name = uriToContentEntry(uri);

      String[] entries = rawContentEntries();
      Arrays.sort(entries);
      boolean exists = Arrays.binarySearch(entries, entry_name) > 0;
      if (!exists) {
        Log.d("Content::exists", "Could not find entry: " + entry_name);
      }
      return exists;
    }

    // Bad uri scheme.
    else {
      Assert.fail("Content::exists. Bad URI scheme: " + uri.toString());
      return false;
    }
  }

  public static String[] list(Uri uri) {
    Log.d("Content::list", uri.toString());

    safePrepare();

    // Handle "file://" scheme.
    if (uri.getScheme().equals("file")) {
      return (new File(uri.getPath())).list();
    }

    // Handle "content://" scheme.
    else if (uri.getScheme().equals("content")) {
      String path_prefix = uriToContentEntry(uri);

      String[] entries = rawContentEntries();
      ArrayList<String> list_entries = new ArrayList<String>();
      for (String entry : entries) {
        if (entry.startsWith(path_prefix)) {
          entry = entry.replace(path_prefix, "");
          Log.d("Content::list", "Found entry: " + entry);
          list_entries.add(entry);
        }
      }
      return list_entries.toArray(new String[0]);
    }

    // Bad uri scheme.
    else {
      Assert.fail("Content::list. Bad URI scheme: " + uri.toString());
      return null;
    }
  }

  public static String getTemporaryFilePath(Uri uri) {
    Log.d("Content::getTemporaryFilePath", uri.toString());

    safePrepare();

    // Handle "file://" scheme.
    if (uri.getScheme().equals("file")) {
      return uri.getPath();
    }

    // Handle "content://" scheme. We must extract the file to a temporary path
    // in order to handle this request.
    else if (uri.getScheme().equals("content")) {
      ZipFile content_file;
      try {
        content_file = new ZipFile(kTempContentPath);
      } catch (IOException ex) {
        Assert.fail("Content::getTemporaryFilePath. " +
                    "Unable to open package file: " + ex.toString());
        return null;
      }
      String entry_name = uriToContentEntry(uri);
      ZipEntry content_entry = content_file.getEntry(entry_name);
      if (content_entry == null) {
        Assert.fail("Content::getTemporaryFilePath. " +
                    "Unable to find entry: " + entry_name);
        return null;
      }
      try {
        InputStream content_stream = content_file.getInputStream(content_entry);
        writeStreamToFile(content_stream, kTempFilePath);
      } catch (IOException ex) {
        Assert.fail("Content::getTemporaryFilePath. " +
                    "Unable to write out entry: " + ex.toString());
        return null;
      }
      return kTempFilePath;
    }

    // Bad uri scheme.
    else {
      Assert.fail("Content::getTemporaryFilePath. " +
                  "Bad URI scheme: " + uri.toString());
      return null;
    }
  }

  static public String[] readFileAndSplit(String file_path, String split) {
    try {
      FileReader file_reader = new FileReader(new File(file_path));
      ArrayList<Character> data_array = new ArrayList<Character>();
      while (file_reader.ready()) {
        data_array.add(new Character((char)file_reader.read()));
      }
      String[] raw_tokens = (new String(toPrimative(data_array))).split(split);
      ArrayList<String> tokens = new ArrayList<String>();
      for (String token : raw_tokens) {
        if (token.length() > 0) {
          tokens.add(token);
        }
      }
      return tokens.toArray(new String[0]);
    } catch (IOException ex) {
      Assert.fail("Could not read file: " + file_path + ": " + ex.toString());
    }
    return new String[0];
  }

  /** This method reads a splits an ASCII text file along any new line
   * boundaries. */
  static public String[] readFileLines(String file_path) {
    return readFileAndSplit(file_path, "\\n");
  }

  /** This method reads a splits an ASCII text file along any white space
   * boundaries. */
  static public String[] readFileTokens(String file_path) {
    return readFileAndSplit(file_path, "\\s");
  }

  /** Utility method to parse a set of key value tokens (as generated by
   * readFileTokens(...) for example) and to insert the key value pairs into a
   * map. Only values with keys already present in the map are accepted and
   * types are checked. The map may not be changed if no key / value pairs are
   * present to it is expected to contain value defaults. */
  static public TreeMap<String, Object> mergeKeyValueTokensWithMap(
      String[] tokens, TreeMap<String, Object> map) {
    Assert.assertEquals("Expected an even number of key-values in tokens.",
                        tokens.length % 2, 0);

    for (int key = 0; key < tokens.length; key += 2) {
      String key_string = tokens[key];
      String value_string = tokens[key + 1];
      Assert.assertTrue("Unknown key name: " + key_string,
                        map.containsKey(key_string));

      Object default_value = map.get(key_string);
      if (default_value instanceof String) {
        map.put(key_string, value_string);
      } else if (default_value instanceof Integer) {
        try {
          map.put(key_string, new Integer(value_string));
        } catch (NumberFormatException ex) {
          Assert.fail("Error, expected Integer: " + ex.toString());
        }
      } else if (default_value instanceof Float) {
        try {
          map.put(key_string, new Float(value_string));
        } catch (NumberFormatException ex) {
          Assert.fail("Error, expected Float: " + ex.toString());
        }
      } else {
        Assert.fail("Unsupported value type: " +
                    default_value.getClass().getName());
      }
    }
    return map;
  }

  static void assertStringNotNone(TreeMap<String, Object> parameters,
                                  String parameter) {
    Assert.assertTrue("Parameter " + parameter + " must be specified.",
                      !((String)parameters.get(parameter)).equals("none"));
  }

  static void assertIntegerNotNone(TreeMap<String, Object> parameters,
                                   String parameter) {
    Assert.assertTrue("Parameter " + parameter + " must be specified.",
                      ((Integer)parameters.get(parameter)).intValue() != -1);
  }

  static private char[] toPrimative(ArrayList<Character> array_list) {
    char[] result = new char[array_list.size()];
    for (int index = 0; index < array_list.size(); ++index) {
      result[index] = array_list.get(index).charValue();
    }
    return result;
  }

  private static String uriToContentEntry(Uri uri) {
    String content_name = uri.getHost() + uri.getPath();

    // Strip any leading / since, while it should be in the Uri, the zip file
    // entries to not contain the "root" slash.
    if (content_name.length() > 0 && content_name.charAt(0) == '/') {
      content_name = content_name.substring(1);
    }
    return "content_package/" + content_name;
  }

  private static void writeStreamToFile(InputStream input_stream,
                                        String output_path) throws IOException {
    (new File(output_path)).createNewFile();
    BufferedOutputStream output_stream =
        new BufferedOutputStream(new FileOutputStream(output_path), 8 * 1024);
    byte[] buffer = new byte[1024];
    int bytes_read;
    while((bytes_read = input_stream.read(buffer)) >= 0)
      output_stream.write(buffer, 0, bytes_read);
    output_stream.close();
  }

  private static String[] rawContentEntries() {
    ZipFile content_file;
    try {
      content_file = new ZipFile(kTempContentPath);
    } catch (IOException ex) {
      Assert.fail("Content::rawContentEntries, " +
                  "Unable to open package file: " + ex.toString());
      return null;
    }
    if (!content_file.entries().hasMoreElements()) {
      Log.d("Content::rawContentEntries", "Content package empty.");
    }

    ArrayList<String> entry_list = new ArrayList<String>();
    for (Enumeration<? extends ZipEntry> entry_it = content_file.entries();
         entry_it.hasMoreElements();) {
      String entry_name = entry_it.nextElement().getName();
      entry_list.add(entry_name);
    }
    return entry_list.toArray(new String[0]);
  }

  private static Resources mResources;

  private static final String kTempFilePath =
      "/data/data/android.com.abb/abbfile.tmp";
  private static final String kTempContentPath =
      "/data/data/android.com.abb/abbpackage.tmp";
}
