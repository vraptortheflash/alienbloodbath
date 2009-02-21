// Copyright 2008 and onwards Matthew Burkhart and Matthew Barnes.
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
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;
import java.lang.Math;
import java.util.TreeMap;
import java.util.Random;
import junit.framework.Assert;


/** The Weapon class represents a single instance of a weapon. Weapons create
 * projectiles which harm enemies. Weapons also define the projectiles they
 * generate. */
public class Weapon extends Entity {
  public Weapon(GameState game_state) {
    super();
    mGameState = game_state;  // Needed for projectile instantiation.
  }

  public void loadFromUri(Uri uri) {
    // The following map defines all of the accepted weapon and projectile
    // parameters. The parameters map is expected to populated with default
    // values letting the user override only a subset if desired within the text
    // resource at the specified uri.
    TreeMap<String, Object> parameters = new TreeMap<String, Object>();
    parameters.put(kParameterDelay, new Float(kDefaultDelay));
    parameters.put(kParameterProjectileRectBottom, new Integer(-1));
    parameters.put(kParameterProjectileRectLeft, new Integer(-1));
    parameters.put(kParameterProjectileRectRight, new Integer(-1));
    parameters.put(kParameterProjectileRectTop, new Integer(-1));
    parameters.put(kParameterSpread, new Float(kDefaultSpread));
    parameters.put(kParameterSprite, "none");
    parameters.put(kParameterWeaponRectBottom, new Integer(-1));
    parameters.put(kParameterWeaponRectLeft, new Integer(-1));
    parameters.put(kParameterWeaponRectRight, new Integer(-1));
    parameters.put(kParameterWeaponRectTop, new Integer(-1));
    parameters.put(kParameterVelocity, new Float(kDefaultVelocity));
    parameters.put(kParameterVibration, new Integer(kDefaultVibration));

    // Given a fully-specified default weapon parameters map, we can parse and
    // merge in the user defined values. Note that the following method rejects
    // all keys provided by the user which were not defined above.
    String file_path = Content.getTemporaryFilePath(uri);
    String[] tokens = Content.readFileTokens(file_path);
    Content.mergeKeyValueTokensWithMap(tokens, parameters);
    Content.assertIntegerNotNone(parameters, kParameterProjectileRectBottom);
    Content.assertIntegerNotNone(parameters, kParameterProjectileRectLeft);
    Content.assertIntegerNotNone(parameters, kParameterProjectileRectRight);
    Content.assertIntegerNotNone(parameters, kParameterProjectileRectTop);
    Content.assertStringNotNone(parameters, kParameterSprite);
    Content.assertIntegerNotNone(parameters, kParameterWeaponRectBottom);
    Content.assertIntegerNotNone(parameters, kParameterWeaponRectLeft);
    Content.assertIntegerNotNone(parameters, kParameterWeaponRectRight);
    Content.assertIntegerNotNone(parameters, kParameterWeaponRectTop);

    // Path names are expected to be relative to the path specified for the
    // weapon definition file.
    String uri_string = uri.toString();
    String base_uri_string =
        uri_string.substring(0, uri_string.lastIndexOf("/") + 1);

    // Now that the user defined weapon parameters have been parsed and merged,
    // we can initialize the weapon instance state accordingly.
    mDelay = ((Float)parameters.get(kParameterDelay)).floatValue();
    mSpread = ((Float)parameters.get(kParameterSpread)).floatValue();
    mVelocity = ((Float)parameters.get(kParameterVelocity)).floatValue();
    mVibration = ((Integer)parameters.get(kParameterVibration)).intValue();
    mSpriteUri = Uri.parse(base_uri_string + (String)parameters.get(kParameterSprite));
    sprite_rect = new Rect(
        ((Integer)parameters.get(kParameterWeaponRectLeft)).intValue(),
        ((Integer)parameters.get(kParameterWeaponRectTop)).intValue(),
        ((Integer)parameters.get(kParameterWeaponRectRight)).intValue(),
        ((Integer)parameters.get(kParameterWeaponRectBottom)).intValue());
    mProjectileRect = new Rect(
        ((Integer)parameters.get(kParameterProjectileRectLeft)).intValue(),
        ((Integer)parameters.get(kParameterProjectileRectTop)).intValue(),
        ((Integer)parameters.get(kParameterProjectileRectRight)).intValue(),
        ((Integer)parameters.get(kParameterProjectileRectBottom)).intValue());
  }

  public void enableShooting(boolean shooting) {
    mShooting = shooting;
  }

