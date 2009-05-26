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


public class Avatar extends AnimatedEntity {
  public Avatar(GameState game_state) {
    super();
    mGameState = game_state;
    mGrappling = new Grappling(game_state);

    //setDrawingScale(kDrawingScale);
    radius = kRadius;  // Collision radius.
  }

  @Override
  public void step(float time_step) {
    if (life <= 0.0f) {
      return;
    }

    // General motion updates.
    ddy = kGravity;
    super.step(time_step);
    dx = Math.min(Math.max(dx, -kMaxHorizontalVelocity), kMaxHorizontalVelocity);
    dy = Math.min(Math.max(dy, -kMaxVerticalVelocity),  kMaxVerticalVelocity);
    if (dy > 0.0f) {
      mJumping = false;
    }

    // Update the horizontal acceleration according to the current controls and
    // the contact with the ground.
    if (ddx > 0.0f && has_ground_contact) {
      ddx = +kGroundAcceleration;
    } else if (ddx > 0.0f && !has_ground_contact) {
      ddx = +kAirAcceleration;
    } else if (ddx < 0.0f && has_ground_contact) {
      ddx = -kGroundAcceleration;
    } else if (ddx < 0.0f && !has_ground_contact) {
      ddx = -kAirAcceleration;
    }

    // The following is a poor hack to simulate "friction" against the ground
    // surface. The problem with this implementation is that it does not account
    // for the time_step. TODO: Fix this friction implementation.
    if (has_ground_contact) {
      if (Math.abs(dx) > kMaxGroundVelocity) {
        dx *= 0.9f;
      }
      if (ddx == 0.0f) {
        dx *= (1.0f - kGroundKineticFriction);
      }
    }

    // Update the avatar animation.
    if (dx < 0.0f) {
      sprite_flipped_horizontal = true;
    } else if (dx > 0.0f) {
      sprite_flipped_horizontal = false;
    }
    stepAnimation(time_step);
    if (has_ground_contact) {
      if (Math.abs(dx) > kAnimationStopThreshold) {
        setAnimation("running");
      } else {
        setAnimation("standing");
      }
    } else {
      setAnimation("jumping");
    }

    // Update the equipped weapon instance.
    if (mWeapon != null) {
      mWeapon.x = x;
      mWeapon.y = y;
      mWeapon.dx = dx;
      mWeapon.dy = dy;
      mWeapon.has_ground_contact = has_ground_contact;
      mWeapon.sprite_flipped_horizontal = sprite_flipped_horizontal;
      mWeapon.setTarget(mTargetX, mTargetY);
      mWeapon.step(time_step);
    }

    // Update the grappling hook.
    if (mGrapplingTimer > 0.0f) {
      mGrappling.step(time_step);
      x = mGrappling.getBottomLink().x;
      y = mGrappling.getBottomLink().y;
      dx = mGrappling.getBottomLink().dx;
      dy = mGrappling.getBottomLink().dy;
    }
  }

  public void drawHud(Graphics graphics) {
    // Draw the avatar life and ammo meters.
    float meter_width = mCanvasWidth - 45;
    mRect.set(0, 0, 31, 17);
    mRectF.set(0, 0, 31, 17);
    graphics.drawImage(mGameState.misc_sprites, mRect, mRectF, false, false, 1);
    if (life > 0.0f) {
      float life_meter_width = meter_width * life;
      mRect.set(0, 22, 64, 26);
      mRectF.set(32, 2, 32 + life_meter_width, 6);
      graphics.drawImage(mGameState.misc_sprites, mRect, mRectF, false, false, 1);
    }
    if (mWeapon != null) {
      float ammo_meter_width =
          Math.min(meter_width,
                   meter_width * mWeapon.getAmmo() / mWeapon.getMaxAmmo());
      ammo_meter_width = Math.max(ammo_meter_width, 0.0f);
      mRect.set(0, 29, 64, 33);
      mRectF.set(32, 8, 32 + ammo_meter_width, 12);
      graphics.drawImage(mGameState.misc_sprites, mRect, mRectF, false, false, 1);
    }

    // Draw the touch screen indicators.
    mRect.set(40, 4, 58, 12);
    mRectF.set(0, 0, 12, 8);
    mRectF.offset(mCanvasWidth / 3 - 6, mCanvasHeight - kTouchMovementHeight);
    graphics.drawImage(mGameState.misc_sprites, mRect, mRectF, false, false, 1);
    mRectF.set(0, 0, 12, 8);
    mRectF.offset(2 * mCanvasWidth / 3 - 6, mCanvasHeight - kTouchMovementHeight);
    graphics.drawImage(mGameState.misc_sprites, mRect, mRectF, false, false, 1);
  }

  @Override
  public void draw(Graphics graphics, float center_x, float center_y,
                   float zoom) {
    if (life <= 0.0f) {
      return;
    }

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
      /*
      super.getPartTransformation("farm_l").getValues(mArray9);
      float hand_lx = mArray9[2];
      float hand_ly = mArray9[5];
      super.getPartTransformation("farm_r").getValues(mArray9);
      float hand_rx = mArray9[2];
      float hand_ry = mArray9[5];
      */
      float hand_lx = mCanvasWidth / 2 - 5;
      float hand_ly = mCanvasHeight / 2;
      float hand_rx = mCanvasWidth / 2 + 5;
      float hand_ry = mCanvasHeight / 2;
      mWeapon.draw(graphics, center_x, center_y, zoom,
                   hand_lx, hand_ly, hand_rx, hand_ry);
    }

