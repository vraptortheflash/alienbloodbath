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

import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;
import android.view.KeyEvent;
import java.lang.Math;
import java.util.TreeMap;
import junit.framework.Assert;


public class Enemy extends ArticulatedEntity {
  public Enemy(Entity target) {
    super();
    mTarget = target;
    radius = kDefaultRadius;
    sprite_rect =
        new Rect(0, kSpriteBase, kSpriteWidth, kSpriteBase + kSpriteHeight);
  }

  public void step(float time_step) {
    super.step(time_step);

    ddy = mGravity;

    // If we have moved close enough to our target, mark it dead.
    if (Math.abs(mTarget.x - x) < radius &&
        Math.abs(mTarget.y - y) < radius) {
      mTarget.alive = false;
    }

    // Always move the enemy towards the target. Set the acceleration and sprite
    // to reflect it.
    int sprite_offset;
    if (mTarget.x < x) {
      sprite_offset = 0;
      ddx = -kAcceleration;
    } else {
      sprite_offset = 2;
      ddx = kAcceleration;
    }
    if (has_ground_contact) {
      ++sprite_offset;
      dy = -kJumpVelocity;
    }

    sprite_rect.top = kSpriteBase + kSpriteHeight * sprite_offset;
    sprite_rect.bottom = kSpriteBase + kSpriteHeight * (sprite_offset + 1);
  }

  public void loadFromUri(Uri uri) {
    TreeMap<String, Object> enemy_parameters = new TreeMap<String, Object>();
    enemy_parameters.put(kParameterEntity, "none");
    enemy_parameters.put(kParameterGravity, new Float(kDefaultGravity));
    enemy_parameters.put(kParameterLife, new Float(kDefaultLife));
    enemy_parameters.put(kParameterRadius, new Float(kDefaultRadius));

    String file_path = Content.getTemporaryFilePath(uri);
    String[] tokens = Content.readFileTokens(file_path);
    Content.mergeKeyValueTokensWithMap(tokens, enemy_parameters);

    mGravity = ((Float)enemy_parameters.get(kParameterGravity)).floatValue();
    mLife = ((Float)enemy_parameters.get(kParameterLife)).floatValue();
    radius = ((Float)enemy_parameters.get(kParameterRadius)).floatValue();

    String entity = (String)enemy_parameters.get(kParameterEntity);
    loadFromUri(Uri.parse(entity));
  }

  private float mGravity;
  private float mLife;
  private Entity mTarget;

  private static final float kAcceleration = 40.0f;
  private static final float kDefaultGravity = 100.0f;
  private static final float kDefaultLife = 1.0f;
  private static final float kDefaultRadius = 20.0f;
  private static final float kJumpVelocity = 100.0f;
  private static final String kParameterEntity = "entity";
  private static final String kParameterGravity = "gravity";
  private static final String kParameterLife = "life";
  private static final String kParameterRadius = "radius";
  private static final int kSpriteBase = 0;
  private static final int kSpriteWidth = 64;
  private static final int kSpriteHeight = 64;
}