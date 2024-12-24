/**
* @Title: MainActivity
* @Package com.openterface.AOS.activity
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
package com.openterface.AOS.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kyleduo.switchbutton.SwitchButton;
import com.openterface.AOS.serial.CustomTouchListener;
import com.openterface.AOS.serial.UsbDeviceManager;
import com.openterface.AOS.target.KeyBoardManager;
import com.openterface.AOS.target.MouseManager;
import com.google.gson.Gson;
import com.openterface.AOS.ImageCapture;
import com.openterface.AOS.VideoCapture;
import com.hjq.permissions.XXPermissions;
import com.serenegiant.opengl.renderer.MirrorMode;
import com.openterface.AOS.CameraHelper;
import com.openterface.AOS.ICameraHelper;
import com.serenegiant.usb.IButtonCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.utils.UriHelper;
import com.openterface.AOS.R;
import com.openterface.AOS.databinding.ActivityMainBinding;
import com.openterface.AOS.fragment.CameraControlsDialogFragment;
import com.openterface.AOS.fragment.DeviceListDialogFragment;
import com.openterface.AOS.fragment.VideoFormatDialogFragment;
import com.openterface.AOS.utils.SaveHelper;

import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Queue;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean DEBUG = true;

    private ActivityMainBinding mBinding;

    private static final int QUARTER_SECOND = 250;

    private static final int DEFAULT_WIDTH = 640;
    private static final int DEFAULT_HEIGHT = 480;

    /**
     * Camera preview width
     */
    private int mPreviewWidth = DEFAULT_WIDTH;
    /**
     * Camera preview height
     */
    private int mPreviewHeight = DEFAULT_HEIGHT;

    private int mPreviewRotation = 0;

    private ICameraHelper mCameraHelper;

    private UsbDevice mUsbDevice;
    private final ICameraHelper.StateCallback mStateCallback = new MyCameraHelperCallback();

    private long mRecordStartTime = 0;
    private Timer mRecordTimer = null;
    private DecimalFormat mDecimalFormat;

    private boolean mIsRecording = false;
    private boolean mIsCameraConnected = false;

    private CameraControlsDialogFragment mControlsDialog;
    private DeviceListDialogFragment mDeviceListDialog;
    private VideoFormatDialogFragment mFormatDialog;

    private UsbManager usbManager;

    private GestureDetector gestureDetector;

    private UsbDeviceManager usbDeviceManager;

    public static boolean mKeyboardRequestSent = false;

    private LinearLayout optionsBar, Fragment_KeyBoard_ShortCut, Fragment_KeyBoard_Function, keyBoardView;
    private boolean isOptionsBarVisible = false;

    private static boolean KeyMouse_state = false;
    private static boolean keyMouseAbsCtrlState = true;


    KeyBoardManager keyBoardManager = new KeyBoardManager(this);

    private final Queue<Character> characterQueue = new LinkedList<>();
    private String currentFunctionKey;

    private Button KeyBoard_ShortCut, KeyBoard_Function;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Window window = getWindow();
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(window, window.getDecorView());
        // Hide the system bars.
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        super.onCreate(savedInstanceState);

        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        setSupportActionBar(mBinding.toolbar);

        checkCameraHelper();

        setListeners();

        //init serial-2.0 permission,you must agree
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        usbDeviceManager = new UsbDeviceManager(this, usbManager);
        usbDeviceManager.init();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        //deal mouse click and button ,you can jump CustomTouchListener java
        mBinding.viewMainPreview.setOnTouchListener(new CustomTouchListener(usbDeviceManager));

        // Setting up the gesture detector
        gestureDetector = new GestureDetector(this, new GestureListener());

        // Initialize the options bar
        optionsBar = findViewById(R.id.optionsBar);

        // Define button IDs
        int[] buttonIds = {R.id.Function1, R.id.Function2, R.id.Function3, R.id.Function4, R.id.Function5, R.id.Function6,
                R.id.Function7, R.id.Function8, R.id.Function9, R.id.Function10, R.id.Function11, R.id.Function12,
                R.id.Win, R.id.PrtSc, R.id.ScrLk, R.id.Pause, R.id.Ins, R.id.Home, R.id.End, R.id.PgUp, R.id.PgDn,
                R.id.NumLk, R.id.TAB, R.id.Esc, R.id.Delete, R.id.ENTER};

        // Loop through button IDs and set click listeners
        for (int buttonId : buttonIds) {
            Button button = findViewById(buttonId);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    KeyBoardManager.handleButtonClick(buttonId);
                }
            });
        }

        Button CtrlAltDelButton = findViewById(R.id.CtrlAltDel);
        CtrlAltDelButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                KeyBoardManager.sendKeyBoardFunctionCtrlAltDel();
            }
        });

        SwitchButton switchButton = findViewById(R.id.switchButton);
        switchButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                KeyMouse_state = true;
                CustomTouchListener.KeyMouse_state(KeyMouse_state, keyMouseAbsCtrlState);
