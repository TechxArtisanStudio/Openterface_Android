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
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.text.TextUtils;

import java.io.IOException;

/**
 * Helper class for OpenGL|ES texture operations
 */
public class GLTexture implements ITexture {
//	private static final boolean DEBUG = false;	// FIXME set to false in production
//	private static final String TAG = "GLTexture";

	/* package */final int mTextureTarget;
	/* package */final int mTextureUnit ;
	/* package */int mTextureId;
	/* package */final float[] mTexMatrix = new float[16];	// Texture transformation matrix
	/* package */int mTexWidth, mTexHeight;
	/* package */int mImageWidth, mImageHeight;
	
	/**
	 * Constructor
	 * Texture unit is always GL_TEXTURE0, so multiple textures cannot be used simultaneously
	 * @param width texture size
	 * @param height texture size
	 * @param filter_param texture interpolation method, GL_LINEAR or GL_NEAREST
	 */
	public GLTexture(final int width, final int height, final int filter_param) {
		this(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE0, width, height, filter_param);
	}
	
	/**
	 * Constructor
	 * @param texTarget GL_TEXTURE_EXTERNAL_OES is not allowed
	 * @param texUnit
	 * @param width texture size
	 * @param height texture size
	 * @param filter_param texture interpolation method, GL_LINEAR or GL_NEAREST
	 */
	public GLTexture(final int texTarget, final int texUnit,
		final int width, final int height, final int filter_param) {
//		if (DEBUG) Log.v(TAG, String.format("Constructor(%d,%d)", width, height));
		mTextureTarget = texTarget;
		mTextureUnit = texUnit;
		// The bitmap used for texture must have power-of-2 dimensions.
		// Furthermore, if using mipmap, it must be square.
		// Use the nearest power-of-2 size equal to or larger than the specified width/height.
		int w = 32;
		for (; w < width; w <<= 1);
		int h = 32;
		for (; h < height; h <<= 1);
		if (mTexWidth != w || mTexHeight != h) {
			mTexWidth = w;
			mTexHeight = h;
		}
//		if (DEBUG) Log.v(TAG, String.format("texSize(%d,%d)", mTexWidth, mTexHeight));
		mTextureId = GLHelper.initTex(mTextureTarget, filter_param);
		// Allocate texture memory
		GLES20.glTexImage2D(mTextureTarget,
			0,							// Mipmap level 0 (no mipmap)
			GLES20.GL_RGBA,				// Internal format
			mTexWidth, mTexHeight,		// Size
			0,							// Border width
			GLES20.GL_RGBA,				// Format of supplied data
			GLES20.GL_UNSIGNED_BYTE,	// Data type
			null);						// No pixel data
		// Initialize texture transformation matrix
		Matrix.setIdentityM(mTexMatrix, 0);
		mTexMatrix[0] = width / (float)mTexWidth;
		mTexMatrix[5] = height / (float)mTexHeight;
//		if (DEBUG) Log.v(TAG, "GLTexture:id=" + mTextureId);
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			release();	// Not ideal since it might not be in GL context
		} finally {
			super.finalize();
		}
	}

	/**
	 * Discard texture
	 * Must be called within GL context/EGL rendering context
	 */
	@Override
	public void release() {
//		if (DEBUG) Log.v(TAG, "release:");
		if (mTextureId > 0) {
			GLHelper.deleteTex(mTextureId);
			mTextureId = 0;
		}
	}

	/**
	 * Enable (bind) the texture managed by this instance
	 */
	@Override
	public void bind() {
//		if (DEBUG) Log.v(TAG, "bind:");
		GLES20.glActiveTexture(mTextureUnit);	// Select texture unit
		GLES20.glBindTexture(mTextureTarget, mTextureId);
	}

	/**
	 * Disable (unbind) the texture managed by this instance
	 */
	@Override
	public void unbind() {
//		if (DEBUG) Log.v(TAG, "unbind:");
		GLES20.glActiveTexture(mTextureUnit);	// Select texture unit
		GLES20.glBindTexture(mTextureTarget, 0);
	}

	/**
	 * Get texture target (GL_TEXTURE_2D)
	 * @return
	 */
	@Override
	public int getTexTarget() { return mTextureTarget; }
	/**
	 * Get texture name
	 * @return
	 */
	@Override
	public int getTexture() { return mTextureId; }
	/**
	 * Get texture coordinate transformation matrix (returns internal array directly, be careful when modifying)
	 * @return
	 */
	@Override
	public float[] getTexMatrix() { return mTexMatrix; }
	/**
	 * Get a copy of the texture coordinate transformation matrix
	 * @param matrix no bounds check, so at least 16 elements must be allocated from offset
	 * @param offset
	 */
	@Override
	public void getTexMatrix(final float[] matrix, final int offset) {
		System.arraycopy(mTexMatrix, 0, matrix, offset, mTexMatrix.length);
	}
	/**
	 * Get texture width
	 * @return
	 */
	@Override
	public int getTexWidth() { return mTexWidth; }
	/**
	 * Get texture height
	 * @return
	 */
	@Override
	public int getTexHeight() { return mTexHeight; }

	/**
	 * Load image from specified file into texture.
	 * Generates IOException/NullPointerException if file does not exist or cannot be read.
	 * @param filePath
	 */
	@Override
	public void loadTexture(final String filePath) throws NullPointerException, IOException {
//		if (DEBUG) Log.v(TAG, "loadTexture:path=" + filePath);
		if (TextUtils.isEmpty(filePath))
			throw new NullPointerException("image file path should not be a null");
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;	// Get only size info without generating Bitmap
		BitmapFactory.decodeFile(filePath, options);
		// Calculate subsampling value to fit the specified image within texture size
		final int imageWidth = options.outWidth;
		final int imageHeight = options.outHeight;
		int inSampleSize = 1;	// Subsampling size
		if ((imageHeight > mTexHeight) || (imageWidth > mTexWidth)) {
			if (imageWidth > imageHeight) {
				inSampleSize = (int)Math.ceil(imageHeight / (float)mTexHeight);
			} else {
				inSampleSize = (int)Math.ceil(imageWidth / (float)mTexWidth);
			}
		}
//		if (DEBUG) Log.v(TAG, String.format("image(%d,%d),tex(%d,%d),inSampleSize=%d",
// 			imageWidth, imageHeight, mTexWidth, mTexHeight, inSampleSize));
		// Actual loading process
		options.inSampleSize = inSampleSize;
		options.inJustDecodeBounds = false;
		loadTexture(BitmapFactory.decodeFile(filePath, options));
	}
	
	/**
	 * Load the specified bitmap into texture
 	 * @param bitmap
	 */
	public void loadTexture(final Bitmap bitmap) throws NullPointerException {
		mImageWidth = bitmap.getWidth();	// Get loaded image size
		mImageHeight = bitmap.getHeight();
		Bitmap texture = Bitmap.createBitmap(mTexWidth, mTexHeight, Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(texture);
		canvas.drawBitmap(bitmap, 0, 0, null);
		bitmap.recycle();
		// Set texture coordinate transformation matrix (scale conversion to fit loaded image size to texture size)
		Matrix.setIdentityM(mTexMatrix, 0);
		mTexMatrix[0] = mImageWidth / (float)mTexWidth;
		mTexMatrix[5] = mImageHeight / (float)mTexHeight;
//		if (DEBUG) Log.v(TAG, String.format("image(%d,%d),scale(%f,%f)",
// 			mImageWidth, mImageHeight, mMvpMatrix[0], mMvpMatrix[5]));
		bind();
		GLUtils.texImage2D(mTextureTarget, 0, texture, 0);
		unbind();
		texture.recycle();
	}
}
