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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.view.MotionEvent;


/** Simple interface which hides most of the Android specifics. All method calls
 * are serialized. */
public interface Game {
  void initializeGraphics(Graphics graphics);
  void reset();

  boolean onFrame(Graphics graphics, float time_step);
  boolean onKeyDown(int key_code);
  boolean onKeyUp(int key_code);
  void onMotionEvent(MotionEvent motion_event);
}
