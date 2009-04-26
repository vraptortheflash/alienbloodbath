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
import android.graphics.Rect;
import android.graphics.RectF;
import java.lang.CloneNotSupportedException;
import java.lang.Math;
import java.util.Random;
import java.util.Stack;


/** The Entity class is intended to be lowest level, drawable, physical in-game
 * object. For example weapon projectiles, blood particles, and the avatar are
 * represented by Entities. */
public class Entity implements Cloneable {
  public float   damage;
  public boolean has_ground_contact;
  public boolean is_flame;
  public float   life         = 1.0f;
  public float   radius;
  public int     sprite_image = -1;
  public Rect    sprite_rect  = new Rect();
  public boolean sprite_flipped_horizontal;
  public boolean sprite_flipped_vertical;

  public float x;    // Position.
  public float y;
  public float dx;   // Velocity.
  public float dy;
  public float ddx;  // Acceleration.
  public float ddy;

  public Entity reset() {
    damage                      = 0.0f;
    has_ground_contact          = false;
    is_flame                    = false;
    life                        = 1.0f;
    radius                      = 0.0f;
    sprite_image                = -1;
    sprite_flipped_horizontal   = false;
    sprite_flipped_vertical     = false;
    x = y = dx = dy = ddx = ddy = 0.0f;
    return this;
  }

  public void stop() {
    dx = dy = ddx = ddy = 0.0f;
  }

  public void step(float time_step) {
    x += 0.5f * ddx * time_step * time_step + dx * time_step;
    y += 0.5f * ddy * time_step * time_step + dy * time_step;
    dx += ddx * time_step;
    dy += ddy * time_step;

    dx = Math.min(Math.max(dx, -kMaxHorizontalVelocity), kMaxHorizontalVelocity);
    dy = Math.min(Math.max(dy, -kMaxVerticalVelocity),  kMaxVerticalVelocity);

    // Flames are a special case of the Entity class. For performance reasons it
    // is benefitial to share a free-list with normal projectiles, but the
    // sprite animation requires extra handling.
    if (is_flame) {
      int frame = (int)(kFlameFrames - life * kFlameFrameRate);
      sprite_rect.left = 0;
      sprite_rect.right = kFlameSpriteWidth;
      sprite_rect.top = kFlameSpriteBase + kFlameSpriteHeight * frame;
      sprite_rect.bottom = kFlameSpriteBase + kFlameSpriteHeight * (frame + 1);
    }
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

  public boolean collidesWith(Entity entity) {
    return (Math.abs(entity.x - x) < radius + entity.radius &&
            Math.abs(entity.y - y) < radius + entity.radius);
  }

  static public Entity obtain() {
    if (!mFreeList.empty()) {
      return mFreeList.pop().reset();
    } else {
      return new Entity();
    }
  }

  public void release() {
    mFreeList.push(this);
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
  // the game. They are intended to be used by this an any child class.
  protected static Random mRandom = new Random();
  protected static RectF  mRectF  = new RectF();

  static private Stack<Entity> mFreeList = new Stack<Entity>();

  private static final int   kFlameFrames           = 13;
  private static final float kFlameFrameRate        = 10.0f;  // Frames / sec.
  private static final int   kFlameSpriteBase       = 521;
  private static final int   kFlameSpriteWidth      = 64;
  private static final int   kFlameSpriteHeight     = 36;
  private static final float kMaxHorizontalVelocity = 500.0f;
  private static final float kMaxVerticalVelocity   = 500.0f;
}
