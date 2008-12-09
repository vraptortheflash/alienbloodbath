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

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLDebugHelper;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Vector;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;  // For EGL_CONTEXT_LOST constant.
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11ExtensionPack;

/** Graphics API designed for ABB. The goal is to support both OpenGL and
 * Android 2D rendering targets efficiently through the same interface. */
public class Graphics {
  /**
   * Public API.
   */

  /** Initialize the graphics sub-system letting the rendering back end target
   * be automatically chosen from the system configuration. */
  public void Initialize(SurfaceHolder surface_holder) {
    DetermineBackendType();

    Rect surface_frame = surface_holder.getSurfaceFrame();
    SurfaceChanged(
        surface_holder, surface_frame.width(), surface_frame.height());

    switch (backend_type_) {
      case ANDROID2D:
        Log.d("Graphics::Initialize", "Initializing Android2D rendering.");
        InitializeAndroid2D();
        break;
      case OPENGL:
        Log.d("Graphics::Initialize", "Initializing OpenGL rendering.");
        InitializeOpenGL();
        break;
    }
  }

  public void SurfaceChanged(SurfaceHolder surface_holder,
                             int width, int height) {
    surface_holder_ = surface_holder;
    surface_width_ = width;
    surface_height_ = height;
    Log.d("Graphics::SurfaceChanged",
          "Size = " + surface_width_ + "x" + surface_height_ + ".");

    switch (backend_type_) {
      case ANDROID2D:
        break;  // Don't care.
      case OPENGL:
        gl_surface_initialized_ = false;
        break;
    }
  }

  public void Destroy() {
    switch (backend_type_) {
      case ANDROID2D:
        // TODO(burkhart): Implement.
        break;
      case OPENGL:
        DestroyOpenGL();
        break;
    }
  }

  public int GetWidth() {
    switch (backend_type_) {
      case ANDROID2D:
        return GetWidthAndroid2D();
      case OPENGL:
        return GetWidthOpenGL();
    }
    return 0;
  }

  public int GetHeight() {
    switch (backend_type_) {
      case ANDROID2D:
        return GetHeightAndroid2D();
      case OPENGL:
        return GetHeightOpenGL();
    }
    return 0;
  }

  /** Load an image from a bitmap and return its handle. For OpenGL
   * compatibility texture dimensions must be powers of 2. Additionally,
   * hardware usually limits texture sizes to 1024 pixels. */
  public int LoadImageFromBitmap(Bitmap bitmap) {
    switch (backend_type_) {
      case ANDROID2D:
        return LoadImageFromBitmapAndroid2D(bitmap);
      case OPENGL:
        return LoadImageFromBitmapOpenGL(bitmap);
    }
    return -1;
  }

  public void FreeImage(int image_handle) {
    switch (backend_type_) {
      case ANDROID2D:
        FreeImageAndroid2D(image_handle);
        break;
      case OPENGL:
        FreeImageOpenGL(image_handle);
        break;
    }
  }

  public void DrawImage(int image_handle, Rect source_rect, RectF dest_rect,
                        boolean flipped_horizontal) {
    switch (backend_type_) {
      case ANDROID2D:
        DrawImageAndroid2D(image_handle, source_rect, dest_rect,
                           flipped_horizontal);
        break;
      case OPENGL:
        DrawImageOpenGL(image_handle, source_rect, dest_rect,
                        flipped_horizontal);
        break;
    }
  }

  public void BeginFrame() {
   switch (backend_type_) {
      case ANDROID2D:
        BeginFrameAndroid2D();
        break;
      case OPENGL:
        BeginFrameOpenGL();
        break;
    }
  }

  public void EndFrame() {
    switch (backend_type_) {
      case ANDROID2D:
        EndFrameAndroid2D();
        break;
      case OPENGL:
        EndFrameOpenGL();
        break;
    }
  }

  /**
   * Private shared methods  and state.
   */

  void DetermineBackendType() {
    // Determine which rendering backend to use based off of the system setup,
    // specifically the presence of rendering hardware. OpenGL appears to be
    // faster on both the Emulator and the HTC Dream handset than the Android2D
    // back end.
    backend_type_ = BackendType.OPENGL;
  }

  private enum BackendType { ANDROID2D, OPENGL }
  private BackendType backend_type_;
  private SurfaceHolder surface_holder_;
  private int surface_height_;
  private int surface_width_;

  /**
   * Private Android 2D backend methods and state.
   */

  private void InitializeAndroid2D() {
    surface_holder_.setType(SurfaceHolder.SURFACE_TYPE_HARDWARE);
  }

