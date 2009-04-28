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
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;


/**
 * Since Java does not allow multiple inheritance, yet we want to expose the
 * following methods to other classes derived from activity, they are made
 * static so that they may be manually injected where desired.
 */
public class AlienBloodBathActivity {
  public static boolean onCreateOptionsMenu(Activity parent, Menu menu) {
    menu.add(0, kFeedback, 0, "Feedback...").setIcon(R.drawable.feedback);
    menu.add(0, kAbout, 0, "About...").setIcon(R.drawable.about);
    return true;
  }

  public static boolean onMenuItemSelected(Activity parent,
                                           int feature_id, MenuItem item) {
    switch (item.getItemId()) {
      case kAbout:
        parent.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(kAboutPage)));
        return true;
      case kFeedback:
        parent.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(kFeedbackPage)));
        return true;
    }
    return false;
  }

  private static final int kAbout = 2;
  private static final String kAboutPage =
      "http://code.google.com/p/alienbloodbath";
  private static final int kFeedback = 3;
  private static final String kFeedbackPage =
      "http://spreadsheets.google.com/embeddedform?key=p8QSDoz2S_XEYxN68-QJMEg";
}