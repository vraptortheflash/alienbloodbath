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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


public class LevelSelectActivity extends ListActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Content.initialize(getResources());

    populateLevels();
    setListAdapter(new LevelArrayAdapter(this));
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    String level_index = Integer.toString(position);
    Uri level_directory = Uri.parse(kRootDirectory);
    startActivity(new Intent(level_index, level_directory,
                             this, AlienBloodBathMain.class));
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    return AlienBloodBathActivity.onCreateOptionsMenu(this, menu);
  }

  @Override
  public boolean onMenuItemSelected(int feature_id, MenuItem item) {
    return AlienBloodBathActivity.onMenuItemSelected(this, feature_id, item);
  }

  private void populateLevels() {
    for (int level_index = 0;; ++level_index) {
      Uri level_uri =
          Uri.parse(kRootDirectory + "level_" + level_index + ".txt");
      if (Content.exists(level_uri)) {
        Level level = new Level();
        mLevels.add(level);
      } else {
        break;
      }
    }
  }

  private class LevelArrayAdapter extends ArrayAdapter {
    LevelArrayAdapter(Context context) {
      super(context, R.layout.level_select_row);
      mContext = context;

      for (int level_index = 0; level_index < mLevels.size(); ++level_index) {
        this.insert(new Object(), level_index);
      }
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
        time.setText("2'32\"");
      }
      if (level.complete) {
        health.setText("72%");
      }

      return row_view;
    }

    private Context mContext;
  }

  private class Level {
    public boolean complete;
    float health;
    String name;
  }

  private ArrayList<Level> mLevels = new ArrayList<Level>();

  private final String kRootDirectory = "content:///The_Second_Wave/";
}
