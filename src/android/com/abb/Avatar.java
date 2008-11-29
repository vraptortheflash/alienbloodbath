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
    if (dx < 0)
      sprite_offset_ = 9;
    else if (dx > 0)
      sprite_offset_ = 0;

    int sprite_index = 0;
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

    // Update the shooting mechanism. The choices for shot direction are
    // specialized for each animation case: in the air, facing left, right, and
    // considering the avatar's speed.
    shot_delay_ -= time_step;
    if (shooting_ && shot_delay_ < time_step) {
      shot_delay_ = kShotDelay;
      float shot_angle;
      float shot_distance = kShotDistance;
      float shot_velocity = kShotVelocity;
      float x_offset;
      float y_offset;
      boolean facing_left = (sprite_offset_ == 9);

      if (!has_ground_contact) {
        shot_angle = shot_phase_;
        if (facing_left)
          shot_angle = -shot_phase_;
        shot_delay_ -= 2.0f * time_step;
        shot_phase_ += 45.0f * (float)Math.PI / 180.0f;
        shot_velocity *= 0.6f;
        x_offset = kShotDistance * (float)Math.cos(shot_angle);
        y_offset = kShotDistance * (float)Math.sin(shot_angle);
      } else if (facing_left) {
        shot_angle = kShotSpread * (float)Math.sin(shot_phase_) + (float)Math.PI;
        shot_phase_ += 10.0f;
        x_offset = -kShotOffsetX;
        y_offset = kShotOffsetY;
        if (Math.abs(dx) > kAnimationStopThreshold)
          y_offset += kShotDistance / 2.0f * (float)Math.sin(shot_phase_);
      } else {
        shot_angle = kShotSpread * (float)Math.sin(shot_phase_);
        shot_phase_ += 10.0f;
        x_offset = kShotOffsetX;
        y_offset = kShotOffsetY;
        if (Math.abs(dx) > kAnimationStopThreshold)
          y_offset += kShotDistance / 2.0f * (float)Math.sin(shot_phase_);
      }

      float dx_offset = shot_velocity * (float)Math.cos(shot_angle);
      float dy_offset = shot_velocity * (float)Math.sin(shot_angle);
      game_state_.CreateFireProjectile(
          x + x_offset, y + y_offset, dx + dx_offset, dy + dy_offset);
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

  private float animation_phase_ = 0.0f;
  private GameState game_state_;
  private boolean shooting_;
  private float shot_delay_;
  private float shot_phase_;
  private int sprite_offset_ = 0;

  private static final float kAirAcceleration = 40.0f;
  private static final float kAirAnimationSpeed = 3.0f;
  private static final float kAnimationStopThreshold = 40.0f;
  private static final float kGravity = 200.0f;
  private static final float kGroundAcceleration = 700.0f;
  private static final float kGroundAnimationSpeed = 1.0f / 600.0f;
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
}
