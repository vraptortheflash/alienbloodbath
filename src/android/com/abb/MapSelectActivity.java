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

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.io.File;
import java.util.ArrayList;


public class MapSelectActivity extends ListActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    int item_layout_id = R.layout.mapselect_item;
    LoadMaps();
    String[] maps = maps_.toArray(new String[0]);
    setListAdapter(new ArrayAdapter<String>(this, item_layout_id, maps));
    getListView().setTextFilterEnabled(true);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    Intent intent = new Intent(Intent.ACTION_VIEW,
                               Uri.parse(map_uris_.get(position)),
                               this, AlienBloodBathMain.class);
    setResult(RESULT_OK, intent);
    finish();
  }

  private void LoadMaps() {
    // Add built-in maps.
    maps_.add("Classic 1");
    map_uris_.add("builtin://0");
    maps_.add("Classic 2");
    map_uris_.add("builtin://1");

    // Add maps located within files.
    for (String root_path : paths_) {
      String[] map_paths = (new File(root_path)).list();
      if (map_paths == null)
        continue;

      for (String map_path : map_paths) {
        String full_path = root_path + "/" + map_path;
        if ((new File(full_path + "/tiles.txt")).exists()) {
          maps_.add(full_path);
          map_uris_.add("file://" + full_path);
        }
      }
    }
  }

  private ArrayList<String> maps_ = new ArrayList<String>();
  private ArrayList<String> map_uris_ = new ArrayList<String>();
  private String[] paths_ = { "/sdcard/abb_maps",
                              "/data/data/android.com.abb/abb_maps" };
}
