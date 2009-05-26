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

import android.graphics.Matrix;
import android.util.Log;


/** The Grappling class controls a series of entities as if they were joined
 * together into a chain. This class implements the required physics and drawing
 * mechanism for the linked entities. */
public class Grappling {
  public Grappling(GameState game_state) {
    mGameState = game_state;
    for (int link = 0; link < kLinkCount; ++link) {
      mLinks[link] = new Link();
      mLinks[link].sprite_rect.set(0, 40, 64, 48);
    }
  }

  public void setGrapple(float target_x, float target_y,
                         float source_x, float source_y) {
    mGrappleX = target_x;
    mGrappleY = target_y;
    for (int link = 0; link < kLinkCount; ++link) {
      float i = link / (float)kLinkCount;
      mLinks[link].x = source_x * (1.0f - i) + target_x * i;
      mLinks[link].y = source_y * (1.0f - i) + target_y * i;
      mLinks[link].stop();
    }
  }

  public Entity getBottomLink() {
    return mLinks[0];
  }

  public void step(float time_step) {
    // First pass, caclulate the force on each link particle.
    mLinks[0].fx = 0.0f;
    mLinks[0].fy = 0.0f;
    for (int link = 0; link < kLinkCount; ++link) {
      float dx;
      float dy;
      if (link < kLinkCount - 1) {
        dx = mLinks[link + 1].x - mLinks[link].x;
        dy = mLinks[link + 1].y - mLinks[link].y;
      } else {
        dx = mGrappleX - mLinks[link].x;
        dy = mGrappleY - mLinks[link].y;
      }
      float norm = (float)Math.sqrt(dx * dx + dy * dy) + 0.0001f;
      float force = kLinkStrength * (norm - kLinkLength);
      float fx = dx / norm * force;
      float fy = dy / norm * force;

      mLinks[link].fx += fx;
      mLinks[link].fy += fy;
      if (link < kLinkCount - 1) {
        mLinks[link + 1].fx = -fx;
        mLinks[link + 1].fy = -fy;
      }
    }

    // Second pass, apply the force to each partice and update the positions.
    for (int link = 0; link < kLinkCount; ++link) {
      final float kDamping = 0.999f;
      mLinks[link].dx *= kDamping;
      mLinks[link].dy *= kDamping;

      float link_mass = link == 0 ? kLinkMassBottom : kLinkMass;
      mLinks[link].ddx = mLinks[link].fx / link_mass;
      mLinks[link].ddy = (mLinks[link].fy + kLinkGravity) / link_mass;
      mLinks[link].dx += mLinks[link].ddx * time_step;
      mLinks[link].dy += mLinks[link].ddy * time_step;
      mLinks[link].x += mLinks[link].dx * time_step;
      mLinks[link].y += mLinks[link].dy * time_step;
    }
    for (int link = 0; link < kLinkCount; ++link) {
      if (link < kLinkCount - 1) {
        mLinks[link].angle = 57.29578f * (float)Math.atan2(
            mLinks[link + 1].y - mLinks[link].y, mLinks[link + 1].x - mLinks[link].x);
      } else {
        mLinks[link].angle = 57.29578f * (float)Math.atan2(
            mGrappleY - mLinks[link].y, mGrappleX - mLinks[link].x);
      }
    }
  }

  public void draw(Graphics graphics, float center_x, float center_y,
                   float zoom) {
    int canvas_width = graphics.getWidth();
    int canvas_height = graphics.getHeight();
    int half_canvas_width = canvas_width / 2;
    int half_canvas_height = canvas_height / 2;

    for (Link link : mLinks) {
      mTransformation.reset();
      mTransformation.preTranslate(
          (link.x - center_x) * zoom + half_canvas_width,
          (link.y - center_y) * zoom + half_canvas_height);
      mTransformation.preRotate(link.angle);
      mTransformation.preScale(kLinkLength * zoom, kLinkWidth * zoom);
      graphics.drawImage(mGameState.misc_sprites, link.sprite_rect,
                         mTransformation, false, false, 1);
    }
  }

  public class Link extends Entity {
    public float fx;
    public float fy;
    public float angle;
  }

  private Link[] mLinks = new Link[kLinkCount];
  private GameState mGameState;
  private float mGrappleX;
  private float mGrappleY;
  private Matrix mTransformation = new Matrix();

  private static final int   kLinkCount  = 1;
  private static final float kLinkGravity = 300.0f;
  private static final float kLinkLength = 120.0f;
  private static final float kLinkMass = 1.0f;
  private static final float kLinkMassBottom = 10.0f;
  private static final float kLinkStrength = 100.0f;
  private static final float kLinkWidth = 6.0f;
}
