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
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;
import junit.framework.Assert;


public class AvatarDatabase {
  public AvatarDatabase(Context context) {
    mDatabaseOpenHelper = new AvatarDatabaseOpenHelper(context);
  }

  void deleteValue(String key) {
    SQLiteDatabase db = mDatabaseOpenHelper.getWritableDatabase();
    db.execSQL("DELETE FROM " + kDatabaseTable + " " +
               "WHERE avatar_key = " + key);
  }

  String getStringValue(String key) {
    SQLiteDatabase db = mDatabaseOpenHelper.getReadableDatabase();
    Cursor cursor = db.rawQuery("SELECT avatar_value FROM " + kDatabaseTable +
                                " WHERE avatar_key = " + key, null);
    Assert.assertTrue(cursor.getCount() == 0 || cursor.getCount() == 1);

    String result = null;
    if (cursor.getCount() == 1) {
      result = cursor.getString(0);
    }
    cursor.close();
    return result;
  }

  void setStringValue(String key, String value) {
    SQLiteDatabase db = mDatabaseOpenHelper.getWritableDatabase();
    db.execSQL("REPLACE INTO " + kDatabaseTable + " " +
               "(avatar_key, avatar_value) VALUES " +
               "(" + key + ", " + value + ")");
  }

  private class AvatarDatabaseOpenHelper extends SQLiteOpenHelper {
    AvatarDatabaseOpenHelper(Context context) {
      super(context, kDatabaseName, null, kDatabaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL("CREATE TABLE " + kDatabaseTable + " ("
                 + "avatar_key VARCHAR(255) NOT NULL PRIMARY KEY,"
                 + "avatar_value VARCHAR(255));");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.w("AvatarDatabase", "Upgrading database from version " + oldVersion +
            " to " + newVersion + ", which will destroy all old data");
      db.execSQL("DROP TABLE IF EXISTS notes");
      onCreate(db);
    }
  }  // class AvatarDatabaseOpenHelper

  private AvatarDatabaseOpenHelper mDatabaseOpenHelper;

  private static final String kDatabaseName = "avatar.db";
  private static final int kDatabaseVersion = 1;
  private static final String kDatabaseTable = "avatar";
}