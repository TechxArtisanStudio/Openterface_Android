/**
 * @Title: TouchPadHelpDialog
 * @Package com.openterface.AOS.view
 * @Description: Help dialog showing TouchPad gesture guide
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
package com.openterface.AOS.view;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;

import com.openterface.AOS.R;

/**
 * Dialog showing a help guide for TouchPad gestures and mouse button strip.
 */
public class TouchPadHelpDialog {

    /**
     * Show the TouchPad help dialog.
     *
     * @param context The context to show the dialog in
     */
    public static void show(Context context) {
        View dialogView = View.inflate(context, R.layout.dialog_touchpad_help, null);

        new AlertDialog.Builder(context)
                .setTitle(R.string.touchpad_help_title)
                .setView(dialogView)
                .setPositiveButton(R.string.ok_button, null)
                .show();
    }
}
