package android.com.abb;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.Window;

import android.com.abb.Game;
import android.com.abb.GameState;
import android.com.abb.GameView;


public class AlienBloodBathMain extends Activity {
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
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
      game_state_.avatar.x = game_state_.map.starting_x + 105.0f;
      game_state_.avatar.y = game_state_.map.starting_y - 100.0f;
      game_state_.avatar.ddy = kGravity;
    }

    public void LoadResources(Resources resources) {
      game_state_.avatar.sprite =
          BitmapFactory.decodeResource(resources, R.drawable.avatar);
      game_state_.map.tiles_bitmap =
          BitmapFactory.decodeResource(resources, R.drawable.tiles_0);
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
      // Step the baddies.
      // Step the avatar.
      game_state_.avatar.Step(time_step);
      game_state_.map.CollideEntity(game_state_.avatar);
      // Step the projectiles.
      // Collide with map.
      // Hurt baddies.
      // Hurt avatar.
    }

    /** Draw the game state. The game map and entities are always drawn with the
     * avatar centered in the screen. */
    protected void DrawGame(Canvas canvas) {
      canvas.drawRGB(0, 0, 0);  // Clear buffer.

      float center_x = game_state_.avatar.x;
      float center_y = game_state_.avatar.y;
      // Draw the map tiles.
      game_state_.map.Draw(canvas, center_x, center_y);
      // Draw the baddies.
      // Draw the avatar.
      game_state_.avatar.Draw(canvas, center_x, center_y);
      // Draw the projectiles.
    }

    private GameState game_state_;

    /** Game constants. */
    private static final float kGravity = 200.0f;
  }
}