  private int LoadImageFromBitmapAndroid2D(Bitmap bitmap) {
    images_android2D_.add(bitmap);
    return images_android2D_.size();
  }

  private void FreeImageAndroid2D(int image_handle) {
    if (image_handle >= 1)
      images_android2D_.set(image_handle - 1, null);
  }

  private int GetWidthAndroid2D() {
    return surface_width_;
  }

  private int GetHeightAndroid2D() {
    return surface_height_;
  }

  private void BeginFrameAndroid2D() {
    canvas_android2D_ = surface_holder_.lockCanvas(null);
    canvas_android2D_.drawRGB(0, 0, 0);
  }

  private void EndFrameAndroid2D() {
    surface_holder_.unlockCanvasAndPost(canvas_android2D_);
  }

  private void DrawImageAndroid2D(int image_handle,
                                  Rect source_rect, RectF dest_rect,
                                  boolean flipped_horizontal) {
    if (image_handle < 1) {
      Log.d("Graphics::DrawImageAndroid2D",
            "Invalid image handle encountered.");
      return;  // Uninitialized / invalid image handle.
    }
    if (flipped_horizontal) {
      transformation_android2D_.setScale(-1.0f, 1.0f);
      transformation_android2D_.postTranslate(
          2.0f * dest_rect.left + dest_rect.width(), 0.0f);
    } else {
      transformation_android2D_.setScale(1.0f, 1.0f);
    }

    Bitmap bitmap = images_android2D_.get(image_handle - 1);
    canvas_android2D_.setMatrix(transformation_android2D_);
    canvas_android2D_.drawBitmap(
        bitmap, source_rect, dest_rect, paint_android2D_);
  }

  private Canvas canvas_android2D_;
  private ArrayList<Bitmap> images_android2D_ = new ArrayList<Bitmap>();
  private Paint paint_android2D_ = new Paint();
  private Matrix transformation_android2D_ = new Matrix();

  /**
   * Private OpenGL backend methods and state.
   */

  private void InitializeOpenGL() {
    // Note that OpenGL ES documentation may be found at:
    // http://java.sun.com/javame/reference/apis/jsr239/javax/microedition
    //                                         /khronos/opengles/GL10.html

    egl_ = (EGL10)EGLContext.getEGL();
    egl_display_ = egl_.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

    // We can now initialize EGL for that display.
    int[] version = new int[2];
    egl_.eglInitialize(egl_display_, version);
    Log.d("Graphics::InitializeOpenGL",
          "Found version: " + version[0] + "." + version[1]);

    int attrib_list[] = {
      EGL10.EGL_RED_SIZE, 0,    // 0 indicates default bit depth.
      EGL10.EGL_GREEN_SIZE, 0,
      EGL10.EGL_BLUE_SIZE, 0,
      EGL10.EGL_NONE
    };

    EGLConfig[] configs = new EGLConfig[1];
    int[] num_config = new int[1];
    egl_.eglChooseConfig(egl_display_, attrib_list, configs, 1, num_config);
    egl_config_ = configs[0];
    egl_context_ = egl_.eglCreateContext(
        egl_display_, egl_config_, EGL10.EGL_NO_CONTEXT, null);
    gl_ = (GL10)egl_context_.getGL();

    final boolean kEnableOpenGLDebugging = false;
    if (kEnableOpenGLDebugging) {
      int debugging_flags = (GLDebugHelper.CONFIG_LOG_ARGUMENT_NAMES |
                             GLDebugHelper.CONFIG_CHECK_THREAD |
                             GLDebugHelper.CONFIG_CHECK_GL_ERROR);
      egl_ = (EGL10)GLDebugHelper.wrap(
          egl_, debugging_flags, new PrintWriter(System.out));
      gl_ = (GL10)GLDebugHelper.wrap(
          gl_, debugging_flags, new PrintWriter(System.out));
    }

    // Create a place holder surface so we can continue with initialization.
    // When the framework gives us a real surface / surface dimensions, it will
    // be recreated.
    InitializeOpenGLSurface();
  }

  /** Create a new rendering surface. This is indented to be called whenever the
   * window size changes, for example. */
  private void InitializeOpenGLSurface() {
    Log.d("Graphics::InitializeOpenGLSurface", "Creating OpenGL surface.");

    surface_holder_.setType(SurfaceHolder.SURFACE_TYPE_GPU);

    if (egl_surface_ != null) {
      // Unbind and destroy the old EGL surface, if there is one.
      egl_.eglMakeCurrent(egl_display_,
                          EGL10.EGL_NO_SURFACE,
                          EGL10.EGL_NO_SURFACE,
                          EGL10.EGL_NO_CONTEXT);
      egl_.eglDestroySurface(egl_display_, egl_surface_);
    }

    // Create an EGL surface we can render into.
    egl_surface_ = egl_.eglCreateWindowSurface(
        egl_display_, egl_config_, surface_holder_, null);

    // Before we can issue GL commands, we need to make sure the context is
    // current and bound to a surface.
    egl_.eglMakeCurrent(egl_display_, egl_surface_, egl_surface_, egl_context_);

    InitializeOpenGLClientState();
  }

