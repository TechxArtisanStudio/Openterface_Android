/**
* @Title: SaveHelper
* @Package com.openterface.AOS.utils
* @Description:
 * ========================================================================== *
 *                                                                            *
 *    This file is part of the Openterface Mini KVM App Android version       *
 *                                                                            *
 *    Copyright (C) 2024   <info@openterface.com>                             *
 *                                                                            *
 *    This program is free software: you can redistribute it and/or modify    *
 *    it under the terms of the GNU General Public License as published by    *
 *    the Free Software Foundation version 3.                                 *
 *                                                                            *
 *    This program is distributed in the hope that it will be useful, but     *
 *    WITHOUT ANY WARRANTY; without even the implied warranty of              *
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU        *
 *    General Public License for more details.                                *
 *                                                                            *
 *    You should have received a copy of the GNU General Public License       *
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.    *
 *                                                                            *
 * ========================================================================== *
*/
package com.openterface.AOS.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import com.serenegiant.utils.UVCUtils;
import com.openterface.AOS.R;

import java.io.File;
import java.util.Date;

/**
 * Utility for generating save paths for photos and videos.
 *
 * On API 30+ (scoped storage enforced), uses app-specific external storage
 * via getExternalFilesDir() which requires no permissions and is scoped
 * storage compliant. On older APIs, still uses app-specific storage for
 * consistency (previously used Environment.getExternalStorageDirectory()).
 */
public class SaveHelper {

    public static String BaseStoragePath = null;

    /**
     * Get the base storage path. Uses app-specific external storage directory
     * which is scoped-storage compliant on all API levels.
     *
     * @param context optional context; if null, falls back to UVCUtils.getApplication()
     */
    public static void checkBaseStoragePath(Context context) {
        if (BaseStoragePath == null) {
            Context ctx = context != null ? context : UVCUtils.getApplication();
            File dir = ctx.getExternalFilesDir(null);
            if (dir != null) {
                BaseStoragePath = dir.getPath();
            } else {
                // Fallback: use internal files dir if external is unavailable
                BaseStoragePath = ctx.getFilesDir().getPath();
            }
        }
    }

    /** @deprecated Use {@link #checkBaseStoragePath(Context)} instead */
    @Deprecated
    public static void checkBaseStoragePath() {
        checkBaseStoragePath(null);
    }

    public static String getSavePhotoPath() {
        checkBaseStoragePath(null);

        String parentPath = BaseStoragePath + File.separator
                + Environment.DIRECTORY_PICTURES + File.separator
                + TimeFormatter.format_yyyyMMdd(new Date()) + File.separator + "photo";
        File folder = new File(parentPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return parentPath + File.separator + TimeFormatter.format_yyyy_MM_dd_HH_mm_ss(new Date()) + ".jpg";
    }

    public static Uri getSavePhotoUri() {
        return Uri.fromFile(new File(getSavePhotoPath()));
    }

    public static String getSaveVideoPath() {
        checkBaseStoragePath(null);

        String parentPath = BaseStoragePath + File.separator
                + Environment.DIRECTORY_MOVIES + File.separator
                + TimeFormatter.format_yyyyMMdd(new Date()) + File.separator + "video";
        File folder = new File(parentPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return parentPath + File.separator + TimeFormatter.format_yyyy_MM_dd_HH_mm_ss(new Date()) + ".mp4";
    }

    public static Uri getSaveVideoUri() {
        return Uri.fromFile(new File(getSaveVideoPath()));
    }

}
