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

import android.net.Uri;
import android.os.Environment;

import com.serenegiant.utils.UVCUtils;
import com.openterface.AOS.R;

import java.io.File;
import java.util.Date;

public class SaveHelper {

    public static String BaseStoragePath = null;

    public static void checkBaseStoragePath() {
        if (BaseStoragePath == null) {
            BaseStoragePath = Environment.getExternalStorageDirectory().getPath() + File.separator + UVCUtils.getApplication().getString(R.string.app_name);
        }
    }

    public static String getSavePhotoPath() {
        checkBaseStoragePath();

        String parentPath = BaseStoragePath + File.separator + TimeFormatter.format_yyyyMMdd(new Date()) + File.separator + "photo";
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
        checkBaseStoragePath();

        String parentPath = BaseStoragePath + File.separator + TimeFormatter.format_yyyyMMdd(new Date()) + File.separator + "video";
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
