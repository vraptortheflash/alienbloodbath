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


public class Blood extends Entity {
  public Blood() {
    super();
    sprite_rect =
        new Rect(0, kSpriteBase, kSpriteWidth, kSpriteBase + kSpriteHeight);
    sprite_flipped_horizontal = random_.nextBoolean();
  }

  public void Step(float time_step) {
    super.Step(time_step);

    time_remaining -= time_step;
    if (time_remaining < 0) {
      alive = false;  // Signal for deletion.
    }
  }

  private float time_remaining = kTimeRemaining;

  private static final float kTimeRemaining = 0.5f;  // Seconds.
  private static final int kSpriteBase = 1 * 64;
  private static final int kSpriteWidth = 64;
  private static final int kSpriteHeight = 64;
}
