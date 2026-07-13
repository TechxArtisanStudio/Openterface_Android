/**
* @Title: MyApplication
* @Package com.openterface.AOS
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
package com.openterface.AOS;

import android.app.Application;
import android.graphics.Color;
import android.util.Log;

import com.openterface.AOS.manager.LocaleManager;
import com.openterface.AOS.manager.ThemeManager;

import jp.wasabeef.takt.Seat;
import jp.wasabeef.takt.Takt;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize locale manager and apply persisted locale
        LocaleManager localeManager = LocaleManager.getInstance();
        localeManager.initialize(this);
        localeManager.applyPersistedLocale();

        // Apply saved theme mode (light/dark/system) at app startup
        ThemeManager themeManager = ThemeManager.getInstance();
        themeManager.initialize(this);
        String savedMode = themeManager.getThemeMode();
        themeManager.applyThemeMode(savedMode);
//        Takt.stock(this)
//                .seat(Seat.TOP_RIGHT)
//                .interval(250)
//                .color(Color.WHITE)
//                .size(14f)
//                .alpha(0.5f)
//                .listener(fps -> {
//                    Log.d("uvcdemo", (int) fps + " fps");
//                });
    }
}
