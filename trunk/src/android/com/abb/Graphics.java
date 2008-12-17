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
import junit.framework.Assert;


/** Graphics API designed for ABB. The goal is to support both OpenGL and
 * Android 2D rendering targets efficiently through the same interface. */
public class Graphics {
  /**
   * Public API.
   */

  /** Initialize the graphics sub-system letting the rendering back end target
   * be automatically chosen from the system configuration. */
  public void initialize(SurfaceHolder surface_holder) {
    determineBackendType();

    Rect surface_frame = surface_holder.getSurfaceFrame();
    surfaceChanged(
        surface_holder, surface_frame.width(), surface_frame.height());

    switch (backend_type_) {
      case ANDROID2D:
        Log.d("Graphics::Initialize", "Initializing Android2D rendering.");
        initializeAndroid2D();
        break;
      case OPENGL:
        Log.d("Graphics::Initialize", "Initializing OpenGL rendering.");
        initializeOpenGL();
        break;
    }
  }

  public void surfaceChanged(SurfaceHolder surface_holder,
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

  public void destroy() {
    switch (backend_type_) {
      case ANDROID2D:
        break;  // Don't care.
      case OPENGL:
        destroyOpenGL();
        break;
    }
  }

  public int getWidth() {
    switch (backend_type_) {
      case ANDROID2D:
        return getWidthAndroid2D();
      case OPENGL:
        return getWidthOpenGL();
    }
    return 0;
  }

  public int getHeight() {
    switch (backend_type_) {
      case ANDROID2D:
        return getHeightAndroid2D();
      case OPENGL:
        return getHeightOpenGL();
    }
    return 0;
  }

  /** Load an image from a bitmap and return its handle. For OpenGL
   * compatibility texture dimensions must be powers of 2. Additionally,
   * hardware usually limits texture sizes to 1024 pixels. */
  public int loadImageFromBitmap(Bitmap bitmap) {
    Assert.assertNotNull(
        "Null bitmap specified in LoadImageFromBitmap", bitmap);

    switch (backend_type_) {
      case ANDROID2D:
        return loadImageFromBitmapAndroid2D(bitmap);
      case OPENGL:
        return loadImageFromBitmapOpenGL(bitmap);
    }
    return -1;
  }

  public void freeImage(int image_handle) {
    switch (backend_type_) {
      case ANDROID2D:
        freeImageAndroid2D(image_handle);
        break;
      case OPENGL:
        freeImageOpenGL(image_handle);
        break;
    }
  }

  public void drawImage(int image_handle, Rect source_rect, RectF dest_rect,
                        boolean flipped_horizontal, boolean flipped_vertical) {
    Assert.assertTrue("Invalid image handle in drawImage", image_handle >= 0);

    switch (backend_type_) {
      case ANDROID2D:
        drawImageAndroid2D(image_handle, source_rect, dest_rect,
                           flipped_horizontal, flipped_vertical);
        break;
      case OPENGL:
        drawImageOpenGL(image_handle, source_rect, dest_rect,
                        flipped_horizontal, flipped_vertical);
        break;
    }
  }

  public void drawImage(int image_handle, Rect source_rect, Matrix dest_matrix,
                        boolean flipped_horizontal, boolean flipped_vertical) {
    Assert.assertTrue("Invalid image handle in drawImage", image_handle >= 0);

    switch (backend_type_) {
      case ANDROID2D:
        drawImageAndroid2D(image_handle, source_rect, dest_matrix,
                           flipped_horizontal, flipped_vertical);
        break;
      case OPENGL:
        drawImageOpenGL(image_handle, source_rect, dest_matrix,
                        flipped_horizontal, flipped_vertical);
        break;
    }
  }

  public void beginFrame() {
   switch (backend_type_) {
      case ANDROID2D:
        beginFrameAndroid2D();
        break;
      case OPENGL:
        beginFrameOpenGL();
        break;
    }
  }

  public void endFrame() {
    switch (backend_type_) {
      case ANDROID2D:
        endFrameAndroid2D();
        break;
      case OPENGL:
        endFrameOpenGL();
        break;
    }
  }

  /**
   * Private shared methods  and state.
   */

  void determineBackendType() {
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

  private void initializeAndroid2D() {
    surface_holder_.setType(SurfaceHolder.SURFACE_TYPE_HARDWARE);
  }

  private int loadImageFromBitmapAndroid2D(Bitmap bitmap) {
    images_android2D_.add(bitmap);
    return images_android2D_.size();
  }

  private void freeImageAndroid2D(int image_handle) {
    if (image_handle >= 1)
      images_android2D_.set(image_handle - 1, null);
  }

  private int getWidthAndroid2D() {
    return surface_width_;
  }

  private int getHeightAndroid2D() {
    return surface_height_;
  }

  private void beginFrameAndroid2D() {
    canvas_android2D_ = surface_holder_.lockCanvas(null);
    canvas_android2D_.drawRGB(0, 0, 0);
  }

  private void endFrameAndroid2D() {
    surface_holder_.unlockCanvasAndPost(canvas_android2D_);
  }

  private void drawImageAndroid2D(int image_handle,
                                  Rect source_rect, RectF dest_rect,
                                  boolean flipped_horizontal,
                                  boolean flipped_vertical) {
    Assert.assertTrue("Vertical flipping not yet implemented.",
                      !flipped_vertical);

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

  private void drawImageAndroid2D(int image_handle,
                                  Rect source_rect, Matrix dest_matrix,
                                  boolean flipped_horizontal,
                                  boolean flipped_vertical) {
    Assert.fail("Method not yet implemented.");
  }

  private Canvas canvas_android2D_;
  private ArrayList<Bitmap> images_android2D_ = new ArrayList<Bitmap>();
  private Paint paint_android2D_ = new Paint();
  private Matrix transformation_android2D_ = new Matrix();

  /**
   * Private OpenGL backend methods and state.
   */

  private void initializeOpenGL() {
    // Note that OpenGL ES documentation may be found at:
    // http://java.sun.com/javame/reference/apis/jsr239/javax/microedition
    //                                         /khronos/opengles/GL10.html

    egl_ = (EGL10)EGLContext.getEGL();
    egl_display_ = egl_.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

    // We can now initialize EGL for that display.
    int[] version = new int[2];
    egl_.eglInitialize(egl_display_, version);
    Log.d("Graphics::initializeOpenGL",
          "Found version: " + version[0] + "." + version[1]);

    int attrib_list[] = {  // Use default bit depths.
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
    initializeOpenGLSurface();
  }

  /** Create a new rendering surface. This is indented to be called whenever the
   * window size changes, for example. */
  private void initializeOpenGLSurface() {
    Log.d("Graphics::initializeOpenGLSurface", "Creating OpenGL surface.");

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

    initializeOpenGLClientState();
  }

  private void initializeOpenGLClientState() {
    Log.d("Graphics::initializeOpenGLClientState", "Setting up client state.");

    // Initialize the orthographic projection within our surface. This must
    // happen whenever the surface size changes.
    gl_.glViewport(0, 0, getWidthOpenGL(), getHeightOpenGL());
    gl_.glMatrixMode(GL10.GL_PROJECTION);
    gl_.glLoadIdentity();
    gl_.glOrthof(0, getWidthOpenGL(), 0, getHeightOpenGL(), -1, 1);

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
    gl_.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    gl_.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
    gl_.glDisable(GL10.GL_ALPHA_TEST);
  }

  private void destroyOpenGL() {
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

  private int loadImageFromBitmapOpenGL(Bitmap bitmap) {
    // Allocate a texture handle within the OpenGL context.
    int[] texture_names = new int[1];
    gl_.glGenTextures(1, texture_names, 0);
    int texture_name = texture_names[0];
    Log.d("Graphics::loadImageFromBitmapOpenGL",
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

  private void freeImageOpenGL(int image_handle) {
    // TODO(burkhart): Implement.
  }

  private int getWidthOpenGL() {
    return surface_width_;
  }

  private int getHeightOpenGL() {
    return surface_height_;
  }

  private void drawImageOpenGL(int image_handle,
                               Rect source_rect, RectF dest_rect,
                               boolean flipped_horizontal,
                               boolean flipped_vertical) {
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

    matrix4x4_[1] = matrix4x4_[2] = matrix4x4_[4] =
        matrix4x4_[6] = matrix4x4_[8] = matrix4x4_[9] = 0.0f;
    if (flipped_vertical) {
      matrix4x4_[5] = (source_rect.top - source_rect.bottom) / texture_height;
      matrix4x4_[13] = source_rect.bottom / texture_height;
    } else {
      matrix4x4_[5] = (source_rect.bottom - source_rect.top) / texture_height;
      matrix4x4_[13] = source_rect.top / texture_height;
    }
    if (flipped_horizontal) {
      matrix4x4_[0] = (source_rect.left - source_rect.right) / texture_width;
      matrix4x4_[12] = source_rect.right / texture_width;
    } else {
      matrix4x4_[0] = (source_rect.right - source_rect.left) / texture_width;
      matrix4x4_[12] = source_rect.left / texture_width;
    }

    gl_.glMatrixMode(GL10.GL_TEXTURE);
    gl_.glLoadMatrixf(matrix4x4_, 0);

    matrix4x4_[0] = dest_rect.right - dest_rect.left;
    matrix4x4_[5] = dest_rect.top - dest_rect.bottom;
    matrix4x4_[12] = dest_rect.left;
    matrix4x4_[13] = surface_height_ - dest_rect.top;
    gl_.glMatrixMode(GL10.GL_MODELVIEW);
    gl_.glLoadMatrixf(matrix4x4_, 0);

    gl_.glDrawArrays(GL10.GL_TRIANGLES, 0, 6);
  }

  private void drawImageOpenGL(int image_handle,
                               Rect source_rect, Matrix dest_matrix,
                               boolean flipped_horizontal,
                               boolean flipped_vertical) {
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

    matrix4x4_[1] = matrix4x4_[2] = matrix4x4_[4] =
        matrix4x4_[6] = matrix4x4_[8] = matrix4x4_[9] = 0.0f;
    if (flipped_vertical) {
      matrix4x4_[5] = (source_rect.top - source_rect.bottom) / texture_height;
      matrix4x4_[13] = source_rect.bottom / texture_height;
    } else {
      matrix4x4_[5] = (source_rect.bottom - source_rect.top) / texture_height;
      matrix4x4_[13] = source_rect.top / texture_height;
    }
    if (flipped_horizontal) {
      matrix4x4_[0] = (source_rect.left - source_rect.right) / texture_width;
      matrix4x4_[12] = source_rect.right / texture_width;
    } else {
      matrix4x4_[0] = (source_rect.right - source_rect.left) / texture_width;
      matrix4x4_[12] = source_rect.left / texture_width;
    }

    gl_.glMatrixMode(GL10.GL_TEXTURE);
    gl_.glLoadMatrixf(matrix4x4_, 0);

    screen_matrix_ = new Matrix();
    screen_matrix_.preTranslate(0.0f, surface_height_);
    screen_matrix_.preScale(1.0f, -1.0f);
    screen_matrix_.preConcat(dest_matrix);
    screen_matrix_.getValues(matrix3x3_);
    matrix4x4_[0] = matrix3x3_[0];
    matrix4x4_[1] = matrix3x3_[3];
    matrix4x4_[2] = matrix3x3_[6];
    matrix4x4_[4] = matrix3x3_[1];
    matrix4x4_[5] = matrix3x3_[4];
    matrix4x4_[6] = matrix3x3_[6];
    matrix4x4_[12] = matrix3x3_[2];
    matrix4x4_[13] = matrix3x3_[5];
    gl_.glMatrixMode(GL10.GL_MODELVIEW);
    gl_.glLoadMatrixf(matrix4x4_, 0);

    gl_.glDrawArrays(GL10.GL_TRIANGLES, 0, 6);
  }

  private void beginFrameOpenGL() {
    if (!gl_surface_initialized_) {
      initializeOpenGLSurface();
      gl_surface_initialized_ = true;
    }
    if (!gl_state_initialized_) {
      initializeOpenGLClientState();
      gl_state_initialized_ = true;
    }
    gl_.glClear(GL10.GL_COLOR_BUFFER_BIT);
  }

  private void endFrameOpenGL() {
    egl_.eglSwapBuffers(egl_display_, egl_surface_);

    // Always check for EGL_CONTEXT_LOST, which means the context and all
    // associated data were lost (For instance because the device went to
    // sleep). We need to sleep until we get a new surface.
    if (egl_.eglGetError() == EGL11.EGL_CONTEXT_LOST)
      Log.d("Graphics::endFrameBufferOpenGL", "Context Lost.");
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

  // The following matrix definitions are used to avoid any allocations within
  // the draw methods.
  private Matrix screen_matrix_ = new Matrix();
  private float[] matrix3x3_ = new float[] {
    1, 0, 0, 0, 1, 0, 0, 0, 1 };
  private float[] matrix4x4_ = new float[] {
    1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };
}
