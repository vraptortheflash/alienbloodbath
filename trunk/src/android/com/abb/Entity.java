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

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import java.lang.Math;
import java.util.Random;


public class Entity {
  public boolean alive = true;  // Should not be deleted from the game.
  public boolean has_ground_contact;
  public float radius;
  public int sprite_image;
  public Rect sprite_rect;
  public boolean sprite_flipped_horizontal;

  public float x;    // Position.
  public float y;
  public float dx;   // Velocity.
  public float dy;
  public float ddx;  // Acceleration.
  public float ddy;

  public void Stop() {
    dx = dy = ddx = ddy = 0.0f;
  }

  public void Step(float time_step) {
    x += 0.5f * ddx * time_step * time_step + dx * time_step;
    y += 0.5f * ddy * time_step * time_step + dy * time_step;
    dx += ddx * time_step;
    dy += ddy * time_step;

    // The following is a poor hack to simulate "friction" against the ground
    // surface. The problem with this implementation is that it does not account
    // for the time_step. TODO(burkhart): Fix this implementation.
    if (has_ground_contact) {
      dx *= (1.0f - kGroundFriction);
    }

    dx = Math.max(dx, -kMaxVelocity);
    dx = Math.min(dx,  kMaxVelocity);
    dy = Math.max(dy, -2.0f * kMaxVelocity);
    dy = Math.min(dy,  2.0f * kMaxVelocity);
  }

  /** Draw the entity to the canvas such that the specified coordinates are
   * centered. */
  public void Draw(Graphics graphics, float center_x, float center_y, float zoom) {
    if (sprite_image != -1) {
      int canvas_width = graphics.GetWidth();
      int canvas_height = graphics.GetHeight();

      RectF sprite_destination =
          new RectF(0, 0,
                    sprite_rect.width() * zoom,
                    sprite_rect.height() * zoom);
      sprite_destination.offset(
          (x - center_x) * zoom +
          (canvas_width - sprite_rect.width() * zoom) / 2.0f,
          (y - center_y) * zoom +
          (canvas_height - sprite_rect.height() * zoom) / 2.0f);
      graphics.DrawImage(sprite_image, sprite_rect, sprite_destination,
                         sprite_flipped_horizontal);
    }
  }

  protected static Random random_ = new Random();

  private static final float kGroundFriction = 0.2f;
  private static final float kMaxVelocity = 200.0f;
}
