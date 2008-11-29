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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import android.com.abb.Avatar;
import android.com.abb.Blood;
import android.com.abb.Enemy;
import android.com.abb.Fire;
import android.com.abb.Game;
import android.com.abb.Map;


public class GameState implements Game {
  public Avatar avatar = new Avatar(this);
  public ArrayList<Entity> enemies = new ArrayList<Entity>();
  public Bitmap enemy_sprites;
  public Uri map_uri = Uri.parse("builtin://0");
  public Bitmap misc_sprites;
  public ArrayList<Entity> particles = new ArrayList<Entity>();
  public ArrayList<Entity> projectiles = new ArrayList<Entity>();

  public GameState(Context context) {
    // Load data resources.
    Resources resources = context.getResources();
    avatar.sprite = BitmapFactory.decodeResource(resources, R.drawable.avatar);
    enemy_sprites = BitmapFactory.decodeResource(resources, R.drawable.enemy_0);
    misc_sprites = BitmapFactory.decodeResource(resources, R.drawable.misc);
    map_.SetResources(resources);  // Let the map load resources on-demand.
    Reset();

    // Load services.
    vibrator_ = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
  }

  /** Initialize the game state structure. Upon returning, game_state_ should be
   * in a state representing a new game life. */
  public void Reset() {
    map_.LoadFromUri(map_uri);
    particles.clear();
    projectiles.clear();
    enemies.clear();
    avatar.Stop();
    avatar.alive = true;
    avatar.x = map_.starting_x;
    avatar.y = map_.starting_y;
    view_x_ = target_view_x_ = avatar.x;
    view_y_ = target_view_y_ = avatar.y;
    death_timer_ = kDeathTimer;
  }

  public boolean OnKeyDown(int key_code) {
    avatar.SetKeyState(key_code, 1);
    return false;  // False to indicate not handled.
  }

  public boolean OnKeyUp(int key_code) {
    avatar.SetKeyState(key_code, 0);
    return false;  // False to indicate not handled.
  }

  public boolean Update(float time_step, Canvas canvas) {
    StepGame(time_step);
    DrawGame(canvas);
    return true;  // True to keep updating.
  }

  /** Run the game simulation for the specified amount of seconds. */
  protected void StepGame(float time_step) {
    // Update the view parameters.
    if (!avatar.has_ground_contact)
      target_zoom_ = kAirZoom;
    else
      target_zoom_ = kGroundZoom;
    target_view_x_ = avatar.x + kViewLead * avatar.dx;
    target_view_y_ = avatar.y + kViewLead * avatar.dy;

    zoom_ += (target_zoom_ - zoom_) * kZoomSpeed;
    view_x_ += (target_view_x_ - view_x_) * kViewSpeed;
    view_y_ += (target_view_y_ - view_y_) * kViewSpeed;

    // Step the avatar.
    if (avatar.alive) {
      avatar.Step(time_step);
      map_.CollideEntity(avatar);
    } else {
      if (death_timer_ == kDeathTimer) {
        for (int n = 0; n < 2 * kBloodBathSize; n++) {
          CreateBloodParticle(
              avatar.x, avatar.y,
              2.0f * kBloodBathVelocity * (0.5f - random_.nextFloat()) + avatar.dx, 
              2.0f * kBloodBathVelocity * (0.5f - random_.nextFloat()) + avatar.dy);
        }
      }
      death_timer_ -= time_step;
      if (death_timer_ < 0)
        Reset();
    }

    // Step the enemies.
    for (Iterator it = enemies.iterator(); it.hasNext();) {
      Enemy enemy = (Enemy)it.next();
      enemy.Step(time_step);
      map_.CollideEntity(enemy);
      if (!enemy.alive) {
        Vibrate();
        for (int n = 0; n < kBloodBathSize; n++) {
          CreateBloodParticle(
              enemy.x, enemy.y,
              kBloodBathVelocity * (0.5f - random_.nextFloat()) + enemy.dx,
              kBloodBathVelocity * (0.5f - random_.nextFloat()) + enemy.dy);
        }
        it.remove();
      }
    }

    // Step the projectiles and collide them against the enemies.
    for (Iterator it = projectiles.iterator(); it.hasNext();) {
      Fire projectile = (Fire)it.next();
      projectile.Step(time_step);
      for (Iterator enemy_it = enemies.iterator(); enemy_it.hasNext();)
        projectile.CollideEntity((Entity)enemy_it.next());
      if (!projectile.alive)
        it.remove();
    }

    // Step the particles.
    for (Iterator it = particles.iterator(); it.hasNext();) {
      Entity particle = (Entity)it.next();
      particle.Step(time_step);
      if (!particle.alive)
        it.remove();
    }
  }

