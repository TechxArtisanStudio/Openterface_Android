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

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static com.serenegiant.opengl.ShaderConst.FRAGMENT_SHADER_SIMPLE;
import static com.serenegiant.opengl.ShaderConst.FRAGMENT_SHADER_SIMPLE_OES;
import static com.serenegiant.opengl.ShaderConst.GL_TEXTURE_2D;
import static com.serenegiant.opengl.ShaderConst.GL_TEXTURE_EXTERNAL_OES;
import static com.serenegiant.opengl.ShaderConst.VERTEX_SHADER;

/**
 * Helper class for drawing textures as 2D across the entire drawing area
 */
public class GLDrawer2D implements IDrawer2D {
//	private static final boolean DEBUG = false; // FIXME set false on release
//	private static final String TAG = "GLDrawer2D";

	private static final float[] VERTICES = { 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f };
	private static final float[] TEXCOORD = { 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f };
	private static final int FLOAT_SZ = Float.SIZE / 8;

	private final int VERTEX_NUM;
	private final int VERTEX_SZ;
	private final FloatBuffer pVertex;
	private final FloatBuffer pTexCoord;
	private final int mTexTarget;
	private int hProgram;
    int maPositionLoc;
    int maTextureCoordLoc;
    int muMVPMatrixLoc;
    int muTexMatrixLoc;
	private final float[] mMvpMatrix = new float[16];

	/**
	 * Constructor
	 * Must be called when GL context/EGL rendering context is valid
	 * @param isOES true if using external texture (GL_TEXTURE_EXTERNAL_OES).
	 * 				false for normal 2D texture
	 */
	public GLDrawer2D(final boolean isOES) {
		this(VERTICES, TEXCOORD, isOES);
	}

