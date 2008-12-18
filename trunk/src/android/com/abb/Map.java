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
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import junit.framework.Assert;

import android.com.abb.Content;
import android.com.abb.Entity;
import android.com.abb.GameState;


public class Map {
  public float starting_x;
  public float starting_y;

  public Map(GameState game_state) {
    mGameState = game_state;
  }

  public void setUri(Uri base_uri) {
    mBaseUri = base_uri;
    mLevelOffset = 0;
  }

  public void reload() {
    if (mBaseUri != null) {
      loadFromUri(mBaseUri, mLevelOffset);
    }
  }

  public void advanceLevel() {
    mLevelOffset += 1;
  }

  public void loadFromUri(Uri base_uri) {
    loadFromUri(base_uri, 0);
  }

  public void loadFromUri(Uri base_uri, int level_offset) {
    Log.d("Map::loadFromUri", "Loading Map from " + base_uri.toString());
    mBaseUri = base_uri;
    mLevelOffset = level_offset;

    // Map data may be either stored within the embedded package zip file or on
    // disk. The Content class is used to access map data in a storage agnostic
    // way.
    //
    // Maps are organized into package where each package is its own set of
    // (ordered) levels, tiles, and tile definitions.

    // Load level tiles.
    Uri level_uri =
        Uri.withAppendedPath(mBaseUri, "level_" + mLevelOffset + ".txt");
    String level_path = Content.getTemporaryFilePath(level_uri);
    loadLevelFromFile(level_path);

    // Load level tile images.
    Uri tiles_uri =
        Uri.withAppendedPath(mBaseUri, "tiles_" + mLevelOffset + ".png");
    if (!Content.exists(tiles_uri))
      tiles_uri = Uri.withAppendedPath(mBaseUri, "tiles_default.png");
    String tiles_path = Content.getTemporaryFilePath(tiles_uri);
    loadTilesFromFile(tiles_path);

    // Load background image.
    Uri background_uri =
        Uri.withAppendedPath(mBaseUri, "background_" + mLevelOffset + ".jpg");
    if (!Content.exists(background_uri))
      background_uri = Uri.withAppendedPath(mBaseUri, "background_default.jpg");
    if (Content.exists(background_uri)) {
      String background_path = Content.getTemporaryFilePath(background_uri);
      mBackgroundBitmap = BitmapFactory.decodeFile(background_path);
    }

    // Load tile effects.
    Uri effects_uri =
        Uri.withAppendedPath(mBaseUri, "effects_" + mLevelOffset + ".txt");
    if (!Content.exists(effects_uri))
      effects_uri = Uri.withAppendedPath(mBaseUri, "effects_default.txt");
    String effects_path = Content.getTemporaryFilePath(effects_uri);
    loadEffectsFromFile(effects_path);
  }

  public void loadLevelFromFile(String file_path) {
    if (file_path == null)
      Log.e("Map::loadLevelFromFile", "Invalid null argument.");
    try {
      FileReader level_reader = new FileReader(new File(file_path));
      mTiles = new char[kMapWidth * kMapHeight];
      level_reader.read(mTiles, 0, kMapWidth * kMapHeight);
      loadLevelFromArray(decodeArray(mTiles));
    } catch (IOException ex) {
      Log.e("Map::loadLevelFromFile", "Cannot find: " + file_path, ex);
    }
  }

  public void loadTilesFromFile(String file_path) {
    if (file_path == null) {
      Log.e("Map::loadTilesFromFile", "Invalid null argument.");
    }
    mTilesBitmap = BitmapFactory.decodeFile(file_path);
    if (mTilesBitmap == null) {
      Log.e("Map::loadTilesFromFile", "Cannot find: " + file_path);
    }
  }

