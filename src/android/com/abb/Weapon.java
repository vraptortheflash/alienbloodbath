// Copyright 2008 and onwards Matthew Barnes.
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
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;
import java.lang.Math;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import junit.framework.Assert;


public class Weapon extends Entity {
  public Weapon(GameState game_state) {
    super();
    //loadWeapon();
    loadWeaponImages();
    game_state_ = game_state;
    sprite_rect = new Rect(0, 0, kSpriteSize,  kSpriteSize);
  }

  public void loadWeapon() {
    //Load weapons.txt
    Uri weapons_uri = Uri.parse("content:///weapons.txt");
    String weapons_path = Content.getTemporaryFilePath(weapons_uri);
    if (weapons_path == null)
      Log.e("Weapon::loadWeaponsFromFile", "Invalid null argument.");
    try {
      StringBuffer file_data = new StringBuffer(1000);
      BufferedReader reader = new BufferedReader(new FileReader(weapons_path));
      char[] buf = new char[1024];
      int num_read = 0;
      while((num_read = reader.read(buf)) != -1){
        file_data.append(buf, 0, num_read);
      }
      reader.close();
      mWeaponString = file_data.toString();
      Log.d("Weapon String", mWeaponString);
    } catch (IOException ex) {
      Assert.fail("Weapon::loadWeaponFromFile. " +
                  "Cannot find:  " + weapons_path + ".");
    }
  }

  public void loadWeaponImages() {
    //Load weapons.png from URI
    Uri weapon_images_uri = Uri.parse("content:///weapons.png");
    String weapon_images_path = Content.getTemporaryFilePath(weapon_images_uri);
    Log.d("Weapons Path", weapon_images_path);

    //Load the weapons.png into Bitmap
    if (weapon_images_path == null)
      Assert.fail("Weapon::loadWeaponImagesFromFile. Invalid argument.");
    mWeaponsBitmap = BitmapFactory.decodeFile(weapon_images_path);
    if (mWeaponsBitmap == null)
    Assert.fail("Weapon::loadWeaponImagesFromFile. " +
                "Cannot find:  " + weapon_images_path + ".");
  }

  public void setImage(Graphics graphics) {
    // Load the image if it hasn't been done already.
    if (mWeaponsBitmap != null) {
      graphics.freeImage(sprite_image);
      sprite_image = graphics.loadImageFromBitmap(mWeaponsBitmap);
      mWeaponsBitmap = null;
    }
  }

  public void setSprite(boolean facing_left) {
    sprite_rect.top = kSpriteSize * mWeaponIndex;
    sprite_rect.bottom = kSpriteSize * mWeaponIndex + kSpriteSize;
    sprite_flipped_horizontal = facing_left;
  }

  private GameState game_state_;

  private String mWeaponString;
  private Bitmap mWeaponsBitmap;

  private int mWeaponIndex = 0;
  private float shotDelay = 0.2f;  // Seconds between shots.
  private float shotDistance = 25.0f;
  private float shotOffsetX = 23.0f;
  private float shotOffsetY = -8.0f;
  private float shotSpread = 15.0f * (float)Math.PI / 180.0f;
  private float shotVelocity = 60.0f;
  private static final int kSpriteSize = 64;
}
