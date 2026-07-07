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
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
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
import com.openterface.AOS.KeyBoardClick.KeyboardSettingsManager;
import com.openterface.AOS.view.KeyPreviewPopup;
import com.openterface.AOS.view.CharacterAlternatesPopup;
import com.openterface.AOS.view.TouchPadView;
import com.openterface.AOS.view.TouchPadHelpDialog;
import com.openterface.AOS.view.MouseControlStripView;
import com.openterface.AOS.KeyBoardClick.TouchPadSettings;
import com.openterface.AOS.dialog.TargetOsPickerDialog;
import com.openterface.AOS.manager.TargetOsManager;
import com.openterface.AOS.manager.ThemeManager;
import com.openterface.AOS.manager.ShortcutProfileManager;
import com.openterface.AOS.model.ShortcutProfile;
import com.openterface.AOS.adapter.ThemeColorAdapter;
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
import com.openterface.AOS.fragment.SettingsFloatingFragment;
import com.openterface.AOS.fragment.DeviceListDialogFragment;
import com.openterface.AOS.fragment.VideoFormatDialogFragment;
import com.openterface.AOS.fragment.VncServerSettingsDialogFragment;
import com.openterface.AOS.fragment.WebRtcDialogFragment;
import com.openterface.AOS.fragment.ShortcutManagerFragment;
import com.openterface.AOS.fragment.ShortcutHubFloatingFragment;
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
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup;
import android.widget.RadioButton;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
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

public class MainActivity extends AppCompatActivity implements SettingsFloatingFragment.SettingsCallback {

    private static final String TAG = "OP-UI";
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

    /**
     * Get the camera preview width (chip resolution)
     */
    public int getPreviewWidth() {
        return mPreviewWidth;
    }

    /**
     * Get the camera preview height (chip resolution)
     */
    public int getPreviewHeight() {
        return mPreviewHeight;
    }

    private int mPreviewRotation = 0;

    public ICameraHelper mCameraHelper;

    private UsbDevice mUsbDevice;
    private final ICameraHelper.StateCallback mStateCallback = new MyCameraHelperCallback();

    private long mRecordStartTime = 0;
    private Timer mRecordTimer = null;
    private DecimalFormat mDecimalFormat;

    private boolean mIsRecording = false;
    private boolean mIsCameraConnected = false;
    // Track whether the main preview surface has been added to camera pipeline
    private boolean mMainSurfaceAdded = false;
    // Track which SurfaceTexture we've already configured the preview for, to prevent
    // duplicate setPreviewDisplay + startPreview calls when both onCameraOpen and
    // onSurfaceTextureAvailable fire for the same surface (e.g. after unplug/replug)
    private SurfaceTexture mCurrentPreviewSurfaceTexture = null;
    // Track the Surface object added to CameraHelper/RendererHolder so we can remove it
    // before adding a new one (e.g. in onSurfaceTextureSizeChanged). Without this, the
    // RendererHolder accumulates stale surfaces which can cause black screen in portrait.
    private android.view.Surface mCurrentPreviewSurface = null;
    // Reference to touch/zoom controller in portrait mode, used to update Matrix on surface ready/size change
    private CustomTouchListener mCustomTouchListener = null;

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

    // Screen orientation state
    private boolean isPortraitMode = false;
    private static final String KEY_PORTRAIT_MODE = "is_portrait_mode";

    // Screen rotation lock state
    private boolean isRotationLocked = false;

    // Portrait 4-zone module state
    public enum ModuleType { NONE, KEYBOARD, MOUSE, IME, SETTINGS }
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
    private View portraitRotationLockButton;
    private View landscapeRotationLockButton;
    private View portraitShortcutStrip;

    // Portrait module views (for reuse)
    private View keyboardModuleView;
    private View mouseModuleView;
    private View imeModuleView;
    private View settingsModuleView;

    // Keyboard settings and popup manager
    private KeyboardSettingsManager keyboardSettingsManager;
    private KeyPreviewPopup keyPreviewPopup;

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
        HidManager.setUsbDeviceManager(usbDeviceManager);
        
        // Set UsbDeviceManager instance in MouseManager for enhanced FE0C support
        HidManager.setUsbDeviceManager(usbDeviceManager);

        // Initialize the mouse event worker thread
        HidManager.initMouse();

        // Load saved mouse speed multiplier (default 50 → 1.0x)
        int initialMouseSpeed = getSharedPreferences(PREF_SETTINGS, MODE_PRIVATE).getInt(KEY_MOUSE_SPEED, 50);
        HidManager.setMouseSpeedMultiplier(progressToMouseSpeed(initialMouseSpeed));

        // Initialize VNC server components
        vncConfig = new VncServerConfig(this);
        vncFrameCapture = new VncFrameCapture(15);
        bindVncService();

        // Initialize WebRTC server components
        webRtcConfig = new WebRtcConfig(this);
        webRtcInputRouter = new WebRtcInputRouter();
        bindWebRtcService();

        // Initialize Target OS Manager
        TargetOsManager.getInstance().initialize(this);

        // Initialize Theme Manager
        ThemeManager.getInstance().initialize(this);
        ThemeManager.getInstance().applyTheme(this);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        //deal mouse click and button ,you can jump CustomTouchListener java
        mCustomTouchListener = new CustomTouchListener(this, usbDeviceManager);

        mBinding.viewMainPreview.setOnTouchListener(mCustomTouchListener);

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

        // Initialize keyboard settings manager
        keyboardSettingsManager = new KeyboardSettingsManager(this);

        if (!isPortraitLayout) {
            // Create KeyPreviewPopup for bubble hint
            KeyPreviewPopup keyPreviewPopup = new KeyPreviewPopup(this);

            //Short Cut Button
            KeyBoardShortCut = new KeyBoardShortCut(mBinding.getRoot());
            //Ctrl Button
            KeyBoardCtrlButton = new KeyBoardCtrl(mBinding.getRoot(), keyPreviewPopup);
            //Shift Button
            KeyBoardShiftButton = new KeyBoardShift(mBinding.getRoot(), keyPreviewPopup);
            //Alt Button
            KeyBoardAltButton = new KeyBoardAlt(mBinding.getRoot(), keyPreviewPopup);
            //Win Button
            KeyBoardWinButton = new KeyBoardWin(mBinding.getRoot(), keyPreviewPopup);
            //FunctionKey Button
            KeyBoardFunction = new KeyBoardFunction(mBinding.getRoot());
            //System Button
            KeyBoardSystem = new KeyBoardSystem(mBinding.getRoot());
            //KeyBoard Close Button
            KeyBoardClose = new KeyBoardClose(mBinding.getRoot());

            // Pass popup and settings to KeyBoardSystem for all-key bubble + sound/vibration
            KeyBoardSystem.setKeyPreviewPopup(keyPreviewPopup);
            KeyBoardSystem.setSettingsManager(keyboardSettingsManager);

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

        HidManager.setKeyBoardLanguage();

        setLanguage();

        // Initialize portrait 4-zone layout
        initPortraitZones();

    }

