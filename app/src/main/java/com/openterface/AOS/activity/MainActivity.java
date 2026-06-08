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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.openterface.AOS.IImageCapture;
import com.openterface.AOS.KeyBoardClick.KeyBoardAlt;
import com.openterface.AOS.KeyBoardClick.KeyBoardClose;
import com.openterface.AOS.KeyBoardClick.KeyBoardCtrl;
import com.openterface.AOS.KeyBoardClick.KeyBoardFunction;
import com.openterface.AOS.KeyBoardClick.KeyBoardOpacity;
import com.openterface.AOS.KeyBoardClick.KeyBoardShift;
import com.openterface.AOS.KeyBoardClick.KeyBoardShortCut;
import com.openterface.AOS.KeyBoardClick.KeyBoardSystem;
import com.openterface.AOS.KeyBoardClick.KeyBoardWin;
import com.openterface.AOS.view.TouchPadView;
import com.openterface.AOS.view.TouchPadHelpDialog;
import com.openterface.AOS.view.MouseControlStripView;
import com.openterface.AOS.KeyBoardClick.TouchPadSettings;
import com.openterface.AOS.drawerLayout.DrawerLayoutDeal;
import com.openterface.AOS.drawerLayout.ZoomLayoutDeal;
import com.openterface.AOS.serial.CustomTouchListener;
import com.openterface.AOS.serial.CH9329Function;
import com.openterface.AOS.serial.UsbDeviceManager;
import com.openterface.AOS.target.CH9329MSKBMap;
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
import com.serenegiant.widget.AspectRatioSurfaceView;

import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Queue;
import java.util.LinkedList;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean DEBUG = true;

    public ActivityMainBinding mBinding;

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

    public ICameraHelper mCameraHelper;

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

    private KeyBoardOpacity mKeyBoardOpacity;

    private TouchPadView touchPadView;
    private MouseControlStripView mouseControlStripView;
    private FrameLayout touchPadContainer;
    private boolean isTouchPadMode = false;
    private TouchPadSettings touchPadSettings;

    private UsbManager usbManager;

    private GestureDetector gestureDetector;

    private UsbDeviceManager usbDeviceManager;

    public static boolean mKeyboardRequestSent = false;

    private final Queue<Character> characterQueue = new LinkedList<>();
    private String currentFunctionKey;

    // Screen orientation state
    private boolean isPortraitMode = false;
    private static final String KEY_PORTRAIT_MODE = "is_portrait_mode";

    // Portrait 4-zone module state
    public enum ModuleType { NONE, KEYBOARD, MOUSE, IME }
    private ModuleType currentModule = ModuleType.NONE;
    private static final String KEY_CURRENT_MODULE = "current_module";

    // Portrait zone views (use View to avoid ClassCastException across orientation changes)
    private View portraitRootLayout;
    private View portraitVideoContainer;
    private View portraitModuleDrawer;
    private View portraitCollapseHandle;
    private FrameLayout portraitModuleContent;
    private View portraitKeyboardTab;
    private View portraitMouseTab;
    private View portraitImeTab;
    private View portraitSettingsTab;
    private View portraitRotationButton;
    private View portraitShortcutStrip;

    // Portrait module views (for reuse)
    private View keyboardModuleView;
    private View mouseModuleView;
    private View imeModuleView;

    // Gesture detector for collapse
    private GestureDetector collapseGestureDetector;

    private Button action_device, action_safely_eject;
    private Drawable action_device_drawable, action_safely_eject_drawable;

    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    public AspectRatioSurfaceView cameraViewSecond;
    private RelativeLayout thumbnail_container;

    @SuppressLint("ClickableViewAccessibility")//add
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Window window = getWindow();
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(window, window.getDecorView());
        // Hide the system bars.
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        super.onCreate(savedInstanceState);

        //Recording Permission
