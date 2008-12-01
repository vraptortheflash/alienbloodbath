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

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class MapSelectActivity extends ListActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    int item_layout_id = R.layout.mapselect_item;
    LoadMaps();
    String[] maps = maps_.toArray(new String[0]);
    setListAdapter(new ArrayAdapter<String>(this, item_layout_id, maps));
    getListView().setTextFilterEnabled(true);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    Intent intent = new Intent(Intent.ACTION_VIEW,
                               Uri.parse(map_uris_.get(position)),
                               this, AlienBloodBathMain.class);
    setResult(RESULT_OK, intent);
    finish();
  }

  private void UnzipMapPackages() {
    for (String root_path : paths_) {
      String[] files = (new File(root_path)).list();
      if (files == null)
        continue;

      for (String file : files) {
        if (!file.endsWith(kMapPackageSuffix)) {
          try {
            // Create the new map directory.
            String zip_file_path = root_path + "/" + file;
            ZipFile zip_file = new ZipFile(zip_file_path);
            String map_path = root_path + "/" + file.replace(kMapPackageSuffix, "");
            (new File(map_path)).mkdir();

            // Populate the map directory with the zipped contents.
            for (Enumeration<? extends ZipEntry> entry_it = zip_file.entries();
                 entry_it.hasMoreElements();) {
              ZipEntry entry = entry_it.nextElement();
              String output_path = map_path + "/" + entry.getName();
              Log.w("MapSelectActivity::UnzipMapPackages",
                    "Unzipping to " + output_path);

              InputStream input_stream = zip_file.getInputStream(entry);
              BufferedOutputStream output_stream =
                  new BufferedOutputStream(new FileOutputStream(output_path));
              byte[] buffer = new byte[1024];
              int bytes_read;
              while((bytes_read = input_stream.read(buffer)) >= 0)
                output_stream.write(buffer, 0, bytes_read);
              input_stream.close();
              output_stream.close();
            }

            // Delete the source zip package.
            (new File(zip_file_path)).delete();
          } catch (IOException ex) {
            Log.w("MapSelectActivity::UnzipMapPackages",
                  "Cannot unzip package, ignoring.");
          }
        }
      }
    }
  }

  private void LoadMaps() {
    // Unzip map package files locaded on the SD card to we can easily find
    // parse the contents.
    UnzipMapPackages();

    // Add built-in maps.
    maps_.add("Classic Map Set");
    map_uris_.add("content://");

    // Add maps located within files.
    for (String root_path : paths_) {
      String[] map_paths = (new File(root_path)).list();
      if (map_paths == null)
        continue;

      for (String map_path : map_paths) {
        String full_path = root_path + "/" + map_path;
        if ((new File(full_path + "/level_0.txt")).exists()) {
          maps_.add(full_path);
          map_uris_.add("file://" + full_path);
        }
      }
    }
  }

  private ArrayList<String> maps_ = new ArrayList<String>();
  private ArrayList<String> map_uris_ = new ArrayList<String>();
  private String[] paths_ = { "/sdcard/abb_maps", "/sdcard" };

  private final String kMapPackageSuffix = ".abb.zip";
}
