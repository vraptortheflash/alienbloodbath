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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;
import java.lang.Math;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Random;
import java.util.Stack;
import junit.framework.Assert;

import android.com.abb.Content;
import android.com.abb.Entity;
import android.com.abb.Graphics;


/** An ArticulatedEntity is a functional replacement to an Entity. It represents
 * a single drawable, collideable game unit. The ArticulatedEntity class
 * provides, on top of Entity, the drawing of an articulated model and joint
 * animations. */
public class ArticulatedEntity extends Entity {
  public ArticulatedEntity() {
    super();
    mRoot.name = "root";
  }

  public void loadFromUri(Uri uri) {
    // The file at the specified path is expected to contain a series of
    // mappings representing a directed acyclic graph (DAG) of articulated
    // parts. There is to be a single root named "root". The file format is
    // expected to be an ASCII text file laid out with a single arc per line.
    // The first line specifies the image Uri. Each line after the first must be
    // of the format "<part name> <root part name> <rect left> <rect top> <rect
    // right> <rect bottom>".
    //
    // For example:
    // content:///entity.png
    // thigh  root   0   0  30  10
    // leg    thigh  10  0  30  20

    String file_path = Content.getTemporaryFilePath(uri);
    String[] tokens = Content.readFileTokens(file_path);

    final int kLineTokenCount = 6;
    Assert.assertTrue("Articulated entity file empty.", tokens.length > 0);
    Assert.assertEquals("Articulated entity file improperly formatted.",
                        tokens.length % kLineTokenCount, 1);

    mImageUri = Uri.parse(tokens[0]);

    for (int index = 1; index < tokens.length; index += kLineTokenCount) {
      Part part = new Part();
      part.name = tokens[index];
      part.image_rect = new Rect(Integer.parseInt(tokens[index + 2]),
                                 Integer.parseInt(tokens[index + 3]),
                                 Integer.parseInt(tokens[index + 4]),
                                 Integer.parseInt(tokens[index + 5]));

      String root_part_name = tokens[index + 1];
      Part root_part = findPartByName(root_part_name);
      Assert.assertNotNull(
          "Illegal unknown root specification: " + root_part_name, root_part);
      root_part.children.add(part);
    }
  }

  public void loadAnimationFromUri(Uri uri) {
    Animation animation = mAnimationCache.get(uri);
    if (animation != null) {
      mAnimation = animation;
    } else {
      mAnimation = new Animation();
      mAnimation.loadFromUri(uri);
      mAnimationCache.put(uri, mAnimation);
    }
  }

  public void stepAnimation(float time_step) {
    mAnimation.step(time_step);
  }

  @Override
  public void draw(Graphics graphics, float center_x, float center_y,
                   float zoom) {
    // Load part image if it hasn't yet been loaded. This is necessary since the
    // graphics class must only be interacted with from the main thread. This is
    // a product of the lack of thread safety in OpenGL.
    if (mImageUri != null) {
      String image_path = Content.getTemporaryFilePath(mImageUri);
      Bitmap image_bitmap = BitmapFactory.decodeFile(image_path);
      mImageHandle = graphics.loadImageFromBitmap(image_bitmap);
      mImageUri = null;
    }

    Matrix root_transformation = new Matrix();
    root_transformation.preTranslate(
        graphics.getWidth() / 2 + (x - center_x) * zoom,
        graphics.getHeight() / 2 + (y - center_y) * zoom);
    root_transformation.preScale(zoom, zoom);
    mRoot.draw(graphics, mImageHandle, root_transformation, mAnimation);
  }

  private Part findPartByName(String part_name) {
    if (part_name.equals("root")) {
      return mRoot;
    } else {
      return mRoot.findPartByName(part_name);
    }
  }


  /** The Part class structure represents a single element of the articulated
   * entity. */
  private class Part {
    public String name;
    public Rect image_rect = new Rect();
    public ArrayList<Part> children = new ArrayList<Part>();

    public void draw(Graphics graphics, int image_handle,
                     Matrix base_transformation, Animation animation) {
      // Draw children.
      Matrix transformation = new Matrix(base_transformation);
      transformation.preRotate(animation.getPartAngle(name));
      transformation.preTranslate(image_rect.width(), 0.0f);
      for (int child_index = 0; child_index < children.size(); ++child_index) {
        children.get(child_index).draw(
            graphics, image_handle, transformation, animation);
      }

      // Draw self.
      if (image_handle != -1) {  // The root is not drawn.
        transformation.preTranslate(-image_rect.width(),
                                    -image_rect.height() / 2);
        transformation.preScale(image_rect.width(), image_rect.height());
        graphics.drawImage(image_handle, image_rect, transformation, false);
      }
    }

