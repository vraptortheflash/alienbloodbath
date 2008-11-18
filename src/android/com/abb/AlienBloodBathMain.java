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
import android.com.abb.GameState;
import android.com.abb.GameView;
import android.os.Bundle;
import android.widget.TextView;


public class AlienBloodBathMain extends Activity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    game_view_ = (GameView)findViewById(R.id.GAME_VIEW);
    game_view_.SetTitleView((TextView)findViewById(R.id.TEXT_VIEW));
    game_view_.SetGame(game_state_);
  }

  private GameState game_state_ = new GameState();
  private GameView game_view_;
}
