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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import java.lang.Math;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import android.com.abb.Content;
import android.com.abb.Entity;
import android.com.abb.GameState;


public class Map {
  public float starting_x;
  public float starting_y;

  public Map(GameState game_state) {
    game_state_ = game_state;
  }

  public void SetResources(Resources resources) {
    resources_ = resources;
  }

  public void AdvanceLevel() {
    LoadFromUri(base_uri_, level_offset_ + 1);
  }

  public void LoadFromUri(Uri base_uri) {
    LoadFromUri(base_uri, 0);
  }

  public void LoadFromUri(Uri base_uri, int level_offset) {
    base_uri_ = base_uri;
    level_offset_ = level_offset;

    // Map data may be either stored within the embedded package zip file or on
    // disk. The Content class is used to access map data in a storage agnostic
    // way.
    //
    // Maps are organized into package where each package is its own set of
    // (ordered) levels, tiles, and tile definitions.

    // Load level tiles.
    Uri level_uri =
        Uri.withAppendedPath(base_uri_, "level_" + level_offset_ + ".txt");
    String level_path = Content.GetTemporaryFilePath(level_uri);
    LoadLevelFromFile(level_path);

    // Load level tile images.
    Uri tiles_uri =
        Uri.withAppendedPath(base_uri_, "tiles_" + level_offset_ + ".png");
    if (!Content.Exists(tiles_uri))
      tiles_uri = Uri.withAppendedPath(base_uri_, "tiles_default.png");
    String tiles_path = Content.GetTemporaryFilePath(tiles_uri);
    LoadTilesFromFile(tiles_path);

    // Load tile effects.
    Uri effects_uri =
        Uri.withAppendedPath(base_uri_, "effects_" + level_offset_ + ".txt");
    if (!Content.Exists(effects_uri))
      effects_uri = Uri.withAppendedPath(base_uri_, "effects_default.txt");
    String effects_path = Content.GetTemporaryFilePath(effects_uri);
    LoadEffectsFromFile(effects_path);
  }

  public void LoadLevelFromFile(String file_path) {
    if (file_path == null)
      Log.e("Map::LoadLevelFromFile", "Invalid null argument.");
    try {
      FileReader level_reader = new FileReader(new File(file_path));
      tiles_ = new char[kMapWidth * kMapHeight];
      level_reader.read(tiles_, 0, kMapWidth * kMapHeight);
      LoadLevelFromArray(DecodeArray(tiles_));
    } catch (IOException ex) {
      Log.e("Map::LoadLevelFromFile", "Cannot find: " + file_path, ex);
    }
  }

  public void LoadTilesFromFile(String file_path) {
    if (file_path == null)
      Log.e("Map::LoadTilesFromFile", "Invalid null argument.");
    tiles_bitmap_ = BitmapFactory.decodeFile(file_path);
    if (tiles_bitmap_ == null)
      Log.e("Map::LoadTilesFromFile", "Cannot find: " + file_path);
  }

  public void LoadEffectsFromFile(String file_path) {
    if (file_path == null)
      Log.e("Map::LoadEffectsFromFile", "Invalid null argument.");
    try {
      FileReader effects_reader = new FileReader(new File(file_path));
      ArrayList<Character> effects_array = new ArrayList<Character>();
      while (effects_reader.ready())
        effects_array.add(new Character((char)effects_reader.read()));
      String[] effects_tokens =
          (new String(ToPrimative(effects_array))).split("\\s");
      if (effects_tokens.length % 2 != 0)
        Log.e("Map::LoadFromUri", "Improperly formatted effects file.");

      effects_death_ = new boolean[kMaxTileCount];
      effects_explode_ = new boolean[kMaxTileCount];
      effects_solid_ = new boolean[kMaxTileCount];
      for (int effect = 0; effect < effects_tokens.length; effect += 2) {
        int tile_id = Integer.parseInt(effects_tokens[effect]);
        String tile_effect = effects_tokens[effect + 1];
        if (tile_effect.equals("death"))
          effects_death_[tile_id] = true;
        else if (tile_effect.equals("explode"))
          effects_explode_[tile_id] = true;
        else if (tile_effect.equals("solid"))
          effects_solid_[tile_id] = true;;
      }
    } catch (IOException ex) {
      Log.e("Map::LoadFromUri", "Cannot find: " + file_path, ex);
    }
  }