  public void loadEffectsFromFile(String file_path) {
    String[] effects_tokens = Content.readFileTokens(file_path);
    Assert.assertEquals("Effects file improperly formatted.",
                        effects_tokens.length % 2, 0);

    mEffectsDeath = new boolean[kMaxTileCount];
    mEffectsExplode = new boolean[kMaxTileCount];
    mEffectsSolid = new boolean[kMaxTileCount];
    for (int effect = 0; effect < effects_tokens.length; effect += 2) {
      int tile_id = Integer.parseInt(effects_tokens[effect]);
      String tile_effect = effects_tokens[effect + 1];
      if (tile_effect.equals("death"))
        mEffectsDeath[tile_id] = true;
      else if (tile_effect.equals("explode"))
        mEffectsExplode[tile_id] = true;
      else if (tile_effect.equals("solid"))
        mEffectsSolid[tile_id] = true;;
    }
  }

  static private char[] decodeArray(char[] tiles) {
    for (int n = 0; n < kMapWidth * kMapHeight; ++n) {
      tiles[n] -= kBaseValue;
    }
    return tiles;
  }

  static private char[] toPrimative(ArrayList<Character> array_list) {
    char[] result = new char[array_list.size()];
    for (int index = 0; index < array_list.size(); ++index) {
      result[index] = array_list.get(index).charValue();
    }
    return result;
  }

  private void loadLevelFromArray(char[] tiles) {
    mTiles = tiles;
    for (int n = 0; n < kMapWidth * kMapHeight; ++n) {
      if (mTiles[n] == kStartingTile) {
        starting_x = (n / kMapWidth) * kTileSize;
        starting_y = (n % kMapWidth) * kTileSize;
      }
    }
  }

  public void setTileAt(float x, float y, char tile_id) {
    int index_x = (int)(x / kTileSize + 0.5f);
    int index_y = (int)(y / kTileSize + 0.5f);
    if (index_x < 0 || index_y < 0 ||
        index_x >= kMapWidth || index_y >= kMapHeight) {
      return;  // Tile out of map range.
    }
    int tile_index = kMapWidth * index_x + index_y;
    mTiles[tile_index] = tile_id;
  }

  public int tileAt(float x, float y) {
    int index_x = (int)(x / kTileSize + 0.5f);
    int index_y = (int)(y / kTileSize + 0.5f);
    if (index_x < 0 || index_y < 0 ||
        index_x >= kMapWidth || index_y >= kMapHeight) {
      return -1;  // Tile out of map range.
    }
    int tile_index = kMapWidth * index_x + index_y;
    return mTiles[tile_index];
  }

  public static boolean tileIsEnemy(int tile_id) {
    return (tile_id == 12);
  }

  public static boolean tileIsGoal(int tile_id) {
    return (tile_id == kEndingTile);
  }