//        ActivityCompat.requestPermissions(this, permissions, 200);

        //Prevent screen from turning off
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        checkCameraHelper();

        setListeners();

        //init serial-2.0 permission,you must agree
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        usbDeviceManager = new UsbDeviceManager(this, usbManager);
        usbDeviceManager.init();
        
        // Set UsbDeviceManager instance in KeyBoardManager for enhanced FE0C support
        KeyBoardManager.setUsbDeviceManager(usbDeviceManager);
        
        // Set UsbDeviceManager instance in MouseManager for enhanced FE0C support
        MouseManager.setUsbDeviceManager(usbDeviceManager);

        // Initialize the mouse event worker thread
        MouseManager.init();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        //deal mouse click and button ,you can jump CustomTouchListener java
        CustomTouchListener customTouchListener = new CustomTouchListener(this, usbDeviceManager);

        mBinding.viewMainPreview.setOnTouchListener(customTouchListener);

        // Setting up the gesture detector
        gestureDetector = new GestureDetector(this, new GestureListener());

        KeyBoardManager keyBoardManager = new KeyBoardManager(this);

        // Check if we're in portrait mode (layout-port has no keyboard overlay buttons)
        boolean isPortraitLayout = (findViewById(R.id.module_selector_bar) != null);
        isPortraitMode = isPortraitLayout;

        // Only initialize landscape keyboard buttons in landscape layout
        // Portrait uses the 4-zone module system instead
        KeyBoardShortCut KeyBoardShortCut = null;
        KeyBoardCtrl KeyBoardCtrlButton = null;
        KeyBoardShift KeyBoardShiftButton = null;
        KeyBoardAlt KeyBoardAltButton = null;
        KeyBoardWin KeyBoardWinButton = null;
        KeyBoardFunction KeyBoardFunction = null;
        KeyBoardSystem KeyBoardSystem = null;
        KeyBoardClose KeyBoardClose = null;
        DrawerLayoutDeal DrawerLayoutDeal = null;

        if (!isPortraitLayout) {
            //Short Cut Button
            KeyBoardShortCut = new KeyBoardShortCut(this);
            //Ctrl Button
            KeyBoardCtrlButton = new KeyBoardCtrl(this);
            //Shift Button
            KeyBoardShiftButton = new KeyBoardShift(this);
            //Alt Button
            KeyBoardAltButton = new KeyBoardAlt(this);
            //Win Button
            KeyBoardWinButton = new KeyBoardWin(this);
            //FunctionKey Button
            KeyBoardFunction = new KeyBoardFunction(this);
            //System Button
            KeyBoardSystem = new KeyBoardSystem(this);
            //KeyBoard Close Button
            KeyBoardClose = new KeyBoardClose(this);

            //Drawer Layout
            DrawerLayoutDeal = new DrawerLayoutDeal(this, savedInstanceState, mIsRecording);
        }

        usbDeviceManager.setOnDataReadListener(new UsbDeviceManager.OnDataReadListener() {
            @Override
            public void onDataRead(byte[] data, int length) {
                sendNextCharacter();
            }
        });

        // Enable debugging for FE0C keyboard testing
        // enableKeyboardDebugging(); // Commented out - was auto-typing 'B' key on startup

        // Register debug broadcast receiver
        registerDebugReceiver();
        // Modifier keys (Ctrl, Shift, Alt, Win) are now initialized in their constructors
        // with long-press toggle and press visual feedback

        if (!isPortraitLayout && KeyBoardShortCut != null) {
            KeyBoardShortCut.setShortCutButtonsClickColor();//deal short cut button click color
        }
        if (!isPortraitLayout && KeyBoardFunction != null) {
            KeyBoardFunction.setFunctionButtonsClickColor();//deal function button click color
        }
        if (!isPortraitLayout && KeyBoardSystem != null) {
            KeyBoardSystem.setSystemButtonsClickColor();//deal system button click color
        }
        if (!isPortraitLayout && KeyBoardClose != null) {
            KeyBoardClose.setCloseButtonClickColor();//deal close button click color
        }

        //Keyboard Opacity
        if (!isPortraitLayout) {
            mKeyBoardOpacity = new KeyBoardOpacity(this);
            mKeyBoardOpacity.setOpacityButtonClick();
        }

        // Initialize TouchPad
        initTouchPad();

        // TouchPad toggle button (landscape only)
        if (!isPortraitLayout) {
            ImageButton KeyBoard_TouchPad = findViewById(R.id.KeyBoard_TouchPad);
            if (KeyBoard_TouchPad != null) {
                KeyBoard_TouchPad.setOnClickListener(v -> toggleTouchPadMode());
            }

            if (DrawerLayoutDeal != null) {
                DrawerLayoutDeal.setDrawerLayoutButtonClickColor();//deal drawer layout button click color
            }
        }

        action_device = findViewById(R.id.action_device);
        action_safely_eject = findViewById(R.id.action_safely_eject);

        if (action_device != null) action_device_drawable = action_device.getCompoundDrawables()[1];
        if (action_safely_eject != null) action_safely_eject_drawable = action_safely_eject.getCompoundDrawables()[1];

        //Zoom layout deal
        setCameraViewSecond();
        ZoomLayoutDeal ZoomLayoutDeal = new ZoomLayoutDeal(this, mCameraHelper, mBinding);

        KeyBoardManager.setKeyBoardLanguage();

        setLanguage();

        // Initialize portrait 4-zone layout
        initPortraitZones();

    }

    private void setLanguage(){
        Button Left_Than_Button = findViewById(R.id.Left_Than_Button);
        String currentLang = Locale.getDefault().getLanguage();
        if (currentLang.equals("us")) {
            if (Left_Than_Button != null) Left_Than_Button.setText("");
            KeyBoardSystem.setKeyboardLanguage("us");
            KeyBoardFunction.setKeyboardLanguage("us");
        } else if (currentLang.equals("de")) {
            Log.d("KeyBoardSystem", "German Button Pressed: ");

            KeyBoardSystem.setKeyboardLanguage("de");
            KeyBoardFunction.setKeyboardLanguage("de");
        }else {
            if (Left_Than_Button != null) Left_Than_Button.setText("");
            KeyBoardSystem.setKeyboardLanguage("us");
            KeyBoardFunction.setKeyboardLanguage("us");
        }
    }

    private void setCameraViewSecond() {
        cameraViewSecond = findViewById(R.id.cameraViewSecond);
        if (cameraViewSecond == null) {
            Log.d(TAG, "cameraViewSecond not found (possibly portrait mode layout)");
            return;
        }
        cameraViewSecond.setAspectRatio(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        cameraViewSecond.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (mCameraHelper != null) {
                    mCameraHelper.addSurface(holder.getSurface(), false);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                if (mCameraHelper != null) {
                    mCameraHelper.removeSurface(holder.getSurface());
                }
            }
        });
    }


    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return CustomTouchListener.handleGenericMotionEvent(event) || super.onGenericMotionEvent(event);
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
        Log.d(TAG, "=== onKeyDown Debug ===");
        Log.d(TAG, "Android KeyCode: " + keyCode + " (" + KeyEvent.keyCodeToString(keyCode) + ")");
        Log.d(TAG, "Event: " + event);

        // Debug trigger: F12 key for FE0C keyboard testing
        if (keyCode == KeyEvent.KEYCODE_F12) {
            Log.d(TAG, "F12 pressed - triggering FE0C keyboard debug test");
            if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
                usbDeviceManager.debugFE0CKeyboard();
                return true;
            } else {
                Log.w(TAG, "Cannot run debug test - UsbDeviceManager not connected");
                return true;
            }
        }

        // Use the new UsbDeviceManager for keyboard handling if available
        if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
            Log.d(TAG, "Using UsbDeviceManager (connected)");
            Log.d(TAG, "Device type: " + (usbDeviceManager.isFE0CDevice() ? "FE0C" : "Other"));
            
            // Convert Android key codes to HID
            int hidKeyCode = UsbDeviceManager.androidKeyToHID(keyCode);
            int modifiers = UsbDeviceManager.getHIDModifiers(event);
            
            Log.d(TAG, "HID KeyCode: 0x" + Integer.toHexString(hidKeyCode));
            Log.d(TAG, "Modifiers: 0x" + Integer.toHexString(modifiers));
            
            if (hidKeyCode != 0) {
                Log.d(TAG, "Sending key via UsbDeviceManager.sendKeyPressWithAutoRelease()");
                // Use the enhanced method with automatic release
                boolean success = usbDeviceManager.sendKeyPressWithAutoRelease(hidKeyCode, modifiers);
                Log.d(TAG, "UsbDeviceManager result: " + (success ? "SUCCESS" : "FAILED"));
                if (success) {
                    mKeyboardRequestSent = true;
                    return true;
                }
            } else {
                Log.w(TAG, "Unsupported key code for HID conversion");
            }
        } else {
            Log.d(TAG, "UsbDeviceManager not available or not connected");
        }
        
        // Fallback to enhanced method first, then original method
        String functionKey = KeyBoardManager.getFunctionKey(event, keyCode);
        String keyName = KeyBoardManager.getKeyName(keyCode);
        
        Log.d(TAG, "=== FALLBACK METHODS DEBUG ===");
        Log.d(TAG, "FunctionKey: '" + functionKey + "', KeyName: '" + keyName + "'");
        Log.d(TAG, "UsbDeviceManager available: " + (usbDeviceManager != null));
        Log.d(TAG, "UsbDeviceManager connected: " + (usbDeviceManager != null && usbDeviceManager.isConnected()));
        
        // Try enhanced method first (for proper FE0C support)
        if (usbDeviceManager != null) {
            Log.d(TAG, ">>> Trying KeyBoardManager.sendKeyBoardDataEnhanced()");
            boolean success = KeyBoardManager.sendKeyBoardDataEnhanced(usbDeviceManager, functionKey, keyName);
            Log.d(TAG, "Enhanced method result: " + (success ? "SUCCESS" : "FAILED"));
            if (success) {
                mKeyboardRequestSent = true;
                return true;
            }
        } else {
            Log.w(TAG, "UsbDeviceManager not available for enhanced method");
        }
        
        // Original method as final fallback
        Log.d(TAG, "Using original KeyBoardManager.sendKeyBoardData()");
        KeyBoardManager.sendKeyBoardData(functionKey, keyName);

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "=== onKeyUp Debug ===");
        Log.d(TAG, "Android KeyCode: " + keyCode + " (" + KeyEvent.keyCodeToString(keyCode) + ")");
        Log.d(TAG, "mKeyboardRequestSent: " + mKeyboardRequestSent);
        
        // Check if we're using the new UsbDeviceManager with FE0C device
        if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
            Log.d(TAG, "UsbDeviceManager connected, device type: " + 
                  (usbDeviceManager.isFE0CDevice() ? "FE0C" : "Other"));
            // For FE0C and other devices using UsbDeviceManager, 
            // key release is handled automatically, just reset flag
            if (mKeyboardRequestSent) {
                Log.d(TAG, "Key release handled automatically, resetting flag");
                mKeyboardRequestSent = false;
                return true;
            }
        } else {
            Log.d(TAG, "UsbDeviceManager not connected, using original release logic");
        }
        
        // Original release logic for fallback cases
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
        String action = intent.getAction();
        Log.d(TAG, "onNewIntent: action=" + action + " isPortrait=" + isPortraitMode);
        if (action != null && action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            if (!mIsCameraConnected) {
                mUsbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Log.d(TAG, "onNewIntent: USB device attached, device=" + (mUsbDevice != null ? mUsbDevice.getDeviceName() : "null"));
                selectDevice(mUsbDevice);
            } else {
                Log.d(TAG, "onNewIntent: camera already connected, ignoring");
            }
        }
        usbDeviceManager.handleUsbDevice(intent);
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

        // Release the mouse event worker thread
        MouseManager.release();

        // Cleanup debug broadcast receiver
        if (debugReceiver != null) {
            unregisterReceiver(debugReceiver);
        }

