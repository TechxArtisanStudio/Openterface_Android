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
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;

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
import com.openterface.AOS.target.HidManager;
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
import com.openterface.AOS.fragment.VncServerSettingsDialogFragment;
import com.openterface.AOS.fragment.WebRtcDialogFragment;
import com.openterface.AOS.vnc.VncFrameCapture;
import com.openterface.AOS.vnc.VncKeyMap;
import com.openterface.AOS.vnc.VncServerCallback;
import com.openterface.AOS.vnc.VncServerConfig;
import com.openterface.AOS.vnc.VncServerService;
import com.openterface.AOS.webrtc.WebRtcConfig;
import com.openterface.AOS.webrtc.WebRtcFrameCapture;
import com.openterface.AOS.webrtc.WebRtcInputRouter;
import com.openterface.AOS.webrtc.WebRtcServerService;
import com.serenegiant.widget.AspectRatioSurfaceView;

import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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

    // VNC Server
    private VncServerService vncService;
    private VncFrameCapture vncFrameCapture;
    private VncServerConfig vncConfig;
    private boolean vncServiceBound = false;

    // VNC server resolution (used for mouse coordinate mapping)
    private int vncServerWidth = 0;
    private int vncServerHeight = 0;

    // WebRTC Server
    private WebRtcServerService webRtcService;
    private WebRtcFrameCapture webRtcFrameCapture;
    private WebRtcConfig webRtcConfig;
    private WebRtcInputRouter webRtcInputRouter;
    private boolean webRtcServiceBound = false;

    // WebRTC server resolution (used for mouse coordinate mapping)
    private int webRtcServerWidth = 0;
    private int webRtcServerHeight = 0;

    private final VncServerCallback vncCallback = new VncServerCallback() {
        @Override
        public void onClientConnected(String ip) {
            Log.i(TAG, "VNC client connected: " + ip);
        }

        @Override
        public void onClientDisconnected(String ip) {
            Log.i(TAG, "VNC client disconnected: " + ip);
        }

        @Override
        public void onPointerEvent(int buttonMask, int x, int y) {
            // Handle VNC client mouse input - route to target via USB serial
            handleVncMouseEvent(buttonMask, x, y);
        }

        @Override
        public void onKeyboardEvent(int keysym, boolean down) {
            // Handle VNC client keyboard input - route to target via USB serial
            handleVncKeyboardEvent(keysym, down);
        }

        @Override
        public void onEncodingChanged(String ip, String encoding) {
            // Show encoding notification popup
            showVncEncodingPopup(ip, encoding);
        }
    };

    public static boolean mKeyboardRequestSent = false;

    private final Queue<Character> characterQueue = new LinkedList<>();
    private String currentFunctionKey;

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
        HidManager.setUsbDeviceManager(usbDeviceManager);
        
        // Set UsbDeviceManager instance in MouseManager for enhanced FE0C support
        HidManager.setUsbDeviceManager(usbDeviceManager);

        // Initialize the mouse event worker thread
        HidManager.initMouse();

        // Initialize VNC server components
        vncConfig = new VncServerConfig(this);
        vncFrameCapture = new VncFrameCapture(15);
        bindVncService();

        // Initialize WebRTC server components
        webRtcConfig = new WebRtcConfig(this);
        webRtcInputRouter = new WebRtcInputRouter();
        bindWebRtcService();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        //deal mouse click and button ,you can jump CustomTouchListener java
        CustomTouchListener customTouchListener = new CustomTouchListener(this, usbDeviceManager);

        mBinding.viewMainPreview.setOnTouchListener(customTouchListener);

        // Setting up the gesture detector
        gestureDetector = new GestureDetector(this, new GestureListener());

        KeyBoardManager keyBoardManager = new KeyBoardManager(this);

        //Short Cut Button
        KeyBoardShortCut KeyBoardShortCut = new KeyBoardShortCut(this);
        //Ctrl Button
        KeyBoardCtrl KeyBoardCtrlButton = new KeyBoardCtrl(this);
        //Shift Button
        KeyBoardShift KeyBoardShiftButton = new KeyBoardShift(this);
        //Alt Button
        KeyBoardAlt KeyBoardAltButton = new KeyBoardAlt(this);
        //Win Button
        KeyBoardWin KeyBoardWinButton = new KeyBoardWin(this);
        //FunctionKey Button
        KeyBoardFunction KeyBoardFunction = new KeyBoardFunction(this);
        //System Button
        KeyBoardSystem KeyBoardSystem = new KeyBoardSystem(this);
        //KeyBoard Close Button
        KeyBoardClose KeyBoardClose = new KeyBoardClose(this);

        //Drawer Layout
        DrawerLayoutDeal DrawerLayoutDeal = new DrawerLayoutDeal(this, savedInstanceState, mIsRecording);


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

        KeyBoardShortCut.setShortCutButtonsClickColor();//deal short cut button click color

        KeyBoardFunction.setFunctionButtonsClickColor();//deal function button click color

        KeyBoardSystem.setSystemButtonsClickColor();//deal system button click color

        KeyBoardClose.setCloseButtonClickColor();//deal close button click color

        //Keyboard Opacity
        mKeyBoardOpacity = new KeyBoardOpacity(this);
        mKeyBoardOpacity.setOpacityButtonClick();

        // Initialize TouchPad
        initTouchPad();

        // TouchPad toggle button
        ImageButton KeyBoard_TouchPad = findViewById(R.id.KeyBoard_TouchPad);
        if (KeyBoard_TouchPad != null) {
            KeyBoard_TouchPad.setOnClickListener(v -> toggleTouchPadMode());
        }

        DrawerLayoutDeal.setDrawerLayoutButtonClickColor();//deal drawer layout button click color

        action_device = findViewById(R.id.action_device);
        action_safely_eject = findViewById(R.id.action_safely_eject);

        action_device_drawable = action_device.getCompoundDrawables()[1];
        action_safely_eject_drawable = action_safely_eject.getCompoundDrawables()[1];

        //Zoom layout deal
        setCameraViewSecond();
        ZoomLayoutDeal ZoomLayoutDeal = new ZoomLayoutDeal(this, mCameraHelper, mBinding);

        HidManager.setKeyBoardLanguage();

        setLanguage();

    }

    private void setLanguage(){
        Button Left_Than_Button = findViewById(R.id.Left_Than_Button);
        String currentLang = Locale.getDefault().getLanguage();
        if (currentLang.equals("us")) {
            Left_Than_Button.setText("");
            KeyBoardSystem.setKeyboardLanguage("us");
            KeyBoardFunction.setKeyboardLanguage("us");
        } else if (currentLang.equals("de")) {
            Log.d("KeyBoardSystem", "German Button Pressed: ");

            KeyBoardSystem.setKeyboardLanguage("de");
            KeyBoardFunction.setKeyboardLanguage("de");
        }else {
            Left_Than_Button.setText("");
            KeyBoardSystem.setKeyboardLanguage("us");
            KeyBoardFunction.setKeyboardLanguage("us");
        }
    }

    private void setCameraViewSecond() {
        cameraViewSecond = findViewById(R.id.cameraViewSecond);
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
        String functionKey = HidManager.getFunctionKey(event, keyCode);
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
            HidManager.sendKeyboardMultiple(keyName);

            HidManager.EmptyKeyboard();

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
        String functionKey = HidManager.getFunctionKey(event, keyCode);
        String keyName = HidManager.getKeyName(keyCode);
        
        Log.d(TAG, "=== FALLBACK METHODS DEBUG ===");
        Log.d(TAG, "FunctionKey: '" + functionKey + "', KeyName: '" + keyName + "'");
        Log.d(TAG, "UsbDeviceManager available: " + (usbDeviceManager != null));
        Log.d(TAG, "UsbDeviceManager connected: " + (usbDeviceManager != null && usbDeviceManager.isConnected()));
        
        // Try enhanced method first (for proper FE0C support)
        if (usbDeviceManager != null) {
            Log.d(TAG, ">>> Trying HidManager.sendKeyBoardDataEnhanced()");
            boolean success = HidManager.sendKeyBoardDataEnhanced(usbDeviceManager, functionKey, keyName);
            Log.d(TAG, "Enhanced method result: " + (success ? "SUCCESS" : "FAILED"));
            if (success) {
                mKeyboardRequestSent = true;
                return true;
            }
        } else {
            Log.w(TAG, "UsbDeviceManager not available for enhanced method");
        }
        
        // Original method as final fallback
        Log.d(TAG, "Using original HidManager.sendKeyBoardData()");
        HidManager.sendKeyBoardData(functionKey, keyName);

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
                    HidManager.EmptyKeyboard();
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
        if (intent.getAction() != null && intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
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

        // Stop VNC server
        if (vncService != null && vncServiceBound) {
            vncService.stopServer();
            unbindService(vncServiceConnection);
        }

        // Stop WebRTC server
        if (webRtcService != null && webRtcServiceBound) {
            webRtcService.stopServer();
            unbindService(webRtcServiceConnection);
        }

        // Release the mouse event worker thread
        HidManager.releaseMouse();

        // Cleanup debug broadcast receiver
        if (debugReceiver != null) {
            unregisterReceiver(debugReceiver);
        }

//        usbDeviceManager.release();
    }

    private void setListeners() {

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

            keyBoardView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    Log.d("keyBoardView","keyBoardView.getWidth():"+keyBoardView.getWidth() + "keyBoardView.getHeight():"+keyBoardView.getHeight());
                    ZoomLayoutDeal.getViewWidthHeight(keyBoardView.getWidth(), keyBoardView.getHeight());
                }
            });
            keyBoardView.setVisibility(View.VISIBLE);
            mKeyBoardOpacity.restoreOpacity();
            //hide floating button keyboard and set_up_button
            FloatingActionButton keyBoard = findViewById(R.id.keyBoard);
            keyBoard.setVisibility(View.GONE);
            FloatingActionButton set_up_button = findViewById(R.id.set_up_button);
            set_up_button.setVisibility(View.GONE);

            Fragment_KeyBoard_Function.setVisibility(View.GONE);
            Fragment_KeyBoard_ShortCut.setVisibility(View.GONE);
            Fragment_KeyBoard_System.setVisibility(View.GONE);

            KeyBoard_Function.setBackgroundResource(R.drawable.nopress_button_background);
            KeyBoard_ShortCut.setBackgroundResource(R.drawable.nopress_button_background);
            KeyBoard_System.setBackgroundResource(R.drawable.nopress_button_background);

            // Initialize TouchPad settings and help buttons
            initTouchPadButtons();

        });
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

            action_safely_eject_drawable.setColorFilter(getResources().getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_IN);
            action_safely_eject.setTextColor(getResources().getColor(android.R.color.holo_red_light));

            action_device_drawable.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_IN);
            action_device.setTextColor(getResources().getColor(android.R.color.white));
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
        mBinding.viewMainPreview.setAspectRatio(mPreviewWidth, mPreviewHeight);
        Log.d(TAG, "1mPreviewWidth: " + mPreviewWidth + " mPreviewHeight: " + mPreviewHeight);
        mBinding.viewMainPreview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                HidManager.width_height(width, height);
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
                    mIsCameraConnected = false;
                    updateUIControls();

                    if (mCameraHelper != null) {
                        // get usb device
                        mCameraHelper.selectDevice(device);

                        action_device_drawable.setColorFilter(getResources().getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_IN);
                        action_device.setTextColor(getResources().getColor(android.R.color.holo_red_light));

                        action_safely_eject_drawable.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_IN);
                        action_safely_eject.setTextColor(getResources().getColor(android.R.color.white));
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
//                cameraViewSecond.setAspectRatio(640, 480);

            }

            if (mBinding.viewMainPreview.getSurfaceTexture() != null) {
                mCameraHelper.addSurface(mBinding.viewMainPreview.getSurfaceTexture(), false);
                mCameraHelper.addSurface(mBinding.cameraViewSecond.getHolder().getSurface(), false);
            }

            mIsCameraConnected = true;
            updateUIControls();