  /** Draw the game state. The game map and entities are always drawn with the
   * avatar centered in the screen. */
  protected void DrawGame(Canvas canvas) {
    // Draw the map tiles.
    map_.Draw(canvas, view_x_, view_y_, zoom_);

    // Draw the enemies.
    for (Iterator it = enemies.iterator(); it.hasNext();)
      ((Entity)it.next()).Draw(canvas, view_x_, view_y_, zoom_);

    // Draw the avatar.
    if (avatar.alive)
      avatar.Draw(canvas, view_x_, view_y_, zoom_);

    // Draw the projectiles.
    for (Iterator it = projectiles.iterator(); it.hasNext();)
      ((Entity)it.next()).Draw(canvas, view_x_, view_y_, zoom_);

    // Draw the particles.
    for (Iterator it = particles.iterator(); it.hasNext();)
      ((Entity)it.next()).Draw(canvas, view_x_, view_y_, zoom_);
  }

  public Entity CreateEnemy(float x, float y) {
    Entity enemy = new Enemy(avatar);
    enemy.sprite = enemy_sprites;
    enemy.x = x;
    enemy.y = y;
    enemies.add(enemy);
    return enemy;
  }

  public Entity CreateBloodParticle(float x, float y, float dx, float dy) {
    Entity blood = new Blood();
    blood.sprite = misc_sprites;
    blood.x = x;
    blood.y = y;
    blood.dx = dx;
    blood.dy = dy;
    blood.ddy = kGravity;
    particles.add(blood);
    return blood;
  }

  public Entity CreateFireProjectile(float x, float y, float dx, float dy) {
    Entity fire = new Fire();
    fire.sprite = misc_sprites;
    fire.x = x;
    fire.y = y;
    fire.dx = dx;
    fire.dy = dy;
    fire.ddy = -50.0f;  // Slight up draft.
    projectiles.add(fire);
    return fire;
  }

  public void Vibrate() {
    vibrator_.vibrate(kVibrateLength);
  }

  public void LoadStateBundle(Bundle saved_instance_state) {
    map_uri = Uri.parse(saved_instance_state.getString("map_uri"));
    Reset();

    target_view_x_ = saved_instance_state.getFloat("target_view_x_");
    target_view_y_ = saved_instance_state.getFloat("target_view_y_");
    view_x_ = saved_instance_state.getFloat("view_x_");
    view_y_ = saved_instance_state.getFloat("view_y_");
    zoom_ = saved_instance_state.getFloat("zoom_");

    avatar.x = saved_instance_state.getFloat("avatar.x");
    avatar.y = saved_instance_state.getFloat("avatar.y");
    avatar.dx = saved_instance_state.getFloat("avatar.dx");
    avatar.dy = saved_instance_state.getFloat("avatar.dy");
    avatar.ddx = saved_instance_state.getFloat("avatar.ddx");
    avatar.ddy = saved_instance_state.getFloat("avatar.ddy");
  }

  public Bundle SaveStateBundle() {
    // Note that particles, projectiles and spawned aliens are lost through
    // serialization.
    Bundle saved_instance_state = new Bundle();

    saved_instance_state.putString("map_uri", map_uri.toString());
    saved_instance_state.putFloat("target_view_x_", target_view_x_);
    saved_instance_state.putFloat("target_view_y_", target_view_y_);
    saved_instance_state.putFloat("view_x_", view_x_);
    saved_instance_state.putFloat("view_y_", view_y_);
    saved_instance_state.putFloat("zoom_", zoom_);

    saved_instance_state.putFloat("avatar.x", avatar.x);
    saved_instance_state.putFloat("avatar.y", avatar.y);
    saved_instance_state.putFloat("avatar.dx", avatar.dx);
    saved_instance_state.putFloat("avatar.dy", avatar.dy);
    saved_instance_state.putFloat("avatar.dx", avatar.ddx);
    saved_instance_state.putFloat("avatar.dy", avatar.ddy);
    return saved_instance_state;
  }

  private float death_timer_ = kDeathTimer;
  private Map map_ = new Map(this);
  private Random random_ = new Random();
  private float target_view_x_ = 0.0f;
  private float target_view_y_ = 0.0f;
  private float target_zoom_ = kGroundZoom;
  private Vibrator vibrator_;
  private float view_x_ = 0.0f;
  private float view_y_ = 0.0f;
  private float zoom_ = kGroundZoom;

  private static final float kAirZoom = 0.6f;
  private static final int kBloodBathSize = 8;  // Number of particles.
  private static final float kBloodBathVelocity = 70.0f;
  private static final float kDeathTimer = 4.0f;
  private static final float kGravity = 200.0f;
  private static final float kGroundZoom = 0.8f;
  private static final long kVibrateLength = 30;  // Milliseconds.
  private static final float kViewLead = 1.0f;
  private static final float kViewSpeed = 0.1f;
  private static final float kZoomSpeed = 0.05f;
}