//        usbDeviceManager.release();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Save current state
        boolean wasPortrait = isPortraitMode;
        ModuleType savedModule = currentModule;

        // Update orientation state
        isPortraitMode = (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT);

        Log.d(TAG, "Configuration changed: " + (isPortraitMode ? "PORTRAIT" : "LANDSCAPE"));

        // Reload layout for new orientation (this already calls initPreviewView)
        reloadLayoutForOrientation();

        // Restore module state if was expanded
        if (savedModule != ModuleType.NONE && savedModule != null) {
            setModuleState(savedModule, false);
        }

        // Adjust video preview size based on orientation
        if (mCameraHelper != null && mIsCameraConnected) {
            resizePreviewView(mCameraHelper.getPreviewSize());
        }

        Log.d(TAG, "Layout reloaded, module state: " + currentModule);
    }

    /**
     * Reload the layout based on current orientation.
     * Portrait uses the 4-zone layout, landscape uses the original layout.
     */
    private void reloadLayoutForOrientation() {
        Log.d(TAG, "reloadLayoutForOrientation: START");
        // Re-inflate view binding for the new orientation
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        Log.d(TAG, "reloadLayoutForOrientation: layout inflated");

        // Re-check orientation after setting content view
        boolean nowPortrait = (findViewById(R.id.module_selector_bar) != null);
        isPortraitMode = nowPortrait;
        Log.d(TAG, "reloadLayoutForOrientation: nowPortrait=" + nowPortrait);

        // Re-setup all views after layout reload
        setListeners();
        initTouchPad();
        initPortraitZones();
        initPreviewView();
        Log.d(TAG, "reloadLayoutForOrientation: views initialized");

        // Re-attach CustomTouchListener to the new preview view after layout re-inflation
        CustomTouchListener customTouchListener = new CustomTouchListener(this, usbDeviceManager);
        mBinding.viewMainPreview.setOnTouchListener(customTouchListener);

        // Re-setup DrawerLayoutDeal for new layout (only in landscape)
        if (!nowPortrait) {
            try {
                new com.openterface.AOS.drawerLayout.DrawerLayoutDeal(this, null, mIsRecording);
                Log.d(TAG, "reloadLayoutForOrientation: DrawerLayoutDeal created");
            } catch (Exception e) {
                Log.w(TAG, "reloadLayoutForOrientation: DrawerLayoutDeal failed (expected in portrait): " + e.getMessage());
            }
        }
        try {
            com.openterface.AOS.drawerLayout.ZoomLayoutDeal zoomLayoutDeal =
                    new com.openterface.AOS.drawerLayout.ZoomLayoutDeal(this, mCameraHelper, mBinding);
            Log.d(TAG, "reloadLayoutForOrientation: ZoomLayoutDeal created");
        } catch (Exception e) {
            Log.w(TAG, "reloadLayoutForOrientation: ZoomLayoutDeal failed: " + e.getMessage());
        }

        // Restore keyboard button states (only in landscape)
        if (!nowPortrait) {
            try {
                KeyBoardShortCut KeyBoardShortCut = new KeyBoardShortCut(this);
                KeyBoardCtrl KeyBoardCtrlButton = new KeyBoardCtrl(this);
                KeyBoardShift KeyBoardShiftButton = new KeyBoardShift(this);
                KeyBoardAlt KeyBoardAltButton = new KeyBoardAlt(this);
                KeyBoardWin KeyBoardWinButton = new KeyBoardWin(this);
                KeyBoardFunction KeyBoardFunction = new KeyBoardFunction(this);
                KeyBoardSystem KeyBoardSystem = new KeyBoardSystem(this);
                KeyBoardClose KeyBoardClose = new KeyBoardClose(this);

                KeyBoardShortCut.setShortCutButtonsClickColor();
                KeyBoardFunction.setFunctionButtonsClickColor();
                KeyBoardSystem.setSystemButtonsClickColor();
                KeyBoardClose.setCloseButtonClickColor();

                mKeyBoardOpacity = new KeyBoardOpacity(this);
                mKeyBoardOpacity.setOpacityButtonClick();
                Log.d(TAG, "reloadLayoutForOrientation: keyboard buttons restored");
            } catch (Exception e) {
                Log.e(TAG, "reloadLayoutForOrientation: keyboard init failed", e);
            }
        }

        action_device = findViewById(R.id.action_device);
        action_safely_eject = findViewById(R.id.action_safely_eject);
        if (action_device != null) {
            Drawable[] drawables = action_device.getCompoundDrawables();
            if (drawables.length > 1 && drawables[1] != null) {
                action_device_drawable = drawables[1];
            }
        }
        if (action_safely_eject != null) {
            Drawable[] drawables = action_safely_eject.getCompoundDrawables();
            if (drawables.length > 1 && drawables[1] != null) {
                action_safely_eject_drawable = drawables[1];
            }
        }
        Log.d(TAG, "reloadLayoutForOrientation: action buttons restored");

        setCameraViewSecond();
        setLanguage();
        Log.d(TAG, "reloadLayoutForOrientation: camera view and language set");

        // Reconnect camera surfaces after layout reload if camera is connected
        if (mCameraHelper != null && mIsCameraConnected) {
            Log.d(TAG, "Reconnecting camera surfaces after orientation change");
            // Wait for surface to be available before reconnecting
            if (mBinding.viewMainPreview != null && mBinding.viewMainPreview.getSurfaceTexture() != null) {
                mCameraHelper.addSurface(mBinding.viewMainPreview.getSurfaceTexture(), false);
                Log.d(TAG, "reloadLayoutForOrientation: main preview surface reconnected");
            }
            // Add secondary camera surface (landscape only)
            if (mBinding.cameraViewSecond != null && mBinding.cameraViewSecond.getHolder() != null
                    && mBinding.cameraViewSecond.getHolder().getSurface() != null) {
                mCameraHelper.addSurface(mBinding.cameraViewSecond.getHolder().getSurface(), false);
                Log.d(TAG, "reloadLayoutForOrientation: secondary camera surface reconnected");
            }
        }

        // Update UI controls to reflect current state
        updateUIControls();
        Log.d(TAG, "reloadLayoutForOrientation: COMPLETE");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_PORTRAIT_MODE, isPortraitMode);
        outState.putString(KEY_CURRENT_MODULE, currentModule != null ? currentModule.name() : "NONE");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isPortraitMode = savedInstanceState.getBoolean(KEY_PORTRAIT_MODE, false);
        String moduleName = savedInstanceState.getString(KEY_CURRENT_MODULE, "NONE");
        try {
            currentModule = ModuleType.valueOf(moduleName);
        } catch (IllegalArgumentException e) {
            currentModule = ModuleType.NONE;
        }
        if (savedInstanceState != null) {
            isPortraitMode = savedInstanceState.getBoolean(KEY_PORTRAIT_MODE, false);
        }
    }

    private void setListeners() {
        // Only setup floating button listeners for landscape layout
        // Portrait mode uses the Zone 4 tab bar instead
        if (mBinding.keyBoard != null) {
            mBinding.keyBoard.setOnClickListener(v -> {
                // Removed InputMethodManager to prevent Android soft keyboard from appearing
                // InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                // imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
                LinearLayout Fragment_KeyBoard_ShortCut = findViewById(R.id.Fragment_KeyBoard_ShortCut);
                LinearLayout Fragment_KeyBoard_Function = findViewById(R.id.Fragment_KeyBoard_Function);
                LinearLayout Fragment_KeyBoard_System = findViewById(R.id.Fragment_KeyBoard_System);
                LinearLayout keyBoardView = findViewById(R.id.KeyBoard_View);
                Button KeyBoard_ShortCut = findViewById(R.id.KeyBoard_ShortCut);
                Button KeyBoard_Function = findViewById(R.id.KeyBoard_Function);
                ImageButton KeyBoard_System = findViewById(R.id.KeyBoard_System);

                if (keyBoardView != null) {
                    keyBoardView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            Log.d("keyBoardView","keyBoardView.getWidth():"+keyBoardView.getWidth() + "keyBoardView.getHeight():"+keyBoardView.getHeight());
                            ZoomLayoutDeal.getViewWidthHeight(keyBoardView.getWidth(), keyBoardView.getHeight());
                        }
                    });
                    keyBoardView.setVisibility(View.VISIBLE);
                }
                if (mKeyBoardOpacity != null) {
                    mKeyBoardOpacity.restoreOpacity();
                }
                //hide floating button keyboard and set_up_button
                FloatingActionButton keyBoard = findViewById(R.id.keyBoard);
                if (keyBoard != null) keyBoard.setVisibility(View.GONE);
                FloatingActionButton set_up_button = findViewById(R.id.set_up_button);
                if (set_up_button != null) set_up_button.setVisibility(View.GONE);

                if (Fragment_KeyBoard_Function != null) Fragment_KeyBoard_Function.setVisibility(View.GONE);
                if (Fragment_KeyBoard_ShortCut != null) Fragment_KeyBoard_ShortCut.setVisibility(View.GONE);
                if (Fragment_KeyBoard_System != null) Fragment_KeyBoard_System.setVisibility(View.GONE);

                if (KeyBoard_Function != null) KeyBoard_Function.setBackgroundResource(R.drawable.nopress_button_background);
                if (KeyBoard_ShortCut != null) KeyBoard_ShortCut.setBackgroundResource(R.drawable.nopress_button_background);
                if (KeyBoard_System != null) KeyBoard_System.setBackgroundResource(R.drawable.nopress_button_background);

                // Initialize TouchPad settings and help buttons
                initTouchPadButtons();
            });
        }
    }

    public void showCameraControlsDialog() {
        if (mControlsDialog == null) {
            mControlsDialog = new CameraControlsDialogFragment(mCameraHelper);
        }
        // When DialogFragment is not showing
        if (!mControlsDialog.isAdded()) {
            mControlsDialog.show(getSupportFragmentManager(), "camera_controls");
        }
    }

    public void showDeviceListDialog() {
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

    public void showVideoFormatDialog() {
        if (mFormatDialog != null && mFormatDialog.isAdded()) {
            return;
        }

        //Determine whether to open the video
        if (mCameraHelper == null || !mIsCameraConnected) {
            Log.e("MainActivity", "Camera not connected");
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

    public void safelyEject() {
        if (mCameraHelper != null) {
            mCameraHelper.closeCamera();

            // Guard against landscape-only views in portrait mode
            if (action_safely_eject_drawable != null && action_safely_eject != null) {
                action_safely_eject_drawable.setColorFilter(getResources().getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_IN);
                action_safely_eject.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            }
            if (action_device_drawable != null && action_device != null) {
                action_device_drawable.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_IN);
                action_device.setTextColor(getResources().getColor(android.R.color.white));
            }
        }
    }

    /**
     * Get the UsbDeviceManager instance
     * @return the UsbDeviceManager instance
     */
    public UsbDeviceManager getUsbDeviceManager() {
        return usbDeviceManager;
    }

    public void rotateBy(int angle) {
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

    public void flipHorizontally() {
        if (mCameraHelper != null) {
            mCameraHelper.setPreviewConfig(
                    mCameraHelper.getPreviewConfig().setMirror(MirrorMode.MIRROR_VERTICAL));
        }
    }

    public void flipVertically() {
        if (mCameraHelper != null) {
            mCameraHelper.setPreviewConfig(
                    mCameraHelper.getPreviewConfig().setMirror(MirrorMode.MIRROR_HORIZONTAL));
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
        if (mBinding == null || mBinding.viewMainPreview == null) {
            Log.w(TAG, "initPreviewView: mBinding or viewMainPreview is null, skipping");
            return;
        }
        mBinding.viewMainPreview.setAspectRatio(mPreviewWidth, mPreviewHeight);
        Log.d(TAG, "initPreviewView: mPreviewWidth=" + mPreviewWidth + " mPreviewHeight=" + mPreviewHeight);
        mBinding.viewMainPreview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                MouseManager.width_height(width, height);
                Log.d(TAG, "onSurfaceTextureAvailable: width=" + width + " height=" + height + " mIsCameraConnected=" + mIsCameraConnected);
                if (mCameraHelper != null) {
                    // Add the new surface to camera pipeline
                    mCameraHelper.addSurface(surface, false);

                    // If camera already connected & previewing, reconnect the new surface
                    if (mIsCameraConnected) {
                        Log.d(TAG, "Camera already connected, reconnecting surface after orientation/creation change");
                        // Make view visible after surface is ready
                        runOnUiThread(() -> {
                            if (mBinding != null && mBinding.viewMainPreview != null) {
                                mBinding.viewMainPreview.setVisibility(View.VISIBLE);
                                mBinding.viewMainPreview.setKeepScreenOn(true);
                                // Re-apply aspect ratio for the new surface dimensions
                                mBinding.viewMainPreview.setAspectRatio(mPreviewWidth, mPreviewHeight);
                            }
                        });
                    }
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
     *     * In Android9+, connected to the UVC CAMERA, CAMERA permission is required
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
                    Log.d(TAG, "selectDevice: CAMERA permission granted, connecting camera");
                    mIsCameraConnected = false;
                    updateUIControls();

                    if (mCameraHelper != null) {
                        // get usb device
                        Log.d(TAG, "selectDevice: calling mCameraHelper.selectDevice()");
                        mCameraHelper.selectDevice(device);

                        // Guard against landscape-only views in portrait mode
                        if (action_device_drawable != null && action_device != null) {
                            action_device_drawable.setColorFilter(getResources().getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_IN);
                            action_device.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                        }
                        if (action_safely_eject_drawable != null && action_safely_eject != null) {
                            action_safely_eject_drawable.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_IN);
                            action_safely_eject.setTextColor(getResources().getColor(android.R.color.white));
                        }
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
            Log.d(TAG, "onCameraOpen:device=" + device.getDeviceName() + " isPortrait=" + isPortraitMode);
            // Guard against null mBinding after orientation change
            if (mBinding == null || mBinding.viewMainPreview == null) {
                Log.w(TAG, "onCameraOpen: mBinding or viewMainPreview is null, deferring camera setup");
                mIsCameraConnected = true; // Still mark as connected
                Log.d(TAG, "onCameraOpen: deferred - updateUIControls will be called when surface becomes available");
                // Preview will be set up when surface becomes available
                return;
            }
            mCameraHelper.startPreview();
            Log.d(TAG, "onCameraOpen: startPreview called");
            // After connecting to the camera, you can get preview size of the camera
            Size size = mCameraHelper.getPreviewSize();
            if (size != null) {
                Log.d(TAG, "onCameraOpen: preview size=" + size.width + "x" + size.height);
                resizePreviewView(size);
//                cameraViewSecond.setAspectRatio(640, 480);

            } else {
                Log.w(TAG, "onCameraOpen: preview size is null");
            }

            boolean hasSurfaceTexture = (mBinding.viewMainPreview.getSurfaceTexture() != null);
            Log.d(TAG, "onCameraOpen: hasSurfaceTexture=" + hasSurfaceTexture);
            if (hasSurfaceTexture) {
                mCameraHelper.addSurface(mBinding.viewMainPreview.getSurfaceTexture(), false);
                // Guard against landscape-only cameraViewSecond in portrait mode
                if (mBinding.cameraViewSecond != null && mBinding.cameraViewSecond.getHolder() != null
                        && mBinding.cameraViewSecond.getHolder().getSurface() != null
                        && mBinding.cameraViewSecond.getHolder().getSurface().isValid()) {
                    mCameraHelper.addSurface(mBinding.cameraViewSecond.getHolder().getSurface(), false);
                }
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
                // Guard against landscape-only cameraViewSecond in portrait mode
                if (cameraViewSecond != null && cameraViewSecond.getHolder() != null
                        && cameraViewSecond.getHolder().getSurface() != null) {
                    mCameraHelper.removeSurface(cameraViewSecond.getHolder().getSurface());
                }
            }

            mIsCameraConnected = false;
            updateUIControls();

            closeAllDialogFragment();

            ZoomLayoutDeal.zoomOut();
        }

        @Override
        public void onDeviceClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDeviceClose:device=" + device.getDeviceName());
            // Guard against landscape-only views in portrait mode
            if (action_safely_eject_drawable != null && action_safely_eject != null) {
                action_safely_eject_drawable.setColorFilter(getResources().getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_IN);
                action_safely_eject.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            }
            if (action_device_drawable != null && action_device != null) {
                action_device_drawable.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_IN);
                action_device.setTextColor(getResources().getColor(android.R.color.white));
            }
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
        // Use actual video dimensions, not screen dimensions, to preserve aspect ratio
        if (size != null && size.width > 0 && size.height > 0) {
            mPreviewWidth = size.width;
            mPreviewHeight = size.height;
        } else {
            // Fallback to screen dimensions if size not available
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            if (wm != null) {
                Display display = wm.getDefaultDisplay();
                display.getRealMetrics(metrics);
                mPreviewWidth = metrics.widthPixels;
                mPreviewHeight = metrics.heightPixels;
            }
        }
        Log.d(TAG, "resizePreviewView: " + mPreviewWidth + "x" + mPreviewHeight);
        // Set the aspect ratio of TextureView to match the aspect ratio of the camera
        mBinding.viewMainPreview.setAspectRatio(mPreviewWidth, mPreviewHeight);
    }

    private void updateUIControls() {
        runOnUiThread(() -> {
            Log.d(TAG, "updateUIControls: mIsCameraConnected=" + mIsCameraConnected);
            if (mIsCameraConnected) {
                if (mBinding.viewMainPreview != null) {
                    mBinding.viewMainPreview.setVisibility(View.VISIBLE);
                    Log.d(TAG, "updateUIControls: viewMainPreview set VISIBLE");
                }
                if (mBinding.tvConnectUSBCameraTip != null) {
                    mBinding.tvConnectUSBCameraTip.setVisibility(View.GONE);
                }

                if (mBinding.keyBoard != null) {
                    mBinding.keyBoard.setVisibility(View.VISIBLE);
                }

                Button Recording_Video = findViewById(R.id.Recording_Video);
                if (Recording_Video != null) {
                    Drawable Recording_Video_drawable = Recording_Video.getCompoundDrawables()[1];
                    // Update record button
                    if (mIsRecording) {
                        Recording_Video_drawable.setColorFilter(getResources().getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_IN);
                        Recording_Video.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                    }else{
                        Recording_Video_drawable.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_IN);
                        Recording_Video.setTextColor(getResources().getColor(android.R.color.white));
                    }
                }

            } else {
                if (mBinding.viewMainPreview != null) {
                    mBinding.viewMainPreview.setVisibility(View.GONE);
                }
                if (mBinding.tvConnectUSBCameraTip != null) {
                    mBinding.tvConnectUSBCameraTip.setVisibility(View.VISIBLE);
                }

                if (mBinding.keyBoard != null) {
                    mBinding.keyBoard.setVisibility(View.GONE);
                }

                if (mBinding.tvVideoRecordTime != null) {
                    mBinding.tvVideoRecordTime.setVisibility(View.GONE);
                }
            }
            invalidateOptionsMenu();
        });
    }

    private Size getSavedPreviewSize() {
        if (mUsbDevice == null) {
            Log.w(TAG, "getSavedPreviewSize: mUsbDevice is null, returning null");
            return null;
        }
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

            // Define the directory where the image will be saved
            String directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/Openterface";
            File directory = new File(directoryPath);

            // Create the directory if it does not exist
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Create a unique file name for the image
            String imageName = "picture_" + System.currentTimeMillis() + ".jpg"; // or .png if you prefer
            File file = new File(directory, imageName);
            ImageCapture.OutputFileOptions options =
                    new ImageCapture.OutputFileOptions.Builder(file).build();
            mCameraHelper.takePicture(options, new ImageCapture.OnImageCaptureCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    Toast.makeText(MainActivity.this,
                            "Saved: \"" + UriHelper.getPath(MainActivity.this, outputFileResults.getSavedUri()) + "\"",
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
                    // Check the recording permission
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                            == PackageManager.PERMISSION_GRANTED) {
                        startRecord();
                    } else {
                        // Check if we should show an explanation for why we need the permission
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                            // Show an explanation to the user
                            new AlertDialog.Builder(this)
                                    .setTitle("Permission Required")
                                    .setMessage("Recording permission is required to record videos with audio. Please grant the permission to continue.")
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        // Request the permission again
                                        ActivityCompat.requestPermissions(this, 
                                                new String[]{Manifest.permission.RECORD_AUDIO}, 
                                                200);
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .create()
                                    .show();
                        } else {
                            // Request recording permission
                            ActivityCompat.requestPermissions(this, 
                                    new String[]{Manifest.permission.RECORD_AUDIO}, 
                                    200);
                        }
                    }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // The permission acquisition was successful.Start Recording
                if (mIsCameraConnected && mCameraHelper != null && !mCameraHelper.isRecording()) {
                    startRecord();
                }
            } else {
                // The permission was rejected.
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                    // User has permanently denied the permission
                    new AlertDialog.Builder(this)
                            .setTitle("Permission Required")
                            .setMessage("Recording permission is required to record videos with audio. Please enable it in Settings.")
                            .setPositiveButton("Settings", (dialog, which) -> {
                                // Open app settings
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", null)
                            .create()
                            .show();
                }
                Toast.makeText(this, "Recording permission is required to record videos", Toast.LENGTH_SHORT).show();
                mIsRecording = false;
                updateUIControls();
            }
        }
    }

    private void setCustomVideoCaptureConfig() {
        mCameraHelper.setVideoCaptureConfig(
                mCameraHelper.getVideoCaptureConfig()
                        .setAudioCaptureEnable(true)
                        .setBitRate((int) (1024 * 1024 * 25 * 0.25))
                        .setVideoFrameRate(25)
                        .setIFrameInterval(1));
    }

    private void startRecord() {

        String directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/Openterface";
        File directory = new File(directoryPath);

        // Create the directory if it does not exist
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Create a unique file name for the video
        String videoName = "video_" + System.currentTimeMillis() + ".mp4";
        File file = new File(directory, videoName);

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
        runOnUiThread(() -> {
            if (mBinding.tvVideoRecordTime != null) {
                mBinding.tvVideoRecordTime.setVisibility(View.VISIBLE);
            }
        });

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
        runOnUiThread(() -> {
            if (mBinding.tvVideoRecordTime != null) {
                mBinding.tvVideoRecordTime.setVisibility(View.GONE);
            }
        });

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
            if (mBinding.tvVideoRecordTime != null) {
                mBinding.tvVideoRecordTime.setText(timeText);
            }
        });
    }

    /**
     *
     *
     * @param time seconds
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
    
    /**
     * Enable keyboard debugging for FE0C testing
     */
    private void enableKeyboardDebugging() {
        Log.d(TAG, "=== MainActivity Keyboard Debug Enabled ===");
        
        // Enable UsbDeviceManager debugging
        if (usbDeviceManager != null) {
            usbDeviceManager.enableDebugLogging();
            
            // Delayed debug test (wait for device to be ready)
            new Handler().postDelayed(() -> {
                if (usbDeviceManager.isConnected()) {
                    usbDeviceManager.debugFE0CKeyboard();
                } else {
                    Log.d(TAG, "Debug: Device not connected yet, skipping test");
                }
            }, 2000); // Wait 2 seconds for device initialization
        }
    }
    
    /**
     * Debug specific key press (can be called from ADB or buttons)
     */
    public void debugKeyPress(int androidKeyCode) {
        Log.d(TAG, "=== Debug Key Press: " + androidKeyCode + " ===");
        
        if (usbDeviceManager == null || !usbDeviceManager.isConnected()) {
            Log.e(TAG, "Debug: UsbDeviceManager not ready");
            return;
        }
        
        int hidKeyCode = UsbDeviceManager.androidKeyToHID(androidKeyCode);
        if (hidKeyCode == 0) {
            Log.e(TAG, "Debug: Unsupported key code: " + androidKeyCode);
            return;
        }
        
        Log.d(TAG, "Debug: Android key " + androidKeyCode + " -> HID key 0x" + Integer.toHexString(hidKeyCode));
        
        // Test with debug timing
        usbDeviceManager.debugKeyboardTiming(hidKeyCode, 0);
    }
    
    /**
     * Test keyboard sequence for debugging
     */
    public void testKeyboardSequence() {
        Log.d(TAG, "=== Testing Keyboard Sequence ===");
        
        if (usbDeviceManager == null || !usbDeviceManager.isConnected()) {
            Log.e(TAG, "Debug: UsbDeviceManager not ready");
            return;
        }
        
        // Test sequence: H-E-L-L-O
        int[] testKeys = {
            KeyEvent.KEYCODE_H,  // H
            KeyEvent.KEYCODE_E,  // E  
            KeyEvent.KEYCODE_L,  // L
            KeyEvent.KEYCODE_L,  // L
            KeyEvent.KEYCODE_O   // O
        };
        
        for (int i = 0; i < testKeys.length; i++) {
            final int keyCode = testKeys[i];
            final int delay = i * 200; // 200ms between keys
            
            new Handler().postDelayed(() -> {
                debugKeyPress(keyCode);
            }, delay);
        }
    }
    
    private BroadcastReceiver debugReceiver;
    
    /**
     * Register broadcast receiver for remote debugging
     */
    private void registerDebugReceiver() {
        debugReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "Debug broadcast received: " + action);
                
                if ("com.openterface.AOS.DEBUG_KEYBOARD".equals(action)) {
                    testKeyboardSequence();
                } else if ("com.openterface.AOS.DEBUG_SINGLE_KEY".equals(action)) {
                    int keyCode = intent.getIntExtra("keyCode", KeyEvent.KEYCODE_A);
                    debugKeyPress(keyCode);
                } else if ("com.openterface.AOS.DEBUG_DEVICE_INFO".equals(action)) {
                    if (usbDeviceManager != null) {
                        usbDeviceManager.debugFE0CKeyboard();
                    }
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.openterface.AOS.DEBUG_KEYBOARD");
        filter.addAction("com.openterface.AOS.DEBUG_SINGLE_KEY");
        filter.addAction("com.openterface.AOS.DEBUG_DEVICE_INFO");
        
        registerReceiver(debugReceiver, filter, RECEIVER_NOT_EXPORTED);
        Log.d(TAG, "Debug broadcast receiver registered");
    }

    // ============================================================
    // Portrait 4-Zone Layout Management
    // ============================================================

    /**
     * Initialize portrait mode zone views and set up listeners.
     * Zone 1: Shortcut Strip (top)
     * Zone 2: Video Stream (center)
     * Zone 3: Module Drawer (hidden by default)
     * Zone 4: Module Selector Bar (bottom)
     */
    private void initPortraitZones() {
        // Try to find portrait-specific views (only exist in portrait layout)
        portraitRootLayout = findViewById(R.id.thisFrameLayout);
        portraitVideoContainer = findViewById(R.id.video_area_container);
        portraitModuleDrawer = findViewById(R.id.module_drawer);
        portraitCollapseHandle = findViewById(R.id.module_collapse_handle);
        portraitModuleContent = findViewById(R.id.module_content_container);
        portraitShortcutStrip = findViewById(R.id.shortcut_strip);

        // Zone 4: Module Selector tabs
        portraitKeyboardTab = findViewById(R.id.tab_keyboard);
        portraitMouseTab = findViewById(R.id.tab_mouse);
        portraitImeTab = findViewById(R.id.tab_ime);
        portraitSettingsTab = findViewById(R.id.tab_settings);
        portraitRotationButton = findViewById(R.id.action_screen_orientation);

        // Check if we're in portrait mode by checking if root layout exists
        boolean isPortrait = (portraitRootLayout != null);
        isPortraitMode = isPortrait;

        if (!isPortrait) {
            // In landscape mode, portrait zone views won't exist
            Log.d(TAG, "initPortraitZones: not in portrait, skipping");
            return;
        }

        Log.d(TAG, "initPortraitZones: setting up portrait zone listeners");

        // Set up module tab click listeners
        if (portraitKeyboardTab != null) {
            portraitKeyboardTab.setOnClickListener(v -> toggleModule(ModuleType.KEYBOARD));
        }
        if (portraitMouseTab != null) {
            portraitMouseTab.setOnClickListener(v -> toggleModule(ModuleType.MOUSE));
        }
        if (portraitImeTab != null) {
            portraitImeTab.setOnClickListener(v -> toggleModule(ModuleType.IME));
        }
        if (portraitSettingsTab != null) {
            portraitSettingsTab.setOnClickListener(v -> openSettingsDrawer());
        }
        if (portraitRotationButton != null) {
            portraitRotationButton.setOnClickListener(v -> toggleScreenOrientation());
        }

        // Collapse handle click listener
        if (portraitCollapseHandle != null) {
            portraitCollapseHandle.setOnClickListener(v -> {
                if (currentModule != ModuleType.NONE) {
                    setModuleState(ModuleType.NONE, true);
                }
            });
        }

        // Setup collapse gesture detector
        collapseGestureDetector = new GestureDetector(this,
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2,
                                       float velocityX, float velocityY) {
                    // Swipe DOWN to collapse
                    if (velocityY > 500 && Math.abs(velocityY) > Math.abs(velocityX)) {
                        if (currentModule != ModuleType.NONE) {
                            setModuleState(ModuleType.NONE, true);
                            return true;
                        }
                    }
                    return false;
                }
            }
        );

        // Set touch listener on module drawer for swipe collapse
        if (portraitModuleDrawer != null) {
            portraitModuleDrawer.setOnTouchListener((v, event) -> {
                if (collapseGestureDetector != null) {
                    collapseGestureDetector.onTouchEvent(event);
                }
                return false;
            });
        }

        // Update tab visuals for current module state
        updateTabVisuals();

        // Set default module state (hidden)
        setModuleState(currentModule != null ? currentModule : ModuleType.NONE, false);
    }

    /**
     * Toggle module visibility.
     * If the same module is active, collapse it.
     * If a different module, switch to that module.
     */
    private void toggleModule(ModuleType type) {
        if (currentModule == type) {
            // Same module: collapse
            setModuleState(ModuleType.NONE, true);
        } else {
            // Different module: expand
            setModuleState(type, true);
        }
    }

    /**
     * Set the active module and update layout accordingly.
     * Module overlays the bottom of the video stream (fixed 200dp height).
     * Video is NOT pushed up - it stays filling the full dynamic area.
     * Zone 3 is positioned ABOVE Zone 4 (bottom bar).
     * @param type Module to show, or NONE to hide
     * @param animate Whether to animate the transition
     */
    private void setModuleState(ModuleType type, boolean animate) {
        currentModule = type;

        if (type == ModuleType.NONE) {
            // COLLAPSE: Hide module drawer
            if (portraitModuleDrawer != null) {
                portraitModuleDrawer.setVisibility(View.GONE);
            }
        } else {
            // EXPAND: Show module drawer at bottom, overlaying video, above bottom bar
            if (portraitModuleDrawer != null) {
                portraitModuleDrawer.setVisibility(View.VISIBLE);
                // Fixed 200dp height, positioned at bottom (above Zone 4)
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    200
                );
                params.gravity = android.view.Gravity.BOTTOM;
                portraitModuleDrawer.setLayoutParams(params);

                // Inflate or show the appropriate module
                inflateModuleView(type);
            }
        }

        updateTabVisuals();

        // Animate transition
        if (animate && portraitVideoContainer != null) {
            androidx.transition.TransitionManager.beginDelayedTransition(
                (ViewGroup) portraitVideoContainer.getParent(),
                new androidx.transition.ChangeBounds().setDuration(300)
            );
        }
    }

    /**
     * Inflate the appropriate module view into the module content container.
     */
    private void inflateModuleView(ModuleType type) {
        if (portraitModuleContent == null) return;

        // Remove existing module views
        portraitModuleContent.removeAllViews();

        View moduleView = null;
        LayoutInflater inflater = LayoutInflater.from(this);

        switch (type) {
            case KEYBOARD:
                if (keyboardModuleView == null) {
                    keyboardModuleView = inflater.inflate(
                        R.layout.module_portrait_keyboard, portraitModuleContent, false);
                    // Setup keyboard module buttons
                    setupKeyboardModule(keyboardModuleView);
                }
                moduleView = keyboardModuleView;
                break;

            case MOUSE:
                if (mouseModuleView == null) {
                    mouseModuleView = inflater.inflate(
                        R.layout.module_portrait_mouse, portraitModuleContent, false);
                    setupMouseModule(mouseModuleView);
                }
                moduleView = mouseModuleView;
                break;

            case IME:
                if (imeModuleView == null) {
                    imeModuleView = inflater.inflate(
                        R.layout.module_portrait_ime, portraitModuleContent, false);
                    setupImeModule(imeModuleView);
                }
                moduleView = imeModuleView;
                break;
        }

        if (moduleView != null && moduleView.getParent() == null) {
            portraitModuleContent.addView(moduleView);
        }
    }

    /**
     * Setup keyboard module button listeners.
     * NOTE: The existing keyboard classes (KeyBoardShortCut, KeyBoardCtrl, etc.)
     * use activity.findViewById() which expects views in the main activity layout.
     * In portrait module, these views are in the inflated module view.
     * For now, use a try-catch to prevent crashes while we wire up properly.
     */
    private void setupKeyboardModule(View view) {
        try {
            // Re-setup all existing keyboard button handlers
            KeyBoardShortCut keyBoardShortCut = new KeyBoardShortCut(this);
            KeyBoardCtrl keyBoardCtrl = new KeyBoardCtrl(this);
            KeyBoardShift keyBoardShift = new KeyBoardShift(this);
            KeyBoardAlt keyBoardAlt = new KeyBoardAlt(this);
            KeyBoardWin keyBoardWin = new KeyBoardWin(this);
            KeyBoardFunction keyBoardFunction = new KeyBoardFunction(this);
            KeyBoardSystem keyBoardSystem = new KeyBoardSystem(this);
            KeyBoardClose keyBoardClose = new KeyBoardClose(this);

            keyBoardShortCut.setShortCutButtonsClickColor();
            keyBoardFunction.setFunctionButtonsClickColor();
            keyBoardSystem.setSystemButtonsClickColor();
            keyBoardClose.setCloseButtonClickColor();

            mKeyBoardOpacity = new KeyBoardOpacity(this);
            mKeyBoardOpacity.setOpacityButtonClick();
        } catch (Exception e) {
            Log.w(TAG, "setupKeyboardModule: keyboard init failed (expected for portrait module): " + e.getMessage());
        }
    }

    /**
     * Setup mouse module (reuse existing TouchPad)
     */
    private void setupMouseModule(View view) {
        com.openterface.AOS.view.TouchPadView portraitTouchPad =
            view.findViewById(R.id.portrait_touchPadArea);
        com.openterface.AOS.view.MouseControlStripView portraitMouseStrip =
            view.findViewById(R.id.portrait_mouseButtonStrip);

        if (portraitTouchPad != null) {
            portraitTouchPad.setOnTouchPadListener(new com.openterface.AOS.view.TouchPadView.OnTouchPadListener() {
                @Override
                public void onTouchMove(float startX, float startY, float lastX, float lastY) {
                    if (lastX == 0 && lastY == 0) {
                        com.openterface.AOS.target.MouseManager.handleTwoFingerPanSlideUpDown(startY);
                    } else {
                        com.openterface.AOS.target.MouseManager.sendHexRelData(
                            "SecNullData", startX, startY, lastX, lastY);
                    }
                }

                @Override
                public void onTouchClick() {
                    sendMouseClick("SecLeftData");
                }

                @Override
                public void onTouchDoubleClick() {
                    // Double left click
                    sendMouseClick("SecLeftData");
                    new Handler().postDelayed(() -> sendMouseClick("SecLeftData"), 50);
                }

                @Override
                public void onTouchRightClick() {
                    sendMouseClick("SecRightData");
                }

                @Override
                public void onTouchLongPress() {
                    Log.d(TAG, "Portrait TouchPad long press -> drag mode");
                }

                @Override
                public void onTouchRelease() {
                    com.openterface.AOS.target.MouseManager.releaseMSRelData();
                }
            });
        }

        if (portraitMouseStrip != null) {
            portraitMouseStrip.setOnMouseClickListener(
                new com.openterface.AOS.view.MouseControlStripView.OnMouseClickListener() {
                    @Override
                    public void onMouseClick(int buttonMask) {
                        String clickType;
                        switch (buttonMask) {
                            case com.openterface.AOS.view.MouseControlStripView.BTN_LEFT:
                                clickType = "SecLeftData"; break;
                            case com.openterface.AOS.view.MouseControlStripView.BTN_RIGHT:
                                clickType = "SecRightData"; break;
                            case com.openterface.AOS.view.MouseControlStripView.BTN_MIDDLE:
                                clickType = "SecMiddleData"; break;
                            default:
                                clickType = "SecNullData";
                        }
                        sendMouseClick(clickType);
                    }

                    @Override
                    public void onMouseRelease() {
                        String releaseData = com.openterface.AOS.target.CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                            com.openterface.AOS.target.CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                            com.openterface.AOS.target.CH9329MSKBMap.getKeyCodeMap().get("address") +
                            com.openterface.AOS.target.CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                            com.openterface.AOS.target.CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                            com.openterface.AOS.target.CH9329MSKBMap.MSRelData().get("FirstData") +
                            com.openterface.AOS.target.CH9329MSKBMap.MSRelData().get("SecNullData") +
                            "00" + "00" + "00";
                        releaseData += com.openterface.AOS.serial.CH9329Function.makeChecksum(releaseData);
                        byte[] bytes = com.openterface.AOS.serial.CH9329Function.hexStringToByteArray(releaseData);
                        if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
                            usbDeviceManager.writeData(bytes);
                        }
                    }

                    @Override
                    public void onScrollClick() {
                        com.openterface.AOS.target.MouseManager.handleTwoPress();
                    }
                });
        }
    }

    /**
     * Send a mouse click via CH9329 HID protocol
     */
    private void sendMouseClick(String clickType) {
        new Thread(() -> {
            try {
                String clickData = com.openterface.AOS.target.CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                    com.openterface.AOS.target.CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                    com.openterface.AOS.target.CH9329MSKBMap.getKeyCodeMap().get("address") +
                    com.openterface.AOS.target.CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                    com.openterface.AOS.target.CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                    com.openterface.AOS.target.CH9329MSKBMap.MSRelData().get("FirstData") +
                    clickType + "00" + "00" + "00";
                clickData += com.openterface.AOS.serial.CH9329Function.makeChecksum(clickData);
                byte[] bytes = com.openterface.AOS.serial.CH9329Function.hexStringToByteArray(clickData);
                if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
                    usbDeviceManager.writeData(bytes);
                }
                // Auto release after 30ms
                Thread.sleep(30);
                String releaseData = com.openterface.AOS.target.CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                    com.openterface.AOS.target.CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                    com.openterface.AOS.target.CH9329MSKBMap.getKeyCodeMap().get("address") +
                    com.openterface.AOS.target.CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                    com.openterface.AOS.target.CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                    com.openterface.AOS.target.CH9329MSKBMap.MSRelData().get("FirstData") +
                    com.openterface.AOS.target.CH9329MSKBMap.MSRelData().get("SecNullData") +
                    "00" + "00" + "00";
                releaseData += com.openterface.AOS.serial.CH9329Function.makeChecksum(releaseData);
                bytes = com.openterface.AOS.serial.CH9329Function.hexStringToByteArray(releaseData);
                if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
                    usbDeviceManager.writeData(bytes);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending mouse click: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Setup IME module button listeners
     */
    private void setupImeModule(View view) {
        android.widget.Button sendButton = view.findViewById(R.id.ime_send_button);
        EditText textInput = view.findViewById(R.id.ime_text_input);

        if (sendButton != null) {
            sendButton.setOnClickListener(v -> {
                if (textInput != null && textInput.getText().length() > 0) {
                    String text = textInput.getText().toString();
                    // Send each character as HID keystroke
                    for (int i = 0; i < text.length(); i++) {
                        final char c = text.charAt(i);
                        new Handler().postDelayed(() -> {
                            if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
                                // Send character via keyboard
                                KeyBoardManager.sendKeyBoardData(null, String.valueOf(c));
                            }
                        }, i * 50L);
                    }
                    textInput.setText("");
                    Toast.makeText(this, "Text sent", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Update tab visual states (active/inactive)
     */
    private void updateTabVisuals() {
        // Keyboard tab
        if (portraitKeyboardTab != null) {
            boolean active = (currentModule == ModuleType.KEYBOARD);
            portraitKeyboardTab.setBackgroundColor(
                active ? 0xFF3700B3 : 0xFF1A1A1A);
            ImageView icon = portraitKeyboardTab.findViewById(R.id.tab_keyboard_icon);
            if (icon != null) {
                icon.setColorFilter(active ? 0xFFFFFFFF : 0xFF888888);
            }
            TextView label = portraitKeyboardTab.findViewById(R.id.tab_keyboard_label);
            if (label != null) label.setTextColor(active ? 0xFFFFFFFF : 0xFF888888);
        }

        // Mouse tab
        if (portraitMouseTab != null) {
            boolean active = (currentModule == ModuleType.MOUSE);
            portraitMouseTab.setBackgroundColor(
                active ? 0xFF3700B3 : 0xFF1A1A1A);
            ImageView icon = portraitMouseTab.findViewById(R.id.tab_mouse_icon);
            if (icon != null) {
                icon.setColorFilter(active ? 0xFFFFFFFF : 0xFF888888);
            }
            TextView label = portraitMouseTab.findViewById(R.id.tab_mouse_label);
            if (label != null) label.setTextColor(active ? 0xFFFFFFFF : 0xFF888888);
        }

        // IME tab
        if (portraitImeTab != null) {
            boolean active = (currentModule == ModuleType.IME);
            portraitImeTab.setBackgroundColor(
                active ? 0xFF3700B3 : 0xFF1A1A1A);
            ImageView icon = portraitImeTab.findViewById(R.id.tab_ime_icon);
            if (icon != null) {
                icon.setColorFilter(active ? 0xFFFFFFFF : 0xFF888888);
            }
            TextView label = portraitImeTab.findViewById(R.id.tab_ime_label);
            if (label != null) label.setTextColor(active ? 0xFFFFFFFF : 0xFF888888);
        }

        // Settings tab (never "active" in the module sense)
        if (portraitSettingsTab != null) {
            portraitSettingsTab.setBackgroundColor(0xFF1A1A1A);
            ImageView icon = portraitSettingsTab.findViewById(R.id.tab_settings_icon);
            if (icon != null) icon.setColorFilter(0xFF888888);
            TextView label = portraitSettingsTab.findViewById(R.id.tab_settings_label);
            if (label != null) label.setTextColor(0xFF888888);
        }
    }

    /**
     * Open the settings drawer (same as existing settings menu)
     */
    private void openSettingsDrawer() {
        // Collapse any active module first
        if (currentModule != ModuleType.NONE) {
            setModuleState(ModuleType.NONE, true);
        }

        // Open the existing settings drawer
        LinearLayout drawerSetup = findViewById(R.id.drawer_layout_setup);
        if (drawerSetup != null) {
            drawerSetup.setVisibility(View.VISIBLE);
        }

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            drawerLayout.openDrawer(androidx.core.view.GravityCompat.END);
            drawerSetup.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Toggle screen orientation between portrait and landscape modes.
     * The orientation is set programmatically and the device will stay in that mode
     * until the user toggles it again.
     */
    public void toggleScreenOrientation() {
        int newOrientation;
        if (isPortraitMode) {
            // Currently portrait, switch to landscape
            newOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
            isPortraitMode = false;
            Toast.makeText(this, R.string.orientation_toast_landscape, Toast.LENGTH_SHORT).show();
        } else {
            // Currently landscape, switch to portrait
            newOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
            isPortraitMode = true;
            Toast.makeText(this, R.string.orientation_toast_portrait, Toast.LENGTH_SHORT).show();
        }

        Log.d(TAG, "toggleScreenOrientation: " + (isPortraitMode ? "PORTRAIT" : "LANDSCAPE"));
        setRequestedOrientation(newOrientation);
    }

    /**
     * Initialize the TouchPad for mouse control with gesture support
     * (Same gesture logic as KeyCmd: single tap=left click, double tap=double click,
     * two-finger tap=right click, long press=drag, two-finger pan=scroll)
     */
    private void initTouchPad() {
        touchPadView = findViewById(R.id.touchPadArea);
        mouseControlStripView = findViewById(R.id.mouseButtonStrip);
        touchPadContainer = findViewById(R.id.TouchPad_View);

        if (touchPadView != null) {
            restoreTouchPadOpacity();

            touchPadView.setOnTouchPadListener(new TouchPadView.OnTouchPadListener() {
                @Override
                public void onTouchMove(float startX, float startY, float lastX, float lastY) {
                    if (lastX == 0 && lastY == 0) {
                        // Scroll mode (two-finger pan)
                        MouseManager.handleTwoFingerPanSlideUpDown(startY);
                    } else {
                        // Mouse move (relative)
                        MouseManager.sendHexRelData("SecNullData", startX, startY, lastX, lastY);
                    }
                }

                @Override
                public void onTouchClick() {
                    Log.d(TAG, "TouchPad single tap -> left click");
                    new Thread(() -> {
                        try {
                            String clickData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                                    CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                                    CH9329MSKBMap.getKeyCodeMap().get("address") +
                                    CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                                    CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                                    CH9329MSKBMap.MSRelData().get("FirstData") +
                                    CH9329MSKBMap.MSRelData().get("SecLeftData") +
                                    "00" + "00" + "00";
                            clickData += CH9329Function.makeChecksum(clickData);
                            byte[] bytes = CH9329Function.hexStringToByteArray(clickData);
                            if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
                                usbDeviceManager.writeData(bytes);
                            }
                            Thread.sleep(30);
                            // Release
                            String releaseData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                                    CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                                    CH9329MSKBMap.getKeyCodeMap().get("address") +
                                    CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                                    CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                                    CH9329MSKBMap.MSRelData().get("FirstData") +
                                    CH9329MSKBMap.MSRelData().get("SecNullData") +
                                    "00" + "00" + "00";
                            releaseData += CH9329Function.makeChecksum(releaseData);
                            bytes = CH9329Function.hexStringToByteArray(releaseData);
                            if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
                                usbDeviceManager.writeData(bytes);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error sending left click: " + e.getMessage());
                        }
                    }).start();
                }

                @Override
                public void onTouchDoubleClick() {
                    Log.d(TAG, "TouchPad double tap -> double click");
                    new Thread(() -> {
                        try {
                            String clickData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                                    CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                                    CH9329MSKBMap.getKeyCodeMap().get("address") +
                                    CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                                    CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                                    CH9329MSKBMap.MSRelData().get("FirstData") +
                                    CH9329MSKBMap.MSRelData().get("SecLeftData") +
                                    "00" + "00" + "00";
                            clickData += CH9329Function.makeChecksum(clickData);
                            byte[] bytes = CH9329Function.hexStringToByteArray(clickData);
                            for (int i = 0; i < 2; i++) {
                                if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
                                    usbDeviceManager.writeData(bytes);
                                }
                                Thread.sleep(30);
                                // Release
                                String releaseData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                                        CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                                        CH9329MSKBMap.getKeyCodeMap().get("address") +
                                        CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                                        CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                                        CH9329MSKBMap.MSRelData().get("FirstData") +
                                        CH9329MSKBMap.MSRelData().get("SecNullData") +
                                        "00" + "00" + "00";
                                releaseData += CH9329Function.makeChecksum(releaseData);
                                bytes = CH9329Function.hexStringToByteArray(releaseData);
                                if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
                                    usbDeviceManager.writeData(bytes);
                                }
                                Thread.sleep(30);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error sending double click: " + e.getMessage());
                        }
                    }).start();
                }

                @Override
                public void onTouchRightClick() {
                    Log.d(TAG, "TouchPad two-finger tap -> right click");
                    new Thread(() -> {
                        try {
                            String clickData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                                    CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                                    CH9329MSKBMap.getKeyCodeMap().get("address") +
                                    CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                                    CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                                    CH9329MSKBMap.MSRelData().get("FirstData") +
                                    CH9329MSKBMap.MSRelData().get("SecRightData") +
                                    "00" + "00" + "00";
                            clickData += CH9329Function.makeChecksum(clickData);
                            byte[] bytes = CH9329Function.hexStringToByteArray(clickData);
                            if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
                                usbDeviceManager.writeData(bytes);
                            }
                            Thread.sleep(30);
                            // Release
                            String releaseData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                                    CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                                    CH9329MSKBMap.getKeyCodeMap().get("address") +
                                    CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                                    CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                                    CH9329MSKBMap.MSRelData().get("FirstData") +
                                    CH9329MSKBMap.MSRelData().get("SecNullData") +
                                    "00" + "00" + "00";
                            releaseData += CH9329Function.makeChecksum(releaseData);
                            bytes = CH9329Function.hexStringToByteArray(releaseData);
                            if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
                                usbDeviceManager.writeData(bytes);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error sending right click: " + e.getMessage());
                        }
                    }).start();
                }

                @Override
                public void onTouchLongPress() {
                    Log.d(TAG, "TouchPad long press -> drag mode");
                }

                @Override
                public void onTouchRelease() {
                    MouseManager.releaseMSRelData();
                }
            });
        }

        if (mouseControlStripView != null) {
            mouseControlStripView.setOnMouseClickListener(new MouseControlStripView.OnMouseClickListener() {
                @Override
                public void onMouseClick(int buttonMask) {
                    String clickType;
                    switch (buttonMask) {
                        case MouseControlStripView.BTN_LEFT:
                            clickType = "SecLeftData";
                            break;
                        case MouseControlStripView.BTN_RIGHT:
                            clickType = "SecRightData";
                            break;
                        case MouseControlStripView.BTN_MIDDLE:
                            clickType = "SecMiddleData";
                            break;
                        default:
                            clickType = "SecNullData";
                    }
                    Log.d(TAG, "Mouse strip button click: " + clickType);
                    new Thread(() -> {
                        try {
                            String clickData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                                    CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                                    CH9329MSKBMap.getKeyCodeMap().get("address") +
                                    CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                                    CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                                    CH9329MSKBMap.MSRelData().get("FirstData") +
                                    clickType + "00" + "00" + "00";
                            clickData += CH9329Function.makeChecksum(clickData);
                            byte[] bytes = CH9329Function.hexStringToByteArray(clickData);
                            if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
                                usbDeviceManager.writeData(bytes);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error sending mouse click: " + e.getMessage());
                        }
                    }).start();
                }

                @Override
                public void onMouseRelease() {
                    Log.d(TAG, "Mouse strip button release");
                    new Thread(() -> {
                        try {
                            String releaseData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                                    CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                                    CH9329MSKBMap.getKeyCodeMap().get("address") +
                                    CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                                    CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                                    CH9329MSKBMap.MSRelData().get("FirstData") +
                                    CH9329MSKBMap.MSRelData().get("SecNullData") +
                                    "00" + "00" + "00";
                            releaseData += CH9329Function.makeChecksum(releaseData);
                            byte[] bytes = CH9329Function.hexStringToByteArray(releaseData);
                            if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
                                usbDeviceManager.writeData(bytes);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error releasing mouse button: " + e.getMessage());
                        }
                    }).start();
                }

                @Override
                public void onScrollClick() {
                    MouseManager.handleTwoPress();
                }
            });
        }

        // Initialize settings and help buttons
        initTouchPadButtons();

        Log.d(TAG, "TouchPad initialized");
    }

    /**
     * Initialize TouchPad settings and help buttons
     */
    public void initTouchPadButtons() {
        android.widget.ImageButton settingsButton = findViewById(R.id.touchPadSettingsButton);
        android.widget.ImageButton helpButton = findViewById(R.id.touchPadHelpButton);

        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> {
                if (touchPadSettings == null) {
                    touchPadSettings = new TouchPadSettings(MainActivity.this);
                }
                // Create a temporary TouchPadView reference for settings if needed
                touchPadSettings.setTouchPadView(touchPadView);
                touchPadSettings.showSettingsDialog();
            });
        }

        if (helpButton != null) {
            helpButton.setOnClickListener(v -> TouchPadHelpDialog.show(MainActivity.this));
        }
    }

    /**
     * Toggle the TouchPad overlay visibility
     */
    public void toggleTouchPadMode() {
        isTouchPadMode = !isTouchPadMode;
        if (touchPadContainer != null) {
            touchPadContainer.setVisibility(isTouchPadMode ? View.VISIBLE : View.GONE);
        }
        Log.d(TAG, "TouchPad mode " + (isTouchPadMode ? "ON" : "OFF"));
    }

    /**
     * Set TouchPad opacity
     */
    public void setTouchPadOpacity(int opacity) {
        if (touchPadContainer != null) {
            touchPadContainer.setAlpha(opacity / 100f);
        }
        if (mouseControlStripView != null) {
            mouseControlStripView.setOpacity(opacity);
        }
        // Save opacity
        getPreferences(Context.MODE_PRIVATE).edit()
                .putInt("touchpad_opacity", opacity).apply();
    }

    /**
     * Restore saved TouchPad opacity
     */
    public void restoreTouchPadOpacity() {
        int savedOpacity = getPreferences(Context.MODE_PRIVATE)
                .getInt("touchpad_opacity", 100);
        if (touchPadContainer != null) {
            touchPadContainer.setAlpha(savedOpacity / 100f);
        }
        if (mouseControlStripView != null) {
            mouseControlStripView.setOpacity(savedOpacity);
        }
    }

    /**
     * Get current TouchPad opacity
     */
    public int getTouchPadOpacity() {
        return getPreferences(Context.MODE_PRIVATE).getInt("touchpad_opacity", 100);
    }
}