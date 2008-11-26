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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;


public class AlienBloodBathMain extends Activity {
  @Override
  public void onCreate(Bundle saved_instance_state) {
    super.onCreate(saved_instance_state);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.main);

    game_view_ = (GameView)findViewById(R.id.GAME_VIEW);
    game_view_.SetTitleView((TextView)findViewById(R.id.TEXT_VIEW));
    game_view_.SetGame(game_state_);

    if (saved_instance_state != null) {
      game_state_.LoadStateBundle(saved_instance_state.getBundle(kStateKey));
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    boolean result = super.onCreateOptionsMenu(menu);
    menu.add(0, kSelectMap, 0, "Load Map...").setIcon(R.drawable.load);
    menu.add(0, kAbout, 0, "About...").setIcon(R.drawable.about);
    return result;
  }

  @Override
  public boolean onMenuItemSelected(int feature_id, MenuItem item) {
    switch(item.getItemId()) {
      case kSelectMap:
        startActivity(new Intent(this, MapSelectActivity.class));
        return true;
      case kAbout:
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(kAboutPage)));
        return true;
    }
    return super.onMenuItemSelected(feature_id, item);
  }

  @Override
  public void onSaveInstanceState(Bundle out_state) {
    out_state.putBundle(kStateKey, game_state_.SaveStateBundle());
  }

  private GameState game_state_ = new GameState();
  private GameView game_view_;

  private final int kAbout = 2;
  private final String kAboutPage = "http://code.google.com/p/alienbloodbath";
  private final int kSelectMap = 1;
  private final String kStateKey = "abb-state";
}
