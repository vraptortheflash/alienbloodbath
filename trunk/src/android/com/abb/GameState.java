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
import android.os.Vibrator;
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
  public ArrayList enemies = new ArrayList();
  public Bitmap enemy_sprites;
  public Map map = new Map(this);
  public Bitmap misc_sprites;
  public ArrayList particles = new ArrayList();
  public ArrayList projectiles = new ArrayList();

  /** Initialize the game state structure. Upon returning, game_state_ should be
   * in a state representing a new game life. */
  public void Reset() {
    avatar.Stop();
    avatar.alive = true;
    avatar.x = map.starting_x;
    avatar.y = map.starting_y;
    view_x_ = target_view_x_ = avatar.x;
    view_y_ = target_view_y_ = avatar.y;
  }

  public void LoadResources(Context context) {
    // Load services.
    vibrator_ = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);

    // Load images.
    Resources resources = context.getResources();
    avatar.sprite = BitmapFactory.decodeResource(resources, R.drawable.avatar);
    enemy_sprites = BitmapFactory.decodeResource(resources, R.drawable.enemy_0);
    misc_sprites = BitmapFactory.decodeResource(resources, R.drawable.misc);

    // Load the maps. TODO: We'd ideally like to enumerate all of the maps
    // declared in the resources file and on disk. However, the following is a
    // bit of a hack.
    int[] tiles_ids = { R.drawable.tiles_0, R.drawable.tiles_1 };
    for (int tiles_id : tiles_ids)
      tiles_.add(BitmapFactory.decodeResource(resources, tiles_id));
    int[] level_ids = { R.string.level_0, R.string.level_1, R.string.level_2 };
    for (int level_id : level_ids) {
      char[] level_array = resources.getText(level_id).toString().toCharArray();
      levels_.add(Map.DecodeArray(level_array));
    }
    LoadLevel(current_level_);
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
    avatar.Step(time_step);
    map.CollideEntity(avatar);
    if (!avatar.alive)
      Reset();
    if (map.TileIsGoal(map.TileAt(avatar.x, avatar.y)))
      LoadLevel(++current_level_);

    // Step the enemies.
    for (Iterator it = enemies.iterator(); it.hasNext();) {
      Enemy enemy = (Enemy)it.next();
      enemy.Step(time_step);
      map.CollideEntity(enemy);
      if (!enemy.alive) {
        Vibrate();
        for (int n = 0; n < kBloodBathSize; n++) {
          float random_angle = random_.nextFloat() * 2.0f * (float)Math.PI;
          float random_magnitude = kBloodBathVelocity * random_.nextFloat() / 3.0f;
          CreateBloodParticle(
              enemy.x, enemy.y,
              enemy.dx + random_magnitude * (float)Math.cos(random_angle),
              enemy.dy + random_magnitude * (float)Math.sin(random_angle));
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
    char background[] = backgrounds_[current_level_ % backgrounds_.length];
    canvas.drawRGB(background[0], background[1], background[2]);

    // Draw the map tiles.
    map.Draw(canvas, view_x_, view_y_, zoom_);

    // Draw the enemies.
    for (Iterator it = enemies.iterator(); it.hasNext();)
      ((Entity)it.next()).Draw(canvas, view_x_, view_y_, zoom_);

    // Draw the avatar.
    avatar.Draw(canvas, view_x_, view_y_, zoom_);

    // Draw the projectiles.
    for (Iterator it = projectiles.iterator(); it.hasNext();)
      ((Entity)it.next()).Draw(canvas, view_x_, view_y_, zoom_);

    // Draw the particles.
    for (Iterator it = particles.iterator(); it.hasNext();)
      ((Entity)it.next()).Draw(canvas, view_x_, view_y_, zoom_);
  }

  public void LoadLevel(int level) {
    level = level % levels_.size();
    map.LoadFromArray((char[])levels_.get(level));
    int tiles = level % tiles_.size();
    map.tiles_bitmap = (Bitmap)tiles_.get(tiles);
    enemies.clear();
    Reset();
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
    projectiles.add(fire);
    return fire;
  }

  public void Vibrate() {
    vibrator_.vibrate(kVibrateLength);
  }

  private char[][] backgrounds_ = {{5, 5, 5}, {50, 50, 50}, {10, 5, 5}};
  private int current_level_ = 0;
  private ArrayList levels_ = new ArrayList();
  private Random random_ = new Random();
  private float target_view_x_ = 0.0f;
  private float target_view_y_ = 0.0f;
  private float target_zoom_ = kGroundZoom;
  private ArrayList tiles_ = new ArrayList();
  private Vibrator vibrator_;
  private float view_x_ = 0.0f;
  private float view_y_ = 0.0f;
  private float zoom_ = kGroundZoom;

  private static final float kAirZoom = 0.6f;
  private static final int kBloodBathSize = 6;  // Number of particles.
  private static final float kBloodBathVelocity = 100.0f;
  private static final float kGroundZoom = 0.8f;
  private static final long kVibrateLength = 30;  // Milliseconds.
  private static final float kViewLead = 1.0f;
  private static final float kViewSpeed = 0.1f;
  private static final float kZoomSpeed = 0.05f;
}
