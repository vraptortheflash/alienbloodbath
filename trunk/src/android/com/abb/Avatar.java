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
import android.view.KeyEvent;
import android.view.MotionEvent;


public class Avatar extends ArticulatedEntity {
  public Avatar(GameState game_state) {
    super();
    mGameState = game_state;
    mWeapon = new Weapon(mGameState);

    setDrawingScale(kDrawingScale);
    radius = kRadius;
  }

  @Override
  public void step(float time_step) {
    ddy = kGravity;
    super.step(time_step);

    mWeapon.x = x;
    mWeapon.y = y;

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
    // for the time_step. TODO(burkhart): Fix this friction implementation.
    if (has_ground_contact && ddx == 0.0f) {
      dx *= (1.0f - kGroundKineticFriction);
    }

    // Update the avatar animation frame according the current entity motion.
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

    mWeapon.setSprite(sprite_flipped_horizontal);

    // Update the shooting mechanism. The choices for shot direction are
    // specialized for each animation case: in the air, facing left, right, and
    // considering the avatar's speed. TODO: Replace all of this with an
    // equivalent in Weapon.java.
    mShotDelay -= time_step;
    if (mShooting && mShotDelay < time_step) {
      mShotDelay = kShotDelay;
      float shot_angle;
      float shot_distance = kShotDistance;
      float shot_velocity = kShotVelocity;
      float x_offset;
      float y_offset;

      if (!has_ground_contact) {
        shot_angle = mShotPhase;
        if (sprite_flipped_horizontal) {
          shot_angle = -mShotPhase;
        }
        mShotDelay -= 2.0f * time_step;
        mShotPhase += 45.0f * (float)Math.PI / 180.0f;
        shot_velocity *= 0.6f;
        x_offset = kShotDistance * (float)Math.cos(shot_angle);
        y_offset = kShotDistance * (float)Math.sin(shot_angle);
      } else if (sprite_flipped_horizontal) {
        shot_angle = kShotSpread * (float)Math.sin(mShotPhase) + (float)Math.PI;
        mShotPhase += 10.0f;
        x_offset = -kShotOffsetX;
        y_offset = kShotOffsetY;
      } else {
        shot_angle = kShotSpread * (float)Math.sin(mShotPhase);
        mShotPhase += 10.0f;
        x_offset = kShotOffsetX;
        y_offset = kShotOffsetY;
      }

      float dx_offset = shot_velocity * (float)Math.cos(shot_angle);
      float dy_offset = shot_velocity * (float)Math.sin(shot_angle);
      mGameState.createFireProjectile(
          x + x_offset, y + y_offset, dx + dx_offset, dy + dy_offset);
    }
  }

  @Override
  public void draw(Graphics graphics, float center_x, float center_y,
                   float zoom) {
    // We intercept the draw method only to get the canvas dimensions. The
    // drawing buffer dimensions are used to interpret the touch events.
    mCanvasWidth = graphics.getWidth();
    mCanvasHeight = graphics.getHeight();

    super.draw(graphics, center_x, center_y, zoom);
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
      mShooting = (state == 1);
    }
  }

  public void onMotionEvent(MotionEvent motion_event) {
    // We translate motion events into key events and then pass it onto the key
    // event handler. Note: Pressure and size measurements are also available
    // from the API, but aren't yet used here.
    int action = motion_event.getAction();
    if (action == MotionEvent.ACTION_DOWN ||
        action == MotionEvent.ACTION_MOVE) {
      // Handled below.
    } else if (action == MotionEvent.ACTION_UP) {
      setKeyState(kKeyLeft, 0);
      setKeyState(kKeyRight, 0);
      setKeyState(kKeyJump, 0);
      setKeyState(kKeyShoot, 0);
      return;
    } else {
      return;
    }
    int x = (int)motion_event.getX();

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
  }

  private int mCanvasWidth;
  private int mCanvasHeight;
  private GameState mGameState;
  private float mShotDelay;
  private boolean mShooting;
  private float mShotPhase;
  public Weapon mWeapon;

  private static final float kAirAcceleration = 40.0f;
  private static final float kAnimationStopThreshold = 40.0f;
  private static final float kDrawingScale = 0.4f;
  private static final float kGravity = 300.0f;
  private static final float kGroundAcceleration = 700.0f;
  private static final float kGroundAnimationSpeed = 1.0f / 1500.0f;
  private static final float kGroundKineticFriction = 0.3f;
  private static final float kJumpVelocity = 250.0f;
  private static final int kKeyLeft = KeyEvent.KEYCODE_A;
  private static final int kKeyRight = KeyEvent.KEYCODE_S;
  private static final int kKeyJump = KeyEvent.KEYCODE_K;
  private static final int kKeyShoot = KeyEvent.KEYCODE_J;
  private static final float kRadius = 25.0f;
  private static final float kShotDelay = 0.2f;  // Seconds between shots.
  private static final float kShotDistance = 25.0f;
  private static final float kShotOffsetX = 23.0f;
  private static final float kShotOffsetY = -8.0f;
  private static final float kShotSpread = 15.0f * (float)Math.PI / 180.0f;
  private static final float kShotVelocity = 60.0f;
  private static final int kSpriteSize = 64;
  private static final int kTouchMovementHeight = 30;
}