  static private char[] DecodeArray(char[] tiles) {
    for (int n = 0; n < kMapWidth * kMapHeight; ++n) {
      tiles[n] -= kBaseValue;
    }
    return tiles;
  }

  static private char[] ToPrimative(ArrayList<Character> array_list) {
    char[] result = new char[array_list.size()];
    for (int index = 0; index < array_list.size(); ++index)
      result[index] = array_list.get(index).charValue();
    return result;
  }

  private void LoadLevelFromArray(char[] tiles) {
    tiles_ = tiles;
    for (int n = 0; n < kMapWidth * kMapHeight; ++n) {
      if (tiles_[n] == kStartingTile) {
        starting_x = (n / kMapWidth) * kTileSize;
        starting_y = (n % kMapWidth) * kTileSize;
      }
    }
  }

  public int TileIndexAt(float x, float y) {
    int index_x = (int)(x / kTileSize + 0.5f);
    int index_y = (int)(y / kTileSize + 0.5f);
    if (index_x < 0 || index_y < 0 ||
        index_x >= kMapWidth || index_y >= kMapHeight)
      return -1;  // Tile out of map range.
    return kMapWidth * index_x + index_y;
  }

  public void SetTileAt(float x, float y, char tile_id) {
    int tile_index = TileIndexAt(x, y);
    if (tile_index >= 0)
      tiles_[tile_index] = tile_id;
  }

  public int TileAt(float x, float y) {
    int tile_index = TileIndexAt(x, y);
    if (tile_index >= 0)
      return tiles_[tile_index];
    else
      return -1;  // Tile out of map range.
  }

  public static boolean TileIsEnemy(int tile_id) {
    return (tile_id == 12);
  }

  public static boolean TileIsGoal(int tile_id) {
    return (tile_id == kEndingTile);
  }

  public void CollideEntity(Entity entity) {
    if (entity.radius <= 0)
      return;  // Collision disabled for this entity.

    entity.has_ground_contact = false;

    // Iterate through map tiles potentially intersecting the entity. The
    // collision model used for entities and tiles are squares.
    float half_tile_size = kTileSize / 2.0f;
    float radius = Math.max(entity.radius, half_tile_size);
    for (float x = entity.x - radius; x <= entity.x + radius; x += kTileSize) {
      for (float y = entity.y - radius; y <= entity.y + radius; y += kTileSize) {
        int tile_id = TileAt(x, y);
        boolean tile_deadly = effects_death_[tile_id];
        boolean tile_exploadable = effects_explode_[tile_id];
        boolean tile_solid = effects_solid_[tile_id];

        int index_x = (int)(x / kTileSize + 0.5f);
        int index_y = (int)(y / kTileSize + 0.5f);
        float tile_x = kTileSize * index_x;
        float tile_y = kTileSize * index_y;

        if (!tile_solid && !tile_exploadable && !tile_deadly)
          continue;  // Not a collideable tile.

        // Determine if a collision has occurred between the two squares.
        float distance_x = entity.x - tile_x;
        float distance_y = entity.y - tile_y;
        if (Math.abs(distance_x) > half_tile_size + entity.radius ||
            Math.abs(distance_y) > half_tile_size + entity.radius)
          continue;  // No collision with this tile.

        // The kEpsilon constant allows the edges of the square to essential be
        // rounded which prevents the small corner collisions which may occur
        // when an entity is sliding over a series of tiles.
        final float kEpsilon = 10.0f;  // Pixels.
        float distance =
            (float)Math.sqrt(distance_x * distance_x + distance_y * distance_y);
        float threshold_radius = half_tile_size + entity.radius - kEpsilon;
        float threshold_distance =
            (float)Math.sqrt(2 * threshold_radius * threshold_radius);
        if (distance > threshold_distance)
          continue;  // No collision with this tile.

        if (tile_deadly) {
          entity.alive = false;
        }
        if (tile_exploadable) {
          entity.dy = Math.min(entity.dy, -kExplosionStrength);
          SetTileAt(x, y, (char)0);  // Clear the exploding tile.
          game_state_.Vibrate();
          for (int n = 0; n < kExplosionSize; n++) {
            float random_angle = random_.nextFloat() * 2.0f * (float)Math.PI;
            float random_magnitude = kExplosionStrength * random_.nextFloat() / 3.0f;
            game_state_.CreateFireProjectile(
                tile_x, tile_y,
                random_magnitude * (float)Math.cos(random_angle),
                random_magnitude * (float)Math.sin(random_angle));
          }
        }
        if (tile_solid) {
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
            } else {               // Entity's right edge.
              impact_normal_x = -1.0f;
              impact_normal_y =  0.0f;
              impact_distance = half_tile_size + entity.radius + distance_x;
            }
          } else {                                            // Along y-axis.
            if (distance_y > 0) {  // Entity's top edge.
              impact_normal_x = 0.0f;
              impact_normal_y = 1.0f;
              impact_distance = half_tile_size + entity.radius - distance_y;
            } else {               // Entity's bottom edge.
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
              -(impact_normal_x * entity.dx + impact_normal_y * entity.dy);
          if (impact_magnitude > 0) {
            entity.dx += impact_normal_x * impact_magnitude;
            entity.dy += impact_normal_y * impact_magnitude;
            entity.x += impact_normal_x * impact_distance;
            entity.y += impact_normal_y * impact_distance;
          }
        }
      }
    }
  }

