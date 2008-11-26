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


public class MapSelectActivity extends ListActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    int item_layout_id = R.layout.mapselect_item;
    String[] maps = GetMaps();
    setListAdapter(new ArrayAdapter<String>(this, item_layout_id, maps));
    getListView().setTextFilterEnabled(true);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("file://foo"),
                               this, AlienBloodBathMain.class);
    setResult(RESULT_OK, intent);
    finish();

    //    startActivity(intent);
    /*
    Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
    String action = getIntent().getAction();
    if (Intent.ACTION_PICK.equals(action) ||
        Intent.ACTION_GET_CONTENT.equals(action)) {
      // The caller is waiting for us to return a note selected by the user. The
      // have clicked on one, so return it now.
      setResult(RESULT_OK, new Intent().setData(uri));
    } else {
      // Launch activity to view/edit the currently selected item
      startActivity(new Intent(Intent.ACTION_EDIT, uri));
    }
    */
  }

  private String[] GetMaps() {
    String[] maps = new String[2];
    maps[0] = "Classic 1";
    maps[1] = "Classic 2";
    return maps;
  }
}
