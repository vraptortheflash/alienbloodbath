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
import android.view.KeyEvent;
import java.lang.Math;

import android.com.abb.Entity;


public class Enemy extends Entity {
  public Enemy(Entity target) {
    super();
    target_ = target;
    radius = kRadius;
    sprite_source =
        new Rect(0, kSpriteBase, kSpriteWidth, kSpriteBase + kSpriteHeight);
  }

  public void Step(float time_step) {
    super.Step(time_step);
    ddy = kGravity;

    // If we have moved close enough to our target, mark it dead.
    if (Math.abs(target_.x - x) < kRadius &&
        Math.abs(target_.y - y) < kRadius) {
      target_.alive = false;
    }

    // Always move the enemy towards the target. Set the acceleration and sprite
    // to reflect it.
    int sprite_offset;
    if (target_.x < x) {
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

    sprite_source.top = kSpriteBase + kSpriteHeight * sprite_offset;
    sprite_source.bottom = kSpriteBase + kSpriteHeight * (sprite_offset + 1);
  }

  private Entity target_;

  private static final float kAcceleration = 40.0f;
  private static final float kGravity = 100.0f;
  private static final float kJumpVelocity = 100.0f;
  private static final float kRadius = 25.0f;
  private static final int kSpriteBase = 0;
  private static final int kSpriteWidth = 64;
  private static final int kSpriteHeight = 64;
}