    /**
     * Show the keyboard settings dialog (sound and vibration preferences)
     */
    @SuppressLint("ClickableViewAccessibility")
    private void showKeyboardSettingsDialog() {
        if (keyboardSettingsManager == null) {
            keyboardSettingsManager = new KeyboardSettingsManager(this);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("键盘设置");

        View view = getLayoutInflater().inflate(R.layout.dialog_keyboard_settings, null);
        builder.setView(view);

        // Sound settings
        final androidx.appcompat.widget.SwitchCompat switchKeySound = view.findViewById(R.id.switch_key_sound);
        final SeekBar seekbarVolume = view.findViewById(R.id.seekbar_volume);
        final LinearLayout layoutVolume = view.findViewById(R.id.layout_volume);

        // Vibration settings
        final androidx.appcompat.widget.SwitchCompat switchKeyVibrate = view.findViewById(R.id.switch_key_vibrate);
        final SeekBar seekbarVibrate = view.findViewById(R.id.seekbar_vibrate);
        final LinearLayout layoutVibrate = view.findViewById(R.id.layout_vibrate);

        // Initialize
        switchKeySound.setChecked(keyboardSettingsManager.isSoundEnabled());
        seekbarVolume.setProgress(keyboardSettingsManager.getSoundVolume());
        switchKeyVibrate.setChecked(keyboardSettingsManager.isVibrateEnabled());
        seekbarVibrate.setProgress(keyboardSettingsManager.getVibrateStrength());

        layoutVolume.setVisibility(switchKeySound.isChecked() ? View.VISIBLE : View.GONE);
        layoutVibrate.setVisibility(switchKeyVibrate.isChecked() ? View.VISIBLE : View.GONE);

        switchKeySound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            keyboardSettingsManager.setSoundEnabled(isChecked);
            layoutVolume.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        seekbarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) keyboardSettingsManager.setSoundVolume(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        switchKeyVibrate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            keyboardSettingsManager.setVibrateEnabled(isChecked);
            layoutVibrate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        seekbarVibrate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) keyboardSettingsManager.setVibrateStrength(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        builder.setPositiveButton("确定", (dialog, which) -> dialog.dismiss());
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void setLanguage(){
        Button Left_Than_Button = findViewById(R.id.Left_Than_Button);
        String currentLang = Locale.getDefault().getLanguage();
        if (currentLang.equals("us")) {
            if (Left_Than_Button != null) Left_Than_Button.setText("");
            KeyBoardSystem.setKeyboardLanguage("us");
            KeyBoardFunction.setKeyboardLanguage("us");
        } else if (currentLang.equals("de")) {
            Log.d(TAG, "German Button Pressed: ");

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
        String functionKey = HidManager.getFunctionKey(event, keyCode);
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE && event.getCharacters() != null) {
            String characters = event.getCharacters();
            Log.d(TAG, "in this keycode: " + keyCode);
            Log.d(TAG, "in this count: " + count);
            Log.d(TAG, "in this event: " + event);
            Log.d(TAG, "Characters: " + characters);

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
        String action = intent.getAction();
        Log.d(TAG, "onNewIntent: action=" + action + " isPortrait=" + isPortraitMode);
        usbDeviceManager.handleUsbDevice(intent);
        if (action != null && action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            if (!mIsCameraConnected) {
                mUsbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Log.d(TAG, "onNewIntent: USB device attached, device=" + (mUsbDevice != null ? mUsbDevice.getDeviceName() : "null"));
                selectDevice(mUsbDevice);
            } else {
                Log.d(TAG, "onNewIntent: camera already connected, ignoring");
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

        // Clear cached module views to ensure they reload in new orientation
        keyboardModuleView = null;
        mouseModuleView = null;
        imeModuleView = null;
        settingsModuleView = null;
        Log.d(TAG, "reloadLayoutForOrientation: layout inflated, module views cleared");

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
        mCustomTouchListener = new CustomTouchListener(this, usbDeviceManager);
        mBinding.viewMainPreview.setOnTouchListener(mCustomTouchListener);

        // Re-setup DrawerLayoutDeal for new layout (only in landscape)
        if (!nowPortrait) {
            try {
                com.openterface.AOS.drawerLayout.DrawerLayoutDeal drawerLayoutDeal =
                        new com.openterface.AOS.drawerLayout.DrawerLayoutDeal(this, null, mIsRecording);
                drawerLayoutDeal.setDrawerLayoutButtonClickColor();
                Log.d(TAG, "reloadLayoutForOrientation: DrawerLayoutDeal created and buttons wired");
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
                // Create KeyPreviewPopup for bubble hint
                KeyPreviewPopup restorePreviewPopup = new KeyPreviewPopup(this);

                KeyBoardShortCut KeyBoardShortCut = new KeyBoardShortCut(mBinding.getRoot());
                KeyBoardCtrl KeyBoardCtrlButton = new KeyBoardCtrl(mBinding.getRoot(), restorePreviewPopup);
                KeyBoardShift KeyBoardShiftButton = new KeyBoardShift(mBinding.getRoot(), restorePreviewPopup);
                KeyBoardAlt KeyBoardAltButton = new KeyBoardAlt(mBinding.getRoot(), restorePreviewPopup);
                KeyBoardWin KeyBoardWinButton = new KeyBoardWin(mBinding.getRoot(), restorePreviewPopup);
                KeyBoardFunction KeyBoardFunction = new KeyBoardFunction(mBinding.getRoot());
                KeyBoardSystem KeyBoardSystem = new KeyBoardSystem(mBinding.getRoot());
                KeyBoardClose KeyBoardClose = new KeyBoardClose(mBinding.getRoot());

                // Pass popup and settings to KeyBoardSystem for all-key bubble + sound/vibration
                com.openterface.AOS.KeyBoardClick.KeyBoardSystem.setKeyPreviewPopup(restorePreviewPopup);
                com.openterface.AOS.KeyBoardClick.KeyBoardSystem.setSettingsManager(keyboardSettingsManager);

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
        // Note: We DON'T try to set up the preview here immediately after inflation because
        // the new TextureView's SurfaceTexture is not available yet. It will be set up
        // asynchronously in onSurfaceTextureAvailable when the surface becomes available.
        if (mCameraHelper != null && mIsCameraConnected) {
            Log.d(TAG, "Camera is connected, waiting for onSurfaceTextureAvailable to reconnect preview");
            // Reset tracking - the preview will be set up in onSurfaceTextureAvailable
            mCurrentPreviewSurfaceTexture = null;
            mCurrentPreviewSurface = null;
            mMainSurfaceAdded = false;
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
                            Log.d(TAG,"keyBoardView.getWidth():"+keyBoardView.getWidth() + "keyBoardView.getHeight():"+keyBoardView.getHeight());
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
            Log.e(TAG, "Camera not connected");
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
        // Portrait mode: disable aspect ratio constraint, TextureView fills container, Matrix handles display transform
        // Landscape mode: keep original aspect ratio constraint behavior
        if (!isPortraitMode) {
            mBinding.viewMainPreview.setAspectRatio(mPreviewWidth, mPreviewHeight);
        } else {
            mBinding.viewMainPreview.setAspectRatioEnabled(false);
            ViewGroup.LayoutParams lp = mBinding.viewMainPreview.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            mBinding.viewMainPreview.setLayoutParams(lp);
        }
        Log.d(TAG, "initPreviewView: mPreviewWidth=" + mPreviewWidth + " mPreviewHeight=" + mPreviewHeight + " isPortrait=" + isPortraitMode);
        mBinding.viewMainPreview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                HidManager.width_height(width, height);
                Log.d(TAG, "onSurfaceTextureAvailable: width=" + width + " height=" + height + " mIsCameraConnected=" + mIsCameraConnected + " mMainSurfaceAdded=" + mMainSurfaceAdded);
                if (mCameraHelper != null && mIsCameraConnected && !mMainSurfaceAdded) {
                    // Camera is already connected but surface hasn't been added yet.
                    // This happens when app launches in portrait and onCameraOpen fired
                    // before the TextureView surface was ready (the Runnable in onCameraOpen
                    // exited early because getSurfaceTexture() returned null at that time).
                    Log.d(TAG, "onSurfaceTextureAvailable: camera connected, connecting surface now");

                    // CRITICAL: Get the ACTUAL camera preview size here, not from
                    // mPreviewWidth/mPreviewHeight which may still be DEFAULT (640×480)
                    // if the Runnable in onCameraOpen hasn't updated them yet.
                    Size cameraSize = mCameraHelper.getPreviewSize();
                    if (cameraSize != null && cameraSize.width > 0 && cameraSize.height > 0) {
                        mPreviewWidth = cameraSize.width;
                        mPreviewHeight = cameraSize.height;
                        Log.d(TAG, "onSurfaceTextureAvailable: updated mPreviewWidth=" + mPreviewWidth + " mPreviewHeight=" + mPreviewHeight);
                    }

                    // IMPORTANT: Set buffer size BEFORE creating the Surface.
                    // setAspectRatio uses post() to defer requestLayout(), so onSurfaceTextureSizeChanged
                    // fires AFTER this method returns. Setting the buffer here ensures the Surface
                    // is created with the correct camera resolution.
                    setSurfaceTextureBufferSize(surface, mPreviewWidth, mPreviewHeight);
                    // Portrait mode: skip setAspectRatio, Matrix handles display transform
                    if (!isPortraitMode) {
                        mBinding.viewMainPreview.setAspectRatio(mPreviewWidth, mPreviewHeight);
                    }

                    // Create Surface from SurfaceTexture
                    android.view.Surface previewSurface = new android.view.Surface(surface);
                    // Use RendererHolder pipeline to enable frame distribution to multiple surfaces (including PIP)
                    mCameraHelper.addSurface(previewSurface, false);
                    mCurrentPreviewSurfaceTexture = surface;
                    mCurrentPreviewSurface = previewSurface;
                    mMainSurfaceAdded = true;
                    Log.d(TAG, "onSurfaceTextureAvailable: addSurface called (RendererHolder pipeline), preview=" + mPreviewWidth + "x" + mPreviewHeight);
                    // Restart preview to trigger frame output to the new surface
                    mCameraHelper.startPreview();
                    Log.d(TAG, "onSurfaceTextureAvailable: startPreview called");
                    // Make view visible after surface is ready
                    runOnUiThread(() -> {
                        if (mBinding != null && mBinding.viewMainPreview != null) {
                            mBinding.viewMainPreview.setVisibility(View.VISIBLE);
                            mBinding.viewMainPreview.setKeepScreenOn(true);
                        }
                    });
                    // Portrait mode: apply Matrix to correct stretch after surface is ready
                    if (isPortraitMode && mCustomTouchListener != null) {
                        mBinding.viewMainPreview.post(() -> mCustomTouchListener.applyPortraitZoomTransform());
                    }
                } else if (mIsCameraConnected && surface == mCurrentPreviewSurfaceTexture) {
                    Log.d(TAG, "onSurfaceTextureAvailable: surface already configured by onCameraOpen, skipping duplicate setup");
                } else if (mIsCameraConnected && mMainSurfaceAdded && surface != mCurrentPreviewSurfaceTexture) {
                    // Surface changed (e.g. TextureView was recreated) — reconnect with new surface
                    Log.d(TAG, "onSurfaceTextureAvailable: reconnecting with new surface after orientation/creation change");

                    // Get the actual camera size before creating the Surface
                    Size cameraSize = mCameraHelper != null ? mCameraHelper.getPreviewSize() : null;
                    if (cameraSize != null && cameraSize.width > 0 && cameraSize.height > 0) {
                        mPreviewWidth = cameraSize.width;
                        mPreviewHeight = cameraSize.height;
                    }

                    // Set buffer size BEFORE creating Surface - ensures Surface uses camera resolution
                    setSurfaceTextureBufferSize(surface, mPreviewWidth, mPreviewHeight);
                    // Portrait mode: skip setAspectRatio, Matrix handles display transform
                    if (!isPortraitMode) {
                        mBinding.viewMainPreview.setAspectRatio(mPreviewWidth, mPreviewHeight);
                    }
                    android.view.Surface previewSurface = new android.view.Surface(surface);
                    // Use RendererHolder pipeline to enable frame distribution to multiple surfaces (including PIP)
                    mCameraHelper.addSurface(previewSurface, false);
                    mCurrentPreviewSurfaceTexture = surface;
                    mCurrentPreviewSurface = previewSurface;
                    mMainSurfaceAdded = true;
                    mCameraHelper.startPreview();
                    Log.d(TAG, "onSurfaceTextureAvailable: reconnected surface, preview=" + mPreviewWidth + "x" + mPreviewHeight);
                    runOnUiThread(() -> {
                        if (mBinding != null && mBinding.viewMainPreview != null) {
                            mBinding.viewMainPreview.setVisibility(View.VISIBLE);
                            mBinding.viewMainPreview.setKeepScreenOn(true);
                        }
                    });
                    // Portrait mode: apply Matrix to correct stretch after surface reconnection
                    if (isPortraitMode && mCustomTouchListener != null) {
                        mBinding.viewMainPreview.post(() -> mCustomTouchListener.applyPortraitZoomTransform());
                    }
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "onSurfaceTextureSizeChanged: " + width + "x" + height +
                    " mMainSurfaceAdded=" + mMainSurfaceAdded + " mIsCameraConnected=" + mIsCameraConnected +
                    " isPortrait=" + isPortraitMode);

                if (mMainSurfaceAdded && mCameraHelper != null && mIsCameraConnected) {
                    // In portrait mode, TextureView is always MATCH_PARENT, size changes only occur during screen rotation.
                    // Skip setAspectRatio (portrait uses Matrix), avoid re-triggering onMeasure which causes flickering.
                    if (!isPortraitMode) {
                        // Surface was already added by onCameraOpen, but the view size changed
                        // (e.g. landscape mode where AspectRatioTextureView's setAspectRatio
                        // triggers onMeasure which changes the view dimensions).
                        // CRITICAL: Remove the OLD Surface from RendererHolder BEFORE adding
                        // a new one. Without removing first, the RendererHolder accumulates
                        // stale Surface objects which causes the preview to render to the
                        // wrong surface (black screen).
                        // Both removeSurface and addSurface post to the same async handler,
                        // so they execute in order: remove first, then add.
                        Log.d(TAG, "onSurfaceTextureSizeChanged: removing old surface and re-adding with new buffer size");
                        if (mCurrentPreviewSurface != null) {
                            mCameraHelper.removeSurface(mCurrentPreviewSurface);
                        }
                        setSurfaceTextureBufferSize(surface, mPreviewWidth, mPreviewHeight);
                        mBinding.viewMainPreview.setAspectRatio(mPreviewWidth, mPreviewHeight);
                        android.view.Surface previewSurface = new android.view.Surface(surface);
                        mCameraHelper.addSurface(previewSurface, false);
                        mCurrentPreviewSurfaceTexture = surface;
                        mCurrentPreviewSurface = previewSurface;
                        mCameraHelper.startPreview();
                        // Keep mMainSurfaceAdded = true — we just re-added it
                    } else {
                        // Portrait mode: surface size changes frequently during keyboard animation, skip surface deletion/recreation to avoid crash.
                        // Still need to set buffer size to camera native resolution to ensure clarity when zoomed.
                        Log.d(TAG, "onSurfaceTextureSizeChanged: portrait mode, updating buffer size without surface recreation");
                        setSurfaceTextureBufferSize(surface, mPreviewWidth, mPreviewHeight);
                        // Update Matrix to recalculate stretch correction
                        if (mCustomTouchListener != null) {
                            mBinding.viewMainPreview.post(() -> mCustomTouchListener.applyPortraitZoomTransform());
                        }
                    }
                } else {
                    // First time or surface was destroyed — set buffer size for later
                    if (!mMainSurfaceAdded) {
                        setSurfaceTextureBufferSize(surface, mPreviewWidth, mPreviewHeight);
                    }
                    // Reset the flag so onSurfaceTextureAvailable will re-add the surface
                    // when the TextureView is fully recreated
                    mMainSurfaceAdded = false;
                    mCurrentPreviewSurface = null;
                }
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                Log.d(TAG, "onSurfaceTextureDestroyed: resetting surface tracking flags");
                if (mCameraHelper != null) {
                    mCameraHelper.removeSurface(surface);
                }
                // Reset the flags so surface can be added again when recreated
                mMainSurfaceAdded = false;
                mCurrentPreviewSurfaceTexture = null;
                mCurrentPreviewSurface = null;
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        });
    }

    /**
     * Set SurfaceTexture buffer size to chip resolution for better zoom quality.
     * When the view is smaller than chip resolution, the default buffer would be
     * the view's small size, causing blur when zoomed in. Setting to chip size
     * gives the GPU more source pixels to sample from.
     */
    private void setSurfaceTextureBufferSize(SurfaceTexture surface, int bufferWidth, int bufferHeight) {
        if (surface != null && bufferWidth > 0 && bufferHeight > 0) {
            // Use chip resolution for buffer, not the small view size
            surface.setDefaultBufferSize(bufferWidth, bufferHeight);
            Log.d(TAG, "setSurfaceTextureBufferSize: " + bufferWidth + "x" + bufferHeight);
        }
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

        // Check if CAMERA permission is already granted
        boolean hasCameraPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;

        if (hasCameraPermission) {
            // Already have camera permission, skip the explanation dialog and go straight to connecting
            // This is the common case when replugging a device that was previously connected
            Log.d(TAG, "selectDevice: CAMERA permission already granted, connecting directly");
            connectCamera(device);
        } else {
            // Show permission explanation dialog first to improve UX
            showPermissionExplanationDialog(() -> {
                // After user acknowledges, request CAMERA permission
                requestCameraPermissionAndConnect(device);
            });
        }
    }

    /**
     * Show a dialog explaining why permissions are needed.
     * This improves UX by preparing the user for multiple permission requests.
     */
    private void showPermissionExplanationDialog(Runnable onContinue) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.permission_required_title)
            .setMessage(R.string.permission_explanation_message)
            .setPositiveButton(R.string.permission_continue, (dialog, which) -> {
                onContinue.run();
            })
            .setNegativeButton(R.string.permission_cancel, null)
            .setCancelable(false)
            .show();
    }

    /**
     * Request CAMERA permission and connect to device.
     * Separated from selectDevice for clearer code flow.
     */
    private void requestCameraPermissionAndConnect(UsbDevice device) {
        XXPermissions.with(this)
                .permission(Manifest.permission.CAMERA)
                .request((permissions, all) -> {
                    if (all) {
                        Log.d(TAG, "selectDevice: CAMERA permission granted, connecting camera");
                        connectCamera(device);
                    } else {
                        Log.w(TAG, "selectDevice: CAMERA permission denied");
                        Toast.makeText(this, R.string.permission_camera_denied, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Connect to camera after permissions are granted.
     */
    private void connectCamera(UsbDevice device) {
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

            // IMPORTANT: Connect serial AFTER the camera has opened and claimed its UVC
            // interfaces. Previously, serial connected first and claimed interfaces on the
            // composite device, which prevented the camera from claiming the video streaming
            // interface. Now camera goes first → serial follows.
            // Small delay to ensure camera has time to claim its interfaces before serial
            // opens its own connection and claims the serial interfaces.
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                usbDeviceManager.connect();
            }, 500);
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

            boolean hasSurfaceTexture = (mBinding.viewMainPreview.getSurfaceTexture() != null);
            Log.d(TAG, "onCameraOpen: hasSurfaceTexture=" + hasSurfaceTexture);

            // Check if we have a surface texture before proceeding
            if (!hasSurfaceTexture) {
                Log.w(TAG, "onCameraOpen: no surface texture available yet, waiting for onSurfaceTextureAvailable");
                // Don't call startPreview() without surface - it may cause black screen
                // We'll handle this in onSurfaceTextureAvailable when surface becomes available
                mIsCameraConnected = true;
                // Don't call updateUIControls yet - wait until we have surface
                return;
            }

            // Make the view visible FIRST to ensure TextureView can properly initialize its SurfaceTexture
            // This is especially important in portrait mode where the layout might not be fully initialized yet
            runOnUiThread(() -> {
                if (mBinding != null && mBinding.viewMainPreview != null) {
                    mBinding.viewMainPreview.setVisibility(View.VISIBLE);
                    mBinding.viewMainPreview.setKeepScreenOn(true);
                    Log.d(TAG, "onCameraOpen: viewMainPreview set VISIBLE BEFORE creating surface");
                }
            });

            // Create Surface from SurfaceTexture in main thread to avoid threading issues.
            // Post to ensure the view has completed its layout pass.
            //
            // IMPORTANT: resizePreviewView is called INSIDE the Runnable (not outside) to ensure:
            // 1. mPreviewWidth/mPreviewHeight are updated BEFORE creating the Surface
            // 2. setSurfaceTextureBufferSize is called BEFORE creating the Surface
            //    (changing buffer size AFTER Surface creation is ignored on some Android versions)
            // 3. The Runnable and Surface creation are atomic on the main thread,
            //    preventing race conditions with onSurfaceTextureSizeChanged
            mBinding.viewMainPreview.post(() -> {
                if (mBinding == null || mBinding.viewMainPreview == null) return;

                // Guard: if onSurfaceTextureAvailable already set up the surface
                // (race condition where callback fires before this Runnable), skip
                if (mMainSurfaceAdded) {
                    Log.d(TAG, "onCameraOpen runnable: surface already added by onSurfaceTextureAvailable, skipping");
                    return;
                }

                SurfaceTexture currentSurfaceTexture = mBinding.viewMainPreview.getSurfaceTexture();
                if (currentSurfaceTexture == null) {
                    Log.w(TAG, "onCameraOpen: SurfaceTexture is null after layout, waiting for onSurfaceTextureAvailable");
                    return;
                }

                // Step 1: Update preview dimensions from camera.
                // This must happen BEFORE creating the Surface, so that
                // mPreviewWidth/mPreviewHeight reflect the actual camera resolution.
                Size previewSize = mCameraHelper != null ? mCameraHelper.getPreviewSize() : null;
                if (previewSize != null && previewSize.width > 0 && previewSize.height > 0) {
                    mPreviewWidth = previewSize.width;
                    mPreviewHeight = previewSize.height;
                    Log.d(TAG, "onCameraOpen runnable: preview size=" + mPreviewWidth + "x" + mPreviewHeight);
                }

                // Step 2: Set aspect ratio on the view (controls how the view is measured)
                // Portrait mode: skip setAspectRatio, Matrix handles display transform
                if (!isPortraitMode) {
                    mBinding.viewMainPreview.setAspectRatio(mPreviewWidth, mPreviewHeight);
                }

                // Step 3: Set SurfaceTexture buffer size to camera resolution BEFORE creating
                // the Surface. This is the critical step - the buffer size is read by
                // SurfaceTexture when the Surface is created, so it MUST be set first.
                setSurfaceTextureBufferSize(currentSurfaceTexture, mPreviewWidth, mPreviewHeight);

                // Step 4: Create Surface from SurfaceTexture with the correct buffer size
                android.view.Surface previewSurface = new android.view.Surface(currentSurfaceTexture);
                // Use RendererHolder pipeline instead of direct preview to enable frame distribution
                // to multiple surfaces (including PIP/cameraViewSecond).
                // addSurface() adds this as a slave surface to RendererHolder, which receives
                // frames from the camera and distributes them to all registered surfaces.
                mCameraHelper.addSurface(previewSurface, false);
                mCurrentPreviewSurfaceTexture = currentSurfaceTexture;
                mCurrentPreviewSurface = previewSurface;
                mMainSurfaceAdded = true;
                Log.d(TAG, "onCameraOpen: addSurface called for main preview (RendererHolder pipeline)");

                // Guard against landscape-only cameraViewSecond in portrait mode
                if (mBinding.cameraViewSecond != null && mBinding.cameraViewSecond.getHolder() != null
                        && mBinding.cameraViewSecond.getHolder().getSurface() != null
                        && mBinding.cameraViewSecond.getHolder().getSurface().isValid()) {
                    mCameraHelper.addSurface(mBinding.cameraViewSecond.getHolder().getSurface(), false);
                    Log.d(TAG, "onCameraOpen: addSurface called for cameraViewSecond (PIP)");
                }

                // Start preview after adding surfaces to ensure frames flow correctly
                mCameraHelper.startPreview();
                Log.d(TAG, "onCameraOpen: startPreview called after adding surfaces, preview=" + mPreviewWidth + "x" + mPreviewHeight);

                // Portrait mode: apply Matrix to correct stretch after surface is ready
                if (isPortraitMode && mCustomTouchListener != null) {
                    mBinding.viewMainPreview.post(() -> mCustomTouchListener.applyPortraitZoomTransform());
                }
            });

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
                // Guard against landscape-only cameraViewSecond in portrait mode
                if (cameraViewSecond != null && cameraViewSecond.getHolder() != null
                        && cameraViewSecond.getHolder().getSurface() != null) {
                    mCameraHelper.removeSurface(cameraViewSecond.getHolder().getSurface());
                }
            }

            // Reset the surface tracking flags when camera closes
            mMainSurfaceAdded = false;
            mCurrentPreviewSurfaceTexture = null;
            mCurrentPreviewSurface = null;

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
        Log.d(TAG, "resizePreviewView: " + mPreviewWidth + "x" + mPreviewHeight + " (mMainSurfaceAdded=" + mMainSurfaceAdded + " isPortrait=" + isPortraitMode + ")");
        // Set the aspect ratio of TextureView to match the aspect ratio of the camera
        // Portrait mode: skip setAspectRatio, Matrix handles display transform
        if (!isPortraitMode) {
            mBinding.viewMainPreview.setAspectRatio(mPreviewWidth, mPreviewHeight);
        }
        // Update SurfaceTexture buffer to chip resolution for better zoom quality.
        // IMPORTANT: Only update buffer size if no Surface has been created from this
        // SurfaceTexture yet. Once a Surface is created (mMainSurfaceAdded=true), the
        // buffer size is locked to what was set at Surface creation time. Changing it
        // here would either be ignored or cause the camera to render at the wrong
        // resolution, resulting in a black screen on first portrait launch.
        if (!mMainSurfaceAdded && mBinding.viewMainPreview.getSurfaceTexture() != null) {
            setSurfaceTextureBufferSize(mBinding.viewMainPreview.getSurfaceTexture(), mPreviewWidth, mPreviewHeight);
        } else {
            Log.d(TAG, "resizePreviewView: skipping buffer size update (surface already added)");
        }
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
        portraitRotationLockButton = findViewById(R.id.portrait_rotation_lock);
        landscapeRotationLockButton = findViewById(R.id.landscape_rotation_lock);

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
            portraitSettingsTab.setOnClickListener(v -> showFloatingSettings());
        }
        if (portraitRotationLockButton != null) {
            portraitRotationLockButton.setOnClickListener(v -> toggleRotationLock());
        }
        if (landscapeRotationLockButton != null) {
            landscapeRotationLockButton.setOnClickListener(v -> toggleRotationLock());
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

        // Setup portrait shortcut strip buttons
        setupPortraitShortcutStrip();

        // Update rotation lock button icon to reflect current state
        updateRotationLockButtonIcon();
    }

    /**
     * Setup portrait mode shortcut strip buttons dynamically from active profile.
     * Shows favorites (My Shortcuts) if available, otherwise first N shortcuts from first category.
     * The profile switch button (left) opens the ShortcutHub floating fragment.
     */
    private void setupPortraitShortcutStrip() {
        // Top-left button: open Shortcut Hub directly
        ImageButton profileSwitchButton = findViewById(R.id.shortcut_profile_switch);
        if (profileSwitchButton != null) {
            profileSwitchButton.setOnClickListener(v -> {
                ShortcutHubFloatingFragment fragment = new ShortcutHubFloatingFragment();
                fragment.setOnDismissListener(() -> {
                    // Refresh shortcut strip after closing (config may have changed)
                    setupPortraitShortcutStrip();
                });
                fragment.show(getSupportFragmentManager(), "shortcut_hub_floating");
            });
        }

        // Dynamically populate the shortcut buttons container
        LinearLayout buttonsContainer = findViewById(R.id.shortcut_buttons_container);
        if (buttonsContainer == null) return;

        // Remove all dynamically added buttons (keep only XML-defined ones)
        // We'll remove everything and re-add the settings button at the end
        buttonsContainer.removeAllViews();

        // Get active profile
        ShortcutProfileManager profileManager = ShortcutProfileManager.getInstance(this);
        ShortcutProfile activeProfile = profileManager.getActiveProfile();

        List<ShortcutProfile.Shortcut> displayShortcuts = new ArrayList<>();

        if (activeProfile != null) {
            // First try favorites
            List<ShortcutProfile.Shortcut> favorites = profileManager.getMyShortcuts(activeProfile.id);
            if (!favorites.isEmpty()) {
                displayShortcuts.addAll(favorites);
            } else {
                // Fall back to all shortcuts (handles both old flat list and new categories)
                List<ShortcutProfile.Shortcut> allShortcuts = activeProfile.getAllShortcutsFlat();
                int limit = Math.min(allShortcuts.size(), 8);
                displayShortcuts.addAll(allShortcuts.subList(0, limit));
            }
        }

        // Add shortcut buttons dynamically
        for (ShortcutProfile.Shortcut shortcut : displayShortcuts) {
            Button btn = new Button(this);
            btn.setText(shortcut.label != null ? shortcut.label : shortcut.name);
            btn.setTextSize(11);
            btn.setTextColor(getResources().getColor(R.color.text_primary));
            btn.setBackgroundResource(R.drawable.shortcut_button_background);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (int) (40 * getResources().getDisplayMetrics().density));
            lp.setMargins((int) (3 * getResources().getDisplayMetrics().density), 0,
                    (int) (3 * getResources().getDisplayMetrics().density), 0);
            btn.setLayoutParams(lp);
            btn.setPadding((int) (12 * getResources().getDisplayMetrics().density), 0,
                    (int) (12 * getResources().getDisplayMetrics().density), 0);
            btn.setAllCaps(false);

            btn.setOnClickListener(v -> executePortraitShortcut(shortcut));
            buttonsContainer.addView(btn);
        }

        // Re-add the keyboard settings button at the end
        ImageButton settingsBtn = new ImageButton(this);
        settingsBtn.setImageResource(R.drawable.baseline_settings_24);
        settingsBtn.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        settingsBtn.setContentDescription("键盘设置");
        int btnSize = (int) (40 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(btnSize, btnSize);
        sLp.setMargins((int) (3 * getResources().getDisplayMetrics().density), 0,
                (int) (3 * getResources().getDisplayMetrics().density), 0);
        settingsBtn.setLayoutParams(sLp);
        settingsBtn.setPadding((int) (10 * getResources().getDisplayMetrics().density),
                (int) (10 * getResources().getDisplayMetrics().density),
                (int) (10 * getResources().getDisplayMetrics().density),
                (int) (10 * getResources().getDisplayMetrics().density));
        settingsBtn.setBackgroundResource(R.drawable.shortcut_button_background);
        settingsBtn.setColorFilter(getResources().getColor(R.color.text_primary));
        settingsBtn.setOnClickListener(v -> showKeyboardSettingsDialog());
        buttonsContainer.addView(settingsBtn);
    }

    /**
     * Execute a shortcut from the portrait strip.
     */
    private void executePortraitShortcut(ShortcutProfile.Shortcut shortcut) {
        if (shortcut == null) return;

        String key = hidKeyCodeToString(shortcut.keyCode);
        if (key == null || key.isEmpty()) {
            Toast.makeText(this, "未知按键", Toast.LENGTH_SHORT).show();
            return;
        }

        // Extract individual modifier flags
        boolean ctrl = (shortcut.modifiers & ShortcutProfile.MOD_CTRL) != 0;
        boolean shift = (shortcut.modifiers & ShortcutProfile.MOD_SHIFT) != 0;
        boolean alt = (shortcut.modifiers & ShortcutProfile.MOD_ALT) != 0;
        boolean win = (shortcut.modifiers & ShortcutProfile.MOD_WIN) != 0;

        Log.d(TAG, "Executing shortcut: ctrl=" + ctrl + " shift=" + shift +
                " alt=" + alt + " win=" + win + " key=" + key);

        // Use HidManager to support both Java and Core implementations
        if (ctrl || shift || alt || win) {
            HidManager.sendKeyBoardFunction(
                    ctrl ? "Ctrl" : "",
                    shift ? "Shift" : "",
                    alt ? "Alt" : "",
                    win ? "Win" : "",
                    key);
        } else {
            HidManager.sendKeyBoardPressAndRelease("", key);
        }
    }

    /**
     * Map HID usage code to the key string expected by KeyBoardManager/HidManager.
     */
    private String hidKeyCodeToString(int keyCode) {
        switch (keyCode) {
            // Letters (a-z)
            case 4: return "A"; case 5: return "B"; case 6: return "C"; case 7: return "D";
            case 8: return "E"; case 9: return "F"; case 10: return "G"; case 11: return "H";
            case 12: return "I"; case 13: return "J"; case 14: return "K"; case 15: return "L";
            case 16: return "M"; case 17: return "N"; case 18: return "O"; case 19: return "P";
            case 20: return "Q"; case 21: return "R"; case 22: return "S"; case 23: return "T";
            case 24: return "U"; case 25: return "V"; case 26: return "W"; case 27: return "X";
            case 28: return "Y"; case 29: return "Z";
            // Numbers (1-0)
            case 30: return "1"; case 31: return "2"; case 32: return "3"; case 33: return "4";
            case 34: return "5"; case 35: return "6"; case 36: return "7"; case 37: return "8";
            case 38: return "9"; case 39: return "0";
            // Enter / Escape / Backspace / Tab / Space
            case 40: return "ENTER"; case 41: return "ESC"; case 42: return "BACKSPACE";
            case 43: return "TAB"; case 44: return "SPACE";
            // Symbols
            case 45: return "-"; case 46: return "="; case 47: return "["; case 48: return "]";
            case 49: return "\\"; case 51: return ";"; case 52: return "'"; case 53: return "`";
            case 54: return ","; case 55: return "."; case 56: return "/";
            // Caps Lock
            case 57: return "CAPSLOCK";
            // Function keys
            case 58: return "F1"; case 59: return "F2"; case 60: return "F3"; case 61: return "F4";
            case 62: return "F5"; case 63: return "F6"; case 64: return "F7"; case 65: return "F8";
            case 66: return "F9"; case 67: return "F10"; case 68: return "F11"; case 69: return "F12";
            // Additional keys
            case 70: return "PRTSC"; case 71: return "SCROLLLOCK"; case 72: return "PAUSE";
            case 73: return "INSERT";
            // Navigation
            case 74: return "HOME"; case 75: return "PAGEUP"; case 76: return "DELETE";
            case 77: return "END"; case 78: return "PAGEDOWN";
            case 79: return "RIGHT"; case 80: return "LEFT"; case 81: return "DOWN"; case 82: return "UP";
            // Num Lock
            case 83: return "NUMLOCK";
            // Numpad operators
            case 84: return "NUMPAD_DIVIDE"; case 85: return "NUMPAD_MULTIPLY";
            case 86: return "NUMPAD_SUBTRACT"; case 87: return "NUMPAD_ADD";
            case 88: return "NUMPAD_ENTER";
            // Numpad digits
            case 89: return "NUMPAD_1"; case 90: return "NUMPAD_2";
            case 91: return "NUMPAD_3"; case 92: return "NUMPAD_4";
            case 93: return "NUMPAD_5"; case 94: return "NUMPAD_6";
            case 95: return "NUMPAD_7"; case 96: return "NUMPAD_8";
            case 97: return "NUMPAD_9"; case 98: return "NUMPAD_0";
            case 99: return "NUMPAD_DECIMAL";
            default: return null;
        }
    }


    @Override
    public void onBackPressed() {
        // Handle back navigation from ShortcutManagerFragment or close module
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            // After popping, re-inflate the settings module if needed
            if (isPortraitMode && currentModule == ModuleType.SETTINGS && portraitModuleContent != null) {
                settingsModuleView = null; // Clear cache to force recreation
                portraitModuleContent.removeAllViews();
                inflateModuleView(ModuleType.SETTINGS);
            }
            return;
        } else if (isPortraitMode && currentModule != ModuleType.NONE) {
            // Close the current module if back stack is empty and a module is open
            setModuleState(ModuleType.NONE, true);
            return;
        }
        super.onBackPressed();
    }

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
            // EXPAND: Show module drawer at bottom
            if (portraitModuleDrawer != null) {
                portraitModuleDrawer.setVisibility(View.VISIBLE);
                // Let the height be determined by content (wrap_content from XML)

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

            case SETTINGS:
                if (settingsModuleView == null) {
                    settingsModuleView = inflater.inflate(
                        R.layout.module_portrait_settings, portraitModuleContent, false);
                    setupSettingsModule(settingsModuleView);
                }
                moduleView = settingsModuleView;
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
            // Create KeyPreviewPopup for bubble hint if not already created
            if (keyPreviewPopup == null) {
                keyPreviewPopup = new KeyPreviewPopup(this);
            }

            KeyBoardShortCut keyBoardShortCut = new KeyBoardShortCut(view);
            KeyBoardCtrl keyBoardCtrl = new KeyBoardCtrl(view, keyPreviewPopup);
            KeyBoardShift keyBoardShift = new KeyBoardShift(view, keyPreviewPopup);
            KeyBoardAlt keyBoardAlt = new KeyBoardAlt(view, keyPreviewPopup);
            KeyBoardWin keyBoardWin = new KeyBoardWin(view, keyPreviewPopup);
            KeyBoardFunction keyBoardFunction = new KeyBoardFunction(view);
            KeyBoardSystem keyBoardSystem = new KeyBoardSystem(view);
            KeyBoardClose keyBoardClose = new KeyBoardClose(view);

            // Pass popup and settings to KeyBoardSystem for all-key bubble + sound/vibration
            KeyBoardSystem.setKeyPreviewPopup(keyPreviewPopup);
            KeyBoardSystem.setSettingsManager(keyboardSettingsManager);

            // Setup CharacterAlternatesPopup for long-press character selection (portrait mode)
            CharacterAlternatesPopup alternatesPopup = new CharacterAlternatesPopup(this);
            KeyBoardSystem.setAlternatesPopup(alternatesPopup);

            keyBoardShortCut.setShortCutButtonsClickColor();
            keyBoardFunction.setFunctionButtonsClickColor();
            keyBoardSystem.setSystemButtonsClickColor();
            keyBoardClose.setCloseButtonClickColor();

            // Use the constructor that accepts rootView for portrait mode
            mKeyBoardOpacity = new KeyBoardOpacity(this, view);
            mKeyBoardOpacity.setOpacityButtonClick();
        } catch (Exception e) {
            Log.w(TAG, "setupKeyboardModule: keyboard init failed: " + e.getMessage());
        }
    }

    /**
     * Setup mouse module for portrait mode.
     * Includes MouseControlStripView (L/M/R) and BasicPortraitScrollStripView.
     */
    private void setupMouseModule(View view) {
        com.openterface.AOS.view.MouseControlStripView portraitMouseStrip =
            view.findViewById(R.id.portrait_mouseButtonStrip);

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
                        // Deprecated: scroll handled by BasicPortraitScrollStripView
                    }

                    @Override
                    public void onScrollUp() {
                        // Deprecated: scroll handled by BasicPortraitScrollStripView
                    }

                    @Override
                    public void onScrollDown() {
                        // Deprecated: scroll handled by BasicPortraitScrollStripView
                    }
                });
        }

        // Setup scroll strip
        com.openterface.AOS.view.BasicPortraitScrollStripView portraitScrollStrip =
            view.findViewById(R.id.portrait_scroll_strip);

        if (portraitScrollStrip != null) {
            portraitScrollStrip.setOnStripScrollListener(new com.openterface.AOS.view.BasicPortraitScrollStripView.OnStripScrollListener() {
                @Override
                public void onStripScroll(int deltaX, int deltaY) {
                    // deltaY > 0 means scroll up, deltaY < 0 means scroll down
                    if (deltaY != 0) {
                        com.openterface.AOS.target.MouseManager.handleTwoFingerPanSlideUpDown((float) deltaY);
                    }
                }
            });
        }

        // Setup help button — opens touchpad help dialog
        android.widget.ImageButton helpButton = view.findViewById(R.id.btn_mouse_help);
        if (helpButton != null) {
            helpButton.setOnClickListener(v -> com.openterface.AOS.view.TouchPadHelpDialog.show(MainActivity.this));
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
     * Updated: TextHidSender for HID send, clear/restore toggle, saved text management, IME insets
     */
    private boolean isSendingText = false;
    private int sendProgress = 0;
    private int sendTotal = 0;
    private ProgressBar imeProgressBar;
    private TextView imeProgressLabel;

    // Clear/Restore undo
    private String undoSnapshot;
    private boolean undoClearEligible;

    // References for Send/Stop toggle
    private java.util.concurrent.atomic.AtomicBoolean cancelSend;
    private android.widget.Button imeSendButton;
    private android.widget.Button imeClearButton;
    private android.widget.Button imeRestoreButton;
    private android.widget.Button imeSaveButton;
    private android.widget.Button imeSavedTextsButton;
    private EditText imeTextInput;
    private android.view.ViewGroup imeSavedTextOverlay;

    // Saved text repository
    private com.openterface.AOS.utils.SavedTextRepository savedTextRepo;

    private void setupImeModule(View view) {
        imeSendButton = view.findViewById(R.id.ime_send_button);
        imeClearButton = view.findViewById(R.id.ime_clear_button);
        imeRestoreButton = view.findViewById(R.id.ime_restore_button);
        imeSaveButton = view.findViewById(R.id.ime_save_button);
        imeSavedTextsButton = view.findViewById(R.id.ime_saved_texts_button);
        imeTextInput = view.findViewById(R.id.ime_text_input);
        imeProgressBar = view.findViewById(R.id.ime_send_progress);
        imeProgressLabel = view.findViewById(R.id.ime_send_progress_label);
        imeSavedTextOverlay = view.findViewById(R.id.ime_saved_text_overlay);

        cancelSend = new java.util.concurrent.atomic.AtomicBoolean(false);
        savedTextRepo = new com.openterface.AOS.utils.SavedTextRepository(this);

        // --- Send button ---
        if (imeSendButton != null && imeTextInput != null) {
            imeSendButton.setOnClickListener(v -> {
                if (isSendingText) {
                    // Cancel send
                    cancelSend.set(true);
                    return;
                }
                String text = imeTextInput.getText().toString();
                validateAndSend(text);
            });
        }

        // --- Clear button (independent of restore) ---
        if (imeClearButton != null && imeTextInput != null) {
            imeClearButton.setOnClickListener(v -> {
                if (isSendingText) return;
                String current = imeTextInput.getText().toString();
                if (current.isEmpty()) return;
                // Always remember the last cleared/sent text so Restore can bring it back
                undoSnapshot = current;
                undoClearEligible = true;
                imeTextInput.setText("");
                updateClearButtonIcon();
                android.util.Log.d(TAG, "Clear: undoSnapshot saved, len=" + current.length() + ", Restore visible");
            });
        }

        // --- Restore button (reuses the last cleared text) ---
        if (imeRestoreButton != null) {
            imeRestoreButton.setOnClickListener(v -> {
                if (isSendingText) return;
                if (undoSnapshot == null || undoSnapshot.isEmpty()) {
                    Toast.makeText(this, "没有可恢复的文本", Toast.LENGTH_SHORT).show();
                    return;
                }
                // IMPORTANT: capture into a local variable BEFORE setText().
                // setText() triggers the TextWatcher synchronously, which nulls
                // undoSnapshot (because undoClearEligible is true and length > 0).
                // Without a local, the setSelection() line would NPE.
                final String textToRestore = undoSnapshot;
                if (imeTextInput != null) {
                    imeTextInput.setText(textToRestore);
                    imeTextInput.setSelection(textToRestore.length());
                }
                // After restore, invalidate the snapshot (one-shot undo)
                undoSnapshot = null;
                undoClearEligible = false;
                updateClearButtonIcon();
                Toast.makeText(this, "已恢复", Toast.LENGTH_SHORT).show();
            });
        }

        // --- Text watcher: typing or selecting a saved item invalidates undo snapshot ---
        if (imeTextInput != null) {
            imeTextInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(android.text.Editable s) {
                    if (undoClearEligible && s != null && s.length() > 0) {
                        // User typed something new — invalidate the undo snapshot
                        undoClearEligible = false;
                        undoSnapshot = null;
                        updateClearButtonIcon();
                    }
                }
            });
        }

        // --- Save button ---
        if (imeSaveButton != null && imeTextInput != null) {
            imeSaveButton.setOnClickListener(v -> {
                if (isSendingText) return;
                String text = imeTextInput.getText().toString();
                if (text.isEmpty()) {
                    Toast.makeText(this, "文本为空，无法保存", Toast.LENGTH_SHORT).show();
                    return;
                }
                com.openterface.AOS.model.SavedTextItem item = savedTextRepo.addFromPlainText(text);
                if (item != null) {
                    Toast.makeText(this, "文本已保存", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // --- Saved texts button ---
        if (imeSavedTextsButton != null) {
            imeSavedTextsButton.setOnClickListener(v -> {
                if (isSendingText) return;
                showSavedTextsDialog();
            });
        }

        // --- IME insets handling ---
        setupImeInsets(view);

        // Initial button state
        updateClearButtonIcon();
    }

    private void setupImeInsets(View root) {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            androidx.core.graphics.Insets bars = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars());
            androidx.core.graphics.Insets cut = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            androidx.core.graphics.Insets ime = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime());
            int leftInset = Math.max(bars.left, cut.left);
            int rightInset = Math.max(bars.right, cut.right);
            int bottomInset = Math.max(ime.bottom, Math.max(bars.bottom, cut.bottom));

            root.setPadding(
                root.getPaddingLeft(),
                root.getPaddingTop(),
                root.getPaddingRight(),
                bottomInset
            );
            return windowInsets;
        });
        root.post(() -> androidx.core.view.ViewCompat.requestApplyInsets(root));
    }

    private void updateClearButtonIcon() {
        if (imeClearButton != null) {
            imeClearButton.setText(undoClearEligible ? "↩ Clear" : "Clear");
        }
        if (imeRestoreButton != null) {
            // Use enabled state instead of visibility — keeps the layout slot reserved
            // so the action bar row doesn't reflow when Restore toggles.
            boolean hasSnapshot = undoSnapshot != null && !undoSnapshot.isEmpty();
            imeRestoreButton.setEnabled(hasSnapshot);
            imeRestoreButton.setAlpha(hasSnapshot ? 1.0f : 0.4f);
        }
    }

    /**
     * Validate text and device connection before sending.
     * Based on KeyCMD's ImeComposeSendGate validation logic.
     *
     * @param text The text to send
     */
    private void validateAndSend(String text) {
        if (isSendingText) return;

        // Check device connection
        boolean isConnected = usbDeviceManager != null && usbDeviceManager.isConnected();
        if (!isConnected) {
            Toast.makeText(this, "No device connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if text is empty
        if (text == null || text.isEmpty()) {
            Toast.makeText(this, "Text is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if text contains non-ASCII characters (warning)
        boolean hasNonAscii = com.openterface.AOS.utils.TextValidator.hasNonAscii(text);
        if (hasNonAscii) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Non-ASCII Characters")
                .setMessage("Text contains non-ASCII characters. These may not be sent correctly.")
                .setPositiveButton("Continue", (dialog, which) -> startSend(text))
                .setNegativeButton("Cancel", null)
                .show();
            return;
        }

        // Check if text is too long (warning)
        boolean isTooLong = com.openterface.AOS.utils.TextValidator.isTooLong(text);
        if (isTooLong) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Long Text")
                .setMessage("Text is very long (" + text.length() + " chars). Sending may take a while.")
                .setPositiveButton("Continue", (dialog, which) -> startSend(text))
                .setNegativeButton("Cancel", null)
                .show();
            return;
        }

        // All validations passed, send directly
        startSend(text);
    }

    private void startSend(String text) {
        if (isSendingText) return;
        isSendingText = true;
        cancelSend.set(false);
        sendProgress = 0;
        sendTotal = com.openterface.AOS.utils.TextHidSender.countSendUnits(text);
        if (sendTotal <= 0) sendTotal = text.length();

        // Disable editor
        if (imeTextInput != null) {
            imeTextInput.setEnabled(false);
        }

        // Show progress
        showSendProgress();

        // Switch send button to stop
        if (imeSendButton != null) {
            imeSendButton.setText("Stop");
        }

        final String sendText = text;
        new Thread(() -> {
            com.openterface.AOS.utils.TextHidSender.Result result;
            try {
                result = com.openterface.AOS.utils.TextHidSender.send(
                    sendText,
                    cancelSend,
                    (completed, total) -> {
                        // Progress callback - update UI on main thread
                        runOnUiThread(() -> {
                            sendProgress = completed;
                            updateSendProgress();
                        });
                    }
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                result = com.openterface.AOS.utils.TextHidSender.Result.CANCELLED;
            }

            // Send complete
            final com.openterface.AOS.utils.TextHidSender.Result finalResult = result;
            runOnUiThread(() -> {
                isSendingText = false;
                if (imeTextInput != null) {
                    imeTextInput.setEnabled(true);
                }
                hideSendProgress();

                if (finalResult == com.openterface.AOS.utils.TextHidSender.Result.CANCELLED) {
                    Toast.makeText(MainActivity.this, "已取消发送", Toast.LENGTH_SHORT).show();
                } else if (finalResult == com.openterface.AOS.utils.TextHidSender.Result.COMPLETED) {
                    Toast.makeText(MainActivity.this, "文本发送完成", Toast.LENGTH_SHORT).show();
                    // Set undo snapshot to the sent text so user can restore it later
                    undoSnapshot = sendText;
                    undoClearEligible = true;
                    updateClearButtonIcon();
                    // Do NOT clear editor after send (keep text)
                }

                // Reset send button
                if (imeSendButton != null) {
                    imeSendButton.setText("Send");
                }
            });
        }, "ime-send").start();
    }

    private void showSendProgress() {
        if (imeProgressBar != null) {
            imeProgressBar.setVisibility(View.VISIBLE);
            imeProgressBar.setMax(sendTotal);
            imeProgressBar.setProgress(0);
        }
        if (imeProgressLabel != null) {
            imeProgressLabel.setVisibility(View.VISIBLE);
            imeProgressLabel.setText("0/" + sendTotal);
        }
    }

    private void updateSendProgress() {
        if (imeProgressBar != null) {
            imeProgressBar.setProgress(sendProgress);
        }
        if (imeProgressLabel != null) {
            int remaining = sendTotal - sendProgress;
            imeProgressLabel.setText(remaining + " left");
        }
    }

    private void hideSendProgress() {
        if (imeProgressBar != null) {
            imeProgressBar.setVisibility(View.GONE);
        }
        if (imeProgressLabel != null) {
            imeProgressLabel.setVisibility(View.GONE);
        }
    }

    private void showSavedTextsDialog() {
        // Open the full-screen RecyclerView overlay (mirrors KeyCMD ImeSavedTextFragment)
        showImeSavedTextOverlay();
    }

    /**
     * Show full-screen saved texts overlay as a Fragment with a RecyclerView,
     * matching KeyCMD's ImeSavedTextFragment design.
     */
    public void showImeSavedTextOverlay() {
        if (imeSavedTextOverlay == null) {
            // Overlay container is missing in this orientation — bail out gracefully.
            return;
        }
        if (getSupportFragmentManager().findFragmentByTag("ime_saved_text") != null) {
            return; // already visible
        }
        androidx.fragment.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(android.R.id.content, new com.openterface.AOS.fragment.ImeSavedTextFragment(), "ime_saved_text");
        ft.addToBackStack("ime_saved_text");
        ft.commit();
        // Note: a true overlay-by-replacing-content pauses the IME module behind the overlay.
        // If we wanted a non-modal overlay we'd inflate the fragment into imeSavedTextOverlay,
        // but the FrameLayout overlay isn't currently wired up for child fragments — using the
        // content area keeps things simple and matches the visual feel of KeyCMD's full-screen page.
        if (imeSavedTextOverlay != null) {
            imeSavedTextOverlay.setVisibility(View.GONE);
        }
    }

    public void hideImeSavedTextOverlay() {
        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
        // The back stack name must match what we used in addToBackStack() above. Without a
        // matching name, popBackStack returns false silently and the fragment is never popped.
        if (fm.findFragmentByTag("ime_saved_text") != null) {
            fm.popBackStack("ime_saved_text", androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        if (imeSavedTextOverlay != null) {
            imeSavedTextOverlay.setVisibility(View.GONE);
        }
    }

    /**
     * Bridge so the saved texts fragment can read/write the IME editor without
     * coupling to MainActivity internals. Mirrors KeyCMD's MainActivity.getImeSavedTextHost().
     */
    public com.openterface.AOS.fragment.ImeSavedTextFragment.Host getImeSavedTextHost() {
        return new com.openterface.AOS.fragment.ImeSavedTextFragment.Host() {
            @NonNull
            @Override
            public String readCurrentEditorText() {
                if (imeTextInput == null) return "";
                String t = imeTextInput.getText().toString();
                return t != null ? t : "";
            }

            @Override
            public void onLoadIntoEditor(@NonNull String content) {
                if (imeTextInput == null) return;
                imeTextInput.setText(content);
                imeTextInput.setSelection(content.length());
                // Loading a saved item invalidates the undo snapshot (we now have new editor content)
                undoSnapshot = null;
                undoClearEligible = false;
                updateClearButtonIcon();
            }

            @Override
            public void onSendSavedText(@NonNull String content) {
                if (isSendingText || content == null) return;
                validateAndSend(content);
            }
        };
    }

    private void showSavedTextItemActions(com.openterface.AOS.model.SavedTextItem item) {
        // Legacy entry point kept for forward-compat with any external callers.
        // The new saved texts overlay handles row actions via popup menus inside the fragment.
        if (item == null) return;
        new android.app.AlertDialog.Builder(this)
            .setTitle(item.title != null ? item.title : "(untitled)")
            .setItems(new String[]{"加载到编辑器", "直接发送", "重命名", "置顶/取消置顶", "删除"}, (dialog, which) -> {
                switch (which) {
                    case 0:
                        if (imeTextInput != null && item.content != null) {
                            imeTextInput.setText(item.content);
                            imeTextInput.setSelection(item.content.length());
                            undoSnapshot = null;
                            undoClearEligible = false;
                            updateClearButtonIcon();
                        }
                        break;
                    case 1:
                        if (!isSendingText && item.content != null) {
                            validateAndSend(item.content);
                        }
                        break;
                    case 2:
                        EditText input = new EditText(this);
                        new android.app.AlertDialog.Builder(this)
                            .setTitle("重命名")
                            .setView(input)
                            .setPositiveButton("确定", (d, w) -> {
                                String newTitle = input.getText().toString().trim();
                                if (!newTitle.isEmpty()) {
                                    savedTextRepo.updateTitle(item.id, newTitle);
                                    Toast.makeText(this, "已重命名", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                        break;
                    case 3: // Pin/Unpin
                        savedTextRepo.setPinned(item.id, !item.pinned);
                        Toast.makeText(this, item.pinned ? "已取消置顶" : "已置顶", Toast.LENGTH_SHORT).show();
                        break;
                    case 4: // Delete
                        savedTextRepo.delete(item.id);
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                        break;
                }
            })
            .setNegativeButton("返回", null)
            .show();
    }

    /**
     * Update tab visual states (active/inactive)
     */
    private void updateTabVisuals() {
        // Get theme-aware colors
        int activeColor = androidx.core.content.ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = androidx.core.content.ContextCompat.getColor(this, R.color.background_light);
        int activeIconColor = androidx.core.content.ContextCompat.getColor(this, R.color.on_primary);
        int inactiveIconColor = androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary);

        // Keyboard tab
        if (portraitKeyboardTab != null) {
            boolean active = (currentModule == ModuleType.KEYBOARD);
            portraitKeyboardTab.setBackgroundColor(active ? activeColor : inactiveColor);
            ImageView icon = portraitKeyboardTab.findViewById(R.id.tab_keyboard_icon);
            if (icon != null) {
                icon.setColorFilter(active ? activeIconColor : inactiveIconColor);
            }
        }

        // Mouse tab
        if (portraitMouseTab != null) {
            boolean active = (currentModule == ModuleType.MOUSE);
            portraitMouseTab.setBackgroundColor(active ? activeColor : inactiveColor);
            ImageView icon = portraitMouseTab.findViewById(R.id.tab_mouse_icon);
            if (icon != null) {
                icon.setColorFilter(active ? activeIconColor : inactiveIconColor);
            }
        }

        // IME tab
        if (portraitImeTab != null) {
            boolean active = (currentModule == ModuleType.IME);
            portraitImeTab.setBackgroundColor(active ? activeColor : inactiveColor);
            ImageView icon = portraitImeTab.findViewById(R.id.tab_ime_icon);
            if (icon != null) {
                icon.setColorFilter(active ? activeIconColor : inactiveIconColor);
            }
        }

        // Settings tab
        if (portraitSettingsTab != null) {
            boolean active = (currentModule == ModuleType.SETTINGS);
            portraitSettingsTab.setBackgroundColor(active ? activeColor : inactiveColor);
            ImageView icon = portraitSettingsTab.findViewById(R.id.tab_settings_icon);
            if (icon != null) {
                icon.setColorFilter(active ? activeIconColor : inactiveIconColor);
            }
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
     * Toggle screen rotation lock.
     * When locked, the screen stays in its current orientation regardless of device rotation.
     * When unlocked, the screen follows the device sensor (auto-rotate).
     */
    public void toggleRotationLock() {
        isRotationLocked = !isRotationLocked;

        if (isRotationLocked) {
            // Lock to current orientation
            int currentOrientation = getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
            Toast.makeText(this, R.string.rotation_lock_locked, Toast.LENGTH_SHORT).show();
        } else {
            // Unlock: allow sensor-based auto-rotate
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            Toast.makeText(this, R.string.rotation_lock_unlocked, Toast.LENGTH_SHORT).show();
        }

        Log.d(TAG, "toggleRotationLock: " + (isRotationLocked ? "LOCKED" : "UNLOCKED"));
        updateRotationLockButtonIcon();
    }

    /**
     * Update the rotation lock button icon and background based on current lock state.
     * Shows lock icon with orange background when locked, unlock icon with default background when unlocked.
     */
    private void updateRotationLockButtonIcon() {
        int iconRes = isRotationLocked ? R.drawable.ic_lock_24 : R.drawable.ic_lock_open_24;
        int bgRes = isRotationLocked ? R.drawable.shortcut_button_background_orange : R.drawable.shortcut_button_background;
        if (portraitRotationLockButton != null) {
            ((ImageButton) portraitRotationLockButton).setImageResource(iconRes);
            portraitRotationLockButton.setBackgroundResource(bgRes);
        }
        if (landscapeRotationLockButton != null) {
            ((ImageButton) landscapeRotationLockButton).setImageResource(iconRes);
            landscapeRotationLockButton.setBackgroundResource(bgRes);
        }
    }

    /**
     * Initialize TouchPad / Mouse strip for landscape mode.
     * TouchPadView has been removed from the layout — only MouseControlStripView remains.
     */
    private void initTouchPad() {
        mouseControlStripView = findViewById(R.id.mouseButtonStrip);
        touchPadContainer = findViewById(R.id.TouchPad_View);

        restoreTouchPadOpacity();

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
                    // Deprecated: scroll handled by BasicPortraitScrollStripView
                }

                @Override
                public void onScrollUp() {
                    // Deprecated: scroll handled by BasicPortraitScrollStripView
                }

                @Override
                public void onScrollDown() {
                    // Deprecated: scroll handled by BasicPortraitScrollStripView
                }
            });
        }

        // Setup landscape scroll strip
        com.openterface.AOS.view.BasicPortraitScrollStripView landscapeScrollStrip =
            findViewById(R.id.landscape_scroll_strip);

        if (landscapeScrollStrip != null) {
            landscapeScrollStrip.setOnStripScrollListener(new com.openterface.AOS.view.BasicPortraitScrollStripView.OnStripScrollListener() {
                @Override
                public void onStripScroll(int deltaX, int deltaY) {
                    // deltaY > 0 means scroll up, deltaY < 0 means scroll down
                    if (deltaY != 0) {
                        com.openterface.AOS.target.MouseManager.handleTwoFingerPanSlideUpDown((float) deltaY);
                    }
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
                // Open settings module
                if (isPortraitMode) {
                    toggleModule(ModuleType.SETTINGS);
                } else {
                    if (touchPadSettings == null) {
                        touchPadSettings = new TouchPadSettings(MainActivity.this);
                    }
                    touchPadSettings.showSettingsDialog();
                }
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

    // ===== Settings Module =====

    private final String PREF_SETTINGS = "AppSettings";
    private final String KEY_SOUND_ENABLED = "key_sound_enabled";
    private final String KEY_SOUND_VOLUME = "key_sound_volume";
    private final String KEY_VIBRATE_ENABLED = "key_vibrate_enabled";
    private final String KEY_VIBRATE_INTENSITY = "key_vibrate_intensity";
    private final String KEY_SCROLL_SPEED = "scroll_speed";
    private final String KEY_MOUSE_SPEED = "mouse_speed";
    private final String KEY_PRE_VALIDATE = "pre_validate_enabled";
    private final String KEY_AUTO_SAVE = "auto_save_enabled";

    private void setupSettingsModule(View view) {
        android.content.SharedPreferences prefs = getSharedPreferences(PREF_SETTINGS, MODE_PRIVATE);

        // === Keyboard Settings ===
        androidx.appcompat.widget.SwitchCompat soundSwitch = view.findViewById(R.id.settings_key_sound_switch);
        LinearLayout volumeLayout = view.findViewById(R.id.settings_volume_layout);
        SeekBar volumeSeekbar = view.findViewById(R.id.settings_volume_seekbar);

        androidx.appcompat.widget.SwitchCompat vibrateSwitch = view.findViewById(R.id.settings_vibrate_switch);
        LinearLayout vibrateIntensityLayout = view.findViewById(R.id.settings_vibrate_intensity_layout);
        SeekBar vibrateSeekbar = view.findViewById(R.id.settings_vibrate_intensity_seekbar);

        // === Mouse Settings ===
        SeekBar scrollSpeedSeekbar = view.findViewById(R.id.settings_scroll_speed_seekbar);
        TextView scrollSpeedValue = view.findViewById(R.id.settings_scroll_speed_value);
        SeekBar mouseSpeedSeekbar = view.findViewById(R.id.settings_mouse_speed_seekbar);
        TextView mouseSpeedValue = view.findViewById(R.id.settings_mouse_speed_value);

        // === IME Settings ===
        androidx.appcompat.widget.SwitchCompat preValidateSwitch = view.findViewById(R.id.settings_pre_validate_switch);
        androidx.appcompat.widget.SwitchCompat autoSaveSwitch = view.findViewById(R.id.settings_auto_save_switch);

        // Load saved values
        boolean soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true);
        int soundVolume = prefs.getInt(KEY_SOUND_VOLUME, 80);
        boolean vibrateEnabled = prefs.getBoolean(KEY_VIBRATE_ENABLED, true);
        int vibrateIntensity = prefs.getInt(KEY_VIBRATE_INTENSITY, 60);
        int scrollSpeed = prefs.getInt(KEY_SCROLL_SPEED, 50);
        int mouseSpeed = prefs.getInt(KEY_MOUSE_SPEED, 50);
        boolean preValidate = prefs.getBoolean(KEY_PRE_VALIDATE, true);
        boolean autoSave = prefs.getBoolean(KEY_AUTO_SAVE, false);

        // Apply saved values
        if (soundSwitch != null) soundSwitch.setChecked(soundEnabled);
        if (volumeSeekbar != null) volumeSeekbar.setProgress(soundVolume);
        if (volumeLayout != null) volumeLayout.setVisibility(soundEnabled ? View.VISIBLE : View.GONE);
        if (vibrateSwitch != null) vibrateSwitch.setChecked(vibrateEnabled);
        if (vibrateSeekbar != null) vibrateSeekbar.setProgress(vibrateIntensity);
        if (vibrateIntensityLayout != null) vibrateIntensityLayout.setVisibility(vibrateEnabled ? View.VISIBLE : View.GONE);
        if (scrollSpeedSeekbar != null) scrollSpeedSeekbar.setProgress(scrollSpeed);
        if (scrollSpeedValue != null) scrollSpeedValue.setText(getScrollSpeedLabel(scrollSpeed));
        if (mouseSpeedSeekbar != null) mouseSpeedSeekbar.setProgress(mouseSpeed);
        if (mouseSpeedValue != null) mouseSpeedValue.setText(getMouseSpeedLabel(mouseSpeed));
        // Apply saved mouse speed multiplier to both Java and Core mouse implementations
        com.openterface.AOS.target.HidManager.setMouseSpeedMultiplier(progressToMouseSpeed(mouseSpeed));
        if (preValidateSwitch != null) preValidateSwitch.setChecked(preValidate);
        if (autoSaveSwitch != null) autoSaveSwitch.setChecked(autoSave);

        // === Keyboard Sound Switch ===
        if (soundSwitch != null) {
            soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_SOUND_ENABLED, isChecked).apply();
                if (volumeLayout != null) volumeLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            });
        }

        // === Volume SeekBar ===
        if (volumeSeekbar != null) {
            volumeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) prefs.edit().putInt(KEY_SOUND_VOLUME, progress).apply();
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // === Vibrate Switch ===
        if (vibrateSwitch != null) {
            vibrateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_VIBRATE_ENABLED, isChecked).apply();
                if (vibrateIntensityLayout != null) vibrateIntensityLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            });
        }

        // === Vibrate Intensity ===
        if (vibrateSeekbar != null) {
            vibrateSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) prefs.edit().putInt(KEY_VIBRATE_INTENSITY, progress).apply();
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // === Scroll Speed ===
        if (scrollSpeedSeekbar != null) {
            scrollSpeedSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        prefs.edit().putInt(KEY_SCROLL_SPEED, progress).apply();
                        if (scrollSpeedValue != null) scrollSpeedValue.setText(getScrollSpeedLabel(progress));
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // === Mouse Move Speed ===
        if (mouseSpeedSeekbar != null) {
            mouseSpeedSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        prefs.edit().putInt(KEY_MOUSE_SPEED, progress).apply();
                        if (mouseSpeedValue != null) mouseSpeedValue.setText(getMouseSpeedLabel(progress));
                        // Apply multiplier immediately so it takes effect on next movement
                        com.openterface.AOS.target.HidManager.setMouseSpeedMultiplier(progressToMouseSpeed(progress));
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // === Pre-validate Switch ===
        if (preValidateSwitch != null) {
            preValidateSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_PRE_VALIDATE, isChecked).apply());
        }

        // === Auto Save Switch ===
        if (autoSaveSwitch != null) {
            autoSaveSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_AUTO_SAVE, isChecked).apply());
        }

        // === Target OS Configuration ===
        LinearLayout targetOsLayout = view.findViewById(R.id.settings_target_os_layout);
        TextView targetOsValue = view.findViewById(R.id.settings_target_os_value);
        if (targetOsLayout != null && targetOsValue != null) {
            // Display current OS
            targetOsValue.setText(TargetOsManager.getInstance().getCurrentOs().getName());

            targetOsLayout.setOnClickListener(v -> {
                TargetOsPickerDialog dialog = new TargetOsPickerDialog(MainActivity.this);
                dialog.setOnTargetOsSelectedListener(os -> {
                    TargetOsManager.getInstance().setCurrentOs(MainActivity.this, os);
                    targetOsValue.setText(os.getName());
                    // Update Win key label if keyboard is initialized
                    if (KeyBoardSystem.getInstance() != null) {
                        KeyBoardSystem.getInstance().applyTargetOsLabels();
                    }
                });
                dialog.show();
            });
        }

        // === Theme Configuration ===
        LinearLayout themeColorLayout = view.findViewById(R.id.settings_theme_color_layout);
        TextView themeColorValue = view.findViewById(R.id.settings_theme_color_value);
        if (themeColorLayout != null && themeColorValue != null) {
            // Display current theme color
            themeColorValue.setText(ThemeManager.getFamilyDisplayName(ThemeManager.getInstance().getThemeFamily()));

            themeColorLayout.setOnClickListener(v -> {
                showThemePickerDialog();
            });
        }

        LinearLayout themeModeLayout = view.findViewById(R.id.settings_theme_mode_layout);
        TextView themeModeValue = view.findViewById(R.id.settings_theme_mode_value);
        if (themeModeLayout != null && themeModeValue != null) {
            // Display current theme mode
            String mode = ThemeManager.getInstance().getThemeMode();
            String modeDisplay = getThemeModeDisplay(mode);
            themeModeValue.setText(modeDisplay);

            themeModeLayout.setOnClickListener(v -> {
                showThemeModeDialog();
            });
        }

        // === Device Connection ===
        LinearLayout deviceConnectLayout = view.findViewById(R.id.settings_device_connect_layout);
        TextView deviceStatus = view.findViewById(R.id.settings_device_status);
        if (deviceConnectLayout != null) {
            updateDeviceStatus(deviceStatus);
            deviceConnectLayout.setOnClickListener(v -> {
                showDeviceListDialog();
                if (currentModule == ModuleType.SETTINGS) {
                    toggleModule(ModuleType.NONE);
                }
            });
        }

        // === Device Disconnect ===
        LinearLayout deviceDisconnectLayout = view.findViewById(R.id.settings_device_disconnect_layout);
        if (deviceDisconnectLayout != null) {
            deviceDisconnectLayout.setOnClickListener(v -> {
                safelyEject();
                updateDeviceStatus(deviceStatus);
            });
        }

        // === Baud Rate ===
        LinearLayout baudrateLayout = view.findViewById(R.id.settings_baudrate_layout);
        TextView baudrateValue = view.findViewById(R.id.settings_baudrate_value);
        if (baudrateLayout != null && baudrateValue != null) {
            int currentBaudrate = usbDeviceManager != null ? usbDeviceManager.getCurrentBaudrate() : 0;
            baudrateValue.setText(String.valueOf(currentBaudrate > 0 ? currentBaudrate : 115200));
            baudrateLayout.setOnClickListener(v -> showBaudrateDialogInPortrait(baudrateValue));
        }

        // === Video Format ===
        LinearLayout videoFormatLayout = view.findViewById(R.id.settings_video_format_layout);
        if (videoFormatLayout != null) {
            videoFormatLayout.setOnClickListener(v -> {
                showVideoFormatDialog();
                if (currentModule == ModuleType.SETTINGS) toggleModule(ModuleType.NONE);
            });
        }

        // === Camera Controls ===
        LinearLayout videoControlLayout = view.findViewById(R.id.settings_video_control_layout);
        if (videoControlLayout != null) {
            videoControlLayout.setOnClickListener(v -> {
                showCameraControlsDialog();
                if (currentModule == ModuleType.SETTINGS) toggleModule(ModuleType.NONE);
            });
        }

        // === Screenshot ===
        LinearLayout screenshotLayout = view.findViewById(R.id.settings_screenshot_layout);
        if (screenshotLayout != null) {
            screenshotLayout.setOnClickListener(v -> takePicture());
        }

        // === Video Recording ===
        LinearLayout videoRecordLayout = view.findViewById(R.id.settings_video_record_layout);
        TextView videoRecordText = view.findViewById(R.id.settings_video_record_text);
        ImageView videoRecordIcon = view.findViewById(R.id.settings_video_record_icon);
        if (videoRecordLayout != null) {
            updateRecordingStatus(videoRecordText, videoRecordIcon);
            videoRecordLayout.setOnClickListener(v -> {
                toggleVideoRecord(!mIsRecording);
                mIsRecording = !mIsRecording;
                updateRecordingStatus(videoRecordText, videoRecordIcon);
            });
        }

        // === VNC Server ===
        LinearLayout vncServerLayout = view.findViewById(R.id.settings_vnc_server_layout);
        TextView vncStatus = view.findViewById(R.id.settings_vnc_status);
        if (vncServerLayout != null) {
            updateVncStatus(vncStatus);
            vncServerLayout.setOnClickListener(v -> {
                showVncServerDialog();
                if (currentModule == ModuleType.SETTINGS) toggleModule(ModuleType.NONE);
            });
        }

        // === WebRTC Server ===
        LinearLayout webrtcServerLayout = view.findViewById(R.id.settings_webrtc_server_layout);
        TextView webrtcStatus = view.findViewById(R.id.settings_webrtc_status);
        if (webrtcServerLayout != null) {
            updateWebrtcStatus(webrtcStatus);
            webrtcServerLayout.setOnClickListener(v -> {
                showWebRtcDialog();
                if (currentModule == ModuleType.SETTINGS) toggleModule(ModuleType.NONE);
            });
        }
    }

    private String getScrollSpeedLabel(int progress) {
        if (progress <= 20) return "很慢";
        if (progress <= 40) return "慢";
        if (progress <= 60) return "中等";
        if (progress <= 80) return "快";
        return "很快";
    }

    /**
     * Convert SeekBar progress (0-100) to mouse speed multiplier (0.25x-3.0x).
     * Uses piecewise linear mapping:
     *   0-50   → 0.25x to 1.0x  (slow range, finer control)
     *   50-100 → 1.0x to 3.0x   (fast range, wider spread)
     * Default progress=50 maps to multiplier=1.0x.
     */
    private float progressToMouseSpeed(int progress) {
        if (progress <= 50) {
            return 0.25f + (progress / 50f) * 0.75f;
        } else {
            return 1.0f + ((progress - 50) / 50f) * 2.0f;
        }
    }

    private String getMouseSpeedLabel(int progress) {
        float multiplier = progressToMouseSpeed(progress);
        String speed;
        if (progress <= 15) speed = "很慢";
        else if (progress <= 35) speed = "慢";
        else if (progress <= 65) speed = "正常";
        else if (progress <= 85) speed = "快";
        else speed = "很快";
        return speed + " " + String.format("%.1f", multiplier) + "x";
    }

    /**
     * Update device connection status in portrait settings
     */
    private void updateDeviceStatus(TextView statusView) {
        if (statusView == null) return;
        if (mIsCameraConnected) {
            statusView.setText(R.string.settings_connected);
            statusView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            statusView.setText(R.string.settings_disconnected);
            statusView.setTextColor(getResources().getColor(R.color.text_secondary));
        }
    }

    /**
     * Update recording status in portrait settings
     */
    private void updateRecordingStatus(TextView textView, ImageView iconView) {
        if (textView != null) {
            textView.setText(mIsRecording ? R.string.settings_stop_recording : R.string.settings_start_recording);
        }
        if (iconView != null) {
            int color = mIsRecording ? android.R.color.holo_red_light : R.color.text_primary;
            iconView.setColorFilter(getResources().getColor(color));
        }
    }

    /**
     * Update VNC server status in portrait settings
     */
    private void updateVncStatus(TextView statusView) {
        if (statusView == null) return;
        boolean running = vncService != null && vncService.isRunning();
        statusView.setText(running ? R.string.settings_running : R.string.settings_stopped);
        statusView.setTextColor(running ?
            getResources().getColor(android.R.color.holo_green_dark) :
            getResources().getColor(R.color.text_secondary));
    }

    /**
     * Update WebRTC server status in portrait settings
     */
    private void updateWebrtcStatus(TextView statusView) {
        if (statusView == null) return;
        boolean running = webRtcService != null && webRtcService.isRunning();
        statusView.setText(running ? R.string.settings_running : R.string.settings_stopped);
        statusView.setTextColor(running ?
            getResources().getColor(android.R.color.holo_green_dark) :
            getResources().getColor(R.color.text_secondary));
    }

    /**
     * Show baudrate selection dialog in portrait mode
     */
    private void showBaudrateDialogInPortrait(TextView baudrateValueView) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_baudrate_selection, null);

        RadioGroup radioGroup = dialogView.findViewById(R.id.baudrate_radio_group);
        RadioButton radio115200 = dialogView.findViewById(R.id.radio_115200);
        RadioButton radio9600 = dialogView.findViewById(R.id.radio_9600);
        TextView currentBaudrateText = dialogView.findViewById(R.id.current_baudrate_text);

        int currentBaudrate = usbDeviceManager != null ? usbDeviceManager.getCurrentBaudrate() : 0;
        currentBaudrateText.setText(currentBaudrate > 0
            ? getString(R.string.baudrate_current, String.valueOf(currentBaudrate))
            : "Current: Not connected");

        int preferredBaudrate = usbDeviceManager != null ? usbDeviceManager.getPreferredBaudrate() : 115200;
        if (preferredBaudrate == 9600) radio9600.setChecked(true);
        else radio115200.setChecked(true);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialogView.findViewById(R.id.button_ok).setOnClickListener(v -> {
            int newBaudrate = radioGroup.getCheckedRadioButtonId() == R.id.radio_9600 ? 9600 : 115200;
            if (newBaudrate != preferredBaudrate) {
                if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
                    usbDeviceManager.reconnectWithBaudrate(newBaudrate);
                } else if (usbDeviceManager != null) {
                    usbDeviceManager.setPreferredBaudrate(newBaudrate);
                }
                if (baudrateValueView != null) baudrateValueView.setText(String.valueOf(newBaudrate));
            }
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.button_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    public boolean isSoundEnabled() {
        return getSharedPreferences(PREF_SETTINGS, MODE_PRIVATE).getBoolean(KEY_SOUND_ENABLED, true);
    }

    public int getSoundVolume() {
        return getSharedPreferences(PREF_SETTINGS, MODE_PRIVATE).getInt(KEY_SOUND_VOLUME, 80);
    }

    public boolean isVibrateEnabled() {
        return getSharedPreferences(PREF_SETTINGS, MODE_PRIVATE).getBoolean(KEY_VIBRATE_ENABLED, true);
    }

    public int getVibrateIntensity() {
        return getSharedPreferences(PREF_SETTINGS, MODE_PRIVATE).getInt(KEY_VIBRATE_INTENSITY, 60);
    }

    public int getScrollSpeed() {
        return getSharedPreferences(PREF_SETTINGS, MODE_PRIVATE).getInt(KEY_SCROLL_SPEED, 50);
    }

    public int getMouseSpeed() {
        return getSharedPreferences(PREF_SETTINGS, MODE_PRIVATE).getInt(KEY_MOUSE_SPEED, 50);
    }

    // ============================================================
    // SettingsFloatingFragment.SettingsCallback implementation
    // ============================================================

    @Override
    public void setupSettingsContent(View view) {
        // Delegate to existing setupSettingsModule logic
        setupSettingsModule(view);
    }

    @Override
    public void setSoundEnabled(boolean enabled) {
        getSharedPreferences(PREF_SETTINGS, MODE_PRIVATE).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
        if (keyboardSettingsManager != null) {
            keyboardSettingsManager.setSoundEnabled(enabled);
        }
    }

    @Override
    public void setSoundVolume(int volume) {
        getSharedPreferences(PREF_SETTINGS, MODE_PRIVATE).edit().putInt(KEY_SOUND_VOLUME, volume).apply();
        if (keyboardSettingsManager != null) {
            keyboardSettingsManager.setSoundVolume(volume);
        }
    }

    @Override
    public void setVibrateEnabled(boolean enabled) {
        getSharedPreferences(PREF_SETTINGS, MODE_PRIVATE).edit().putBoolean(KEY_VIBRATE_ENABLED, enabled).apply();
        if (keyboardSettingsManager != null) {
            keyboardSettingsManager.setVibrateEnabled(enabled);
        }
    }

    @Override
    public void setVibrateIntensity(int intensity) {
        getSharedPreferences(PREF_SETTINGS, MODE_PRIVATE).edit().putInt(KEY_VIBRATE_INTENSITY, intensity).apply();
        if (keyboardSettingsManager != null) {
            keyboardSettingsManager.setVibrateStrength(intensity);
        }
    }

    @Override
    public void setScrollSpeed(int speed) {
        getSharedPreferences(PREF_SETTINGS, MODE_PRIVATE).edit().putInt(KEY_SCROLL_SPEED, speed).apply();
    }

    @Override
    public void setMouseSpeed(int speed) {
        getSharedPreferences(PREF_SETTINGS, MODE_PRIVATE).edit().putInt(KEY_MOUSE_SPEED, speed).apply();
        HidManager.setMouseSpeedMultiplier(progressToMouseSpeed(speed));
    }

    /**
     * Show floating settings dialog instead of bottom drawer.
     */
    private void showFloatingSettings() {
        SettingsFloatingFragment fragment = new SettingsFloatingFragment();
        fragment.setOnDismissListener(() -> {
            currentModule = ModuleType.NONE;
            updateTabVisuals();
        });
        fragment.show(getSupportFragmentManager(), "settings_floating");
    }

    private void showThemePickerDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("选择主题颜色");

        String[] families = ThemeManager.getAllFamilies();
        String[] displayNames = new String[families.length];
        for (int i = 0; i < families.length; i++) {
            displayNames[i] = ThemeManager.getFamilyDisplayName(families[i]);
        }

        String currentFamily = ThemeManager.getInstance().getThemeFamily();
        int selectedIndex = 0;
        for (int i = 0; i < families.length; i++) {
            if (families[i].equals(currentFamily)) {
                selectedIndex = i;
                break;
            }
        }

        builder.setSingleChoiceItems(displayNames, selectedIndex, (dialog, which) -> {
            String selectedFamily = families[which];
            ThemeManager.getInstance().setThemeFamily(this, selectedFamily);
            ThemeManager.getInstance().applyTheme(this);

            // Update settings UI display
            android.view.View settingsView = findViewById(R.id.settings_module_root);
            if (settingsView != null) {
                TextView themeValue = settingsView.findViewById(R.id.settings_theme_color_value);
                if (themeValue != null) {
                    themeValue.setText(displayNames[which]);
                }
            }

            dialog.dismiss();
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void showThemeModeDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("选择显示模式");

        String[] modes = {ThemeManager.MODE_LIGHT, ThemeManager.MODE_DARK, ThemeManager.MODE_SYSTEM};
        String[] displayNames = {"浅色模式", "深色模式", "跟随系统"};

        String currentMode = ThemeManager.getInstance().getThemeMode();
        int selectedIndex = 0;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(currentMode)) {
                selectedIndex = i;
                break;
            }
        }

        builder.setSingleChoiceItems(displayNames, selectedIndex, (dialog, which) -> {
            String selectedMode = modes[which];
            ThemeManager.getInstance().setThemeMode(this, selectedMode);
            ThemeManager.getInstance().applyThemeMode(selectedMode);

            // Update settings UI display
            android.view.View settingsView = findViewById(R.id.settings_module_root);
            if (settingsView != null) {
                TextView themeValue = settingsView.findViewById(R.id.settings_theme_mode_value);
                if (themeValue != null) {
                    themeValue.setText(displayNames[which]);
                }
            }

            // Apply theme mode change (requires Activity recreation)
            recreate();

            dialog.dismiss();
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private String getThemeModeDisplay(String mode) {
        if (mode == null) {
            return "跟随系统";
        }
        switch (mode) {
            case ThemeManager.MODE_LIGHT:
                return "浅色模式";
            case ThemeManager.MODE_DARK:
                return "深色模式";
            case ThemeManager.MODE_SYSTEM:
                return "跟随系统";
            default:
                return "跟随系统";
        }
    }

    /**
     * Show dialog for switching shortcut profiles in portrait mode.
     * Displays a list of available profiles with the active one checked.
     * Selecting a profile updates the active profile and refreshes the shortcut strip.
     * Includes a "Manage" button to open the ShortcutManagerFragment.
     */
    private void showProfileSwitcherDialog() {
        ShortcutProfileManager profileManager = ShortcutProfileManager.getInstance(this);
        java.util.List<ShortcutProfile> profiles = profileManager.getAllProfiles();

        if (profiles.isEmpty()) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("快捷键配置")
                .setMessage("暂无可用的快捷键配置")
                .setPositiveButton("确定", null)
                .show();
            return;
        }

        String[] profileNames = new String[profiles.size()];
        String activeProfileId = profileManager.getActiveProfile() != null ? profileManager.getActiveProfile().id : null;
        int checkedItem = -1;

        for (int i = 0; i < profiles.size(); i++) {
            ShortcutProfile profile = profiles.get(i);
            profileNames[i] = profile.name;
            if (profile.id.equals(activeProfileId)) {
                checkedItem = i;
            }
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("选择快捷键配置");
        builder.setSingleChoiceItems(profileNames, checkedItem, (dialog, which) -> {
            ShortcutProfile selected = profiles.get(which);
            profileManager.setActiveProfile(selected.id);
            // Update the shortcut strip buttons to reflect the new profile
            setupPortraitShortcutStrip();
            dialog.dismiss();
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        // Add "Manage" button to open ShortcutHubFloatingFragment
        builder.setNeutralButton("管理", (dialog, which) -> {
            dialog.dismiss();
            // Open Shortcut Hub as a floating panel (left side, dual-panel design)
            ShortcutHubFloatingFragment fragment = new ShortcutHubFloatingFragment();
            fragment.setOnDismissListener(() -> {
                // Refresh shortcut strip after closing (config may have changed)
                setupPortraitShortcutStrip();
            });
            fragment.show(getSupportFragmentManager(), "shortcut_hub_floating");
        });

        builder.show();
    }
}