    public Part findPartByName(String part_name) {
      if (part_name.equals(name)) {
        return this;
      }
      for (int child_index = 0; child_index < children.size(); ++child_index) {
        Part result = children.get(child_index).findPartByName(part_name);
        if (result != null) {
          return result;
        }
      }
      return null;
    }
  }

  /** The Animation class stores and provides access to a independent, time
   * varying set of values called tracks. */
  private class Animation {
    public void loadFromUri(Uri uri) {
      // The file format is expected to be an ASCII text file laid out with a
      // single key-frame per line. Each line / key frame must have the format
      // "<part name> <time in seconds> <angle>". Animations are expected to
      // repeat exactly after the final key frame. Key frames must be specified
      // in temporal order with respect to each track, but tracks may be
      // interleaved. Angle units are *degrees*.
      //
      // For example:
      // thigh  0.0  45
      // thigh  0.5  60
      // leg    0.0  180
      // leg    0.5  170

      String file_path = Content.getTemporaryFilePath(uri);
      String[] tokens = Content.readFileTokens(file_path);

      final int kLineTokenCount = 3;
      Assert.assertTrue("Animation file empty.", tokens.length > 0);
      Assert.assertEquals("Animation file improperly formatted.",
                          tokens.length % kLineTokenCount, 0);

      mLength = 0.0f;
      for (int index = 0; index < tokens.length; index += kLineTokenCount) {
        KeyFrame key_frame = new KeyFrame();
        key_frame.time = Float.parseFloat(tokens[index + 1]);
        key_frame.angle = Float.parseFloat(tokens[index + 2]);
        mLength = Math.max(mLength, key_frame.time);

        String part_name = tokens[index];
        ArrayList<KeyFrame> track = mKeyFrames.get(part_name);
        if (track == null) {
          track = new ArrayList<KeyFrame>();
          mKeyFrames.put(part_name, track);
        }
        track.add(key_frame);
      }

      Assert.assertTrue("Animation must be more than 0s.", mLength > 0.0f);
    }

    public void step(float time_step) {
      mTime += time_step;
      while (mTime > mLength) {
        mTime -= mLength;
      }
    }

    public float getPartAngle(String part_name) {
      // We need to find the two nearest key frames and interpolate between
      // them. The following assumes the key frames are sorted within each track
      // prior to this call.
      float time_a = 0.0f;
      float angle_a = 0.0f;
      float time_b = 0.0f;
      float angle_b = 0.0f;

      ArrayList<KeyFrame> part_keyframes = mKeyFrames.get(part_name);
      if (part_keyframes == null) {
        return 0.0f;
      }

      for (int index = 0; index < part_keyframes.size(); ++index) {
        KeyFrame keyframe_a = part_keyframes.get(index);
        if (keyframe_a.time <= mTime) {
          time_b = time_a = keyframe_a.time;
          angle_b = angle_a = keyframe_a.angle;
          if (index < part_keyframes.size() - 1) {
            KeyFrame keyframe_b = part_keyframes.get(index + 1);
            time_b = keyframe_b.time;
            angle_b = keyframe_b.angle;
          }
        } else {
          break;
        }
      }

      if (time_b <= time_a) {
        return angle_a;
      } else {
        float interpolation = (mTime - time_a) / (time_b - time_a);
        return (angle_b * interpolation + angle_a * (1.0f - interpolation));
      }
    }

    /** The KeyFrame class structure represents a single key frame within a
     * single animation track. */
    private class KeyFrame {
      public float time;
      public float angle;
    }

    private float mLength;
    private float mTime;
    private TreeMap<String, ArrayList<KeyFrame>> mKeyFrames =
        new TreeMap<String, ArrayList<KeyFrame>>();
  }

  private Animation mAnimation = new Animation();
  private TreeMap<Uri, Animation> mAnimationCache =
      new TreeMap<Uri, Animation>();
  private int mImageHandle = -1;
  private Uri mImageUri;
  private Part mRoot = new Part();
}
