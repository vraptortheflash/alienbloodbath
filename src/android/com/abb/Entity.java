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
import android.graphics.Rect;
import android.graphics.RectF;
import java.lang.CloneNotSupportedException;
import java.lang.Math;
import java.util.Random;


/** The Entity class is intended to be lowest level, drawable, physical in-game
 * object. */
public class Entity implements Cloneable {
  public boolean alive = true;  // Should not be deleted from the game.
  public boolean has_ground_contact;
  public float radius;
  public int sprite_image;
  public Rect sprite_rect;
  public boolean sprite_flipped_horizontal;
  public boolean sprite_flipped_vertical;

  public float x;    // Position.
  public float y;
  public float dx;   // Velocity.
  public float dy;
  public float ddx;  // Acceleration.
  public float ddy;

  public void stop() {
    dx = dy = ddx = ddy = 0.0f;
  }

  public void step(float time_step) {
    x += 0.5f * ddx * time_step * time_step + dx * time_step;
    y += 0.5f * ddy * time_step * time_step + dy * time_step;
    dx += ddx * time_step;
    dy += ddy * time_step;

    dx = Math.max(dx, -kMaxVelocity);
    dx = Math.min(dx,  kMaxVelocity);
    dy = Math.max(dy, -2.0f * kMaxVelocity);
    dy = Math.min(dy,  2.0f * kMaxVelocity);
  }

  /** Draw the entity to the canvas such that the specified coordinates are
   * centered. */
  public void draw(Graphics graphics, float center_x, float center_y,
                   float zoom) {
    if (sprite_image != -1) {
      int canvas_width = graphics.getWidth();
      int canvas_height = graphics.getHeight();

      mRectF.left = (x - center_x) * zoom +
          (canvas_width - sprite_rect.width() * zoom) / 2.0f;
      mRectF.top = (y - center_y) * zoom +
          (canvas_height - sprite_rect.height() * zoom) / 2.0f;
      mRectF.right = mRectF.left + sprite_rect.width() * zoom;
      mRectF.bottom = mRectF.top + sprite_rect.height() * zoom;

      graphics.drawImage(
          sprite_image, sprite_rect, mRectF,
          sprite_flipped_horizontal, sprite_flipped_vertical);
    }
  }

  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException ex) {
      throw new InternalError(ex.toString());
    }
  }

  // The following allocations are made here to avoid allocating anything during
  // the game. They are intended to be used by this an any child classe.
  protected static Random mRandom = new Random();
  protected static RectF mRectF = new RectF();

  private static final float kMaxVelocity = 200.0f;
}
