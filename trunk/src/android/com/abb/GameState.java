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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;;
import android.graphics.Rect;
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
  public Map map = new Map(this);
  public Avatar avatar = new Avatar(this);
  public ArrayList enemies = new ArrayList();
  public ArrayList particles = new ArrayList();
  public ArrayList projectiles = new ArrayList();

  public Bitmap enemy_sprites;
  public Bitmap misc_sprites;

  /** Initialize the game state structure. Upon returning, game_state_ should be
   * in a state representing a new game. */
  public void Reset() {
    avatar.Stop();
    avatar.alive = true;
    avatar.x = map.starting_x;
    avatar.y = map.starting_y;
  }

  public void LoadResources(Resources resources) {
    avatar.sprite =
        BitmapFactory.decodeResource(resources, R.drawable.avatar);
    map.tiles_bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.tiles_0);
    enemy_sprites =
        BitmapFactory.decodeResource(resources, R.drawable.enemy_0);
    misc_sprites =
        BitmapFactory.decodeResource(resources, R.drawable.misc);
    map.LoadFromArray(resources.getIntArray(R.array.level_0));
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
    // Step the avatar.
    avatar.Step(time_step);
    map.CollideEntity(avatar);  
    if (!avatar.alive)
        Reset();

    // Step the enemies.
    for (Iterator it = enemies.iterator(); it.hasNext();) {
      Enemy enemy = (Enemy)it.next();
      enemy.Step(time_step);
      map.CollideEntity(enemy);
      if (!enemy.alive) {
        // Add blood particles whenever an enemy dies.
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
    canvas.drawRGB(0, 0, 0);  // Clear the buffer.

    float center_x = avatar.x;
    float center_y = avatar.y;

    // Draw the map tiles.
    map.Draw(canvas, center_x, center_y);

    // Draw the enemies.
    for (Iterator it = enemies.iterator(); it.hasNext();)
      ((Entity)it.next()).Draw(canvas, center_x, center_y);

    // Draw the avatar.
    avatar.Draw(canvas, center_x, center_y);

    // Draw the projectiles.
    for (Iterator it = projectiles.iterator(); it.hasNext();)
      ((Entity)it.next()).Draw(canvas, center_x, center_y);

    // Draw the particles.
    for (Iterator it = particles.iterator(); it.hasNext();)
      ((Entity)it.next()).Draw(canvas, center_x, center_y);
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

  private Random random_ = new Random();

  private static final int kBloodBathSize = 6;  // Particles.
  private static final float kBloodBathVelocity = 100.0f;
}