//                Log.d(TAG, "Change ABS KeyMouse");
            } else {
                KeyMouse_state = false;
                CustomTouchListener.KeyMouse_state(KeyMouse_state, keyMouseAbsCtrlState);
//                Log.d(TAG, "Change REL KeyMouse");
            }
        });

        ImageButton btnToggleAppBar = findViewById(R.id.btnToggleAppBar);
        final AppBarLayout HideAppBarLayout = findViewById(R.id.HideAppBarLayout);

        FloatingActionButton btnOpenAppBar = findViewById(R.id.btnOpenAppBar);
        btnOpenAppBar.setVisibility(View.GONE);

        btnToggleAppBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (HideAppBarLayout.getVisibility() == View.VISIBLE) {
                    HideAppBarLayout.setVisibility(View.GONE);
                    btnOpenAppBar.setVisibility(View.VISIBLE);
                    Log.d(TAG, "toggle is GONE");
                } else {
                    HideAppBarLayout.setVisibility(View.VISIBLE);
                    btnOpenAppBar.setVisibility(View.GONE);
                    Log.d(TAG, "toggle is visible");
                }
            }
        });

        btnOpenAppBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (HideAppBarLayout.getVisibility() == View.VISIBLE){
                    HideAppBarLayout.setVisibility(View.GONE);
                    btnOpenAppBar.setVisibility(View.VISIBLE);
                }else {
                    HideAppBarLayout.setVisibility(View.VISIBLE);
                    btnOpenAppBar.setVisibility(View.GONE);
                }
            }
        });

        usbDeviceManager.setOnDataReadListener(new UsbDeviceManager.OnDataReadListener() {
            @Override
            public void onDataRead() {

                sendNextCharacter();

            }
        });

        Fragment_KeyBoard_ShortCut = findViewById(R.id.Fragment_KeyBoard_ShortCut);
        Fragment_KeyBoard_Function = findViewById(R.id.Fragment_KeyBoard_Function);
        keyBoardView = findViewById(R.id.KeyBoard_View);

        KeyBoard_ShortCut = findViewById(R.id.KeyBoard_ShortCut);
        KeyBoard_Function = findViewById(R.id.KeyBoard_Function);

        KeyBoard_ShortCut.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);//close keyboard

                if (Fragment_KeyBoard_ShortCut.getVisibility() == View.VISIBLE){
                    KeyBoard_ShortCut.setBackgroundResource(R.drawable.nopress_button_background);
                    Fragment_KeyBoard_ShortCut.setVisibility(View.GONE);
                }else {
                    Fragment_KeyBoard_ShortCut.setVisibility(View.VISIBLE);
                    KeyBoard_ShortCut.setBackgroundResource(R.drawable.press_button_background);
                }

                if (Fragment_KeyBoard_Function.getVisibility() == View.VISIBLE) {
                    Fragment_KeyBoard_Function.setVisibility(View.GONE);
                    KeyBoard_Function.setBackgroundResource(R.drawable.nopress_button_background);
                }
            }
        });


        KeyBoard_Function.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);//close keyboard

                if (Fragment_KeyBoard_Function.getVisibility() == View.VISIBLE){
                    Fragment_KeyBoard_Function.setVisibility(View.GONE);
                    KeyBoard_Function.setBackgroundResource(R.drawable.nopress_button_background);
                }else {
                    Fragment_KeyBoard_Function.setVisibility(View.VISIBLE);
                    KeyBoard_Function.setBackgroundResource(R.drawable.press_button_background);
                }

                if (Fragment_KeyBoard_ShortCut.getVisibility() == View.VISIBLE) {
                    Fragment_KeyBoard_ShortCut.setVisibility(View.GONE);
                    KeyBoard_ShortCut.setBackgroundResource(R.drawable.nopress_button_background);
                }
            }
        });

        ImageButton KeyBoard_Close = findViewById(R.id.KeyBoard_Close);
        KeyBoard_Close.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);//open keyboard
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                keyBoardView.setVisibility(View.GONE);
            }
        });

        FloatingActionButton set_up_button = findViewById(R.id.set_up_button);
        DrawerLayout drawer_layout = findViewById(R.id.drawer_layout);
        drawer_layout.setScrimColor(0x00ffffff);
        set_up_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (drawer_layout.isDrawerOpen(GravityCompat.END)) {
                    drawer_layout.closeDrawer(GravityCompat.END);
                } else {
                    drawer_layout.openDrawer(GravityCompat.END);
                }
            }
        });

        LinearLayout buttonLayout, optionLayout;
        buttonLayout = findViewById(R.id.buttonLayout);
        optionLayout = findViewById(R.id.optionLayout);

        Button Abs_ctrl_default_button = findViewById(R.id.Abs_ctrl_default_button);
        Button Abs_ctrl_drag_button = findViewById(R.id.Abs_ctrl_drag_button);
        Drawable Abs_ctrl_default_button_drawable = Abs_ctrl_default_button.getCompoundDrawables()[1];
        Drawable Abs_ctrl_drag_button_drawable = Abs_ctrl_drag_button.getCompoundDrawables()[1];
        Button drawerButton = findViewById(R.id.drawerButton);


        Abs_ctrl_default_button_drawable.setColorFilter(getResources().getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_IN);
        Abs_ctrl_default_button.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        Abs_ctrl_default_button.setOnClickListener(v -> {

            if (Abs_ctrl_default_button_drawable != null) {

                Abs_ctrl_default_button_drawable.setColorFilter(getResources().getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_IN);
                Abs_ctrl_default_button.setTextColor(getResources().getColor(android.R.color.holo_red_light));

                Abs_ctrl_drag_button_drawable.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_IN);
                Abs_ctrl_drag_button.setTextColor(getResources().getColor(android.R.color.white));

            }
        });

        Abs_ctrl_drag_button.setOnClickListener(v -> {

            if (Abs_ctrl_drag_button_drawable != null) {

                Abs_ctrl_drag_button_drawable.setColorFilter(getResources().getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_IN);
                Abs_ctrl_drag_button.setTextColor(getResources().getColor(android.R.color.holo_red_light));

                Abs_ctrl_default_button_drawable.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_IN);
                Abs_ctrl_default_button.setTextColor(getResources().getColor(android.R.color.white));

                KeyMouse_state = true;
                CustomTouchListener.KeyMouse_state(KeyMouse_state, keyMouseAbsCtrlState);

            }
        });

        drawerButton.setOnClickListener(v -> {
            buttonLayout.setVisibility(View.GONE);
            optionLayout.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.optionA).setOnClickListener(v -> {
            buttonLayout.setVisibility(View.VISIBLE);
            optionLayout.setVisibility(View.GONE);
        });

        findViewById(R.id.optionB).setOnClickListener(v -> {
            buttonLayout.setVisibility(View.VISIBLE);
            optionLayout.setVisibility(View.GONE);
        });
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
        String functionKey = KeyBoardManager.getFunctionKey(event, keyCode);
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE && event.getCharacters() != null) {
            String characters = event.getCharacters();
            System.out.println("in this keycode: " + keyCode);
            System.out.println("in this count: " + count);
            System.out.println("in this event: " + event);
            System.out.println("Characters: " + characters);

            for (char Multiple_key : characters.toCharArray()) {
                characterQueue.add(Multiple_key);
            }

            currentFunctionKey = functionKey;
            sendNextCharacter();

            return true;
        }
        return super.onKeyMultiple(keyCode, count, event);
    }

    private void sendNextCharacter() {
        if (!characterQueue.isEmpty() && currentFunctionKey != null) {
            char nextChar = characterQueue.poll();
            String keyName = String.valueOf(nextChar);
            KeyBoardManager.sendKeyboardMultiple(keyName);

            KeyBoardManager.EmptyKeyboard();

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "event: " + event);

        String functionKey = KeyBoardManager.getFunctionKey(event, keyCode);
        String keyName = KeyBoardManager.getKeyName(keyCode);

        String pressedChar = String.valueOf((char) event.getUnicodeChar());
        String targetChars = "~!@#$%^&*()_+{}|:\"<>?";
        if (targetChars.contains(pressedChar)){
            Log.d(TAG, "Detected special character: " + pressedChar);
            KeyBoardManager.sendKeyboardRequest(functionKey, pressedChar);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mKeyboardRequestSent = true;
                }
            }, 100);
