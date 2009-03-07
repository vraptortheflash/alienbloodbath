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

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.MotionEvent;


public class Avatar extends ArticulatedEntity {
  public Avatar(GameState game_state) {
    super();
    mGameState = game_state;

    setDrawingScale(kDrawingScale);
    radius = kRadius;  // Collision radius.
  }

  @Override
  public void step(float time_step) {
    ddy = kGravity;
    super.step(time_step);

    // Update the horizontal acceleration according to the current controls and
    // the contact with the ground.
    if (ddx > 0 && has_ground_contact) {
      ddx = +kGroundAcceleration;
    } else if (ddx > 0 && !has_ground_contact) {
      ddx = +kAirAcceleration;
    } else if (ddx < 0 && has_ground_contact) {
      ddx = -kGroundAcceleration;
    } else if (ddx < 0 && !has_ground_contact) {
      ddx = -kAirAcceleration;
    }

    // The following is a poor hack to simulate "friction" against the ground
    // surface. The problem with this implementation is that it does not account
    // for the time_step. TODO: Fix this friction implementation.
    if (has_ground_contact && ddx == 0.0f) {
      dx *= (1.0f - kGroundKineticFriction);
    }

    // Update the avatar animation.
    if (dx < 0) {
      sprite_flipped_horizontal = true;
    } else if (dx > 0) {
      sprite_flipped_horizontal = false;
    }
    if (has_ground_contact) {
      if (Math.abs(dx) > kAnimationStopThreshold) {
        loadAnimationFromUri("content:///run.humanoid.animation");
        stepAnimation(kGroundAnimationSpeed * Math.abs(dx));
      } else {
        loadAnimationFromUri("content:///stand.humanoid.animation");
        stepAnimation(time_step);
      }
    } else {
      loadAnimationFromUri("content:///jump.humanoid.animation");
      stepAnimation(time_step);
    }

    // Update the equipped weapon instance.
    if (mWeapon != null) {
      mWeapon.x = x;
      mWeapon.y = y;
      mWeapon.has_ground_contact = has_ground_contact;
      mWeapon.sprite_flipped_horizontal = sprite_flipped_horizontal;
      mWeapon.step(time_step);
    }
  }

  @Override
  public void draw(Graphics graphics, float center_x, float center_y,
                   float zoom) {
    // We intercept the draw method only to get the canvas dimensions. The
    // drawing buffer dimensions are used to interpret the touch events.
    mCanvasWidth = graphics.getWidth();
    mCanvasHeight = graphics.getHeight();

    // Draw the articulated entity.
    super.draw(graphics, center_x, center_y, zoom);

    // The weapon must be drawn after the avatar's articulated entity. In
    // addition to the fact that no z-buffer is used, the articulated entity's
    // draw method calculates the model's hand positions which we use to draw
    // the weapon on top of.
    if (mWeapon != null) {
      super.getPartTransformation("farm_l").getValues(mArray9);
      float hand_lx = mArray9[2];
      float hand_ly = mArray9[5];
      super.getPartTransformation("farm_r").getValues(mArray9);
      float hand_rx = mArray9[2];
      float hand_ry = mArray9[5];
      mWeapon.draw(graphics, center_x, center_y, zoom,
                   hand_lx, hand_ly, hand_rx, hand_ry);
    }

    // Draw the avatar life and ammo meters.
    float meter_width = mCanvasWidth / 2.0f;
    mRect.set(0, 0, 28, 13);
    mRectF.set(0, 0, 28, 13);
    graphics.drawImage(mGameState.misc_sprites, mRect, mRectF, false, false);
    if (life > 0.0f) {
      float life_meter_width =
          meter_width * life;
      mRect.set(0, 16, 64, 20);
      mRectF.set(30, 0, 30 + life_meter_width, 6);
      graphics.drawImage(mGameState.misc_sprites, mRect, mRectF, false, false);
    }
    if (mWeapon != null) {
      float ammo_meter_width =
          meter_width * mWeapon.getAmmo() / mWeapon.getMaxAmmo();
      ammo_meter_width = Math.max(ammo_meter_width, 0.0f);
      mRect.set(0, 23, 64, 27);
      mRectF.set(30, 8, 30 + ammo_meter_width, 14);
      graphics.drawImage(mGameState.misc_sprites, mRect, mRectF, false, false);
    }
  }

