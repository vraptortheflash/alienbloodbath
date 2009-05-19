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
import java.util.TreeMap;
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

    switch (mBackendType) {
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
    mSurfaceHolder = surface_holder;
    mSurfaceWidth = width;
    mSurfaceHeight = height;
    Log.d("Graphics::surfaceChanged",
          "Size = " + mSurfaceWidth + "x" + mSurfaceHeight + ".");

    switch (mBackendType) {
      case ANDROID2D:
        break;  // Don't care.
      case OPENGL:
        mGlSurfaceInitialized = false;
        break;
    }
  }

  public void destroy() {
    switch (mBackendType) {
      case ANDROID2D:
        break;  // Don't care.
      case OPENGL:
        destroyOpenGL();
        break;
    }
  }

  public int getWidth() {
    switch (mBackendType) {
      case ANDROID2D:
        return getWidthAndroid2D();
      case OPENGL:
        return getWidthOpenGL();
    }
    return 0;
  }

  public int getHeight() {
    switch (mBackendType) {
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

    int bitmap_hash = hashBitmap(bitmap);
    Integer cached_image_handle = mImageCache.get(new Integer(bitmap_hash));
    if (cached_image_handle != null) {
      return cached_image_handle.intValue();
    } else {
      int image_handle = -1;
      switch (mBackendType) {
        case ANDROID2D:
          image_handle = loadImageFromBitmapAndroid2D(bitmap);
          break;
        case OPENGL:
          image_handle = loadImageFromBitmapOpenGL(bitmap);
          break;
      }
      mImageCache.put(new Integer(bitmap_hash), new Integer(image_handle));
      return image_handle;
    }
  }

  public void freeImage(int image_handle) {
    switch (mBackendType) {
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
    /* DEBUGGING ONLY
    Assert.assertTrue("Invalid image handle in drawImage", image_handle >= 0);
    // The drawImageOpenGL implementation has been inlined here for performance
    // reasons. TODO: Inline the 2D API implementation here.
    Assert.assertEquals(mBackendType, BackendType.OPENGL);
    */
    if (mBackendType == BackendType.ANDROID2D) {
      drawImageAndroid2D(image_handle, source_rect, dest_rect,
                         flipped_horizontal, flipped_vertical);
      return;
    }

    if (image_handle >= mTextureData.size()) {
      Log.d("Graphics::drawImage", "Unknown image handle encountered. " +
            "Assuming OpenGL context has been lost. Exiting.");
      mContextLost = true;
      return;
    }

    if (image_handle != mCurrentTexture) {
      mCurrentTexture = image_handle;
      mGl.glBindTexture(GL10.GL_TEXTURE_2D, image_handle);
    }

    // The vertex and texture coordinate arrays have already been initialized.
    // All that is left is to set up the texture and model view transformation
    // matrices and render. Note that the OpenGL API expects matrices with a
    // column-major layout.
    TextureData texture_data = mTextureData.get(image_handle);
    float texture_width = texture_data.width;
    float texture_height = texture_data.height;

    mMatrix4x4[1] = mMatrix4x4[2] = mMatrix4x4[4] =
        mMatrix4x4[6] = mMatrix4x4[8] = mMatrix4x4[9] = 0.0f;
    if (flipped_vertical) {
      mMatrix4x4[5] = (source_rect.top - source_rect.bottom) / texture_height;
      mMatrix4x4[13] = source_rect.bottom / texture_height;
    } else {
      mMatrix4x4[5] = (source_rect.bottom - source_rect.top) / texture_height;
      mMatrix4x4[13] = source_rect.top / texture_height;
    }
    if (flipped_horizontal) {
      mMatrix4x4[0] = (source_rect.left - source_rect.right) / texture_width;
      mMatrix4x4[12] = source_rect.right / texture_width;
    } else {
      mMatrix4x4[0] = (source_rect.right - source_rect.left) / texture_width;
      mMatrix4x4[12] = source_rect.left / texture_width;
    }

    mGl.glMatrixMode(GL10.GL_TEXTURE);
    mGl.glLoadMatrixf(mMatrix4x4, 0);

    mMatrix4x4[0] = dest_rect.right - dest_rect.left;
    mMatrix4x4[5] = dest_rect.top - dest_rect.bottom;
    mMatrix4x4[12] = dest_rect.left;
    mMatrix4x4[13] = mSurfaceHeight - dest_rect.top;
    mGl.glMatrixMode(GL10.GL_MODELVIEW);
    mGl.glLoadMatrixf(mMatrix4x4, 0);

    mGl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4);
  }

  public void drawImage(int image_handle, Rect source_rect, Matrix dest_matrix,
                        boolean flipped_horizontal, boolean flipped_vertical) {
    /* DEBUGGING ONLY
    Assert.assertTrue("Invalid image handle in drawImage", image_handle >= 0);
    // The drawImageOpenGL implementation has been inlined here for performance
    // reasons. TODO: Inline the 2D API implementation here.
    Assert.assertEquals(mBackendType, BackendType.OPENGL);
    */
    if (mBackendType == BackendType.ANDROID2D) {
      return;
    }

    if (image_handle >= mTextureData.size()) {
      Log.d("Graphics::drawImage", "Unknown image handle encountered. " +
            "Assuming OpenGL context has been lost. Exiting.");
      mContextLost = true;
      return;
    }

    if (image_handle != mCurrentTexture) {
      mCurrentTexture = image_handle;
      mGl.glBindTexture(GL10.GL_TEXTURE_2D, image_handle);
    }

    // The vertex and texture coordinate arrays have already been initialized.
    // All that is left is to set up the texture and model view transformation
    // matrices and render. Note that the OpenGL API expects matrices with a
    // column-major layout.
    TextureData texture_data = mTextureData.get(image_handle);
    float texture_width = texture_data.width;
    float texture_height = texture_data.height;

    mMatrix4x4[1] = mMatrix4x4[2] = mMatrix4x4[4] =
        mMatrix4x4[6] = mMatrix4x4[8] = mMatrix4x4[9] = 0.0f;
    if (flipped_vertical) {
      mMatrix4x4[5] = (source_rect.top - source_rect.bottom) / texture_height;
      mMatrix4x4[13] = source_rect.bottom / texture_height;
    } else {
      mMatrix4x4[5] = (source_rect.bottom - source_rect.top) / texture_height;
      mMatrix4x4[13] = source_rect.top / texture_height;
    }
    if (flipped_horizontal) {
      mMatrix4x4[0] = (source_rect.left - source_rect.right) / texture_width;
      mMatrix4x4[12] = source_rect.right / texture_width;
    } else {
      mMatrix4x4[0] = (source_rect.right - source_rect.left) / texture_width;
      mMatrix4x4[12] = source_rect.left / texture_width;
    }

    mGl.glMatrixMode(GL10.GL_TEXTURE);
    mGl.glLoadMatrixf(mMatrix4x4, 0);

    mScreenMatrix.reset();
    mScreenMatrix.preTranslate(0.0f, mSurfaceHeight);
    mScreenMatrix.preScale(1.0f, -1.0f);
    mScreenMatrix.preConcat(dest_matrix);
    mScreenMatrix.getValues(mMatrix3x3);
    mMatrix4x4[0] = mMatrix3x3[0];
    mMatrix4x4[1] = mMatrix3x3[3];
    mMatrix4x4[2] = mMatrix3x3[6];
    mMatrix4x4[4] = mMatrix3x3[1];
    mMatrix4x4[5] = mMatrix3x3[4];
    mMatrix4x4[6] = mMatrix3x3[6];
    mMatrix4x4[12] = mMatrix3x3[2];
    mMatrix4x4[13] = mMatrix3x3[5];
    mGl.glMatrixMode(GL10.GL_MODELVIEW);
    mGl.glLoadMatrixf(mMatrix4x4, 0);

    mGl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4);
  }

  /**
   * Begin the start of the frame drawing operations. All drawing methods must
   * be called between a call to beginFrame() and endFrame(). The beginFrame()
   * method may return false to indicate a critical error, signaling that the
   * graphics system must be restarted.
   */
  public boolean beginFrame() {
    switch (mBackendType) {
      case ANDROID2D:
        return beginFrameAndroid2D();
      case OPENGL:
        return beginFrameOpenGL();
    }
    return false;
  }

  public void endFrame() {
    switch (mBackendType) {
      case ANDROID2D:
        endFrameAndroid2D();
        break;
      case OPENGL:
        endFrameOpenGL();
        break;
    }
  }

  public boolean hasHardwareAcceleration() {
    return mHasHardwareAcceleration;
  }

  /**
   * Private shared methods  and state.
   */

  private void determineBackendType() {
    // Determine which rendering back-end to use based off of the system setup,
    // specifically the presence of any rendering hardware. OpenGL appears to be
    // faster on both the Emulator and the HTC Dream handset than the Android2D
    // back end. The software OpenGL rasterizer is faster than the Android2D
    // graphics API so it should be preferred in nearly all situations. However,
    // when using either software back-end, pixel fill rate has been
    // experimentally been shown to be a bottleneck.
    mBackendType = BackendType.OPENGL;
  }

  /** Determine a (generally) unique hash code from a Bitmap reference. This is
   * intended to be used to quickly identify exact images while only examining a
   * small subset of the pixels. */
  static private int hashBitmap(Bitmap bitmap) {
    int hash_result = 0;
    hash_result = (hash_result << 7) ^ bitmap.getHeight();
    hash_result = (hash_result << 7) ^ bitmap.getWidth();
    for (int pixel = 0; pixel < 20; ++pixel) {
      int x = (pixel * 50) % bitmap.getWidth();
      int y = (pixel * 100) % bitmap.getHeight();
      hash_result = (hash_result << 7) ^ bitmap.getPixel(x, y);
    }
    return hash_result;
  }

  private enum BackendType { ANDROID2D, OPENGL }
  private BackendType               mBackendType;
  private TreeMap<Integer, Integer> mImageCache = new TreeMap<Integer, Integer>();
  private SurfaceHolder             mSurfaceHolder;
  private int                       mSurfaceHeight;
  private int                       mSurfaceWidth;

  /**
   * Private Android 2D backend methods and state.
   */

  private void initializeAndroid2D() {
    mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_HARDWARE);
  }

  private int loadImageFromBitmapAndroid2D(Bitmap bitmap) {
    mImagesAndroid2D.add(bitmap);
    return mImagesAndroid2D.size();
  }

  private void freeImageAndroid2D(int image_handle) {
    if (image_handle >= 1) {
      mImagesAndroid2D.set(image_handle - 1, null);
    }
  }

  private int getWidthAndroid2D() {
    return mSurfaceWidth;
  }

  private int getHeightAndroid2D() {
    return mSurfaceHeight;
  }

  private boolean beginFrameAndroid2D() {
    mCanvasAndroid2D = mSurfaceHolder.lockCanvas(null);
    mCanvasAndroid2D.drawRGB(0, 0, 0);
    return true;
  }

  private void endFrameAndroid2D() {
    mSurfaceHolder.unlockCanvasAndPost(mCanvasAndroid2D);
  }

  private void drawImageAndroid2D(int image_handle,
                                  Rect source_rect, RectF dest_rect,
                                  boolean flipped_horizontal,
                                  boolean flipped_vertical) {
    Assert.assertTrue("Vertical flipping not yet implemented.",
                      !flipped_vertical);

    if (flipped_horizontal) {
      mTransformationAndroid2D.setScale(-1.0f, 1.0f);
      mTransformationAndroid2D.postTranslate(
          2.0f * dest_rect.left + dest_rect.width(), 0.0f);
    } else {
      mTransformationAndroid2D.setScale(1.0f, 1.0f);
    }

    Bitmap bitmap = mImagesAndroid2D.get(image_handle - 1);
    mCanvasAndroid2D.setMatrix(mTransformationAndroid2D);
    mCanvasAndroid2D.drawBitmap(
        bitmap, source_rect, dest_rect, mPaintAndroid2D);
  }

  private void drawImageAndroid2D(int image_handle,
                                  Rect source_rect, Matrix dest_matrix,
                                  boolean flipped_horizontal,
                                  boolean flipped_vertical) {
    Assert.fail("Method not yet implemented.");
  }

  private Canvas            mCanvasAndroid2D;
  private ArrayList<Bitmap> mImagesAndroid2D = new ArrayList<Bitmap>();
  private Paint             mPaintAndroid2D = new Paint();
  private Matrix            mTransformationAndroid2D = new Matrix();

  /**
   * Private OpenGL backend methods and state.
   */

  private void initializeOpenGL() {
    // Note that OpenGL ES documentation may be found at:
    // http://java.sun.com/javame/reference/apis/jsr239/javax/microedition
    //                                         /khronos/opengles/GL10.html

    mEgl = (EGL10)EGLContext.getEGL();
    mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

    // We can now initialize EGL for that display.
    int[] version = new int[2];
    mEgl.eglInitialize(mEglDisplay, version);
    Log.d("Graphics::initializeOpenGL",
          "Found version: " + version[0] + "." + version[1]);

    int attrib_list[] = {  // Use default bit depths.
      EGL10.EGL_NONE
    };

    EGLConfig[] configs = new EGLConfig[1];
    int[] num_config = new int[1];
    mEgl.eglChooseConfig(mEglDisplay, attrib_list, configs, 1, num_config);
    mEglConfig = configs[0];
    mEglContext = mEgl.eglCreateContext(
        mEglDisplay, mEglConfig, EGL10.EGL_NO_CONTEXT, null);
    mGl = (GL10)mEglContext.getGL();

    final boolean kEnableOpenGLDebugging = false;
    if (kEnableOpenGLDebugging) {
      int debugging_flags = (GLDebugHelper.CONFIG_LOG_ARGUMENT_NAMES |
                             GLDebugHelper.CONFIG_CHECK_THREAD |
                             GLDebugHelper.CONFIG_CHECK_GL_ERROR);
      mEgl = (EGL10)GLDebugHelper.wrap(
          mEgl, debugging_flags, new PrintWriter(System.out));
      mGl = (GL10)GLDebugHelper.wrap(
          mGl, debugging_flags, new PrintWriter(System.out));
    }

    // Create a place holder surface so we can continue with initialization.
    // When the framework gives us a real surface / surface dimensions, it will
    // be recreated.
    initializeOpenGLSurface();
  }

  /** Create a new rendering surface. This is indented to be called whenever the
   * window size changes, for example. */
  private void initializeOpenGLSurface() {
    Log.d("Graphics::initializeOpenGLSurface", "Freeing old OpenGL surface.");

    mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_GPU);

    if (mEglSurface != null) {
      // Unbind and destroy the old EGL surface, if there is one.
      mEgl.eglMakeCurrent(mEglDisplay,
                          EGL10.EGL_NO_SURFACE,
                          EGL10.EGL_NO_SURFACE,
                          EGL10.EGL_NO_CONTEXT);
      mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
      mEglSurface = null;
    }

    // Create an EGL surface we can render into.
    Log.d("Graphics::initializeOpenGLSurface", "Creating new OpenGL surface.");
    mEglSurface = mEgl.eglCreateWindowSurface(
        mEglDisplay, mEglConfig, mSurfaceHolder, null);

    // Before we can issue GL commands, we need to make sure the context is
    // current and bound to a surface.
    Log.d("Graphics::initializeOpenGLSurface", "Updating OpenGL context.");
    mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext);

    initializeOpenGLClientState();
  }

  private void initializeOpenGLClientState() {
    Log.d("Graphics::initializeOpenGLClientState", "Setting up client state.");

    String gl_renderer = mGl.glGetString(GL10.GL_RENDERER);
    Log.d("Graphics::initializeOpenGLClientState",
          "Found renderer: " + gl_renderer);

    // Default settings for unknown hardware considerations. We choose to be on
    // the save side with respect to performance considerations.
    mHasHardwareAcceleration = true;

    // Dream / G1.
    if (gl_renderer.indexOf("Q3Dimension") != -1) {
      mHasHardwareAcceleration = true;
    }

    // Emulator / Software drivers.
    if (gl_renderer.indexOf("PixelFlinger") != -1) {
      mHasHardwareAcceleration = false;
    }

    // Initialize the orthographic projection within our surface. This must
    // happen whenever the surface size changes.
    mGl.glViewport(0, 0, getWidthOpenGL(), getHeightOpenGL());
    mGl.glMatrixMode(GL10.GL_PROJECTION);
    mGl.glLoadIdentity();
    mGl.glOrthof(0, getWidthOpenGL(), 0, getHeightOpenGL(), -1, 1);

    // Since we will only be rendering quads, set up a shared vertex and texture
    // coordinate array. The following is so convoluted I really wonder if this
    // is right of if the Java / OpenGL ES folks need their heads examined.
    float[] corner_array = { 0, 0,  1, 0,  1, 1, 0, 1 };
    ByteBuffer corner_byte_buffer =
        ByteBuffer.allocateDirect(4 * corner_array.length);
    corner_byte_buffer.order(ByteOrder.nativeOrder());
    FloatBuffer corner_float_buffer = corner_byte_buffer.asFloatBuffer();
    corner_float_buffer.put(corner_array);
    corner_float_buffer.position(0);

    mGl.glVertexPointer(2, GL10.GL_FLOAT, 0, corner_float_buffer);
    mGl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    mGl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, corner_float_buffer);
    mGl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

    // OpenGL rendering state configuration.
    mGl.glEnable(GL10.GL_TEXTURE_2D);
    mGl.glDisable(GL10.GL_CULL_FACE);
    mGl.glDisable(GL10.GL_DEPTH_TEST);
    mGl.glEnable(GL10.GL_BLEND);
    mGl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
    mGl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
    mGl.glDisable(GL10.GL_ALPHA_TEST);
  }

  private void destroyOpenGL() {
    Log.d("Graphics::destroyOpenGL", "Destroying open gl.");

    if (mEglSurface != null) {
      mEgl.eglMakeCurrent(mEglDisplay,
                          EGL10.EGL_NO_SURFACE,
                          EGL10.EGL_NO_SURFACE,
                          EGL10.EGL_NO_CONTEXT);
      mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
      mEglSurface = null;
    }
    if (mEglContext != null) {
      mEgl.eglDestroyContext(mEglDisplay, mEglContext);
      mEglContext = null;
    }
    if (mEglDisplay != null) {
      mEgl.eglTerminate(mEglDisplay);
      mEglDisplay = null;
    }
  }

  private int loadImageFromBitmapOpenGL(Bitmap bitmap) {
    // Allocate a texture handle within the OpenGL context.
    int[] texture_names = new int[1];
    mGl.glGenTextures(1, texture_names, 0);
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
    mGl.glBindTexture(GL10.GL_TEXTURE_2D, texture_name);
    mGl.glTexImage2D(GL10.GL_TEXTURE_2D,
                     0,                      // Mipmap level.
                     GL10.GL_RGBA,           // Internal format.
                     bitmap.getWidth(),
                     bitmap.getHeight(),
                     0,                      // Border.
                     GL10.GL_RGBA,           // Format.
                     GL10.GL_UNSIGNED_BYTE,
                     bitmap_data_buffer);
    mGl.glTexParameterf(GL10.GL_TEXTURE_2D,
                        GL10.GL_TEXTURE_MIN_FILTER,
                        GL10.GL_NEAREST);
    mGl.glTexParameterf(GL10.GL_TEXTURE_2D,
                        GL10.GL_TEXTURE_MAG_FILTER,
                        GL10.GL_NEAREST);

    // The size must be manually stored for retrieval during the rendering
    // process since the texture coordinate scheme under OpenGL is normalized
    // where as under the Android2D back end, texture coordinates are absolute.
    if (mTextureData.size() <= texture_name) {
      mTextureData.setSize(texture_name + 1);
    }
    TextureData texture_data = new TextureData();
    texture_data.width = bitmap.getWidth();
    texture_data.height = bitmap.getHeight();
    mTextureData.set(texture_name, texture_data);
    return texture_name;
  }

  private void freeImageOpenGL(int image_handle) {
    // TODO(burkhart): Implement.
  }

  private int getWidthOpenGL() {
    return mSurfaceWidth;
  }

  private int getHeightOpenGL() {
    return mSurfaceHeight;
  }

  private boolean beginFrameOpenGL() {
    if (!mGlSurfaceInitialized) {
      initializeOpenGLSurface();
      mGlSurfaceInitialized = true;
    }
    if (!mGlStateInitialized) {
      initializeOpenGLClientState();
      mGlStateInitialized = true;
    }
    mGl.glClear(GL10.GL_COLOR_BUFFER_BIT);
    return !mContextLost;
  }

  private void endFrameOpenGL() {
    mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);

    // Always check for EGL_CONTEXT_LOST, which means the context and all
    // associated data were lost (For instance because the device went to
    // sleep). We need to sleep until we get a new surface.
    if (mEgl.eglGetError() == EGL11.EGL_CONTEXT_LOST) {
      mContextLost = true;
      Log.d("Graphics::endFrameBufferOpenGL", "Context Lost.");
    }
  }

  private int             mCurrentTexture = -1;
  private boolean         mContextLost;
  private EGL10           mEgl;
  private EGLConfig       mEglConfig;
  private EGLContext      mEglContext;
  private EGLDisplay      mEglDisplay;
  private EGLSurface      mEglSurface;
  private GL10            mGl;
  private boolean         mGlStateInitialized;
  private boolean         mGlSurfaceInitialized;
  private boolean         mHasHardwareAcceleration;

  class TextureData {
    public int height;
    public int width;
  }
  private Vector<TextureData> mTextureData = new Vector<TextureData>();

  // The following matrix definitions are used to avoid any allocations within
  // the draw methods.
  private Matrix  mScreenMatrix = new Matrix();
  private float[] mMatrix3x3 = new float[] {
    1, 0, 0, 0, 1, 0, 0, 0, 1 };
  private float[] mMatrix4x4 = new float[] {
    1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };
}