  @Override
  public void step(float time_step) {
    super.step(time_step);

    // Update the shooting mechanism. The following is specialized for running
    // or standing on the ground versus jumping.
    mCurrentDelay -= time_step;
    if (mShooting && mCurrentDelay < time_step && sprite_image != -1) {
      mCurrentDelay = mDelay;
      mPhase += 10.0f;

      float shot_angle;
      float shot_distance = 3 * sprite_rect.width() / 4;
      float shot_velocity = mVelocity;
      float x_offset = shot_distance;
      float y_offset = -10.0f;

      if (!has_ground_contact) {
        shot_angle = mPhase;
        x_offset = shot_distance * (float)Math.cos(shot_angle);
        y_offset = shot_distance * (float)Math.sin(shot_angle);
      } else {
        shot_angle = mSpread * (float)Math.sin(mPhase);
      }

      float dx_offset = shot_velocity * (float)Math.cos(shot_angle);
      float dy_offset = shot_velocity * (float)Math.sin(shot_angle);

      if (sprite_flipped_horizontal) {
        x_offset *= -1.0f;
        dx_offset *= -1.0f;
      }

      mGameState.createProjectile(x + x_offset, y + y_offset,
                                  dx + dx_offset, dy + dy_offset,
                                  sprite_image, mProjectileRect,
                                  sprite_flipped_horizontal);

      if (mVibration > 0) {
        mGameState.vibrate(mVibration);
      }
    }
  }

  /** Draw the weapon position given the positions of the owners "hand"
   * positions. The coordinates are expected to be *screen coordinates*, not
   * world coordinates. */
  public void draw(Graphics graphics, float center_x, float center_y,
                   float zoom, float hand_lx, float hand_ly, float hand_rx,
                   float hand_ry) {
    // Load part image if it hasn't yet been loaded. This is necessary since the
    // graphics class must only be interacted with from the main thread. This is
    // a product of the lack of thread safety in OpenGL.
    if (mSpriteUri != null) {
      String image_path = Content.getTemporaryFilePath(mSpriteUri);
      Bitmap image_bitmap = BitmapFactory.decodeFile(image_path);
      sprite_image = graphics.loadImageFromBitmap(image_bitmap);
      mSpriteUri = null;
    }

    // Note that the avatar hand coordinates are specified in screen
    // coordinates, not world coordinates. They are set via the
    // setHandsPositions method.
    if (sprite_image != -1) {
      int x_offset = -sprite_rect.width() / 4;
      int y_offset = -sprite_rect.height() / 2;
      mDrawingMatrix.setTranslate(hand_rx, hand_ry);
      mDrawingMatrix.preRotate(
          57.2958f * (float)Math.atan2(hand_ly - hand_ry, hand_lx - hand_rx));
      mDrawingMatrix.preTranslate(x_offset, y_offset);
      mDrawingMatrix.preScale(sprite_rect.width(), sprite_rect.height());
      graphics.drawImage(sprite_image, sprite_rect, mDrawingMatrix,
                         false, sprite_flipped_horizontal);
    }
  }

  @Override
  public Object clone() {
    return super.clone();
  }

  private float mCurrentDelay;
  private float mDelay;
  private static Matrix mDrawingMatrix = new Matrix();
  private GameState mGameState;  // Needed for projectile instantiation.
  private float mPhase;
  private Rect mProjectileRect;
  private boolean mShooting;
  private float mSpread;
  private Uri mSpriteUri;
  private float mVelocity;
  private int mVibration;

  private static final float kDefaultDelay = 0.2f;  // Seconds between shots.
  private static final float kDefaultSpread = 15.0f * (float)Math.PI / 180.0f;
  private static final float kDefaultVelocity = 60.0f;
  private static final int kDefaultVibration = 0;

  private static final String kParameterDelay = "delay";
  private static final String kParameterProjectileRectBottom = "projectile_rect_bottom";
  private static final String kParameterProjectileRectLeft = "projectile_rect_left";
  private static final String kParameterProjectileRectRight = "projectile_rect_right";
  private static final String kParameterProjectileRectTop = "projectile_rect_top";
  private static final String kParameterSpread = "spread";
  private static final String kParameterSprite = "sprite";
  private static final String kParameterWeaponRectBottom = "weapon_rect_bottom";
  private static final String kParameterWeaponRectLeft = "weapon_rect_left";
  private static final String kParameterWeaponRectRight = "weapon_rect_right";
  private static final String kParameterWeaponRectTop = "weapon_rect_top";
  private static final String kParameterVerticalSpread = "vertical_spread";
  private static final String kParameterVelocity = "velocity";
  private static final String kParameterVibration = "vibration";
}
