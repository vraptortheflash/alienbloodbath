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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;


public class AlienBloodBathMain extends Activity {
  @Override
  public void onCreate(Bundle saved_instance_state) {
    super.onCreate(saved_instance_state);
    Content.initialize(getResources());
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                         WindowManager.LayoutParams.FLAG_FULLSCREEN);

    setContentView(R.layout.main);

    mGameState = new GameState((Context)this);
    mGameView = (GameView)findViewById(R.id.GAME_VIEW);
    mGameView.setGame(mGameState);

    if (saved_instance_state != null) {
      mGameState.loadStateBundle(saved_instance_state.getBundle("mGameState"));
    } else {
      mGameState.map.loadFromUri(Uri.parse(kStartupMap));
      mGameState.reset();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    boolean result = super.onCreateOptionsMenu(menu);
    //menu.add(0, kSelectMap, 0, "Load Map...").setIcon(R.drawable.load);
    //menu.add(0, kDownloadMap, 0, "More Maps...").setIcon(R.drawable.download);
    menu.add(0, kFeedback, 0, "Feedback...").setIcon(R.drawable.feedback);
    menu.add(0, kAbout, 0, "About...").setIcon(R.drawable.about);
    return result;
  }

  @Override
  public boolean onMenuItemSelected(int feature_id, MenuItem item) {
    switch (item.getItemId()) {
      case kSelectMap:
        startActivityForResult(
            new Intent(this, MapSelectActivity.class), kSelectMap);
        return true;
      case kDownloadMap:
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(kMapsPage)));
        return true;
      case kAbout:
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(kAboutPage)));
        return true;
      case kFeedback:
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(kFeedbackPage)));
        return true;
    }
    return super.onMenuItemSelected(feature_id, item);
  }

  @Override
  public void onActivityResult(int request_code, int result_code,
                               Intent intent) {
    switch (request_code) {
      case kSelectMap:
        if (intent != null) {
          mGameState.map.loadFromUri(intent.getData());
          mGameState.reset();
        }
        break;
    }
  }

  @Override
  public void onSaveInstanceState(Bundle saved_instance_state) {
    saved_instance_state.putBundle("mGameState", mGameState.saveStateBundle());
    super.onSaveInstanceState(saved_instance_state);

    // Clean up any temporary files used by the content management library.
    Content.cleanup();
  }

  @Override
  public void onRestoreInstanceState(Bundle saved_instance_state) {
    super.onRestoreInstanceState(saved_instance_state);
    mGameState.loadStateBundle(saved_instance_state.getBundle("mGameState"));
  }

  private GameState mGameState;
  private GameView mGameView;

  private final int kAbout = 2;
  private final int kFeedback = 3;
  private final int kDownloadMap = 4;
  private final String kAboutPage = "http://code.google.com/p/alienbloodbath";
  private final String kFeedbackPage = "http://spreadsheets.google.com/embeddedform?key=p8QSDoz2S_XEYxN68-QJMEg";
  private final String kMapsPage = "http://abbserver.appspot.com";
  private final int kSelectMap = 1;
  private final String kStartupMap = "content:///Classic/";
  //private final String kStartupMap = "content:///Demo/";
  //private final String kStartupMap = "content:///The_Second_Wave/";
}
