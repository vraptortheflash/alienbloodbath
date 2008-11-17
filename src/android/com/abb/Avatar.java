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

import android.com.abb.Entity;
import android.com.abb.GameState;


public class Avatar extends Entity {
  public Avatar(GameState game_state) {
    super();
    game_state_ = game_state;
    sprite_source = new Rect(0, 0, kSpriteSize, kSpriteSize);
    radius = kRadius;
  }

  public void Step(float time_step) {
    super.Step(time_step);
    ddy = kGravity;

    // Update the horizontal acceleration acceleration according to the current
    // controls and the contact with the ground.
    if (ddx > 0 && has_ground_contact)
      ddx = +kGroundAcceleration;
    if (ddx > 0 && !has_ground_contact)
      ddx = +kAirAcceleration;
    if (ddx < 0 && has_ground_contact)
      ddx = -kGroundAcceleration;
    if (ddx < 0 && !has_ground_contact)
      ddx = -kAirAcceleration;

    // Update the avatar animation frame according the current entity motion.
    int sprite_index = 0;
    if (dx < 0)
      sprite_offset_ = 9;
    else if (dx > 0)
      sprite_offset_ = 0;

    if (has_ground_contact) {
      if (Math.abs(dx) > kAnimationStopThreshold) {
        animation_phase_ += kGroundAnimationSpeed * Math.abs(dx);
        while (animation_phase_ > 1.0f)
          animation_phase_ -= 1.0f;
        sprite_index = 1 + (int)(3 * animation_phase_);
      }
    } else {  // In the air
      animation_phase_ += kAirAnimationSpeed * time_step;
      while (animation_phase_ > 1.0f)
        animation_phase_ -= 1.0f;
      sprite_index = 5 + (int)(4 * animation_phase_);
    }

    SetSprite(sprite_offset_ + sprite_index);

    // Update the shooting mechanism.
    shot_delay_ -= time_step;
    if (shooting_ && shot_delay_ < time_step) {
      shot_phase_ += 10.0f;  // Essentially randomized angle.

      float x_offset = kShotOffsetX;
      float y_offset = kShotOffsetY;
      float dx_offset = kShotVelocity;
      float dy_offset = kShotSpread * (float)Math.sin(shot_phase_);

      if (sprite_offset_ == 9) x_offset *= -1.0f;
      if (sprite_offset_ == 9) dx_offset *= -1.0f;
      game_state_.CreateFireProjectile(
          x + x_offset, y + y_offset, dx + dx_offset, dy + dy_offset);
      shot_delay_ = kShotDelay;
    }
  }

  public void SetKeyState(int key_code, int state) {
    if (key_code == kKeyLeft)
      ddx = -kGroundAcceleration * state;
    if (key_code == kKeyRight)
      ddx = +kGroundAcceleration * state;
    if (key_code == kKeyJump && state == 1 && has_ground_contact)
      dy -= kJumpVelocity;
    if (key_code == kKeyShoot)
      shooting_ = (state == 1);
  }

  private void SetSprite(int index) {
    sprite_source.top = kSpriteSize * index;
    sprite_source.bottom = kSpriteSize * index + kSpriteSize;
  }

  private GameState game_state_;
  private boolean shooting_;
  private float shot_delay_;
  private float shot_phase_;
  private float animation_phase_ = 0.0f;
  private int sprite_offset_ = 0;

  private static final float kAirAcceleration = 25.0f;
  private static final float kAirAnimationSpeed = 4.0f;
  private static final float kAnimationStopThreshold = 30.0f;
  private static final float kGravity = 200.0f;
  private static final float kGroundAcceleration = 300.0f;
  private static final float kGroundAnimationSpeed = 1.0f / 500.0f;
  private static final float kJumpVelocity = 250.0f;
  private static final float kRadius = 32.0f;
  private static final float kShotDelay = 0.2f;  // Seconds between shots.
  private static final int kShotOffsetX = 23;
  private static final int kShotOffsetY = -8;
  private static final float kShotSpread = 15.0f;
  private static final float kShotVelocity = 60.0f;
  private static final int kSpriteSize = 64;
  private static final int kKeyLeft = KeyEvent.KEYCODE_A;
  private static final int kKeyRight = KeyEvent.KEYCODE_S;
  private static final int kKeyJump = KeyEvent.KEYCODE_K;
  private static final int kKeyShoot = KeyEvent.KEYCODE_J;
}
