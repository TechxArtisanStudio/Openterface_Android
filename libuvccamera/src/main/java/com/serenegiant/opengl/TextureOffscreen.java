package com.serenegiant.opengl;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2018 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.serenegiant.uvccamera.BuildConfig;

/**
 * Offscreen drawing class for drawing to a texture via OpenGL|ES
 * Assigns a texture to FBO as color buffer
 */
public class TextureOffscreen {
	private static final boolean DEBUG = BuildConfig.DEBUG;
	private static final String TAG = "TextureOffscreen";

	private static final boolean DEFAULT_ADJUST_POWER2 = false;

	private final int TEX_TARGET;
	private final int TEX_UNIT;
	private final boolean mHasDepthBuffer, mAdjustPower2;
	/** Drawing area size */
	private int mWidth, mHeight;
	/** Texture size */
	private int mTexWidth, mTexHeight;
	/** Texture name used for offscreen color buffer */
	private int mFBOTextureName = -1;
	/** Buffer object for offscreen */
	private int mDepthBufferObj = -1, mFrameBufferObj = -1;
	/** Texture coordinate transformation matrix */
	private final float[] mTexMatrix = new float[16];

	/**
	 * Constructor (GL_TEXTURE_2D), no depth buffer
	 * Texture unit is GL_TEXTURE0
	 * @param width
	 * @param height
	 */
	public TextureOffscreen(final int width, final int height) {
		this(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE0, -1,
			width, height, false, DEFAULT_ADJUST_POWER2);
	}

	/**
	 * Constructor (GL_TEXTURE_2D), no depth buffer
	 * Texture unit is GL_TEXTURE0
	 * @param tex_unit
	 * @param width
	 * @param height
	 */
	public TextureOffscreen(final int tex_unit,
		final int width, final int height) {

		this(GLES20.GL_TEXTURE_2D, tex_unit, -1,
			width, height,
			false, DEFAULT_ADJUST_POWER2);
	}

	/**
	 * Constructor (GL_TEXTURE_2D)
	 * Texture unit is GL_TEXTURE0
	 * @param width dimension of offscreen(width)
	 * @param height dimension of offscreen(height)
	 * @param use_depth_buffer set true if you use depth buffer. the depth is fixed as 16bits
	 */
	public TextureOffscreen(final int width, final int height,
		final boolean use_depth_buffer) {

		this(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE0, -1,
			width, height, use_depth_buffer, DEFAULT_ADJUST_POWER2);
	}

	/**
	 * Constructor to wrap an existing texture (GL_TEXTURE_2D)
	 * Texture unit is GL_TEXTURE0
	 * @param tex_unit
	 * @param width
	 * @param height
	 * @param use_depth_buffer
	 */
	public TextureOffscreen(final int tex_unit,
		final int width, final int height, final boolean use_depth_buffer) {

		this(GLES20.GL_TEXTURE_2D, tex_unit, -1,
			width, height,
			use_depth_buffer, DEFAULT_ADJUST_POWER2);
	}
	
	/**
	 * Constructor (GL_TEXTURE_2D)
	 * Texture unit is GL_TEXTURE0
	 * @param width
	 * @param height
	 * @param use_depth_buffer
	 * @param adjust_power2
	 */
	public TextureOffscreen(final int width, final int height,
		final boolean use_depth_buffer, final boolean adjust_power2) {

		this(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE0, -1,
			width, height, use_depth_buffer, adjust_power2);
	}
	
	/**
	 * Constructor (GL_TEXTURE_2D)
	 * @param tex_unit
	 * @param width
	 * @param height
	 * @param use_depth_buffer
	 * @param adjust_power2
	 */
	public TextureOffscreen(final int tex_unit,
		final int width, final int height,
		final boolean use_depth_buffer, final boolean adjust_power2) {

		this(GLES20.GL_TEXTURE_2D, tex_unit, -1,
			width, height, use_depth_buffer, adjust_power2);
	}

	/**
	 * Constructor to wrap an existing texture (GL_TEXTURE_2D), no depth buffer
	 * @param tex_id
	 * @param tex_unit
	 * @param width
	 * @param height
	 */
	public TextureOffscreen(final int tex_unit, final int tex_id,
		final int width, final int height) {

		this(GLES20.GL_TEXTURE_2D, tex_unit, tex_id,
			width, height,
			false, DEFAULT_ADJUST_POWER2);
	}