//            KeyBoardManager.DetectedCharacter(functionKey, pressedChar, targetChars);
            return true;
        }else{
            KeyBoardManager.sendKeyBoardData(functionKey, keyName);
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "event: " + event);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (mKeyboardRequestSent) {
                    KeyBoardManager.EmptyKeyboard();
                    mKeyboardRequestSent = false;
                } else {
                    new Handler().postDelayed(this, 50);
                }
            }
        });
        return super.onKeyUp(keyCode, event);
    }

    //deal long click event
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        usbDeviceManager.handleUsbDevice(intent);
        if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            if (!mIsCameraConnected) {
                mUsbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                selectDevice(mUsbDevice);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        initPreviewView();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mIsRecording) {
            toggleVideoRecord(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearCameraHelper();

//        usbDeviceManager.release();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_control) {
            showCameraControlsDialog();
        } else if (id == R.id.action_device) {
            showDeviceListDialog();
        } else if (id == R.id.action_safely_eject) {
            safelyEject();
        } else if (id == R.id.action_settings) {
        } else if (id == R.id.action_video_format) {
            showVideoFormatDialog();
        } else if (id == R.id.action_rotate_90_CW) {
            rotateBy(90);
        } else if (id == R.id.action_rotate_90_CCW) {
            rotateBy(-90);
        } else if (id == R.id.action_flip_horizontally) {
            flipHorizontally();
        } else if (id == R.id.action_flip_vertically) {
            flipVertically();
        } else if (id == R.id.keyBoard) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);//open keyboard
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
        } else if (id == R.id.FunctionKey) {
            toggleOptionsBar();
        }

        return true;
    }

    private void toggleOptionsBar() {
        if (isOptionsBarVisible) {
            optionsBar.setVisibility(View.GONE);
        } else {
            optionsBar.setVisibility(View.VISIBLE);
        }
        isOptionsBarVisible = !isOptionsBarVisible;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mIsCameraConnected) {
            menu.findItem(R.id.action_control).setVisible(true);
            menu.findItem(R.id.action_safely_eject).setVisible(true);
            menu.findItem(R.id.action_video_format).setVisible(true);
            menu.findItem(R.id.action_rotate_90_CW).setVisible(true);
            menu.findItem(R.id.action_rotate_90_CCW).setVisible(true);
            menu.findItem(R.id.action_flip_horizontally).setVisible(true);
            menu.findItem(R.id.action_flip_vertically).setVisible(true);
        } else {
            menu.findItem(R.id.action_control).setVisible(false);
            menu.findItem(R.id.action_safely_eject).setVisible(false);
            menu.findItem(R.id.action_video_format).setVisible(false);
            menu.findItem(R.id.action_rotate_90_CW).setVisible(false);
            menu.findItem(R.id.action_rotate_90_CCW).setVisible(false);
            menu.findItem(R.id.action_flip_horizontally).setVisible(false);
            menu.findItem(R.id.action_flip_vertically).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private void setListeners() {
        mBinding.fabPicture.setOnClickListener(v -> {
            XXPermissions.with(this)
                    .permission(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                    .request((permissions, all) -> {
                        takePicture();
                    });
        });

//        mBinding.fabVideo.setOnClickListener(v -> {
//            XXPermissions.with(this)
//                    .permission(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
//                    .permission(Manifest.permission.RECORD_AUDIO)
//                    .request((permissions, all) -> {
//                        toggleVideoRecord(!mIsRecording);
//                    });
//        });

        mBinding.keyBoard.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);//open keyboard
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);

            keyBoardView.setVisibility(View.VISIBLE);

            Fragment_KeyBoard_Function.setVisibility(View.GONE);
            Fragment_KeyBoard_ShortCut.setVisibility(View.GONE);

            KeyBoard_Function.setBackgroundResource(R.drawable.nopress_button_background);
            KeyBoard_ShortCut.setBackgroundResource(R.drawable.nopress_button_background);

        });
    }

    private void showCameraControlsDialog() {
        if (mControlsDialog == null) {
            mControlsDialog = new CameraControlsDialogFragment(mCameraHelper);
        }
        // When DialogFragment is not showing
        if (!mControlsDialog.isAdded()) {
            mControlsDialog.show(getSupportFragmentManager(), "camera_controls");
        }
    }

    private void showDeviceListDialog() {
        if (mDeviceListDialog != null && mDeviceListDialog.isAdded()) {
            return;
        }

        mDeviceListDialog = new DeviceListDialogFragment(mCameraHelper, mIsCameraConnected ? mUsbDevice : null);
        mDeviceListDialog.setOnDeviceItemSelectListener(usbDevice -> {
            if (mIsCameraConnected) {
                mCameraHelper.closeCamera();
            }
            mUsbDevice = usbDevice;
            selectDevice(mUsbDevice);
        });

        mDeviceListDialog.show(getSupportFragmentManager(), "device_list");
    }

    private void showVideoFormatDialog() {
        if (mFormatDialog != null && mFormatDialog.isAdded()) {
            return;
        }

        mFormatDialog = new VideoFormatDialogFragment(mCameraHelper.getSupportedFormatList(), mCameraHelper.getPreviewSize());
        mFormatDialog.setOnVideoFormatSelectListener(size -> {
            if (mIsCameraConnected && !mCameraHelper.isRecording()) {
                mCameraHelper.stopPreview();
                mCameraHelper.setPreviewSize(size);
                mCameraHelper.startPreview();
                resizePreviewView(size);
                // save selected preview size
                setSavedPreviewSize(size);
            }
        });

        mFormatDialog.show(getSupportFragmentManager(), "video_format");
    }

    private void closeAllDialogFragment() {
        if (mControlsDialog != null && mControlsDialog.isAdded()) {
            mControlsDialog.dismiss();
        }
        if (mDeviceListDialog != null && mDeviceListDialog.isAdded()) {
            mDeviceListDialog.dismiss();
        }
        if (mFormatDialog != null && mFormatDialog.isAdded()) {
            mFormatDialog.dismiss();
        }
    }

    private void safelyEject() {
        if (mCameraHelper != null) {
            mCameraHelper.closeCamera();
        }
    }

    private void rotateBy(int angle) {
        mPreviewRotation += angle;
        mPreviewRotation %= 360;
        if (mPreviewRotation < 0) {
            mPreviewRotation += 360;
        }

        if (mCameraHelper != null) {
            mCameraHelper.setPreviewConfig(
                    mCameraHelper.getPreviewConfig().setRotation(mPreviewRotation));
        }
    }

    private void flipHorizontally() {
        if (mCameraHelper != null) {
            mCameraHelper.setPreviewConfig(
                    mCameraHelper.getPreviewConfig().setMirror(MirrorMode.MIRROR_HORIZONTAL));
        }
    }

    private void flipVertically() {
        if (mCameraHelper != null) {
            mCameraHelper.setPreviewConfig(
                    mCameraHelper.getPreviewConfig().setMirror(MirrorMode.MIRROR_VERTICAL));
        }
    }

    private void checkCameraHelper() {
        if (!mIsCameraConnected) {
            clearCameraHelper();
        }
        initCameraHelper();
    }

    private void initCameraHelper() {
        if (mCameraHelper == null) {
            mCameraHelper = new CameraHelper();
            mCameraHelper.setStateCallback(mStateCallback);

            setCustomImageCaptureConfig();
            setCustomVideoCaptureConfig();
        }
    }

    private void clearCameraHelper() {
        if (DEBUG) Log.v(TAG, "clearCameraHelper:");
        if (mCameraHelper != null) {
            mCameraHelper.release();
            mCameraHelper = null;
        }
    }

    private void initPreviewView() {
        mBinding.viewMainPreview.setAspectRatio(mPreviewWidth, mPreviewHeight);
        Log.d(TAG, "1mPreviewWidth: " + mPreviewWidth + " mPreviewHeight: " + mPreviewHeight);
        mBinding.viewMainPreview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                MouseManager.width_height(width, height);
                Log.d(TAG, "1width: " + width + " height: " + height);
                if (mCameraHelper != null) {
                    mCameraHelper.addSurface(surface, false);
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                if (mCameraHelper != null) {
                    mCameraHelper.removeSurface(surface);
                }
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        });
    }

    public void attachNewDevice(UsbDevice device) {
        if (mUsbDevice == null) {
            mUsbDevice = device;

            selectDevice(device);
        }
    }

    /**
     * In Android9+, connected to the UVC CAMERA, CAMERA permission is required
     *
     * @param device
     */
    protected void selectDevice(UsbDevice device) {
        if (DEBUG) Log.v(TAG, "selectDevice:device=" + device.getDeviceName());

        if (isFinishing()) {
            Log.w(TAG, "Activity is finishing, cannot request permissions.");
            return;
        }

        XXPermissions.with(this)
                .permission(Manifest.permission.CAMERA)
                .request((permissions, all) -> {
                    mIsCameraConnected = false;
                    updateUIControls();

                    if (mCameraHelper != null) {
                        // get usb device
                        mCameraHelper.selectDevice(device);
                    }
                });
    }

    private class MyCameraHelperCallback implements ICameraHelper.StateCallback {
        @Override
        public void onAttach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onAttach:device=" + device.getDeviceName());

            attachNewDevice(device);
        }

        /**
         * After obtaining USB device permissions, connect the USB camera
         */
        @Override
        public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
            if (DEBUG) Log.v(TAG, "onDeviceOpen:device=" + device.getDeviceName());

            mCameraHelper.openCamera(getSavedPreviewSize());

            mCameraHelper.setButtonCallback(new IButtonCallback() {
                @Override
                public void onButton(int button, int state) {
                    Toast.makeText(MainActivity.this, "onButton(button=" + button + "; " +
                            "state=" + state + ")", Toast.LENGTH_SHORT).show();
                }
            });

            usbDeviceManager.init();
        }

        @Override
        public void onCameraOpen(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCameraOpen:device=" + device.getDeviceName());
            mCameraHelper.startPreview();

            // After connecting to the camera, you can get preview size of the camera
            Size size = mCameraHelper.getPreviewSize();
            if (size != null) {
                resizePreviewView(size);
            }

            if (mBinding.viewMainPreview.getSurfaceTexture() != null) {
                mCameraHelper.addSurface(mBinding.viewMainPreview.getSurfaceTexture(), false);
            }

            mIsCameraConnected = true;
            updateUIControls();
//            usbDeviceManager.init();
        }

        @Override
        public void onCameraClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCameraClose:device=" + device.getDeviceName());