  private void InitializeOpenGLClientState() {
    Log.d("Graphics::InitializeOpenGLClientState", "Setting up client state.");

    // Initialize the orthographic projection within our surface. This must
    // happen whenever the surface size changes.
    gl_.glViewport(0, 0, GetWidthOpenGL(), GetHeightOpenGL());
    gl_.glMatrixMode(GL10.GL_PROJECTION);
    gl_.glLoadIdentity();
    gl_.glOrthof(0, GetWidthOpenGL(), 0, GetHeightOpenGL(), -1, 1);

    // Since we will only be rendering quads, set up a shared vertex and texture
    // coordinate array. The following is so convoluted I really wonder if this
    // is right of if the Java / OpenGL ES folks need their heads examined.
    float[] corner_array = { 0, 0,  1, 0,  1, 1, 0, 0,  1, 1,  0, 1 };
    ByteBuffer corner_byte_buffer =
        ByteBuffer.allocateDirect(4 * corner_array.length);
    corner_byte_buffer.order(ByteOrder.nativeOrder());
    FloatBuffer corner_float_buffer = corner_byte_buffer.asFloatBuffer();
    corner_float_buffer.put(corner_array);
    corner_float_buffer.position(0);

    gl_.glVertexPointer(2, GL10.GL_FLOAT, 0, corner_float_buffer);
    gl_.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl_.glTexCoordPointer(2, GL10.GL_FLOAT, 0, corner_float_buffer);
    gl_.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

    // OpenGL rendering state configuration.
    gl_.glEnable(GL10.GL_TEXTURE_2D);
    gl_.glDisable(GL10.GL_CULL_FACE);
    gl_.glDisable(GL10.GL_DEPTH_TEST);
    gl_.glEnable(GL10.GL_BLEND);
    gl_.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
    gl_.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);  // Black with full alpha.
  }

  private void DestroyOpenGL() {
    if (egl_surface_ != null) {
      egl_.eglMakeCurrent(egl_display_,
                          EGL10.EGL_NO_SURFACE,
                          EGL10.EGL_NO_SURFACE,
                          EGL10.EGL_NO_CONTEXT);
      egl_.eglDestroySurface(egl_display_, egl_surface_);
      egl_surface_ = null;
    }
    if (egl_context_ != null) {
      egl_.eglDestroyContext(egl_display_, egl_context_);
      egl_context_ = null;
    }
    if (egl_display_ != null) {
      egl_.eglTerminate(egl_display_);
      egl_display_ = null;
    }
  }

  private int LoadImageFromBitmapOpenGL(Bitmap bitmap) {
    if (bitmap == null)
      Log.d("Graphics::LoadImageFromBitmapOpenGL", "Null bitmap provided");

    // Allocate a texture handle within the OpenGL context.
    int[] texture_names = new int[1];
    gl_.glGenTextures(1, texture_names, 0);
    int texture_name = texture_names[0];
    Log.d("Graphics::LoadImageFromBitmapOpenGL",
          "Allocated texture handle: " + texture_name);

    // Bind the pixel data to the texture handle. The Android Bitmap class gives
    // us data in an ARGB pixel format, but we must convert this to an RGBA
    // pixel format for OpenGL. Additionally, we need to flip the byte ordering.
    int[] bitmap_data = new int[bitmap.getWidth() * bitmap.getHeight()];
    bitmap.getPixels(bitmap_data, 0, bitmap.getWidth(), 0, 0,
                     bitmap.getWidth(), bitmap.getHeight());
    for (int n = 0; n < bitmap.getWidth() * bitmap.getHeight(); ++n) {
      int pixel = bitmap_data[n];
      bitmap_data[n] = (((0xFF000000 & pixel)) |        // Alpha.
                        ((0x00FF0000 & pixel) >> 16) |  // Red.
                        ((0x0000FF00 & pixel)) |        // Green.
                        ((0x000000FF & pixel) << 16));  // Blue.
    }
    IntBuffer bitmap_data_buffer = IntBuffer.wrap(bitmap_data);
    gl_.glBindTexture(GL10.GL_TEXTURE_2D, texture_name);
    gl_.glTexImage2D(GL10.GL_TEXTURE_2D,
                     0,                      // Mipmap level.
                     GL10.GL_RGBA,           // Internal format.
                     bitmap.getWidth(),
                     bitmap.getHeight(),
                     0,                      // Border.
                     GL10.GL_RGBA,           // Format.
                     GL10.GL_UNSIGNED_BYTE,
                     bitmap_data_buffer);
    gl_.glTexParameterf(GL10.GL_TEXTURE_2D,
                       GL10.GL_TEXTURE_MIN_FILTER,
                       GL10.GL_NEAREST);
    gl_.glTexParameterf(GL10.GL_TEXTURE_2D,
                       GL10.GL_TEXTURE_MAG_FILTER,
                       GL10.GL_NEAREST);

    // The size must be manually stored for retrieval during the rendering
    // process since the texture coordinate scheme under OpenGL is normalized
    // where as under the Android2D back end, texture coordinates are absolute.
    if (texture_widths_.size() <= texture_name)
      texture_widths_.setSize(texture_name + 1);
    if (texture_heights_.size() <= texture_name)
      texture_heights_.setSize(texture_name + 1);
    texture_widths_.set(texture_name, bitmap.getWidth());
    texture_heights_.set(texture_name, bitmap.getHeight());
    return texture_name;
  }

  private void FreeImageOpenGL(int image_handle) {
    // TODO(burkhart): Implement.
  }

  private int GetWidthOpenGL() {
    return surface_width_;
  }

  private int GetHeightOpenGL() {
    return surface_height_;
  }

  private void DrawImageOpenGL(int image_handle,
                               Rect source_rect, RectF dest_rect,
                               boolean flipped_horizontal) {
    if (image_handle != current_texture_) {
      current_texture_ = image_handle;
      gl_.glBindTexture(GL10.GL_TEXTURE_2D, image_handle);
    }

    // The vertex and texture coordinate arrays have already been initialized.
    // All that is left is to set up the texture and model view transformation
    // matrices and render. Note that the OpenGL API expects matrices with a
    // column-major layout.
    float texture_width = texture_widths_.get(image_handle);
    float texture_height = texture_heights_.get(image_handle);
    matrix_[0] = (source_rect.right - source_rect.left) / texture_width;
    matrix_[5] = (source_rect.bottom - source_rect.top) / texture_height;
    matrix_[12] = source_rect.left / texture_width;
    matrix_[13] = source_rect.top / texture_height;
    if (flipped_horizontal) {
      matrix_[0] *= -1.0f;
      matrix_[12] = 1.0f - matrix_[12];
    }
    gl_.glMatrixMode(GL10.GL_TEXTURE);
    gl_.glLoadMatrixf(matrix_, 0);

    matrix_[0] = dest_rect.right - dest_rect.left;
    matrix_[5] = dest_rect.top - dest_rect.bottom;
    matrix_[12] = dest_rect.left;
    matrix_[13] = surface_height_ - dest_rect.top;
    gl_.glMatrixMode(GL10.GL_MODELVIEW);
    gl_.glLoadMatrixf(matrix_, 0);

    gl_.glDrawArrays(GL10.GL_TRIANGLES, 0, 6);
  }

  private void BeginFrameOpenGL() {
    if (!gl_surface_initialized_) {
      InitializeOpenGLSurface();
      gl_surface_initialized_ = true;
    }
    if (!gl_state_initialized_) {
      InitializeOpenGLClientState();
      gl_state_initialized_ = true;
    }
    gl_.glClear(GL10.GL_COLOR_BUFFER_BIT);
  }

  private void EndFrameOpenGL() {
    egl_.eglSwapBuffers(egl_display_, egl_surface_);

    // Always check for EGL_CONTEXT_LOST, which means the context and all
    // associated data were lost (For instance because the device went to
    // sleep). We need to sleep until we get a new surface.
    if (egl_.eglGetError() == EGL11.EGL_CONTEXT_LOST)
      Log.d("Graphics::EndFrameBufferOpenGL", "Context Lost.");
  }

  private int current_texture_ = -1;
  private Vector<Integer> texture_widths_ = new Vector<Integer>();
  private Vector<Integer> texture_heights_ = new Vector<Integer>();
  private EGL10 egl_;
  private EGLConfig egl_config_;
  private EGLContext egl_context_;
  private EGLDisplay egl_display_;
  private EGLSurface egl_surface_;
  private GL10 gl_;
  private boolean gl_state_initialized_ = false;
  private boolean gl_surface_initialized_ = false;
  private float[] matrix_ = new float[] {
    1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };  // 4x4 identity matrix.
}
