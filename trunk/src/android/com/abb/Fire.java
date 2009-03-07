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


public class Fire extends Projectile {
  public Fire() {
    super();
    radius = kRadius;
    sprite_rect =
        new Rect(0, kSpriteBase, kSpriteWidth, kSpriteBase + kSpriteHeight);
    sprite_flipped_horizontal = mRandom.nextBoolean();
  }

  public void step(float time_step) {
    super.step(time_step);

    // Update the sprite to reflect the age / life of the fire entity.
    mFrame += time_step * kFrameRate;
    int rounded_frame = (int)mFrame;
    if (rounded_frame <= kFrames) {
      sprite_rect.top = kSpriteBase + kSpriteHeight * rounded_frame;
      sprite_rect.bottom = kSpriteBase + kSpriteHeight * (rounded_frame + 1);
    } else {
      life = 0.0f;  // Signal for deletion.
    }
  }

  private float mFrame = 0.0f;

  private static final int kFrames = 13;
  private static final float kFrameRate = 10.0f;  // Frames / sec.
  private static final float kRadius = 3.0f;
  private static final int kSpriteBase = 521;
  private static final int kSpriteWidth = 64;
  private static final int kSpriteHeight = 36;
}