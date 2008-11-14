package android.com.abb;

import android.com.abb.Avatar;
import android.com.abb.Map;

public class GameState {
  public GameState() {
    avatar = new Avatar();
    map = new Map();
  }

  public Avatar avatar;
  public Map map;
}
