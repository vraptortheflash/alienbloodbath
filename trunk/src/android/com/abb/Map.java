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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import java.lang.Math;
import java.util.Random;

import android.com.abb.Entity;
import android.com.abb.GameState;


public class Map {
  public float starting_x;
  public float starting_y;
  public float ending_x;
  public float ending_y;
  public int[] tiles;
  public Bitmap tiles_bitmap;

  public Map(GameState game_state) {
    game_state_ = game_state;
  }

  public void LoadFromArray(int[] new_tiles) {
    tiles = new_tiles;
    // Search for the starting and ending tiles.
    for (int n = 0; n < kMapWidth * kMapHeight; ++n) {
      if (tiles[n] == kStartingTile) {
        starting_x = (n / kMapWidth) * kTileSize;
        starting_y = (n % kMapWidth) * kTileSize;
      }
      if (tiles[n] == kEndingTile) {
        ending_x = (n / kMapWidth) * kTileSize;
        ending_y = (n % kMapWidth) * kTileSize;
      }
    }
  }

  public int TileIndexAt(float x, float y) {
    int index_x = (int)(x / kTileSize + 0.5f);
    int index_y = (int)(y / kTileSize + 0.5f);
    if (index_x < 0 || index_y < 0 ||
        index_x >= kMapWidth || index_y >= kMapHeight)
      return -1;  // Tile out of map range;
    return kMapWidth * index_x + index_y;
  }

  public void SetTileAt(float x, float y, int tile_id) {
    int tile_index = TileIndexAt(x, y);
    if (tile_index >= 0)
      tiles[tile_index] = tile_id;
  }

  public int TileAt(float x, float y) {
    int tile_index = TileIndexAt(x, y);
    if (tile_index >= 0)
      return tiles[tile_index];
    else
      return -1;  // Tile out of map range.
  }

  public static boolean TileIsCollideable(int tile_id) {
    return ((tile_id >= 1 && tile_id <= 5) || tile_id == 7);
  }

  public static boolean TileIsDeath(int tile_id) {
    return (tile_id == 4);
  }

  public static boolean TileIsEnemy(int tile_id) {
    return (tile_id == 12);
  }

  public static boolean TileIsExploadable(int tile_id) {
    return (tile_id == 9);
  }

