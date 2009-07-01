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

import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.animation.TranslateAnimation;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import junit.framework.Assert;


public class LevelSelectActivity extends TabActivity implements ListView.OnItemClickListener, Runnable {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.level_select_main);

    TabHost tab_host = getTabHost();
    tab_host.addTab(tab_host.newTabSpec("levellistview")
                    .setIndicator("Levels", getResources().getDrawable(R.drawable.maps))
                    .setContent(R.id.levellistview));
    /*
    tab_host.addTab(tab_host.newTabSpec("avatarview")
                    .setIndicator("", getResources().getDrawable(R.drawable.avatar))
                    .setContent(R.id.avatarview));
    tab_host.addTab(tab_host.newTabSpec("settingsview")
                    .setIndicator("", getResources().getDrawable(R.drawable.settings))
                    .setContent(R.id.settingsview));
    */
    tab_host.addTab(tab_host.newTabSpec("helpview")
                    .setIndicator("Help", getResources().getDrawable(R.drawable.help))
                    .setContent(R.id.helpview));
    tab_host.setCurrentTab(0);

    WebView web_view = (WebView)findViewById(R.id.helpview);
    web_view.loadUrl("file:///android_asset/help/help.htm");

    // Start the asset pre-caching and level loading. This is done in another
    // thread since it is relatively slow. Note that the loading thread needs to
    // signal this thread via a "handler" since Android views may only be
    // touched via the main thread.
    mPrecachingDialog = ProgressDialog.show(
        this, null, "Precaching...", true, false);
    mPrecachingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    (new Thread(this)).start();
  }

  public void run() {
    Content.initialize(this);
    loadLevels();
    mRunDoneHandler.sendEmptyMessage(0);
  }

  private Handler mRunDoneHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
        mPrecachingDialog.dismiss();
        mLevelArrayAdapter = new LevelArrayAdapter(mThis);
        ListView list_view = (ListView)findViewById(R.id.levellistview);
        list_view.setAdapter(mLevelArrayAdapter);
        list_view.setOnItemClickListener(mThis);
      }
    };

  @Override
  public void onItemClick(AdapterView parent, View v, int position, long id) {
    // Since the loading may take a while, we hide the current view by moving
    // out of the screen to give the user immediate feedback.
    TabHost tab_host = getTabHost();
    tab_host.startAnimation(new TranslateAnimation(0, 0, 1024, 1024));

    String level_index = Integer.toString(position);
    Uri level_directory = Uri.parse(kRootDirectory);
    startActivityForResult(new Intent(level_index, level_directory,
                                      this, AlienBloodBathMain.class), 0);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode,
                                  Intent data) {
    // Since the user may have beat a new level, we need to refresh the list of
    // levels.
    loadLevels();
    mLevelArrayAdapter.refresh();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    return AlienBloodBathActivity.onCreateOptionsMenu(this, menu);
  }

  @Override
  public boolean onMenuItemSelected(int feature_id, MenuItem item) {
    return AlienBloodBathActivity.onMenuItemSelected(this, feature_id, item);
  }

  @Override
  public void onSaveInstanceState(Bundle saved_instance_state) {
    super.onSaveInstanceState(saved_instance_state);
  }

  private void loadLevels() {
    AvatarDatabase avatar_database = new AvatarDatabase(this);

    mLevels.clear();
    for (int level_index = 0;; ++level_index) {
      String level_path = kRootDirectory + "level_" + level_index + ".txt";
      Uri level_path_uri = Uri.parse(level_path);
      if (Content.exists(level_path_uri)) {
        String level_string = kRootDirectory + level_index;
        String level_kills =
            avatar_database.getStringValue(level_string + "_kills");
        String level_health =
            avatar_database.getStringValue(level_string + "_health");
        String level_time =
            avatar_database.getStringValue(level_string + "_time");

        Level level = new Level();

        String level_description_path =
            kRootDirectory + "description_" + level_index + ".txt";
        String[] description =
            Content.readUriLines(Uri.parse(level_description_path));
        level.name = description[0];

        if (level_kills == null) {
          Assert.assertNull(level_health);
          level.complete = false;
          mLevels.add(level);
          break;
        } else {
          level.complete = true;
          level.health = Float.valueOf(level_health);
          level.kills = Integer.valueOf(level_kills);
          level.time = Float.valueOf(level_time);
          mLevels.add(level);
        }
      } else {
        break;
      }
    }
  }

  private class LevelArrayAdapter extends ArrayAdapter {
    LevelArrayAdapter(Context context) {
      super(context, R.layout.level_select_row);
      mContext = context;
      refresh();
    }

    public void refresh() {
      clear();
      for (int level_index = 0; level_index < mLevels.size(); ++level_index) {
        insert(new Object(), level_index);
      }
      notifyDataSetChanged();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(
          Context.LAYOUT_INFLATER_SERVICE);
      View row_view = inflater.inflate(R.layout.level_select_row, null);
      Level level = mLevels.get(position);

      ImageView icon = (ImageView)row_view.findViewById(R.id.ROW_ICON);
      TextView label = (TextView)row_view.findViewById(R.id.ROW_LABEL);
      TextView kills = (TextView)row_view.findViewById(R.id.ROW_KILLS);
      TextView health = (TextView)row_view.findViewById(R.id.ROW_HEALTH);
      TextView time = (TextView)row_view.findViewById(R.id.ROW_TIME);

      label.setText("Stage " + (position + 1) + ": " + level.name);
      if (level.complete) {
        DecimalFormat formatter = new DecimalFormat("0.0");
        time.setText(formatter.format(level.time) + "s");
        health.setText(formatter.format(100.0f * level.health) + "%");
        kills.setText(Integer.toString(level.kills));
        icon.setImageResource(R.drawable.level_old);
      }
      return row_view;
    }

    private Context mContext;
  }  // class LevelArrayAdapter

  private class Level {
    public boolean complete;
    float health;
    int kills;
    String name;
    float time;
  }

  private LevelSelectActivity mThis = this;
  private LevelArrayAdapter mLevelArrayAdapter;
  private ArrayList<Level> mLevels = new ArrayList<Level>();
  private ProgressDialog mPrecachingDialog;

  //private final String kRootDirectory = "content:///Classic/";
  //private final String kRootDirectory = "content:///Demo/";
  private final String kRootDirectory = "file:///android_asset/The_Second_Wave/";
  //private final String kRootDirectory = "file:///android_asset/Maps/";
}
