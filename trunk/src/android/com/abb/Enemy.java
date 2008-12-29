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
  }

  public void step(float time_step) {
    super.step(time_step);
    super.stepAnimation(time_step);

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
      sprite_flipped_horizontal = true;
      ddx = -kAcceleration;

    } else {
      sprite_flipped_horizontal = false;
      ddx = kAcceleration;
    }
    if (has_ground_contact) {
      dy = -kJumpVelocity;
    }
  }

  public void loadFromUri(Uri uri) {
    TreeMap<String, Object> enemy_parameters = new TreeMap<String, Object>();
    enemy_parameters.put(kParameterAnimation, "none");
    enemy_parameters.put(kParameterEntity, "none");
    enemy_parameters.put(kParameterGravity, new Float(kDefaultGravity));
    enemy_parameters.put(kParameterLife, new Float(kDefaultLife));
    enemy_parameters.put(kParameterRadius, new Float(kDefaultRadius));

    String file_path = Content.getTemporaryFilePath(uri);
    String[] tokens = Content.readFileTokens(file_path);
    Content.mergeKeyValueTokensWithMap(tokens, enemy_parameters);

    String animation = (String)enemy_parameters.get(kParameterAnimation);
    String entity = (String)enemy_parameters.get(kParameterEntity);
    mGravity = ((Float)enemy_parameters.get(kParameterGravity)).floatValue();
    mLife = ((Float)enemy_parameters.get(kParameterLife)).floatValue();
    radius = ((Float)enemy_parameters.get(kParameterRadius)).floatValue();

    String uri_string = uri.toString();
    String base_uri_string = uri_string.substring(0, uri_string.lastIndexOf("/"));
    super.loadFromUri(Uri.parse(base_uri_string + "/" + entity));
    super.loadAnimationFromUri(Uri.parse(base_uri_string + "/" + animation));
  }

  private float mGravity;
  private float mLife;
  private Entity mTarget;

  private static final float kAcceleration = 40.0f;
  private static final float kDefaultGravity = 100.0f;
  private static final float kDefaultLife = 1.0f;
  private static final float kDefaultRadius = 32.0f;
  private static final float kJumpVelocity = 100.0f;
  private static final String kParameterAnimation = "animation";
  private static final String kParameterEntity = "entity";
  private static final String kParameterGravity = "gravity";
  private static final String kParameterLife = "life";
  private static final String kParameterRadius = "radius";
}