//            usbDeviceManager.init();

            // Start VNC frame capture if VNC server is running
            // Use actual camera resolution, not screen dimensions
            Size cameraSize = mCameraHelper.getPreviewSize();
            if (vncService != null && vncService.isRunning() && cameraSize != null) {
                vncFrameCapture.start(mCameraHelper, vncService, cameraSize.width, cameraSize.height);
            }

            // Auto-start VNC server if configured (camera is now ready)
            if (vncConfig.isAutoStart() && vncService != null && !vncService.isRunning()) {
                startVncServer();
            }

            // Auto-start WebRTC server if configured (camera is now ready)
            if (webRtcConfig.isAutoStart() && webRtcService != null && !webRtcService.isRunning() && (vncService == null || !vncService.isRunning())) {
                startWebRtcServer();
            }
        }

        @Override
        public void onCameraClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCameraClose:device=" + device.getDeviceName());

//            usbDeviceManager.release();

            if (mIsRecording) {
                toggleVideoRecord(false);
            }

            // Stop VNC frame capture
            if (vncFrameCapture != null) {
                vncFrameCapture.stop(mCameraHelper);
            }

            if (mCameraHelper != null && mBinding.viewMainPreview.getSurfaceTexture() != null) {
                mCameraHelper.removeSurface(mBinding.viewMainPreview.getSurfaceTexture());
                mCameraHelper.removeSurface(cameraViewSecond.getHolder().getSurface());
            }

            mIsCameraConnected = false;
            updateUIControls();

            closeAllDialogFragment();

            ZoomLayoutDeal.zoomOut();
        }

        @Override
        public void onDeviceClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDeviceClose:device=" + device.getDeviceName());
            action_safely_eject_drawable.setColorFilter(getResources().getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_IN);
            action_safely_eject.setTextColor(getResources().getColor(android.R.color.holo_red_light));

            action_device_drawable.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_IN);
            action_device.setTextColor(getResources().getColor(android.R.color.white));
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

                Button Recording_Video = findViewById(R.id.Recording_Video);
                Drawable Recording_Video_drawable = Recording_Video.getCompoundDrawables()[1];
                // Update record button
                if (mIsRecording) {
                    Recording_Video_drawable.setColorFilter(getResources().getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_IN);
                    Recording_Video.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                }else{
                    Recording_Video_drawable.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_IN);
                    Recording_Video.setTextColor(getResources().getColor(android.R.color.white));
                }

            } else {
                mBinding.viewMainPreview.setVisibility(View.GONE);
                mBinding.tvConnectUSBCameraTip.setVisibility(View.VISIBLE);

                mBinding.keyBoard.setVisibility(View.GONE);

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

    // ========================= VNC Server =========================

    private final ServiceConnection vncServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            VncServerService.LocalBinder binder = (VncServerService.LocalBinder) service;
            vncService = binder.getService();
            vncServiceBound = true;
            vncService.setCallback(vncCallback);

            // Auto-start will be handled when camera connects (onCameraOpen)
            // to ensure camera is ready before starting VNC server
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            vncServiceBound = false;
            vncService = null;
        }
    };

    private void bindVncService() {
        Intent intent = new Intent(this, VncServerService.class);
        bindService(intent, vncServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void startVncServer() {
        if (vncService == null || !vncServiceBound) return;

        // Use actual camera resolution, not screen dimensions
        int width = DEFAULT_WIDTH;
        int height = DEFAULT_HEIGHT;
        if (mCameraHelper != null) {
            Size size = mCameraHelper.getPreviewSize();
            if (size != null) {
                width = size.width;
                height = size.height;
            }
        }

        // Store VNC resolution for mouse coordinate mapping
        vncServerWidth = width;
        vncServerHeight = height;
        Log.e(TAG, "VNC resolution set to: " + width + "x" + height);
        HidManager.width_height(width, height);

        boolean started = vncService.startServer(vncConfig.getPassword(), vncConfig.getPort(), width, height, vncConfig.getEncoding(), vncConfig.getQualityLevel(), vncConfig.getCompressLevel());
        if (started && mCameraHelper != null) {
            vncFrameCapture.start(mCameraHelper, vncService, width, height);
        }
    }

    public void showVncServerDialog() {
        VncServerSettingsDialogFragment dialog = new VncServerSettingsDialogFragment();
        dialog.setVncService(vncService);
        dialog.setWebrtcAutoStartEnabled(webRtcConfig.isAutoStart());
        dialog.setWebrtcServerRunning(webRtcService != null && webRtcService.isRunning());

        // Pass current camera resolution to dialog
        final int cameraWidth;
        final int cameraHeight;
        if (mCameraHelper != null) {
            Size size = mCameraHelper.getPreviewSize();
            if (size != null) {
                cameraWidth = size.width;
                cameraHeight = size.height;
            } else {
                cameraWidth = DEFAULT_WIDTH;
                cameraHeight = DEFAULT_HEIGHT;
            }
        } else {
            cameraWidth = DEFAULT_WIDTH;
            cameraHeight = DEFAULT_HEIGHT;
        }
        dialog.setPreviewSize(cameraWidth, cameraHeight);

        dialog.setListener(new VncServerSettingsDialogFragment.OnVncSettingsListener() {
            @Override
            public void onServerStarted(int port, String password) {
                // Store VNC resolution for mouse mapping
                vncServerWidth = cameraWidth;
                vncServerHeight = cameraHeight;
                // Start frame capture when VNC server starts
                if (mCameraHelper != null) {
                    Size size = mCameraHelper.getPreviewSize();
                    if (size != null) {
                        vncFrameCapture.start(mCameraHelper, vncService, size.width, size.height);
                    }
                }
            }

            @Override
            public void onServerStopped() {
                if (vncFrameCapture != null) {
                    vncFrameCapture.stop(mCameraHelper);
                }
            }

            @Override
            public void onSettingsChanged() {
                // No action needed
            }
        });
        dialog.show(getSupportFragmentManager(), "VncServerSettingsDialog");
    }

    private int lastVncButtonMask = 0;

    private void handleVncMouseEvent(int buttonMask, int x, int y) {
        // Set MouseManager dimensions to VNC resolution for proper normalization
        if (vncServerWidth > 0 && vncServerHeight > 0) {
            HidManager.width_height(vncServerWidth, vncServerHeight);
        }

        Log.e(TAG, "VNC mouse: button=" + buttonMask + " pos=" + x + "," + y);

        // Button state change detection
        if (buttonMask != lastVncButtonMask) {
            // Button state changed
            if (buttonMask != 0) {
                // Button press - send click
                String mouseClick = "SecNullData";
                if ((buttonMask & 0x01) != 0) mouseClick = "SecLeftData";
                else if ((buttonMask & 0x02) != 0) mouseClick = "SecMiddleData";
                else if ((buttonMask & 0x04) != 0) mouseClick = "SecRightData";
                Log.e(TAG, "VNC mouse click: " + mouseClick);
                HidManager.sendHexAbsButtonClickData(mouseClick, x, y);
            } else {
                // Button release - send release (null click at position)
                Log.e(TAG, "VNC mouse release");
                HidManager.sendHexAbsData(x, y);
            }
            lastVncButtonMask = buttonMask;
        } else if (buttonMask == 0) {
            // Pure movement - just move cursor
            HidManager.sendHexAbsData(x, y);
        }
    }

    private void handleVncKeyboardEvent(int keysym, boolean down) {
        String keyName = VncKeyMap.vncKeysymToKeyName(keysym);
        if (keyName == null) {
            Log.w(TAG, "VNC keysym 0x" + Integer.toHexString(keysym) + " not mapped to keyName");
            return;
        }

        Log.d(TAG, "VNC keyboard: keysym=0x" + Integer.toHexString(keysym) + " keyName=" + keyName + " down=" + down);

        if (VncKeyMap.isModifier(keysym)) {
            // Modifier key
            String functionKey = getModifierFunctionKey(keysym);
            if (down) {
                HidManager.sendKeyBoardPress(functionKey, keyName);
            } else {
                HidManager.sendKeyBoardRelease();
            }
        } else {
            // Regular key - check if it exists in keycode map
            if (down) {
                HidManager.sendKeyBoardData("00", keyName);
            } else {
                HidManager.sendKeyBoardRelease();
            }
        }
    }

    private String getModifierFunctionKey(int keysym) {
        switch (keysym) {
            case VncKeyMap.XK_Shift_L:
            case VncKeyMap.XK_Shift_R: return "Shift";
            case VncKeyMap.XK_Control_L:
            case VncKeyMap.XK_Control_R: return "Ctrl";
            case VncKeyMap.XK_Alt_L:
            case VncKeyMap.XK_Alt_R: return "Alt";
            case VncKeyMap.XK_Meta_L:
            case VncKeyMap.XK_Meta_R:
            case VncKeyMap.XK_Super_L:
            case VncKeyMap.XK_Super_R: return "Win";
            default: return "00";
        }
    }

    /* ======================== WebRTC Server ======================== */

    /**
     * Callback for WebRTC server events.
     */
    private final WebRtcServerService.WebRtcCallback webRtcCallback = new WebRtcServerService.WebRtcCallback() {
        @Override
        public void onClientConnected(String clientInfo) {
            Log.i(TAG, "WebRTC client connected: " + clientInfo);
        }

        @Override
        public void onClientDisconnected(String clientInfo) {
            Log.i(TAG, "WebRTC client disconnected: " + clientInfo);
        }

        @Override
        public void onServerError(String error) {
            Log.e(TAG, "WebRTC server error: " + error);
        }

        @Override
        public void onMouseEvent(int buttonMask, int x, int y, boolean pressed) {
            if (webRtcInputRouter != null) {
                webRtcInputRouter.onMouseEvent(buttonMask, x, y, pressed);
            }
        }

        @Override
        public void onKeyboardEvent(int keysym, boolean down) {
            Log.i(TAG, "MainActivity.onKeyboardEvent: keysym=" + keysym + ", down=" + down + ", router=" + (webRtcInputRouter != null));
            if (webRtcInputRouter != null) {
                webRtcInputRouter.onKeyboardEvent(keysym, down);
            }
        }

        @Override
        public void onConnectionStateChanged(String state) {
            Log.i(TAG, "WebRTC connection state: " + state);
        }
    };

    /**
     * Bind to WebRTC service.
     */
    private void bindWebRtcService() {
        Intent intent = new Intent(this, WebRtcServerService.class);
        bindService(intent, webRtcServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Service connection for WebRTC.
     */
    private final ServiceConnection webRtcServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            WebRtcServerService.LocalBinder binder = (WebRtcServerService.LocalBinder) service;
            webRtcService = binder.getService();
            webRtcServiceBound = true;
            webRtcService.setCallback(webRtcCallback);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            webRtcServiceBound = false;
            webRtcService = null;
        }
    };

    /**
     * Start WebRTC server.
     */
    private void startWebRtcServer() {
        if (webRtcService == null || !webRtcServiceBound) return;

        WebRtcConfig config = webRtcConfig;

        // Use actual camera resolution
        int width = DEFAULT_WIDTH;
        int height = DEFAULT_HEIGHT;
        if (mCameraHelper != null) {
            Size size = mCameraHelper.getPreviewSize();
            if (size != null) {
                width = size.width;
                height = size.height;
            }
        }

        // Store WebRTC resolution for mouse coordinate mapping
        webRtcServerWidth = width;
        webRtcServerHeight = height;
        Log.i(TAG, "WebRTC resolution set to: " + width + "x" + height);
        HidManager.width_height(width, height);

        boolean started = webRtcService.startServer(
                config.getSignallingPort(),
                config.getStunServer(),
                width,
                height,
                config.getVideoFps()
        );

        if (started && mCameraHelper != null) {
            if (webRtcFrameCapture == null) {
                webRtcFrameCapture = new WebRtcFrameCapture(config.getVideoFps());
            }
            webRtcFrameCapture.start(mCameraHelper, webRtcService, width, height);

            if (webRtcInputRouter != null) {
                webRtcInputRouter.setFramebufferSize(width, height);
            }
        }
    }

    /**
     * Stop WebRTC server.
     */
    private void stopWebRtcServer() {
        if (webRtcService == null || !webRtcServiceBound) return;

        if (webRtcFrameCapture != null) {
            webRtcFrameCapture.stop(mCameraHelper);
        }
        webRtcService.stopServer();
    }

    /**
     * Show WebRTC server settings dialog.
     */
    public void showWebRtcDialog() {
        if (webRtcService == null || !webRtcServiceBound) {
            Toast.makeText(this, "WebRTC service not available", Toast.LENGTH_SHORT).show();
            return;
        }

        WebRtcDialogFragment dialog = new WebRtcDialogFragment();
        dialog.setWebRtcService(webRtcService);
        dialog.setWebRtcConfig(webRtcConfig);
        dialog.setVncAutoStartEnabled(vncConfig.isAutoStart());
        dialog.setVncServerRunning(vncService != null && vncService.isRunning());

        // Pass current camera resolution
        final int cameraWidth;
        final int cameraHeight;
        if (mCameraHelper != null) {
            Size size = mCameraHelper.getPreviewSize();
            if (size != null) {
                cameraWidth = size.width;
                cameraHeight = size.height;
            } else {
                cameraWidth = DEFAULT_WIDTH;
                cameraHeight = DEFAULT_HEIGHT;
            }
        } else {
            cameraWidth = DEFAULT_WIDTH;
            cameraHeight = DEFAULT_HEIGHT;
        }
        dialog.setPreviewSize(cameraWidth, cameraHeight);

        dialog.setListener(new WebRtcDialogFragment.OnWebRtcSettingsListener() {
            @Override
            public void onServerStarted() {
                webRtcServerWidth = cameraWidth;
                webRtcServerHeight = cameraHeight;
                if (mCameraHelper != null) {
                    Size size = mCameraHelper.getPreviewSize();
                    if (size != null) {
                        if (webRtcFrameCapture == null) {
                            webRtcFrameCapture = new WebRtcFrameCapture(webRtcConfig.getVideoFps());
                        }
                        webRtcFrameCapture.start(mCameraHelper, webRtcService, size.width, size.height);
                        if (webRtcInputRouter != null) {
                            webRtcInputRouter.setFramebufferSize(size.width, size.height);
                        }
                    }
                }
            }

            @Override
            public void onServerStopped() {
                if (webRtcFrameCapture != null) {
                    webRtcFrameCapture.stop(mCameraHelper);
                }
            }

            @Override
            public void onSettingsChanged() {
            }
        });

        dialog.show(getSupportFragmentManager(), "WebRtcDialog");
    }

    /* ======================== VNC encoding popup ======================== */
    private TextView tvVncEncodingPopup;
    private Runnable vncEncodingHideRunnable;

    private void showVncEncodingPopup(String ip, String encoding) {
        runOnUiThread(() -> {
            if (tvVncEncodingPopup == null) {
                tvVncEncodingPopup = findViewById(R.id.vnc_encoding_popup);
            }
            // Cancel any pending hide
            if (vncEncodingHideRunnable != null) {
                tvVncEncodingPopup.removeCallbacks(vncEncodingHideRunnable);
            }
            tvVncEncodingPopup.setText("VNC: " + encoding + " (" + ip + ")");
            tvVncEncodingPopup.setVisibility(View.VISIBLE);
            vncEncodingHideRunnable = () -> tvVncEncodingPopup.setVisibility(View.GONE);
            tvVncEncodingPopup.postDelayed(vncEncodingHideRunnable, 5000);
        });
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
                        HidManager.handleTwoFingerPanSlideUpDown(startY);
                    } else {
                        // Mouse move (relative)
                        HidManager.sendHexRelData("SecNullData", startX, startY, lastX, lastY);
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
                    HidManager.releaseMSRelData();
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
                    HidManager.handleTwoPress();
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