//            usbDeviceManager.release();

            if (mIsRecording) {
                toggleVideoRecord(false);
            }

            if (mCameraHelper != null && mBinding.viewMainPreview.getSurfaceTexture() != null) {
                mCameraHelper.removeSurface(mBinding.viewMainPreview.getSurfaceTexture());
            }

            mIsCameraConnected = false;
            updateUIControls();

            closeAllDialogFragment();
        }

        @Override
        public void onDeviceClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDeviceClose:device=" + device.getDeviceName());
        }

        @Override
        public void onDetach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDetach:device=" + device.getDeviceName());

            if (device.equals(mUsbDevice)) {
                mUsbDevice = null;
            }
        }

        @Override
        public void onCancel(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCancel:device=" + device.getDeviceName());

            if (device.equals(mUsbDevice)) {
                mUsbDevice = null;
            }
        }
    }

    private void resizePreviewView(Size size) {
        // Update the preview size
//        mPreviewWidth = size.width;
//        mPreviewHeight = size.height;
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        if (wm != null) {
            Display display = wm.getDefaultDisplay();
            display.getRealMetrics(metrics);
            mPreviewWidth = metrics.widthPixels;
            mPreviewHeight = metrics.heightPixels;

        }
        Log.d(TAG, "22mPreviewWidth: " + mPreviewWidth + " mPreviewHeight: " + mPreviewHeight);
        // Set the aspect ratio of TextureView to match the aspect ratio of the camera
        mBinding.viewMainPreview.setAspectRatio(mPreviewWidth, mPreviewHeight);
    }

    private void updateUIControls() {
        runOnUiThread(() -> {
            if (mIsCameraConnected) {
                mBinding.viewMainPreview.setVisibility(View.VISIBLE);
                mBinding.tvConnectUSBCameraTip.setVisibility(View.GONE);

                mBinding.keyBoard.setVisibility(View.VISIBLE);
//                mBinding.fabPicture.setVisibility(View.VISIBLE);
//                mBinding.fabVideo.setVisibility(View.VISIBLE);

                // Update record button
                int colorId = R.color.WHITE;
                if (mIsRecording) {
                    colorId = R.color.RED;
                }
                ColorStateList colorStateList = ColorStateList.valueOf(getResources().getColor(colorId));
//                mBinding.fabVideo.setSupportImageTintList(colorStateList);

            } else {
                mBinding.viewMainPreview.setVisibility(View.GONE);
                mBinding.tvConnectUSBCameraTip.setVisibility(View.VISIBLE);

                mBinding.keyBoard.setVisibility(View.GONE);
//                mBinding.fabPicture.setVisibility(View.GONE);
//                mBinding.fabVideo.setVisibility(View.GONE);

                mBinding.tvVideoRecordTime.setVisibility(View.GONE);
            }
            invalidateOptionsMenu();
        });
    }

    private Size getSavedPreviewSize() {
        String key = getString(R.string.saved_preview_size) + USBMonitor.getProductKey(mUsbDevice);
        String sizeStr = getPreferences(MODE_PRIVATE).getString(key, null);
        if (TextUtils.isEmpty(sizeStr)) {
            return null;
        }
        Gson gson = new Gson();
        return gson.fromJson(sizeStr, Size.class);
    }

    private void setSavedPreviewSize(Size size) {
        String key = getString(R.string.saved_preview_size) + USBMonitor.getProductKey(mUsbDevice);
        Gson gson = new Gson();
        String json = gson.toJson(size);
        getPreferences(MODE_PRIVATE)
                .edit()
                .putString(key, json)
                .apply();
    }

    private void setCustomImageCaptureConfig() {
//        mCameraHelper.setImageCaptureConfig(
//                mCameraHelper.getImageCaptureConfig().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY));
        mCameraHelper.setImageCaptureConfig(
                mCameraHelper.getImageCaptureConfig().setJpegCompressionQuality(90));
    }

    public void takePicture() {
        if (mIsRecording) {
            return;
        }

        try {
            File file = new File(SaveHelper.getSavePhotoPath());
            ImageCapture.OutputFileOptions options =
                    new ImageCapture.OutputFileOptions.Builder(file).build();
            mCameraHelper.takePicture(options, new ImageCapture.OnImageCaptureCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    Toast.makeText(MainActivity.this,
                            "save \"" + UriHelper.getPath(MainActivity.this, outputFileResults.getSavedUri()) + "\"",
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(int imageCaptureError, @NonNull String message, @Nullable Throwable cause) {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    public void toggleVideoRecord(boolean isRecording) {
        try {
            if (isRecording) {
                if (mIsCameraConnected && mCameraHelper != null && !mCameraHelper.isRecording()) {
                    startRecord();
                }
            } else {
                if (mIsCameraConnected && mCameraHelper != null && mCameraHelper.isRecording()) {
                    stopRecord();
                }
                stopRecordTimer();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
            stopRecordTimer();
        }

        mIsRecording = isRecording;

        updateUIControls();
    }

    private void setCustomVideoCaptureConfig() {
        mCameraHelper.setVideoCaptureConfig(
                mCameraHelper.getVideoCaptureConfig()
//                        .setAudioCaptureEnable(false)
                        .setBitRate((int) (1024 * 1024 * 25 * 0.25))
                        .setVideoFrameRate(25)
                        .setIFrameInterval(1));
    }

    private void startRecord() {
        File file = new File(SaveHelper.getSaveVideoPath());
        VideoCapture.OutputFileOptions options =
                new VideoCapture.OutputFileOptions.Builder(file).build();
        mCameraHelper.startRecording(options, new VideoCapture.OnVideoCaptureCallback() {
            @Override
            public void onStart() {
                startRecordTimer();
            }

            @Override
            public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                toggleVideoRecord(false);

                Toast.makeText(
                        MainActivity.this,
                        "save \"" + UriHelper.getPath(MainActivity.this, outputFileResults.getSavedUri()) + "\"",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                toggleVideoRecord(false);

                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void stopRecord() {
        mCameraHelper.stopRecording();
    }

    private void startRecordTimer() {
        runOnUiThread(() -> mBinding.tvVideoRecordTime.setVisibility(View.VISIBLE));

        // Set "00:00:00" to record time TextView
        setVideoRecordTimeText(formatTime(0));

        // Start Record Timer
        mRecordStartTime = SystemClock.elapsedRealtime();
        mRecordTimer = new Timer();
        //The timer is refreshed every quarter second
        mRecordTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long recordTime = (SystemClock.elapsedRealtime() - mRecordStartTime) / 1000;
                if (recordTime > 0) {
                    setVideoRecordTimeText(formatTime(recordTime));
                }
            }
        }, QUARTER_SECOND, QUARTER_SECOND);
    }

    private void stopRecordTimer() {
        runOnUiThread(() -> mBinding.tvVideoRecordTime.setVisibility(View.GONE));

        // Stop Record Timer
        mRecordStartTime = 0;
        if (mRecordTimer != null) {
            mRecordTimer.cancel();
            mRecordTimer = null;
        }
        // Set "00:00:00" to record time TextView
        setVideoRecordTimeText(formatTime(0));
    }

    private void setVideoRecordTimeText(String timeText) {
        runOnUiThread(() -> {
            mBinding.tvVideoRecordTime.setText(timeText);
        });
    }

    /**
     *
     *
     * @param time 秒
     * @return
     */
    private String formatTime(long time) {
        if (mDecimalFormat == null) {
            mDecimalFormat = new DecimalFormat("00");
        }
        String hh = mDecimalFormat.format(time / 3600);
        String mm = mDecimalFormat.format(time % 3600 / 60);
        String ss = mDecimalFormat.format(time % 60);
        return hh + ":" + mm + ":" + ss;
    }
}