  public void setKeyState(int key_code, int state) {
    if (key_code == kKeyLeft) {
      ddx = -kGroundAcceleration * state;
    } else if (key_code == kKeyRight) {
      ddx = +kGroundAcceleration * state;
    } else if (key_code == kKeyJump && state == 1 && has_ground_contact) {
      dy -= kJumpVelocity;
      has_ground_contact = false;
    } else if (key_code == kKeyShoot) {
      if (mWeapon != null) {
        mWeapon.enableShooting(state == 1);
      }
    }
  }

  public void onMotionEvent(MotionEvent motion_event) {
    // We translate motion events into key events and then pass it onto the key
    // event handler. Note: Pressure and size measurements are also available
    // from the API, but aren't yet used here.
    int action = motion_event.getAction();
    if (action == MotionEvent.ACTION_DOWN ||
        action == MotionEvent.ACTION_MOVE) {
      // This case handled below.
    } else if (action == MotionEvent.ACTION_UP) {
      setKeyState(kKeyLeft, 0);
      setKeyState(kKeyRight, 0);
      setKeyState(kKeyJump, 0);
      setKeyState(kKeyShoot, 0);
      motion_event.recycle();
      return;
    } else {
      motion_event.recycle();
      return;
    }

    // The touch event was in the movement section of the display surface.
    if (motion_event.getY() > mCanvasHeight - kTouchMovementHeight) {
      setKeyState(kKeyJump, 0);
      setKeyState(kKeyShoot, 0);
      if (motion_event.getX() < mCanvasWidth / 2) {
        setKeyState(kKeyRight, 0);
        setKeyState(kKeyLeft, 1);
      } else {
        setKeyState(kKeyLeft, 0);
        setKeyState(kKeyRight, 1);
      }
    }
    // the touch event was in the action section. (Any area above the movement
    // section of the display surface.)
    else {
      setKeyState(kKeyLeft, 0);
      setKeyState(kKeyRight, 0);
      if (motion_event.getX() < mCanvasWidth / 2) {
        setKeyState(kKeyShoot, 0);
        setKeyState(kKeyJump, 1);
      } else {
        setKeyState(kKeyJump, 0);
        setKeyState(kKeyShoot, 1);
      }
    }
    motion_event.recycle();
  }

  void setWeapon(Weapon weapon) {
    mWeapon = weapon;
  }

  void releaseWeapon() {
    mWeapon = null;
  }

  private int mCanvasWidth;
  private int mCanvasHeight;
  private GameState mGameState;
  public Weapon mWeapon;

  // To avoid allocations:
  private float[] mArray9 = new float[9];
  private Rect mRect = new Rect();
  private RectF mRectF = new RectF();

  private static final float kAirAcceleration = 40.0f;
  private static final float kAnimationStopThreshold = 40.0f;
  private static final float kDrawingScale = 0.4f;
  private static final float kGravity = 300.0f;
  private static final float kGroundAcceleration = 700.0f;
  private static final float kGroundAnimationSpeed = 1.0f / 1500.0f;
  private static final float kGroundKineticFriction = 0.3f;
  private static final float kJumpVelocity = 275.0f;
  private static final int kKeyLeft = KeyEvent.KEYCODE_A;
  private static final int kKeyRight = KeyEvent.KEYCODE_S;
  private static final int kKeyJump = KeyEvent.KEYCODE_K;
  private static final int kKeyShoot = KeyEvent.KEYCODE_J;
  private static final float kRadius = 25.0f;
  private static final int kSpriteSize = 64;
  private static final int kTouchMovementHeight = 30;
}