  public void collideEntity(Entity entity) {
    if (entity.radius <= 0) {
      return;  // Collision disabled for this entity.
    }

    entity.has_ground_contact = false;

    // Iterate through map tiles potentially intersecting the entity. The
    // collision model used for entities and tiles are squares.
    float half_tile_size = kTileSize / 2.0f;
    float radius = Math.max(entity.radius, half_tile_size);
    for (float x = entity.x - radius; x <= entity.x + radius; x += kTileSize) {
      for (float y = entity.y - radius; y <= entity.y + radius; y += kTileSize) {
        int tile_id = tileAt(x, y);
        boolean tile_deadly = mEffectsDeath[tile_id];
        boolean tile_exploadable = mEffectsExplode[tile_id];
        boolean tile_solid = mEffectsSolid[tile_id];

        int index_x = (int)(x / kTileSize + 0.5f);
        int index_y = (int)(y / kTileSize + 0.5f);
        float tile_x = kTileSize * index_x;
        float tile_y = kTileSize * index_y;

        if (!tile_solid && !tile_exploadable && !tile_deadly) {
          continue;  // Not a collideable tile.
        }

        // Determine if a collision has occurred between the two squares.
        float distance_x = entity.x - tile_x;
        float distance_y = entity.y - tile_y;
        if (Math.abs(distance_x) > half_tile_size + entity.radius ||
            Math.abs(distance_y) > half_tile_size + entity.radius) {
          continue;  // No collision with this tile
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
          setTileAt(x, y, (char)0);  // Clear the exploding tile.
          mGameState.vibrate();
          for (int n = 0; n < kExplosionSize; n++) {
            float random_angle = mRandom.nextFloat() * 2.0f * (float)Math.PI;
            float random_magnitude =
                kExplosionStrength * mRandom.nextFloat() / 3.0f;
            mGameState.createFireProjectile(
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
  public void draw(Graphics graphics, float center_x, float center_y,
                   float zoom) {
    // Load the images if it hasn't been done already.
    if (mTilesBitmap != null) {
      graphics.freeImage(mTilesImage);
      mTilesImage = graphics.loadImageFromBitmap(mTilesBitmap);
      mTilesBitmap = null;
    }
    if (mBackgroundBitmap != null) {
      graphics.freeImage(mBackgroundImage);
      mBackgroundImage = graphics.loadImageFromBitmap(mBackgroundBitmap);
      mBackgroundBitmap = null;
    }

    // Draw background.
    int canvas_width = graphics.getWidth();
    int canvas_height = graphics.getHeight();
    if (mBackgroundImage != -1) {
      float scaled_background_size = kBackgroundScale * kBackgroundSize;
      Rect background_src = new Rect(0, 0, kBackgroundSize, kBackgroundSize);
      RectF background_dst = new RectF(
          0.0f, 0.0f, scaled_background_size, scaled_background_size);
      background_dst.offset(-center_x / (kTileSize * kMapWidth) *
                            (scaled_background_size - canvas_width),
                            -center_y / (kTileSize * kMapHeight) *
                            (scaled_background_size - canvas_height));
      graphics.drawImage(
          mBackgroundImage, background_src, background_dst, false, false);
    }

    // Draw tiles.
    int half_canvas_width = canvas_width / 2;
    int half_canvas_height = canvas_height / 2;
    float x_min = center_x - half_canvas_width / zoom;
    float x_max = center_x + (half_canvas_width + kTileSize) / zoom;
    float y_min = center_y - half_canvas_height / zoom;
    float y_max = center_y + (half_canvas_height + kTileSize) / zoom;
    for (float x = x_min; x < x_max; x += kTileSize) {
      for (float y = y_min; y < y_max; y += kTileSize) {
        // Determine tile id for this world position x, y.
        int tile_id = tileAt(x, y);

        // Spawn enemies if we happen to pass over an enemy tile.
        if (tileIsEnemy(tile_id)) {
          mGameState.createEnemy(x, y);
          setTileAt(x, y, (char)0);  // Clear the tile.
        }

        if (tile_id == 0 || tile_id == 12) {
          continue;  // Not a visual tile.
        }

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
        graphics.drawImage(
            mTilesImage, tile_source, tile_destination, false, false);
      }
    }
  }

  public void loadStateBundle(Bundle saved_instance_state) {
    mBaseUri = Uri.parse(saved_instance_state.getString("mBaseUri"));
    mLevelOffset = saved_instance_state.getInt("mLevelOffset");
    loadFromUri(mBaseUri, mLevelOffset);
  }

  public Bundle saveStateBundle() {
    Bundle saved_instance_state = new Bundle();
    saved_instance_state.putString("mBaseUri", mBaseUri.toString());
    saved_instance_state.putInt("mLevelOffset", mLevelOffset);
    return saved_instance_state;
  }

  private Bitmap mBackgroundBitmap;
  private int mBackgroundImage = -1;
  private Uri mBaseUri;
  private boolean[] mEffectsDeath;
  private boolean[] mEffectsExplode;
  private boolean[] mEffectsSolid;
  private GameState mGameState;
  private int mLevelOffset = 0;  // Level within the mBaseUri package.
  private Random mRandom = new Random();
  private char[] mTiles;
  private Bitmap mTilesBitmap;
  private int mTilesImage = -1;

  private static final float kBackgroundScale = 2.0f;
  private static final int kBackgroundSize = 512;
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