    if (mGrapplingTimer > 0.0f) {
      mGrappling.draw(graphics, center_x, center_y, zoom);
    }
  }

  public void setKeyState(int key_code, int state) {
    if (life <= 0.0f) {
      return;
    }

    // Horizontal movement.
    if (key_code == kKeyLeft) {
      ddx = -kGroundAcceleration * state;
    }
    if (key_code == kKeyRight) {
      ddx = +kGroundAcceleration * state;
    }

    // Vertical movement. The following is tricky since plausible physics
    // simulations are not preferred by users, yet we still want to let users
    // interact with physical objects in the game.
    if (key_code == kKeyJump && state == 1 && has_ground_contact) {
      mGameState.playSound(kSoundJump);
      dy -= kJumpVelocity;
      has_ground_contact = false;
      mJumping = true;
    }
    // The following body of code handles the jump release mechanism for jumping
    // short heights. It has been disabled for the touch controls since multi-
    // touch on the phone is not available.
    // if (key_code == kKeyJump && state == 0 && mJumping && dy < 0.0f) {
    //   dy = Math.min(0.0f, dy + kJumpVelocity);
    //   mJumping = false;
    // }

    // Shooting.
    if (key_code == kKeyShoot1 || key_code == kKeyShoot2) {
      if (mWeapon != null) {
        mWeapon.enableShooting(state == 1);
      }
    }

    // Grappling.
    if (key_code == kKeyGrappling && state == 1) {
      if (mGrapplingTimer <= 0.0f) {
        mGrappling.setGrapple(x + 84, y - 84, x, y);
        mGrappling.getBottomLink().dx = dx;
        mGrappling.getBottomLink().dy = dy;
        mGrapplingTimer = kGrapplingTime;
      } else {
        mGrapplingTimer = 0.0f;
      }
    }
  }

  public void onMotionEvent(MotionEvent motion_event) {
    if (life <= 0.0f) {
      return;
    }

    // We translate motion events into key events and then pass it onto the key
    // event handler. Note: Pressure and size measurements are also available
    // from the API, but aren't yet used here.
    int action = motion_event.getAction();

    if (action == MotionEvent.ACTION_UP) {
      setKeyState(kKeyLeft, 0);
      setKeyState(kKeyRight, 0);
      setKeyState(kKeyJump, 0);
      setKeyState(kKeyShoot1, 0);
      return;
    }

    if (action == MotionEvent.ACTION_DOWN ||
        action == MotionEvent.ACTION_MOVE) {
      // The touch event was in the movement section of the display surface.
      if (motion_event.getY() > mCanvasHeight - kTouchMovementHeight) {
        int third_width = mCanvasWidth / 3;
        if (motion_event.getX() < third_width) {
          setKeyState(kKeyShoot1, 0);
          setKeyState(kKeyJump, 0);
          setKeyState(kKeyRight, 0);
          setKeyState(kKeyLeft, 1);
        } else if (motion_event.getX() < 2 * third_width) {
          setKeyState(kKeyShoot1, 0);
          setKeyState(kKeyRight, 0);
          setKeyState(kKeyLeft, 0);
          setKeyState(kKeyJump, 1);
        } else {
          setKeyState(kKeyShoot1, 0);
          setKeyState(kKeyLeft, 0);
          setKeyState(kKeyJump, 0);
          setKeyState(kKeyRight, 1);
        }
        return;
      }

      // The touch event was in the action section. (Any area outside of the
      // movement section of the display surface.)
      mTargetX = motion_event.getX() - mCanvasWidth / 2;
      mTargetY = motion_event.getY() - mCanvasHeight / 2;
      setKeyState(kKeyShoot1, 1);
    }
    return;
  }

  void setWeapon(Weapon weapon) {
    mWeapon = weapon;
  }

  void releaseWeapon() {
    mWeapon = null;
  }

  private int       mCanvasWidth;
  private int       mCanvasHeight;
  private GameState mGameState;
  private Grappling mGrappling;
  private float     mGrapplingTimer;
  private boolean   mJumping;
  private float     mTargetX;
  private float     mTargetY;
  public Weapon     mWeapon;

  // To avoid allocations, the following are used.
  private float[] mArray9 = new float[9];
  private Rect    mRect   = new Rect();
  private RectF   mRectF  = new RectF();

  private static final float kAirAcceleration        = 2000.0f;
  private static final float kAnimationStopThreshold = 40.0f;
  private static final float kDrawingScale           = 0.4f;
  private static final float kGrapplingTime          = 1.0f;
  private static final float kGravity                = 300.0f;
  private static final float kGroundAcceleration     = 2000.0f;
  private static final float kGroundAnimationSpeed   = 1.0f / 1500.0f;
  private static final float kGroundKineticFriction  = 0.3f;
  private static final float kMaxGroundVelocity      = 200.0f;
  private static final float kMaxHorizontalVelocity  = 300.0f;
  private static final float kMaxVerticalVelocity    = 300.0f;
  private static final float kJumpVelocity           = 295.0f;
  private static final int   kKeyLeft                = KeyEvent.KEYCODE_A;
  private static final int   kKeyRight               = KeyEvent.KEYCODE_S;
  private static final int   kKeyJump                = KeyEvent.KEYCODE_K;
  private static final int   kKeyGrappling           = KeyEvent.KEYCODE_I;
  private static final int   kKeyShoot1              = KeyEvent.KEYCODE_J;
  private static final int   kKeyShoot2              = KeyEvent.KEYCODE_L;
  private static final float kRadius                 = 22.0f;
  private static final Uri   kSoundJump              = Uri.parse("file:///android_asset/avatar_jump.mp3");
  private static final int   kTouchMovementHeight    = 45;
  private static final float kVelocityBoost          = 40.0f;
}
