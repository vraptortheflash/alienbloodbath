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

import android.com.abb.Game;
import android.com.abb.Graphics;


public class GameView extends SurfaceView implements SurfaceHolder.Callback {
  class GameThread extends Thread {
    public GameThread(SurfaceHolder surface_holder) {
      surface_holder_ = surface_holder;
    }

    @Override
    public void run() {
      while (game_ == null) {
        try {
          Thread.sleep(100);  // Wait 100ms.
        } catch (InterruptedException ex) {}
        continue;
      }

      synchronized (game_) {
        graphics_  = new Graphics();
        graphics_.Initialize(surface_holder_);
        game_.InitializeGraphics(graphics_);
      }

      // Since our target platform is a mobile device, we should do what we can
      // to save power. In the case of a game like this, we should 1) limit the
      // framerate to something "reasonable" and 2) pause the updates as much as
      // possible. Here we define the maximum framerate which needs to make the
      // trade off between graphics fluidity and power savings.
      final float kMaxFrameRate = 15.0f;  // Frames / second.
      final float kMinFrameRate = 4.0f;   // Frames / second.
      final float kMinTimeStep = 1.0f / kMaxFrameRate;  // Seconds.
      final float kMaxTimeStep = 1.0f / kMinFrameRate;  // Seconds.

      // The timers available through the Java APIs appear sketchy in general. I
      // was able to find the following resource useful:
      // http://blogs.sun.com/dholmes/entry/inside_the_hotspot_vm_clocks
      long time = System.nanoTime();

      while (running_) {
        if (paused_) {
          try {
            Thread.sleep(100);  // Wait 100ms.
          } catch (InterruptedException ex) {}
          continue;
        }

        // Calculate the interval between this and the previous frame. See note
        // above regarding system timers. If we have exceeded our framerate
        // budget, sleep. TODO: Look into the cost of these clocks.
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
        }
        time_step = Math.max(time_step, kMinTimeStep);
        time_step = Math.min(time_step, kMaxTimeStep);

        Canvas canvas = null;
        try {
          synchronized (game_) {
            graphics_.BeginFrame();
            game_.OnFrame(graphics_, time_step);
          }
        } finally {
          graphics_.EndFrame();
        }
      }
    }

    public void SetGame(Game game) {
      game_ = game;
    }

    public void Pause(boolean pause) {
      paused_ = pause;
    }

    public void SurfaceChanged(SurfaceHolder surface_holder,
                               int width, int height) {
      if (graphics_ != null)
        graphics_.SurfaceChanged(surface_holder, width, height);
    }

    public void Halt() {
      running_ = false;
    }

    boolean running_ = true;
    boolean paused_ = false;
    Game game_;
    Graphics graphics_;
    SurfaceHolder surface_holder_;
  }

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    getHolder().addCallback(this);
    getHolder().setType(SurfaceHolder.SURFACE_TYPE_GPU);
  }

  public void SetGame(Game game) {
    game_ = game;
    if (game_thread_ != null)
      game_thread_.SetGame(game);
  }

  /** Set up the android widget for the title screen to be displayed until any
   * key is pressed. */
  public void SetTitleView(TextView title_view) {
    title_view_ = title_view;
  }

  /** Standard override to get key-press events. */
  @Override
  public boolean onKeyDown(int key_code, KeyEvent msg) {
    if (!title_view_hidden_) {
      title_view_.setText("");
      title_view_hidden_ = true;
    }

    synchronized (game_) {
      return game_.OnKeyDown(key_code);
    }
  }

  /** Standard override for key-up. We actually care about these, so we can turn
   * off the engine or stop rotating. */
  @Override
  public boolean onKeyUp(int key_code, KeyEvent msg) {
    synchronized (game_) {
      return game_.OnKeyUp(key_code);
    }
  }

  /** Standard window-focus override. Notice focus lost so we can pause on focus
   * lost. e.g. user switches to take a call. */
  @Override
  public void onWindowFocusChanged(boolean window_has_focus) {
    super.onWindowFocusChanged(window_has_focus);
    if (game_thread_ != null)
      game_thread_.Pause(!window_has_focus);
  }

  /** Callback invoked when the Surface has been created and is ready to be
   * used. */
  public void surfaceCreated(SurfaceHolder holder) {
    // Make sure we get key events and register our interest in hearing about
    // surface changes.
    setFocusable(true);
    getHolder().addCallback(this);
    getHolder().setType(SurfaceHolder.SURFACE_TYPE_GPU);

    game_thread_ = new GameThread(holder);
    game_thread_.SetGame(game_);
    game_thread_.start();
    game_thread_started_ = true;
  }

  /** Callback invoked when the surface dimensions change. */
  public void surfaceChanged(SurfaceHolder surface_holder, int format,
                             int width, int height) {
    game_thread_.SurfaceChanged(surface_holder, width, height);
  }

  /** Callback invoked when the Surface has been destroyed and must no longer be
   * touched. */
  public void surfaceDestroyed(SurfaceHolder holder) {
    boolean retry = true;
    game_thread_.Halt();
    while (retry) {
      try {
        game_thread_.join();
        game_thread_ = null;
        retry = false;
      } catch (InterruptedException e) {}
    }
  }

  private Game game_;
  private GameThread game_thread_;
  private boolean game_thread_started_ = false;
  private TextView title_view_;
  private boolean title_view_hidden_ = false;
}
