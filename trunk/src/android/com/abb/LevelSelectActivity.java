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

import android.content.Context;
import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


public class LevelSelectActivity extends ListActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Content.initialize(getResources());

    populateLevels();
    mLevelArrayAdapter = new LevelArrayAdapter(this);
    setListAdapter(mLevelArrayAdapter);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
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
    populateLevels();
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

    // Clean up any temporary files used by the content management library.
    Content.cleanup();
  }

  private void populateLevels() {
    AvatarDatabase avatar_database = new AvatarDatabase(this);

    mLevels.clear();
    for (int level_index = 0;; ++level_index) {
      String level_path = kRootDirectory + "level_" + level_index + ".txt";
      Uri level_path_uri = Uri.parse(level_path);
      if (Content.exists(level_path_uri)) {
      String level_string = kRootDirectory + level_index;
        String level_health =
            avatar_database.getStringValue(level_string + "_health");
        String level_time =
            avatar_database.getStringValue(level_string + "_time");

        Level level = new Level();
        if (level_time == null) {
          Assert.assertNull(level_health);
          level.complete = false;
          mLevels.add(level);
          break;
        } else {
          level.complete = true;
          level.health = Float.valueOf(level_health);
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
      TextView time = (TextView)row_view.findViewById(R.id.ROW_TIME);
      TextView health = (TextView)row_view.findViewById(R.id.ROW_HEALTH);

      if (level.complete) {
        icon.setImageResource(R.drawable.level_old);
      }
      label.setText("Stage " + (position + 1) + ": " + level.name);
      if (level.complete) {
        DecimalFormat formatter = new DecimalFormat("0.0");
        time.setText(formatter.format(level.time) + "s");
      }
      if (level.complete) {
        DecimalFormat formatter = new DecimalFormat("0.0");
        health.setText(formatter.format(100.0f * level.health) + "%");
      }
      return row_view;
    }

    private Context mContext;
  }  // class LevelArrayAdapter

  private class Level {
    public boolean complete;
    float health;
    float time;
    String name;
  }

  private LevelArrayAdapter mLevelArrayAdapter;
  private ArrayList<Level> mLevels = new ArrayList<Level>();

  private final String kRootDirectory = "content:///The_Second_Wave/";
}
