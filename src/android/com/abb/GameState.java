// Copyright 2008 and onwards Matthew Burkhart and Matt Barnes.
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
import android.view.KeyEvent;
import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.TreeMap;


public class GameState implements Game {
  public Avatar            avatar      = new Avatar(this);
  public ArrayList<Enemy>  enemies     = new ArrayList<Enemy>();
  public Map               map         = new Map(this);
  public int               misc_sprites;
  public ArrayList<Entity> particles   = new ArrayList<Entity>();
  public ArrayList<Entity> projectiles = new ArrayList<Entity>();

  public GameState(Context context) {
    mContext = context;
    mVibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
  }

  public void initializeGraphics(Graphics graphics) {
    avatar.loadFromUri(Uri.parse("content:///humanoid.entity"));
    String misc_sprites_path =
        Content.getTemporaryFilePath(Uri.parse("content:///misc.png"));
    Bitmap misc_sprites_bitmap = BitmapFactory.decodeFile(misc_sprites_path);
    misc_sprites = graphics.loadImageFromBitmap(misc_sprites_bitmap);
  }

  /** Initialize the game state structure. Upon returning the game state should
   * be in a state representing a new game "life". */
  public void reset() {
    for (Entity particle : particles) {
      particle.release();
    }
    particles.clear();
    for (Entity projectile : projectiles) {
      projectile.release();
    }
    projectiles.clear();
    enemies.clear();
    avatar.stop();
    avatar.releaseWeapon();
    avatar.life = 1.0f;
    avatar.x = map.getStartingX();
    avatar.y = map.getStartingY();
    mViewX = mTargetViewX = avatar.x;
    mViewY = mTargetViewY = avatar.y;
    mDeathTimer = kDeathTimer;
  }

  public boolean onKeyDown(int key_code) {
    // Developement level navigation.
    if (key_code == KeyEvent.KEYCODE_B) {
      map.advanceLevel();
      avatar.life = 0.0f;
    }

    avatar.setKeyState(key_code, 1);
    return false;  // False to indicate not handled.
  }

  public boolean onKeyUp(int key_code) {
    avatar.setKeyState(key_code, 0);
    return false;  // False to indicate not handled.
  }

  public void onMotionEvent(MotionEvent motion_event) {
    avatar.onMotionEvent(motion_event);
  }

  public boolean onFrame(Graphics graphics, float time_step) {
    stepGame(time_step);
    drawGame(graphics);
    return true;  // True to keep updating.
  }

  /** Run the game simulation for the specified amount of seconds. */
  protected void stepGame(float time_step) {
    // Update the view parameters.
    if (avatar.life > 0.0f) {
      if (!avatar.has_ground_contact) {
        mTargetZoom = kAirZoom;
      } else {
        mTargetZoom = kGroundZoom;
      }
    }
    mTargetViewX = avatar.x + kViewLead * avatar.dx;
    mTargetViewY = avatar.y + kViewLead * avatar.dy;

    mZoom += (mTargetZoom - mZoom) * kZoomSpeed * time_step;
    mViewX += (mTargetViewX - mViewX) * kViewSpeed * time_step;
    mViewY += (mTargetViewY - mViewY) * kViewSpeed * time_step;

    // Step the avatar.
    if (avatar.life > 0.0f) {
      avatar.step(time_step);
      map.collideEntity(avatar);
      map.processTriggers(avatar);
      if (Map.tileIsGoal(map.tileAt(avatar.x, avatar.y))) {
        map.advanceLevel();
        map.reload();
        reset();
      }
    } else {
      if (mDeathTimer == kDeathTimer) {
        avatar.stop();
        mVibrator.vibrate(400);  // Milliseconds.
        mTargetZoom = kDeathZoom;
        for (int n = 0; n < 2 * kBloodBathSize; n++) {
          createBloodParticle(
              avatar.x, avatar.y,
              2.0f * kBloodBathVelocity * (0.5f - mRandom.nextFloat()) + avatar.dx,
              2.0f * kBloodBathVelocity * (0.5f - mRandom.nextFloat()) + avatar.dy);
        }
      }
      mDeathTimer -= time_step;
      if (mDeathTimer < 0) {
        map.reload();
        reset();
      }
    }

    // Step the enemies.
    for (int index = 0; index < enemies.size(); ++index) {
      Enemy enemy = enemies.get(index);
      enemy.step(time_step);
      map.collideEntity(enemy);
      if (enemy.collidesWith(avatar) && avatar.life > 0.0f) {
        avatar.life -= enemy.damage;
        enemy.life = 0.0f;
        vibrate(kEnemyAttackVibrateLength);
        createBloodParticle(avatar.x, avatar.y, enemy.dx, enemy.dy);
      }
      if (enemy.life <= 0.0f) {
        enemies.remove(index);
        vibrate(kEnemyDeathVibrateLength);
        for (int n = 0; n < kBloodBathSize; n++) {
          createBloodParticle(
              enemy.x, enemy.y,
              kBloodBathVelocity * (0.5f - mRandom.nextFloat()) + enemy.dx,
              kBloodBathVelocity * (0.5f - mRandom.nextFloat()) + enemy.dy);
        }
      }
    }

    // Step the projectiles and collide them against the enemies.
    for (int index = 0; index < projectiles.size(); ++index) {
      Entity projectile = projectiles.get(index);
      projectile.step(time_step);
      projectile.life -= time_step;
      for (int enemy_index = 0; enemy_index < enemies.size(); ++enemy_index) {
        Enemy enemy = enemies.get(enemy_index);
        if (projectile.collidesWith(enemy)) {
          createBloodParticle((enemy.x + projectile.x) / 2.0f,
                              (enemy.y + projectile.y) / 2.0f,
                              projectile.dx / 2.0f, projectile.dy / 2.0f);
          projectile.life = 0.0f;
          enemy.life -= projectile.damage;
          break;
        }
      }
      if (projectile.life <= 0.0f) {
        projectile.release();
        projectiles.remove(index);
      }
    }

    // Step the particles.
    for (int index = 0; index < particles.size(); ++index) {
      Entity particle = particles.get(index);
      particle.life -= time_step;
      particle.step(time_step);
      if (particle.life <= 0.0f) {
        particle.release();
        particles.remove(index);
      }
    }
  }