	/**
	 * Constructor
	 * Must be called when GL context/EGL rendering context is valid
	 * @param vertices vertex coordinates, 8 floats = (x,y) x 4 pairs
	 * @param texcoord texture coordinates, 8 floats = (s,t) x 4 pairs
	 * @param isOES true if using external texture (GL_TEXTURE_EXTERNAL_OES).
	 * 				false for normal 2D texture
	 */
	public GLDrawer2D(final float[] vertices,
		final float[] texcoord, final boolean isOES) {

		VERTEX_NUM = Math.min(
			vertices != null ? vertices.length : 0,
			texcoord != null ? texcoord.length : 0) / 2;
		VERTEX_SZ = VERTEX_NUM * 2;

		mTexTarget = isOES ? GL_TEXTURE_EXTERNAL_OES : GL_TEXTURE_2D;
		pVertex = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		pVertex.put(vertices);
		pVertex.flip();
		pTexCoord = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		pTexCoord.put(texcoord);
		pTexCoord.flip();

		if (isOES) {
			hProgram = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_SIMPLE_OES);
		} else {
			hProgram = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_SIMPLE);
		}
		// Initialize model-view transformation matrix
		Matrix.setIdentityM(mMvpMatrix, 0);
		init();
	}

	/**
	 * Release processing. Must be called within GL context/EGL rendering context
	 */
	@Override
	public void release() {
		if (hProgram >= 0) {
			GLES20.glDeleteProgram(hProgram);
		}
		hProgram = -1;
	}

	/**
	 * Whether to use external texture
	 * @return
	 */
	public boolean isOES() {
		return mTexTarget == GL_TEXTURE_EXTERNAL_OES;
	}

	/**
	 * Get model-view transformation matrix (returns internal array directly, be careful when modifying)
	 * @return
	 */
	@Override
	public float[] getMvpMatrix() {
		return mMvpMatrix;
	}

	/**
	 * Assign a matrix to the model-view transformation matrix
	 * @param matrix no bounds check, so at least 16 elements must exist from offset
	 * @param offset
	 * @return
	 */
	@Override
	public IDrawer2D setMvpMatrix(final float[] matrix, final int offset) {
		System.arraycopy(matrix, offset, mMvpMatrix, 0, 16);
		return this;
	}

	/**
	 * Get a copy of the model-view transformation matrix
	 * @param matrix no bounds check, so at least 16 elements must exist from offset
	 * @param offset
	 */
	@Override
	public void getMvpMatrix(final float[] matrix, final int offset) {
		System.arraycopy(mMvpMatrix, 0, matrix, offset, 16);
	}

	/**
	 * Helper method to draw the specified texture across the entire drawing area using the specified texture transformation matrix.
	 * If the model-view transformation matrix of this class instance is set, it will also be applied during drawing.
	 * @param texId texture ID
	 * @param tex_matrix texture transformation matrix; if null, the previously applied matrix is reused.
	 * 					no bounds check, so at least 16 elements must be allocated from offset
	 */
	@Override
	public synchronized void draw(final int texId,
		final float[] tex_matrix, final int offset) {

//		if (DEBUG) Log.v(TAG, "draw");
		if (hProgram < 0) return;
		GLES20.glUseProgram(hProgram);
		if (tex_matrix != null) {
			// When texture transformation matrix is specified
			GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, tex_matrix, offset);
		}
		// Set model-view transformation matrix
		GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(mTexTarget, texId);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_NUM);
		GLES20.glBindTexture(mTexTarget, 0);
        GLES20.glUseProgram(0);
	}

	/**
	 * Helper method to draw a Texture object.
	 * Uses the texture name and texture coordinate transformation matrix managed by the Texture object.
	 * @param texture
	 */
	@Override
	public void draw(final ITexture texture) {
		draw(texture.getTexture(), texture.getTexMatrix(), 0);
	}

	/**
	 * Helper method to draw a TextureOffscreen object.
	 * @param offscreen
	 */
	@Override
	public void draw(final TextureOffscreen offscreen) {
		draw(offscreen.getTexture(), offscreen.getTexMatrix(), 0);
	}

	/**
	 * Helper method for texture name generation.
	 * Simply calls GLHelper#initTex.
	 * @return texture ID
	 */
	public int initTex() {
		return GLHelper.initTex(mTexTarget, GLES20.GL_NEAREST);
	}

	/**
	 * Helper method for texture name disposal.
	 * Simply calls GLHelper.deleteTex.
	 * @param hTex
	 */
	public void deleteTex(final int hTex) {
		GLHelper.deleteTex(hTex);
	}

	/**
	 * Change vertex shader and fragment shader.
	 * Must be called within GL context/EGL rendering context.
	 * Returns with glUseProgram called.
	 * @param vs vertex shader string
	 * @param fs fragment shader string
	 */
	public synchronized void updateShader(final String vs, final String fs) {
		release();
		hProgram = GLHelper.loadShader(vs, fs);
		init();
	}

	/**
	 * Change fragment shader.
	 * Must be called within GL context/EGL rendering context.
	 * Returns with glUseProgram called.
	 * @param fs fragment shader string
	 */
	public void updateShader(final String fs) {
		updateShader(VERTEX_SHADER, fs);
	}

	/**
	 * Reset vertex shader and fragment shader to default
	 */
	public void resetShader() {
		release();
		if (isOES()) {
			hProgram = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_SIMPLE_OES);
		} else {
			hProgram = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_SIMPLE);
		}
		init();
	}

	/**
	 * Get attribute variable location.
	 * Returns with glUseProgram called.
	 * @param name
	 * @return
	 */
	@Override
	public int glGetAttribLocation(final String name) {
		GLES20.glUseProgram(hProgram);
		return GLES20.glGetAttribLocation(hProgram, name);
	}

	/**
	 * Get uniform variable location.
	 * Returns with glUseProgram called.
	 * @param name
	 * @return
	 */
	@Override
	public int glGetUniformLocation(final String name) {
		GLES20.glUseProgram(hProgram);
		return GLES20.glGetUniformLocation(hProgram, name);
	}

	/**
	 * Returns with glUseProgram called.
	 */
	@Override
	public void glUseProgram() {
		GLES20.glUseProgram(hProgram);
	}

	/**
	 * Initialization processing when shader program changes.
	 * Returns with glUseProgram called.
	 */
	private void init() {
		GLES20.glUseProgram(hProgram);
		maPositionLoc = GLES20.glGetAttribLocation(hProgram, "aPosition");
		maTextureCoordLoc = GLES20.glGetAttribLocation(hProgram, "aTextureCoord");
		muMVPMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uMVPMatrix");
		muTexMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uTexMatrix");
		//
		GLES20.glUniformMatrix4fv(muMVPMatrixLoc,
			1, false, mMvpMatrix, 0);
		GLES20.glUniformMatrix4fv(muTexMatrixLoc,
			1, false, mMvpMatrix, 0);
		GLES20.glVertexAttribPointer(maPositionLoc,
			2, GLES20.GL_FLOAT, false, VERTEX_SZ, pVertex);
		GLES20.glVertexAttribPointer(maTextureCoordLoc,
			2, GLES20.GL_FLOAT, false, VERTEX_SZ, pTexCoord);
		GLES20.glEnableVertexAttribArray(maPositionLoc);
		GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
	}
}