	/**
	 * Constructor to wrap an existing texture (GL_TEXTURE_2D)
	 * @param tex_unit
	 * @param tex_id
	 * @param width
	 * @param height
	 * @param use_depth_buffer
	 */
	public TextureOffscreen(final int tex_unit, final int tex_id,
		final int width, final int height, final boolean use_depth_buffer) {

		this(GLES20.GL_TEXTURE_2D, tex_unit, tex_id,
			width, height,
			use_depth_buffer, DEFAULT_ADJUST_POWER2);
	}

	/**
	 * Constructor to wrap an existing texture
	 * @param tex_target GL_TEXTURE_2D
	 * @param tex_id
	 * @param width
	 * @param height
	 * @param use_depth_buffer
	 * @param adjust_power2
	 */
	public TextureOffscreen(final int tex_target, final int tex_unit, final int tex_id,
		final int width, final int height,
		final boolean use_depth_buffer, final boolean adjust_power2) {

		if (DEBUG) Log.v(TAG, "Constructor");
		TEX_TARGET = tex_target;
		TEX_UNIT = tex_unit;
		mWidth = width;
		mHeight = height;
		mHasDepthBuffer = use_depth_buffer;
		mAdjustPower2 = adjust_power2;

		createFrameBuffer(width, height);
		int tex = tex_id;
		if (tex < 0) {
			tex = genTexture(tex_target, tex_unit, mTexWidth, mTexHeight);
		}
		assignTexture(tex, width, height);
	}

	/** Release resources */
	public void release() {
		if (DEBUG) Log.v(TAG, "release");
		releaseFrameBuffer();
	}