  /** Draw the entity to the canvas such that the specified coordinates are
   * centered. Tile locations in world coordinates correspond to the *center* of
   * the tile, eg. (0, 0) is the center of the first tile. */
  public void Draw(Canvas canvas, float center_x, float center_y, float zoom) {
    canvas.drawRGB(background_[0], background_[1], background_[2]);

    int half_canvas_width = canvas.getWidth() / 2;
    int half_canvas_height = canvas.getHeight() / 2;
    for (float x = center_x - half_canvas_width / zoom;
         x < center_x + (half_canvas_width + kTileSize) / zoom;
         x += kTileSize) {
      for (float y = center_y - half_canvas_height / zoom;
           y < center_y + (half_canvas_height + kTileSize) / zoom;
           y += kTileSize) {
        // Determine tile id for this world position x, y.
        int tile_id = TileAt(x, y);

        // Spawn enemies if we happen to pass over an enemy tile.
        if (TileIsEnemy(tile_id)) {
          game_state_.CreateEnemy(x, y);
          SetTileAt(x, y, (char)0);  // Clear the tile.
        }

        if (tile_id < 1 || tile_id > 11)
          continue;  // Not a visual tile.

        int index_x = (int)(x / kTileSize + 0.5f);
        int index_y = (int)(y / kTileSize + 0.5f);
        Rect tile_source = new Rect(
            0, kTileSize * tile_id,
            kTileSize, kTileSize * tile_id + kTileSize);
        RectF tile_destination = new RectF(
            kTileSize * index_x * zoom, kTileSize * index_y * zoom,
            (kTileSize * index_x + kTileSize) * zoom,
            (kTileSize * index_y + kTileSize) * zoom);
        tile_destination.offset(
            -center_x * zoom + half_canvas_width - kTileSize / 2 * zoom,
            -center_y * zoom + half_canvas_height - kTileSize / 2 * zoom);
        canvas.drawBitmap(tiles_bitmap_, tile_source, tile_destination, paint_);
      }
    }
  }

  public void LoadStateBundle(Bundle saved_instance_state) {
    base_uri_ = Uri.parse(saved_instance_state.getString("base_uri_"));
    level_offset_ = saved_instance_state.getInt("level_offset_");
    LoadFromUri(base_uri_, level_offset_);
  }

  public Bundle SaveStateBundle() {
    Bundle saved_instance_state = new Bundle();
    saved_instance_state.putString("base_uri_", base_uri_.toString());
    saved_instance_state.putInt("level_offset_", level_offset_);
    return saved_instance_state;
  }

  private Uri base_uri_;
  private boolean[] effects_death_;
  private boolean[] effects_explode_;
  private boolean[] effects_solid_;
  private char[] background_ = {0, 0, 0};
  private GameState game_state_;
  private int level_offset_ = 0;  // Level within the base_uri_ package.
  private Paint paint_ = new Paint();
  private Random random_ = new Random();
  private Resources resources_;
  private char[] tiles_;
  private Bitmap tiles_bitmap_;

  private static final char kBaseValue = 'a';
  private static final int kEndingTile = 11;
  private static final int kExplosionSize = 15;  // Number of particles.
  private static final float kExplosionStrength = 200.0f;
  private static final int kMapHeight = 100;
  private static final int kMapWidth = 100;
  private static final int kMaxTileCount = 25;
  private static final int kStartingTile = 10;
  private static final int kTileSize = 64;
}
