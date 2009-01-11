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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import java.lang.Math;
import java.lang.System;
import java.lang.Thread;
import junit.framework.Assert;


public class GameView extends SurfaceView implements SurfaceHolder.Callback {
  class GameThread extends Thread {
    public GameThread(SurfaceHolder surface_holder) {
      mSurfaceHolder = surface_holder;
    }

    @Override
    public void run() {
      synchronized (this) {
        while (mGame == null && mRunning) {
          try {
            wait();  // Sleep thread until notification.
          } catch (java.lang.InterruptedException ex) {
            continue;
          }
        }

        Assert.assertEquals(
            "GameView thread must only be run once.", mGraphics, null);
        mGraphics = new Graphics();
        mGraphics.initialize(mSurfaceHolder);
        mGame.initializeGraphics(mGraphics);
      }

      // Since our target platform is a mobile device, we should do what we can
      // to save power. In the case of a game like this, we should 1) limit the
      // framerate to something "reasonable" and 2) pause the updates as much as
      // possible. Here we define the maximum framerate which needs to make the
      // trade off between graphics fluidity and power savings.
      final float kMaxFrameRate = 30.0f;  // Frames / second.
      final float kMinFrameRate = 6.0f;   // Frames / second.
      final float kMinTimeStep = 1.0f / kMaxFrameRate;  // Seconds.
      final float kMaxTimeStep = 1.0f / kMinFrameRate;  // Seconds.

      // The timers available through the Java APIs appear sketchy in general.
      // The following resource was useful:
      // http://blogs.sun.com/dholmes/entry/inside_the_hotspot_vm_clocks
      long time = System.nanoTime();

      while (mRunning) {
        synchronized (this) {
          while (mPaused && mRunning) {
            try {
              wait();  // Sleep thread until notification.
            } catch (java.lang.InterruptedException ex) {
              continue;
            }
          }
        }

        // Calculate the interval between this and the previous frame. See note
        // above regarding system timers. If we have exceeded our framerate
        // budget, sleep.
        long current_time = System.nanoTime();
        float time_step = (float)(current_time - time) * 1.0e-9f;
        time = current_time;
        if (time_step < kMinTimeStep) {
          float remaining_time = kMinTimeStep - time_step;
          time_step = kMinTimeStep;
          try {
            long sleep_milliseconds = (long)(remaining_time * 1.0e3f);
            remaining_time -= sleep_milliseconds * 1.0e-3f;
            int sleep_nanoseconds = (int)(remaining_time * 1.0e9f);
            Thread.sleep(sleep_milliseconds, sleep_nanoseconds);
          } catch (InterruptedException ex) {
            // If someone has notified this thread, just forget about it and
            // continue on. It's not worth the cycles to handle.
          }
        } else {
          // In the case where the thread took too long, let the thread yield to
          // other processes. This should usually only happen in the case
          // something "big" is happening and we don't need / want to starve the
          // more important system threads.
          yield();
        }
        time_step = Math.max(time_step, kMinTimeStep);
        time_step = Math.min(time_step, kMaxTimeStep);

        Canvas canvas = null;
        try {
          synchronized (this) {
            mGraphics.beginFrame();
            mGame.onFrame(mGraphics, time_step);
          }
        } finally {
          mGraphics.endFrame();
        }
      }
      mGraphics.destroy();
    }

    synchronized public void setGame(Game game) {
      mGame = game;
      notifyAll();
    }

    synchronized public void pause(boolean pause) {
      mPaused = pause;
      notifyAll();
    }

    synchronized public void surfaceChanged(SurfaceHolder surface_holder,
                                            int width, int height) {
      if (mGraphics != null) {
        mGraphics.surfaceChanged(surface_holder, width, height);
      }
    }

    synchronized public void halt() {
      mRunning = false;
      notifyAll();
    }

    boolean mRunning = true;
    boolean mPaused = false;
    Game mGame;
    Graphics mGraphics;
    SurfaceHolder mSurfaceHolder;
  }

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    getHolder().addCallback(this);
    getHolder().setType(SurfaceHolder.SURFACE_TYPE_GPU);
  }

  public void setGame(Game game) {
    mGame = game;
    if (mGameThread != null) {
      mGameThread.setGame(game);
    }
  }

  /** Set up the android widget for the title screen to be displayed until any
   * key is pressed. */
  public void setTitleView(TextView title_view) {
    mTitleView = title_view;
  }

  /** Standard override to get key-press events. */
  @Override
  public boolean onKeyDown(int key_code, KeyEvent msg) {
    if (!mTitleViewHidden) {
      mTitleView.setText("");
      mTitleViewHidden = true;
    }

    if (key_code == kProfileKey) {
      if (!mProfiling) {
        Debug.startMethodTracing(kProfilePath);
        mProfiling = true;
      } else {
        Debug.stopMethodTracing();
        mProfiling = false;
      }
    }

    synchronized (mGame) {
      return mGame.onKeyDown(key_code);
    }
  }

  /** Standard override for key-up. We actually care about these, so we can turn
   * off the engine or stop rotating. */
  @Override
  public boolean onKeyUp(int key_code, KeyEvent msg) {
    synchronized (mGame) {
      return mGame.onKeyUp(key_code);
    }
  }

  /** Standard window-focus override. Notice focus lost so we can pause on focus
   * lost. e.g. user switches to take a call. */
  @Override
  public void onWindowFocusChanged(boolean window_has_focus) {
    super.onWindowFocusChanged(window_has_focus);
    if (mGameThread != null) {
      mGameThread.pause(!window_has_focus);
    }
  }

  /** Callback invoked when the Surface has been created and is ready to be
   * used. */
  public void surfaceCreated(SurfaceHolder holder) {
    // Make sure we get key events and register our interest in hearing about
    // surface changes.
    setFocusable(true);
    getHolder().addCallback(this);
    getHolder().setType(SurfaceHolder.SURFACE_TYPE_GPU);

    mGameThread = new GameThread(holder);
    mGameThread.setGame(mGame);
    mGameThread.start();
    mGameThreadStarted = true;
  }

  /** Callback invoked when the surface dimensions change. */
  public void surfaceChanged(SurfaceHolder surface_holder, int format,
                             int width, int height) {
    mGameThread.surfaceChanged(surface_holder, width, height);
  }

  /** Callback invoked when the Surface has been destroyed and must no longer be
   * touched. */
  public void surfaceDestroyed(SurfaceHolder holder) {
    boolean retry = true;
    mGameThread.halt();
    while (retry) {
      try {
        mGameThread.join();
        mGameThread = null;
        retry = false;
      } catch (InterruptedException e) {}
    }
  }

  private Game mGame;
  private GameThread mGameThread;
  private boolean mGameThreadStarted = false;
  private boolean mProfiling = false;
  private TextView mTitleView;
  private boolean mTitleViewHidden = false;

  private static final int kProfileKey = KeyEvent.KEYCODE_T;
  private static final String kProfilePath = "abb.trace";
}
