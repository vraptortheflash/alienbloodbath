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
import java.util.ArrayList;

import android.com.abb.Avatar;
import android.com.abb.Blood;
import android.com.abb.Enemy;
import android.com.abb.Fire;
import android.com.abb.Map;


public class GameState {
  public Map map = new Map(this);
  public Avatar avatar = new Avatar(this);
  public ArrayList enemies = new ArrayList();
  public ArrayList particles = new ArrayList();
  public ArrayList projectiles = new ArrayList();

  public Bitmap enemy_sprites;
  public Bitmap misc_sprites;

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
}
