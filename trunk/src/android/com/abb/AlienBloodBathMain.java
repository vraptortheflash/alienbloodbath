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

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.Window;
import java.util.Iterator;
import java.util.Random;

import android.com.abb.Game;
import android.com.abb.GameState;
import android.com.abb.GameView;


public class AlienBloodBathMain extends Activity {
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    game_ = new AlienBloodBathGame();
    game_view_ = (GameView)findViewById(R.id.GAME_VIEW);
    game_view_.SetGame(game_);
  }

  private AlienBloodBathGame game_;
  private GameView game_view_;

  private class AlienBloodBathGame implements Game {
    public AlienBloodBathGame() {
      game_state_ = new GameState();
    }

    /** Initialize the game state structure. Upon returning, game_state_ should
     * be in a state representing a new game. */
    public void Reset() {
      game_state_.avatar.Stop();
      game_state_.avatar.alive = true;
      game_state_.avatar.x = game_state_.map.starting_x;
      game_state_.avatar.y = game_state_.map.starting_y;
    }

    public void LoadResources(Resources resources) {
      game_state_.avatar.sprite =
          BitmapFactory.decodeResource(resources, R.drawable.avatar);
      game_state_.map.tiles_bitmap =
          BitmapFactory.decodeResource(resources, R.drawable.tiles_0);
      game_state_.enemy_sprites =
          BitmapFactory.decodeResource(resources, R.drawable.enemy_0);
      game_state_.misc_sprites =
          BitmapFactory.decodeResource(resources, R.drawable.misc);
      game_state_.map.LoadFromArray(resources.getIntArray(R.array.level_0));
    }

    public boolean OnKeyDown(int key_code) {
      game_state_.avatar.SetKeyState(key_code, 1);
      return false;  // False to indicate not handled.
    }

    public boolean OnKeyUp(int key_code) {
      game_state_.avatar.SetKeyState(key_code, 0);
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
      game_state_.avatar.Step(time_step);
      game_state_.map.CollideEntity(game_state_.avatar);  
      if (!game_state_.avatar.alive)
        Reset();

      // Step the enemies.
      for (Iterator it = game_state_.enemies.iterator(); it.hasNext();) {
        Enemy enemy = (Enemy)it.next();
        enemy.Step(time_step);
        game_state_.map.CollideEntity(enemy);
        if (!enemy.alive) {
          // Add blood particles whenever an enemy dies.
          for (int n = 0; n < kBloodBathSize; n++) {
            float random_angle = random_.nextFloat() * 2.0f * (float)Math.PI;
            float random_magnitude = kBloodBathVelocity * random_.nextFloat() / 3.0f;
            game_state_.CreateBloodParticle(
                enemy.x, enemy.y,
                enemy.dx + random_magnitude * (float)Math.cos(random_angle),
                enemy.dy + random_magnitude * (float)Math.sin(random_angle));
          }
          it.remove();
        }
      }

      // Step the projectiles and collide them against the enemies.
      for (Iterator it = game_state_.projectiles.iterator(); it.hasNext();) {
        Fire projectile = (Fire)it.next();
        projectile.Step(time_step);
        for (Iterator enemy_it = game_state_.enemies.iterator(); enemy_it.hasNext();)
          projectile.CollideEntity((Entity)enemy_it.next());
        if (!projectile.alive)
          it.remove();
      }

      // Step the particles.
      for (Iterator it = game_state_.particles.iterator(); it.hasNext();) {
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

      float center_x = game_state_.avatar.x;
      float center_y = game_state_.avatar.y;

      // Draw the map tiles.
      game_state_.map.Draw(canvas, center_x, center_y);

      // Draw the enemies.
      for (Iterator it = game_state_.enemies.iterator(); it.hasNext();)
        ((Entity)it.next()).Draw(canvas, center_x, center_y);

      // Draw the avatar.
      game_state_.avatar.Draw(canvas, center_x, center_y);

      // Draw the projectiles.
      for (Iterator it = game_state_.projectiles.iterator(); it.hasNext();)
        ((Entity)it.next()).Draw(canvas, center_x, center_y);

      // Draw the particles.
      for (Iterator it = game_state_.particles.iterator(); it.hasNext();)
        ((Entity)it.next()).Draw(canvas, center_x, center_y);
    }

    private GameState game_state_;
    private Random random_ = new Random();

    private static final int kBloodBathSize = 6;  // Particles.
    private static final float kBloodBathVelocity = 100.0f;
  }
}
