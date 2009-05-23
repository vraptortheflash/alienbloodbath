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
import android.net.Uri;
import android.util.Log;
import java.util.TreeMap;
import junit.framework.Assert;


/** An AnimatedEntity is a functional replacement to an Entity instance. It
 * represents a single drawable, physical game object. The AnimatedEntity class
 * provides, on top of Entity, the selection and drawing of frames from a single
 * image file. */
public class AnimatedEntity extends Entity {
  public AnimatedEntity() {
    super();
  }

  @Override
  public Object clone() {
    return super.clone();
  }

  public void loadFromUri(Uri uri) {
    // The file at the specified path is expected to contain an image name
    // followed by a series of animation sequence specifications. The file
    // format is expected to be an ASCII text file laid out with a single
    // animation sequence line. The first line specifies the image file name
    // containing the animation frames (animations proceed down the image). Each
    // line after the first must be of the format "<animation name> <start x>
    // <start y> <frame width> <frame height> <frame count> <frame rate>".
    //
    // For example:
    // entity.png
    // stand  0  0   64  64  1  1
    // run    0  64  64  64  3  20
    String file_path = Content.getFilePath(uri);
    String[] tokens = Content.readFileTokens(file_path);

    final int kLineTokenCount = 7;
    Assert.assertTrue("Animated entity file empty.", tokens.length > 1);
    Assert.assertEquals("Animated entity file improperly formatted.",
                        (tokens.length - 1) % kLineTokenCount, 0);

    // Path names are expected to be relative to the path specified for the
    // animation definition file.
    String uri_string = uri.toString();
    Log.d("AnimatedEntity::loadFromUri", "Found uri= " + uri_string);
    String base_uri_string = uri_string.substring(0, uri_string.lastIndexOf("/"));
    mImageUri = Uri.parse(base_uri_string + "/" + tokens[0]);

    for (int index = 1; index < tokens.length; index += kLineTokenCount) {
      Animation animation = new Animation();
      animation.start_x = Integer.parseInt(tokens[index + 1]);
      animation.start_y = Integer.parseInt(tokens[index + 2]);
      animation.frame_width = Integer.parseInt(tokens[index + 3]);
      animation.frame_height = Integer.parseInt(tokens[index + 4]);
      animation.frame_count = Integer.parseInt(tokens[index + 5]);
      animation.frame_rate = Float.parseFloat(tokens[index + 6]);
      String animation_name = tokens[index];
      mAnimations.put(animation_name, animation);
    }
  }

  public void setAnimation(String animation) {
    mCurrentAnimation = mAnimations.get(animation);
    Assert.assertNotNull("Error: could not find animation: " + animation,
                         mCurrentAnimation);
  }

  public void stepAnimation(float time_step) {
    mTime += time_step;

    if (mCurrentAnimation == null) {
      return;  // setAnimation(...) must be called first.
    }

    int frame = ((int)(mCurrentAnimation.frame_rate * mTime) %
                 mCurrentAnimation.frame_count);
    sprite_rect.left = mCurrentAnimation.start_x;
    sprite_rect.top = mCurrentAnimation.start_y;
    sprite_rect.right = mCurrentAnimation.start_x + mCurrentAnimation.frame_width;
    sprite_rect.bottom = mCurrentAnimation.start_y + mCurrentAnimation.frame_height;
    sprite_rect.offset(0, mCurrentAnimation.frame_height * frame);
  }

  @Override
  public void draw(Graphics graphics, float center_x, float center_y,
                   float zoom) {
    // Load part image if it hasn't yet been loaded. This is necessary since the
    // graphics class must only be interacted with from the main thread. This is
    // a product of the lack of thread safety in OpenGL.
    if (mImageUri != null) {
      String image_path = Content.getFilePath(mImageUri);
      Bitmap image_bitmap = BitmapFactory.decodeFile(image_path);
      sprite_image = graphics.loadImageFromBitmap(image_bitmap);
      mImageUri = null;
    }

    super.draw(graphics, center_x, center_y, zoom);
  }

  class Animation {
    public int start_x;
    public int start_y;
    public int frame_width;
    public int frame_height;
    public int frame_count;
    public float frame_rate;
  }  // class Animation

  private TreeMap<String, Animation> mAnimations =
      new TreeMap<String, Animation>();
  private Animation mCurrentAnimation;
  private Uri mImageUri;
  private float mTime;
}