  public void CollideEntity(Entity entity) {
    if (entity.radius <= 0) {
      return;  // Collision disabled for this entity.
    }

    entity.has_ground_contact = false;

    // Iterate through map tiles potentially intersecting the entity. The
    // collision model used for entities and tiles are squares.
    float radius = entity.radius;
    float half_tile_size = kTileSize / 2;
    for (float x = entity.x - radius; x <= entity.x + radius; x += kTileSize) {
      for (float y = entity.y - radius; y <= entity.y + radius; y += kTileSize) {
        int tile_id = TileAt(x, y);
        boolean tile_collideable = TileIsCollideable(tile_id);
        boolean tile_deadly = TileIsDeath(tile_id);
        boolean tile_exploadable = TileIsExploadable(tile_id);

        int index_x = (int)(x / kTileSize + 0.5f);
        int index_y = (int)(y / kTileSize + 0.5f);
        float tile_x = kTileSize * index_x;
        float tile_y = kTileSize * index_y;


        if (!tile_collideable && !tile_exploadable && !tile_deadly) {
          continue;  // Not a collideable tile.
        }

        // Determine if a collision has occurred between the two squares.
        float distance_x = entity.x - tile_x;
        float distance_y = entity.y - tile_y;
        if (Math.abs(distance_x) > half_tile_size + entity.radius ||
            Math.abs(distance_y) > half_tile_size + entity.radius) {
          continue;  // No collision with this tile.
        }

        // The kEpsilon constant allows the edges of the square to essential be
        // rounded which prevents the small corner collisions which may occur
        // when an entity is sliding over a series of tiles.
        final float kEpsilon = 10.0f;  // Pixels.
        float distance =
            (float)Math.sqrt(distance_x * distance_x + distance_y * distance_y);
        float threshold_radius = half_tile_size + entity.radius - kEpsilon;
        float threshold_distance =
            (float)Math.sqrt(2 * threshold_radius * threshold_radius);
        if (distance > threshold_distance) {
          continue;  // No collision with this tile.
        }

        if (tile_deadly) {
          entity.alive = false;
        }
        if (tile_exploadable) {
          entity.dy = Math.min(entity.dy, -kExplosionStrength);
          SetTileAt(x, y, 0);  // Clear the exploding tile.
          for (int n = 0; n < kExplosionSize; n++) {
            float random_angle = random_.nextFloat() * 2.0f * (float)Math.PI;
            float random_magnitude = kExplosionStrength * random_.nextFloat() / 3.0f;
            game_state_.CreateFire(
                tile_x, tile_y,
                random_magnitude * (float)Math.cos(random_angle),
                random_magnitude * (float)Math.sin(random_angle));
          }
        }
        if (tile_collideable) {
          float impact_normal_x;
          float impact_normal_y;
          float impact_distance;

          // Determine which edges have the least amount of overlap, these will
          // be the edges which are considered "in-collision".
          if (Math.abs(distance_x) > Math.abs(distance_y)) {  // Along x-axis.
            if (distance_x > 0) {  // Entity's left edge.
              impact_normal_x = 1.0f;
              impact_normal_y = 0.0f;
              impact_distance = half_tile_size + entity.radius - distance_x;
            } else {                 // Entity's right edge.
              impact_normal_x = -1.0f;
              impact_normal_y =  0.0f;
              impact_distance = half_tile_size + entity.radius + distance_x;
            }
          } else {  // Along y-axis.
            if (distance_y > 0) {  // Entity's top edge.
              impact_normal_x = 0.0f;
              impact_normal_y = 1.0f;
              impact_distance = half_tile_size + entity.radius - distance_y;
            } else {                 // Entity's bottom edge.
              impact_normal_x =  0.0f;
              impact_normal_y = -1.0f;
              impact_distance = half_tile_size + entity.radius + distance_y;
              entity.has_ground_contact = true;
            }
          }

          // Apply the force of the impact on the entity. The following friction
          // implementation is a bit of a hack, as is everything else here,
          // since we don't have any sense of entity mass.
          float impact_magnitude =
              impact_normal_x * entity.dx + impact_normal_y * entity.dy;
          entity.dx -= impact_normal_x * impact_magnitude;
          entity.dy -= impact_normal_y * impact_magnitude;
          entity.x += impact_normal_x * impact_distance;
          entity.y += impact_normal_y * impact_distance;
        }
      }
    }
  }

  /** Draw the entity to the canvas such that the specified coordinates are
   * centered. Tile locations in world coordinates correspond to the *center* of
   * the tile, eg. (0, 0) is the center of the first tile. */
  public void Draw(Canvas canvas, float center_x, float center_y) {
    int half_canvas_width = canvas.getWidth() / 2;
    int half_canvas_height = canvas.getHeight() / 2;
    for (float x = center_x - half_canvas_width;
         x < center_x + half_canvas_width + kTileSize;
         x += kTileSize) {
      for (float y = center_y - half_canvas_height;
           y < center_y + half_canvas_height + kTileSize;
           y += kTileSize) {
        // Determine tile id for this world position x, y.
        int tile_id = TileAt(x, y);

        // Spawn enemies if we happen to pass over an enemy tile.
        if (TileIsEnemy(tile_id)) {
          game_state_.CreateEnemy(x, y);
          SetTileAt(x, y, 0);  // Clear the tile.
        }

        if (tile_id < 1 || tile_id > 11) {
          continue;  // Not a visual tile.
        }
        int index_x = (int)(x / kTileSize + 0.5f);
        int index_y = (int)(y / kTileSize + 0.5f);
        Rect tile_source = new Rect(
            0, kTileSize * tile_id,
            kTileSize, kTileSize * tile_id + kTileSize);
        RectF tile_destination = new RectF(
            kTileSize * index_x, kTileSize * index_y,
            kTileSize * index_x + kTileSize, kTileSize * index_y + kTileSize);
        tile_destination.offset(
            -center_x + half_canvas_width - kTileSize / 2,
            -center_y + half_canvas_height - kTileSize / 2);
        canvas.drawBitmap(tiles_bitmap, tile_source, tile_destination, paint_);
      }
    }
  }

  private GameState game_state_;
  private Paint paint_ = new Paint();  // Rendering settings.;
  private Random random_ = new Random();

  private static final int kEndingTile = 11;
  private static final int kExplosionSize = 15;
  private static final float kExplosionStrength = 250.0f;
  private static final int kMapHeight = 100;
  private static final int kMapWidth = 100;
  private static final int kStartingTile = 10;
  private static final int kTileSize = 64;
}
