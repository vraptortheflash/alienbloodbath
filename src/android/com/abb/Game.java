package android.com.abb;

import android.content.res.Resources;
import android.graphics.Canvas;


/** Simple interface which hides most of the Android specifics. All method calls
 * are serialized. */
public interface Game {
  void LoadResources(Resources resources);

  void Reset();

  boolean OnKeyDown(int key_code);
  boolean OnKeyUp(int key_code);

  boolean Update(float time_step, Canvas canvas);
}
