package android.com.abb;

import android.graphics.Rect;
import android.view.KeyEvent;

import android.com.abb.Entity;


public class Avatar extends Entity {
  public int health;

  public Avatar() {
    super();
    sprite_source = new Rect(0, 0, kSpriteSize, kSpriteSize);
    health = kHealth;
    radius = kRadius;
  }

  public void Step(float time_step) {
    super.Step(time_step);

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
      if (Math.abs(dx) > 30.0f) {
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
  }

  public void SetKeyState(int key_code, int state) {
    if (key_code == kKeyLeft)
      ddx = -kGroundAcceleration * state;
    if (key_code == kKeyRight)
      ddx = +kGroundAcceleration * state;
    if (key_code == kKeyJump && state == 1 && has_ground_contact)
      dy -= kJumpVelocity;
  }

  private void SetSprite(int index) {
    sprite_source.top = kSpriteSize * index;
    sprite_source.bottom = kSpriteSize * index + kSpriteSize;
  }

  private int sprite_offset_ = 0;
  private float animation_phase_ = 0.0f;
  private static final float kJumpVelocity = 250.0f;
  private static final float kGroundAcceleration = 300.0f;
  private static final float kAirAcceleration = 10.0f;
  private static final float kGroundAnimationSpeed = 1.0f / 700.0f;
  private static final float kAirAnimationSpeed = 5.0f;
  private static final int kHealth = 1;
  private static final float kRadius = 32.0f;
  private static final int kSpriteSize = 64;

  private static final int kKeyLeft = KeyEvent.KEYCODE_A;
  private static final int kKeyRight = KeyEvent.KEYCODE_S;
  private static final int kKeyJump = KeyEvent.KEYCODE_J;
}