	/**
	 * Switch to the rendering buffer for offscreen drawing
	 * This also changes the Viewport, so if necessary set the Viewport after unbinding
	 */
	public void bind() {
//		if (DEBUG) Log.v(TAG, "bind:");
		GLES20.glActiveTexture(TEX_UNIT);
		GLES20.glBindTexture(TEX_TARGET, mFBOTextureName);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj);
		GLES20.glViewport(0, 0, mWidth, mHeight);
	}

	/**
	 * Restore to the default rendering buffer
	 */
	public void unbind() {
//		if (DEBUG) Log.v(TAG, "unbind:");
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glActiveTexture(TEX_UNIT);
		GLES20.glBindTexture(TEX_TARGET, 0);
	}

	private final float[] mResultMatrix = new float[16];
	/**
	 * get copy of texture matrix
	 * @return
	 */
	public float[] getTexMatrix() {
		System.arraycopy(mTexMatrix, 0, mResultMatrix, 0, 16);
		return mResultMatrix;
	}

	/**
	 * Get the texture coordinate transformation matrix (returns the internal array directly, be careful when modifying)
	 * @return
	 */
	public float[] getRawTexMatrix() {
		return mTexMatrix;
	}

	/**
	 * Return a copy of the texture transformation matrix
	 * No bounds checking is performed, so ensure at least 16 elements are available from the offset position
	 * @param matrix
	 */
	public void getTexMatrix(final float[] matrix, final int offset) {
		System.arraycopy(mTexMatrix, 0, matrix, offset, mTexMatrix.length);
	}

	/**
	 * Get the offscreen texture name
	 * This can be used to draw using the image written to this offscreen as a texture
	 * @return
	 */
	public int getTexture() {
		return mFBOTextureName;
	}

	/** Assign the specified texture to this offscreen */
	public void assignTexture(final int texture_name,
		final int width, final int height) {

		if ((width > mTexWidth) || (height > mTexHeight)) {
			mWidth = width;
			mHeight = height;
			releaseFrameBuffer();
			createFrameBuffer(width, height);
		}
		mFBOTextureName = texture_name;
		GLES20.glActiveTexture(TEX_UNIT);
		 // Bind the framebuffer object
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj);
		GLHelper.checkGlError("glBindFramebuffer " + mFrameBufferObj);
		// Connect a color buffer (texture) to the framebuffer
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
			TEX_TARGET, mFBOTextureName, 0);
		GLHelper.checkGlError("glFramebufferTexture2D");

		if (mHasDepthBuffer) {
			// Connect a depth buffer to the framebuffer
			GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
				GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mDepthBufferObj);
			GLHelper.checkGlError("glFramebufferRenderbuffer");
		}

		// Check if it completed successfully
		final int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
		if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
			throw new RuntimeException("Framebuffer not complete, status=" + status);
		}

		 // Restore to the default framebuffer
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

		// Initialize the texture coordinate transformation matrix
		Matrix.setIdentityM(mTexMatrix, 0);
		mTexMatrix[0] = width / (float)mTexWidth;
		mTexMatrix[5] = height / (float)mTexHeight;
	}

	/** Load a texture from a Bitmap */
	public void loadBitmap(final Bitmap bitmap) {
		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();
		if ((width > mTexWidth) || (height > mTexHeight)) {
			mWidth = width;
			mHeight = height;
			releaseFrameBuffer();
			createFrameBuffer(width, height);
		}
		GLES20.glActiveTexture(TEX_UNIT);
		GLES20.glBindTexture(TEX_TARGET, mFBOTextureName);
		GLUtils.texImage2D(TEX_TARGET, 0, bitmap, 0);
		GLES20.glBindTexture(TEX_TARGET, 0);
		// initialize texture matrix
		Matrix.setIdentityM(mTexMatrix, 0);
		mTexMatrix[0] = width / (float)mTexWidth;
		mTexMatrix[5] = height / (float)mTexHeight;
	}
	
	/**
	 * Generate a texture for the color buffer
	 * @param tex_target
	 * @param tex_unit
	 * @param tex_width
	 * @param tex_height
	 * @return
	 */
	private static int genTexture(final int tex_target, final int tex_unit,
		final int tex_width, final int tex_height) {
		// Generate a texture for the color buffer
		final int tex_name = GLHelper.initTex(tex_target, tex_unit,
			GLES20.GL_LINEAR, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE);
		// Allocate texture memory
		GLES20.glTexImage2D(tex_target, 0, GLES20.GL_RGBA, tex_width, tex_height, 0,
			GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
		GLHelper.checkGlError("glTexImage2D");
		return tex_name;
	}
	
	/** Generate a framebuffer object for offscreen drawing */
	private final void createFrameBuffer(final int width, final int height) {
		final int[] ids = new int[1];

		if (mAdjustPower2) {
			// Texture size must be a power of 2
			int w = 1;
			for (; w < width; w <<= 1) ;
			int h = 1;
			for (; h < height; h <<= 1) ;
			if (mTexWidth != w || mTexHeight != h) {
				mTexWidth = w;
				mTexHeight = h;
			}
		} else {
			mTexWidth = width;
			mTexHeight = height;
		}

		if (mHasDepthBuffer) {
			// If a depth buffer is required, generate and initialize a renderbuffer object
			GLES20.glGenRenderbuffers(1, ids, 0);
			mDepthBufferObj = ids[0];
			GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mDepthBufferObj);
			// Depth buffer is 16 bits
			GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER,
				GLES20.GL_DEPTH_COMPONENT16, mTexWidth, mTexHeight);
		}
		// Generate and bind a framebuffer object
		GLES20.glGenFramebuffers(1, ids, 0);
		GLHelper.checkGlError("glGenFramebuffers");
		mFrameBufferObj = ids[0];
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj);
		GLHelper.checkGlError("glBindFramebuffer " + mFrameBufferObj);

		// Restore to the default framebuffer
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

	}

	/** Release the offscreen framebuffer */
    private final void releaseFrameBuffer() {
        final int[] names = new int[1];
		// Release the depth buffer if present
		if (mDepthBufferObj >= 0) {
			names[0] = mDepthBufferObj;
			GLES20.glDeleteRenderbuffers(1, names, 0);
			mDepthBufferObj = -1;
		}
		// Release the offscreen color buffer texture
		if (mFBOTextureName >= 0) {
			names[0] = mFBOTextureName;
			GLES20.glDeleteTextures(1, names, 0);
			mFBOTextureName = -1;
		}
		// Release the offscreen framebuffer object
		if (mFrameBufferObj >= 0) {
			names[0] = mFrameBufferObj;
			GLES20.glDeleteFramebuffers(1, names, 0);
			mFrameBufferObj = -1;
		}
    }

	/**
	 * get dimension(width) of this offscreen
	 * @return
	 */
	public int getWidth() {
		return mWidth;
	}

	/**
	 * get dimension(height) of this offscreen
	 * @return
	 */
	public int getHeight() {
		return mHeight;
	}

	/**
	 * get backing texture dimension(width) of this offscreen
	 * @return
	 */
	public int getTexWidth() {
		return mTexWidth;
	}

	/**
	 * get backing texture dimension(height) of this offscreen
	 * @return
	 */
	public int getTexHeight() {
		return mTexHeight;
	}
	
	public int getTexTarget() {
		return TEX_TARGET;
	}
	
	public int getTexUnit() {
		return TEX_UNIT;
	}
}