  /** Draw the game state. The game map and entities are always drawn with the
   * avatar centered in the screen. */
  protected void drawGame(Graphics graphics) {
    // Draw the map tiles.
    map.draw(graphics, mViewX, mViewY, mZoom);

    // Draw the enemies.
    for (int index = 0; index < enemies.size(); ++index) {
      enemies.get(index).draw(graphics, mViewX, mViewY, mZoom);
    }

    // Draw the avatar.
    avatar.draw(graphics, mViewX, mViewY, mZoom);

    // Draw the projectiles.
    for (int index = 0; index < projectiles.size(); ++index) {
      projectiles.get(index).draw(graphics, mViewX, mViewY, mZoom);
    }

    // Draw the particles.
    for (int index = 0; index < particles.size(); ++index) {
      particles.get(index).draw(graphics, mViewX, mViewY, mZoom);
    }
  }

  synchronized public void addNotification(String notification) {
    mPendingNotifications.add(notification);
  }

  synchronized public String getPendingNotification() {
    return mPendingNotifications.poll();
  }

  public Entity createEnemyFromUri(Uri uri, float x, float y) {
    Enemy enemy = mEnemyCache.get(uri);
    if (enemy == null) {
      enemy = new Enemy(avatar);
      enemy.loadFromUri(uri);
      mEnemyCache.put(uri, enemy);
    }

    enemy = (Enemy)enemy.clone();
    enemy.x = x;
    enemy.y = y;
    enemies.add(enemy);
    return enemy;
  }

  public Entity createBloodParticle(float x, float y, float dx, float dy) {
    final float kTimeRemaining = 0.75f;  // Seconds.
    final int kSpriteBase      = 1 * 64;
    final int kSpriteWidth     = 64;
    final int kSpriteHeight    = 64;

    Entity blood = Entity.obtain();
    blood.sprite_rect.set(
        0, kSpriteBase, kSpriteWidth, kSpriteBase + kSpriteHeight);
    blood.sprite_flipped_horizontal = mRandom.nextBoolean();
    blood.sprite_image = misc_sprites;
    blood.life = kTimeRemaining;
    blood.x = x;
    blood.y = y;
    blood.dx = dx;
    blood.dy = dy;
    blood.ddy = kGravity;
    particles.add(blood);
    return blood;
  }

  public Weapon createWeaponFromUri(Uri uri) {
    Weapon weapon = mWeaponCache.get(uri);
    if (weapon == null) {
      weapon = new Weapon(this);
      weapon.loadFromUri(uri);
      mWeaponCache.put(uri, weapon);
    }
    return (Weapon)weapon.clone();
  }

  public void createProjectile(float x, float y, float dx, float dy,
                               float timeout, float damage,
                               int image_handle, Rect image_rect,
                               boolean sprite_flipped_horizontal) {
    Entity projectile = Entity.obtain();
    projectile.x = x;
    projectile.y = y;
    projectile.dx = dx;
    projectile.dy = dy;
    projectile.life = timeout;
    projectile.damage = damage;
    projectile.sprite_image = image_handle;
    projectile.sprite_rect.set(image_rect);
    projectile.sprite_flipped_horizontal = sprite_flipped_horizontal;
    projectile.radius = Math.min(image_rect.width(), image_rect.height());
    projectiles.add(projectile);
  }

