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


public class Grappling {
  public Grappling() {
  }

  public void step(float time_step) {
  }

  public void draw(Graphics graphics, float center_x, float center_y,
                   float zoom) {
    for (Link link : mLinks) {
      link.draw(graphics, center_x, center_y, zoom);
    }
  }

  public class Link extends Entity {
  }

  private Link[] mLinks = new Link[kLinkCount];

  private static final int   kLinkCount  = 5;
  private static final float kLinkLength = 10;
}
