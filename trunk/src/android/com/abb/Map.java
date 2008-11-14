package android.com.abb;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import java.lang.Math;

import android.com.abb.Entity;


public class Map {
  public float starting_x;
  public float starting_y;
  public float ending_x;
  public float ending_y;
  public int[] tiles;
  public Bitmap tiles_bitmap;

  public Map() {
    starting_x = starting_y = 0.0f;
    ending_x = ending_y = 0.0f;
    paint_ = new Paint();  // Rendering settings.
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

  public static boolean IsCollideable(int tile_id) {
    return ((tile_id >= 1 && tile_id <= 5) || tile_id == 7);
  }

  public static boolean IsExploadable(int tile_id) {
    return (tile_id == 9);
  }

  public void CollideEntity(Entity entity) {
    if (entity.radius <= 0) {
      return;  // Collision disabled for this entity.
    }

    entity.has_ground_contact = false;

    // Iterate through map tiles potentially intersecting the entity.
    float radius = entity.radius;
    float half_tile_size = kTileSize / 2;
    for (float x = entity.x - radius; x <= entity.x + radius; x += kTileSize) {
      for (float y = entity.y - radius; y <= entity.y + radius; y += kTileSize) {
        int tile_id = TileAt(x, y);
        boolean tile_collideable = IsCollideable(tile_id);
        boolean tile_exploadable = IsExploadable(tile_id);

        if (!tile_collideable && !tile_exploadable) {
          continue;  // Not a collideable tile.
        }

        int index_x = (int)(x / kTileSize + 0.5f);
        int index_y = (int)(y / kTileSize + 0.5f);
        float tile_x = kTileSize * index_x;
        float tile_y = kTileSize * index_y;

        float distance_x = entity.x - tile_x;
        float distance_y = entity.y - tile_y;
        if (Math.abs(distance_x) > half_tile_size + entity.radius ||
            Math.abs(distance_y) > half_tile_size + entity.radius) {
          continue;  // No collision with this tile.
        }

        if (tile_exploadable) {
          float distance =
              (float)Math.sqrt(distance_x * distance_x + distance_y * distance_y);
          entity.dx += kExplosionStrength * distance_x / distance;
          entity.dy += kExplosionStrength * distance_y / distance;
          SetTileAt(x, y, 0);  // Clear the exploding tile.
        }
        if (tile_collideable) {
          float impact_normal_x;
          float impact_normal_y;
          float impact_distance;

          // Determine which edges have the least amount of overlap, these will
          // be the edges which are considered "in-collision". The vertical skew
          // constant allows for preference to be given to vertical collisions
          // over horizonal collisions. This is useful in avoiding scraping tile
          // corners while sliding across the boundary.
          float kVerticalSkew = 5.0f;  // Pixels.
          if (Math.abs(distance_x) > Math.abs(distance_y) + kVerticalSkew) {  // Along x-axis.
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
          // implementation  is a bit  of a  hack, as  is everything  else here,
          // since we don't have any sense of entity mass.
          float impact_magnitude =
              impact_normal_x * entity.dx + impact_normal_y * entity.dy;
          entity.dx -= impact_normal_x * impact_magnitude;
          entity.dy -= impact_normal_y * impact_magnitude;
          entity.x += impact_normal_x * impact_distance;
          entity.y += impact_normal_y * impact_distance;

          /*
          final float kKineticFrictionCoefficient = 0.05f;
          final float kStaticFrictionCoefficient = 0.5f;
          if (Math.abs(entity.dx) < kStaticFrictionCoefficient)
            entity.dx = 0.0f;
          if (Math.abs(entity.dy) < kStaticFrictionCoefficient)
            entity.dy = 0.0f;
          entity.dx -= kKineticFrictionCoefficient * -impact_normal_y * entity.dx;
          entity.dy -= kKineticFrictionCoefficient * -impact_normal_x * entity.dy;
          */
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
        // Determine tile index for this world position x, y.
        int tile_id = TileAt(x, y);
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

  private Paint paint_;

  private static final int kStartingTile = 10;
  private static final int kEndingTile = 11;
  private static final int kMapWidth = 100;
  private static final int kMapHeight = 100;
  private static final int kTileSize = 64;
  private static final float kExplosionStrength = 250.0f;
}