  public Entity createFireProjectile(float x, float y, float dx, float dy,
                                     float damage,
                                     boolean sprite_flipped_horizontal) {
    Entity fire = Entity.obtain();
    fire.sprite_image = misc_sprites;
    fire.x = x;
    fire.y = y;
    fire.dx = dx;
    fire.dy = dy;
    fire.ddy = -50.0f;   // Give fire a slight "up draft".
    fire.life = 1.3f;
    fire.damage = damage;
    fire.is_flame = true;
    fire.radius = 3.0f;
    fire.sprite_flipped_horizontal = sprite_flipped_horizontal;
    projectiles.add(fire);
    return fire;
  }

  public Entity createFireProjectile(float x, float y, float dx, float dy) {
    return createFireProjectile(x, y, dx, dy, 0.2f, false);
  }

  public void vibrate(long vibrate_milliseconds) {
    mVibrator.vibrate(vibrate_milliseconds);
  }

  public void loadStateBundle(Bundle saved_instance_state) {
    map.loadStateBundle(saved_instance_state.getBundle("map"));
    reset();

    mTargetViewX = saved_instance_state.getFloat("mTargetViewX");
    mTargetViewY = saved_instance_state.getFloat("mTargetViewY");
    mViewX = saved_instance_state.getFloat("mViewX");
    mViewY = saved_instance_state.getFloat("mViewY");
    mZoom = saved_instance_state.getFloat("mZoom");

    avatar.x = saved_instance_state.getFloat("avatar.x");
    avatar.y = saved_instance_state.getFloat("avatar.y");
    avatar.dx = saved_instance_state.getFloat("avatar.dx");
    avatar.dy = saved_instance_state.getFloat("avatar.dy");
    avatar.ddx = saved_instance_state.getFloat("avatar.ddx");
    avatar.ddy = saved_instance_state.getFloat("avatar.ddy");
    avatar.life = saved_instance_state.getFloat("avatar.life");
  }

  public Bundle saveStateBundle() {
    // Note that particles, projectiles and spawned enemies are lost through
    // serialization.
    Bundle saved_instance_state = new Bundle();
    saved_instance_state.putBundle("map", map.saveStateBundle());

    saved_instance_state.putFloat("mTargetViewX", mTargetViewX);
    saved_instance_state.putFloat("mTargetViewY", mTargetViewY);
    saved_instance_state.putFloat("mViewX", mViewX);
    saved_instance_state.putFloat("mViewY", mViewY);
    saved_instance_state.putFloat("mZoom", mZoom);

    saved_instance_state.putFloat("avatar.x", avatar.x);
    saved_instance_state.putFloat("avatar.y", avatar.y);
    saved_instance_state.putFloat("avatar.dx", avatar.dx);
    saved_instance_state.putFloat("avatar.dy", avatar.dy);
    saved_instance_state.putFloat("avatar.dx", avatar.ddx);
    saved_instance_state.putFloat("avatar.dy", avatar.ddy);
    saved_instance_state.putFloat("avatar.life", avatar.life);
    return saved_instance_state;
  }

  private Context              mContext;
  private float                mDeathTimer           = kDeathTimer;
  private TreeMap<Uri, Enemy>  mEnemyCache           = new TreeMap<Uri, Enemy>();
  private LinkedList<String>   mPendingNotifications = new LinkedList<String>();
  private Random               mRandom               = new Random();
  private float                mTargetViewX          = 0.0f;
  private float                mTargetViewY          = 0.0f;
  private float                mTargetZoom           = kGroundZoom;
  private Vibrator             mVibrator;
  private float                mViewX                = 0.0f;
  private float                mViewY                = 0.0f;
  private TreeMap<Uri, Weapon> mWeaponCache          = new TreeMap<Uri, Weapon>();
  private float                mZoom                 = kGroundZoom;

  private static final float kAirZoom                  = 0.6f;
  private static final int   kBloodBathSize            = 20;  // Particle count.
  private static final float kBloodBathVelocity        = 60.0f;
  private static final float kDeathTimer               = 3.0f;
  private static final float kDeathZoom                = 1.5f;
  private static final long  kEnemyAttackVibrateLength = 50;  // Milliseconds.
  private static final long  kEnemyDeathVibrateLength  = 30;  // Milliseconds.
  private static final float kGravity                  = 200.0f;
  private static final float kGroundZoom               = 0.8f;
  private static final float kViewLead                 = 1.0f;
  private static final float kViewSpeed                = 2.0f;
  private static final float kZoomSpeed                = 1.0f;
}
