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


/** The Enemy class encapsulates a simple NPC instance, essentially an avatar
 * which is controlled by the computer. */
public class Enemy extends ArticulatedEntity {
  public Enemy(Entity target) {
    super();
    mTarget = target;
  }

  @Override
  public void step(float time_step) {
    ddy = mGravity;
    super.step(time_step);
    super.stepAnimation(time_step);

    // If we have moved close enough to our target, mark it dead.
    if (Math.abs(mTarget.x - x) < radius && Math.abs(mTarget.y - y) < radius) {
      mTarget.life -= mDamage;
      life -= mDamage;
    }

    // If the target has moved far enough away from this entity, destroy it.
    // This may happen if the client leaves an enemy behind on the map. We want
    // to release resources allocated to it.
    if (Math.abs(mTarget.x - x) > kRange || Math.abs(mTarget.y - y) > kRange) {
      life = 0.0f;
    }

    // Always move the enemy towards the target. Set the acceleration and sprite
    // to reflect it.
    int sprite_offset;
    if (mTarget.x < x) {
      sprite_flipped_horizontal = true;
      ddx = -mAcceleration;
    } else {
      sprite_flipped_horizontal = false;
      ddx = mAcceleration;
    }
    if (has_ground_contact) {
      dy = -mJumpVelocity;
    }
  }

  public void loadFromUri(Uri uri) {
    // The following map defines all of the accepted enemy parameters. The
    // parameters map is expected to populated with default values letting the
    // user override only a subset if desired within the text resource at the
    // specified uri.
    TreeMap<String, Object> parameters = new TreeMap<String, Object>();
    parameters.put(kParameterAcceleration, new Float(kDefaultAcceleration));
    parameters.put(kParameterDamage, new Float(kDefaultDamage));
    parameters.put(kParameterDrawingScale, new Float(kDefaultDrawingScale));
    parameters.put(kParameterAnimation, "none");
    parameters.put(kParameterEntity, "none");
    parameters.put(kParameterJumpVelocity, new Float(kDefaultJumpVelocity));
    parameters.put(kParameterGravity, new Float(kDefaultGravity));
    parameters.put(kParameterLife, new Float(kDefaultLife));
    parameters.put(kParameterRadius, new Float(kDefaultRadius));

    // Given a fully-specified default enemy parameters map, we can parse and
    // merge in the user defined values. Note that the following method rejects
    // all keys provided by the user which were not defined above.
    String file_path = Content.getTemporaryFilePath(uri);
    String[] tokens = Content.readFileTokens(file_path);
    Content.mergeKeyValueTokensWithMap(tokens, parameters);
    Content.assertStringNotNone(parameters, kParameterEntity);
    Content.assertStringNotNone(parameters, kParameterAnimation);

    // Now that the user defined enemy parameters have been parsed and merged,
    // we can initialize the enemy instance state accordingly.
    mAcceleration = ((Float)parameters.get(kParameterAcceleration)).floatValue();
    mDamage = ((Float)parameters.get(kParameterDamage)).floatValue();
    setDrawingScale(((Float)parameters.get(kParameterDrawingScale)).floatValue());
    mGravity = ((Float)parameters.get(kParameterGravity)).floatValue();
    life = ((Float)parameters.get(kParameterLife)).floatValue();
    radius = ((Float)parameters.get(kParameterRadius)).floatValue();
    String uri_string = uri.toString();
    String base_uri_string = uri_string.substring(0, uri_string.lastIndexOf("/"));
    String entity = (String)parameters.get(kParameterEntity);
    String animation = (String)parameters.get(kParameterAnimation);
    super.loadFromUri(Uri.parse(base_uri_string + "/" + entity));
    super.loadAnimationFromUri(base_uri_string + "/" + animation);
  }

  @Override
  public Object clone() {
    return super.clone();
  }

  private float mAcceleration;
  private float mDamage;
  private float mGravity;
  private float mJumpVelocity;
  private Entity mTarget;

  private static final float kDefaultAcceleration = 40.0f;
  private static final float kDefaultDamage = 0.34f;
  private static final float kDefaultDrawingScale = 1.0f;
  private static final float kDefaultJumpVelocity = 100.0f;
  private static final float kDefaultGravity = 100.0f;
  private static final float kDefaultLife = 1.0f;
  private static final float kDefaultRadius = 32.0f;
  private static final float kRange = 1000.0f;

  private static final String kParameterAcceleration = "acceleration";
  private static final String kParameterAnimation = "animation";
  private static final String kParameterDamage = "damage";
  private static final String kParameterDrawingScale = "drawing_scale";
  private static final String kParameterEntity = "entity";
  private static final String kParameterJumpVelocity = "jump_velocity";
  private static final String kParameterGravity = "gravity";
  private static final String kParameterLife = "life";
  private static final String kParameterRadius = "radius";
}