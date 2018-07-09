package com.esp.uvc.usbcamera;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.support.v4.app.ActivityCompat;
import android.os.Environment;


import java.io.FileWriter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.esp.android.usb.camera.core.IFrameCallback;
import com.esp.android.usb.camera.core.Size;
import com.esp.android.usb.camera.core.USBMonitor;
import com.esp.android.usb.camera.core.USBMonitor.OnDeviceConnectListener;
import com.esp.android.usb.camera.core.USBMonitor.UsbControlBlock;
import com.esp.android.usb.camera.core.UVCCamera;
import com.esp.uvc.R;
import com.esp.uvc.widget.UVCCameraTextureView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.esp.android.usb.camera.core.UVCCamera.PRODUCT_VERSION_EX8029;
import static com.esp.android.usb.camera.core.UVCCamera.PRODUCT_VERSION_EX8036;
import static com.esp.android.usb.camera.core.UVCCamera.PRODUCT_VERSION_EX8037;



public class CameraMainActivity extends AppCompatActivity {

    private static final boolean DEBUG = true;
    private boolean Button = false;

    private static final String TAG = "CameraMainActivity";
private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };


    public static void verifyStoragePermissions(Activity activity) {

        try {
        //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
        // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    static {
        System.loadLibrary("cube_volume");
    }

    public static native String cube_volume(byte[] yuyy_frame,int yuyv_length,int[] pixels, int w, int h);

    private ImageView mImageViewOne = null;
    private ImageView mImageViewTwo = null;
    private ImageView mImageViewThree = null;
    private ImageView mImageViewFour = null;
    private ImageView mImageViewFive = null;
    private ImageView mImageViewSix = null;
    private SeekBar mIRSeekBar;
    private TextView mText_IRValue;
    private TextView mTextViewIR = null;

    private LinearLayout mMainLayoutTop;
    private RelativeLayout mRelativeLayout_L;
    private RelativeLayout mRelativeLayout_R;

    private ImageView mImageViewMeasureSpot1;
    private ImageView mImageViewMeasureSpot2;
    private TextView mTextViewMeasure;
    private TextView mTextViewCube;

    private Context mContext;

    // for thread pool
    private static final int CORE_POOL_SIZE = 1;
    private static final int MAX_POOL_SIZE = 4;
    private static final int KEEP_ALIVE_TIME = 10;
    protected static final ThreadPoolExecutor EXECUTER
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor = null;
    private UVCCamera mUVCCamera = null;
    private UVCCameraTextureView mUVCCameraViewR = null;
    private UVCCameraTextureView mUVCCameraViewL = null;
    private Surface mLeftPreviewSurface = null;
    private Surface mRightPreviewSurface = null;
    private FileOutputStream mFileOutputStreamColor = null;
    private FileOutputStream mFileOutputStreamDepth = null;
    private boolean mOnlyColor = true;
    private int mColorCount = 0;
    private int mDepthCount = 0;
    private int Color_W = 0;
    private int Color_H = 0;
    private int Depth_W = 0;
    private int Depth_H = 0;
    private String mSupportedSize = "";
    private int mWindowType = 0;
    private boolean mSwitchTwoWindow = false;

    private boolean mReverse = false;
    private boolean mLandscape = true;
    private boolean mFlip = false;
    private boolean mAEStatus = false;
    private boolean mAWBStatus = false;
    private boolean mCameraSensorChange = false;

    private boolean cube_volume_label = false;

    //Menu control
    private Menu mOptionsMenu;
    private static final int MENU_SETTINGS = Menu.FIRST;
    private static final int MENU_SWITCH = Menu.FIRST + 1;

    private final File mInternal_sd_path = new File("/storage/sdcard0/Android/data/com.etron.usbcamera");

    private String mProductVersion;

    // IR default value
    public static final int IR_DEFAULT_VALUE_8029 = 0x0A;
    public static final int IR_DEFAULT_VALUE_8036 = 0x04;
    public static final int IR_DEFAULT_VALUE_8037 = 0x04;
    public static final int IR_DEFAULT_VALUE_8029_MAX = 0x10;
    public static final int IR_DEFAULT_VALUE_8036_MAX = 0x06;
    public static final int IR_DEFAULT_VALUE_8037_MAX = 0x0A;

    // IR parameter
    private boolean mHasIR = false;
    private boolean mIRSwitch = false;
    private int mIRValueCurrent = 0;
    private int mIRValueDefault = 0;
    private int mIRValueMax = 0;

    private final boolean mMoveMeasureSpot = true;
    private boolean mShowMeasure = false;
    private boolean mShowCube = false;
    private int[] mZDBuffer;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.camera_main);
        initAll();
        verifyStoragePermissions(CameraMainActivity.this);
        mUSBMonitor = new USBMonitor(CameraMainActivity.this, mOnDeviceConnectListener);
        if (mUSBMonitor == null) {
            Log.d(TAG, "Error!! can not get USBMonitor ");
            return;
        }
    }

    private void readSettings() {

        AppSettings appSettings = AppSettings.getInstance(mContext);
        mOnlyColor = appSettings.get(AppSettings.ONLY_COLOR, mOnlyColor);
        Color_W = appSettings.get(AppSettings.COLOR_W, 640);
        Color_H = appSettings.get(AppSettings.COLOR_H, 480);
        Depth_W = appSettings.get(AppSettings.DEPTH_W, 320);
        Depth_H = appSettings.get(AppSettings.Depth_H, 480);
        mWindowType = appSettings.get(AppSettings.WINDOW_TYPE, mWindowType);

        mAWBStatus = appSettings.get(AppSettings.AWB_STATUS, false);
        mAEStatus = appSettings.get(AppSettings.AE_STATUS, false);
        mCameraSensorChange = appSettings.get(AppSettings.CAMERA_SENSOR_CHANGE, false);

        if (mWindowType == 1)
            mSwitchTwoWindow = true;
        else
            mSwitchTwoWindow = false;
        if (DEBUG) {
            Log.i(TAG, "readSettings:");
            Log.i(TAG, ">>>> mOnlyColor:" + mOnlyColor);
            Log.i(TAG, ">>>> Color_W:" + Color_W);
            Log.i(TAG, ">>>> Color_H:" + Color_H);
            Log.i(TAG, ">>>> Depth_W:" + Depth_W);
            Log.i(TAG, ">>>> Depth_H:" + Depth_H);
            Log.i(TAG, ">>>> mAWBStatus:" + mAWBStatus);
            Log.i(TAG, ">>>> mAEStatus:" + mAEStatus);
            Log.i(TAG, ">>>> mSwitchTwoWindow:" + mSwitchTwoWindow);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        readSettings();
        checkViewOrientation();

        if (mUSBMonitor != null)
            mUSBMonitor.register();

        if (mUVCCamera != null) {
            mUVCCamera.destroy();
            mUVCCamera = null;
        }

        updateOptionsMenu();
        calculateDisplaySize();

        resetIR();
    }

    private void resetIR() {

        mHasIR = false;
        mTextViewIR.setClickable(false);
        mTextViewIR.setTextColor(Color.GRAY);
        mTextViewIR.setAlpha(0.7f);
    }

    private int getIRMaxValue() {

        int value = -1;
        if (mUVCCamera != null) {
            value = mUVCCamera.getIRMaxValue();
            if (value == -1 && mProductVersion.equals(PRODUCT_VERSION_EX8029)) {
                value = 0x10;
            }
        }

        return value;
    }

    private void checkIRValue() {

        if (mUVCCamera != null) {

            int IRMaxValue = getIRMaxValue();

            switch (mProductVersion) {
                case PRODUCT_VERSION_EX8029:
                    mHasIR = true;
                    mIRValueDefault = IR_DEFAULT_VALUE_8029;
                    if (mIRValueMax == 0 || mIRValueMax > IRMaxValue) {
                        mIRValueMax = IR_DEFAULT_VALUE_8029_MAX;
                    }
                    break;

                case PRODUCT_VERSION_EX8036:
                    mHasIR = true;
                    mIRValueDefault = IR_DEFAULT_VALUE_8036;
                    if (mIRValueMax == 0 || mIRValueMax > IRMaxValue) {
                        mIRValueMax = IR_DEFAULT_VALUE_8036_MAX;
                    }
                    break;

                case PRODUCT_VERSION_EX8037:
                    mHasIR = true;
                    mIRValueDefault = IR_DEFAULT_VALUE_8037;
                    if (mIRValueMax == 0 || mIRValueMax > IRMaxValue) {
                        mIRValueMax = IR_DEFAULT_VALUE_8037_MAX;
                    }
                    break;

                default:
                    mHasIR = false;
            }

            if (mHasIR) {
                mIRValueCurrent = mUVCCamera.getIRCurrentValue();
            } else {
                mIRValueCurrent = 0x00;
            }
        }
    }

    private void setIRIcon() {

        if (mHasIR) {

            mTextViewIR.setClickable(true);
            mTextViewIR.setAlpha(1.0f);

            if (mIRSwitch) {
                mTextViewIR.setTextColor(Color.RED);
            } else {
                mTextViewIR.setTextColor(Color.BLACK);
            }

        } else {

            mTextViewIR.setClickable(false);
            mTextViewIR.setTextColor(Color.GRAY);
            mTextViewIR.setAlpha(0.7f);
        }
    }

    private void calculateDisplaySize() {

        LinearLayout.LayoutParams params_vl;
        LinearLayout.LayoutParams params_vr;
        if (mLandscape) {
            mMainLayoutTop.setOrientation(LinearLayout.HORIZONTAL);
            params_vl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
            params_vr = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);

        } else {
            mMainLayoutTop.setOrientation(LinearLayout.VERTICAL);
            params_vl = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
            params_vr = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
        }

        params_vl.weight = 1;
        mRelativeLayout_L.setLayoutParams(params_vl);

        params_vr.weight = 1;
        mRelativeLayout_R.setLayoutParams(params_vr);

        mUVCCameraViewL.setAspectRatio(Color_W / (float) Color_H);
        mUVCCameraViewR.setAspectRatio(Depth_W * 2 / (float) Depth_H);

        if (!mOnlyColor && mSwitchTwoWindow) {
            mRelativeLayout_R.setVisibility(View.VISIBLE);
            mUVCCameraViewR.setVisibility(View.VISIBLE);
            mImageViewFour.setVisibility(View.VISIBLE);
        } else {
            mRelativeLayout_R.setVisibility(View.GONE);
            mUVCCameraViewR.setVisibility(View.GONE);
            mImageViewFour.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        if (mUVCCamera != null) {
            mUVCCamera.destroy();
            mUVCCamera = null;
        }

        if (mUSBMonitor != null)
            mUSBMonitor.unregister();

        try {
            if (mFileOutputStreamColor != null)
                mFileOutputStreamColor.close();
            if (mFileOutputStreamDepth != null)
                mFileOutputStreamDepth.close();
        } catch (IOException io) {
            Log.e(TAG, "IOException:" + io.toString());
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mUVCCamera != null) {
            mUVCCamera.destroy();
            mUVCCamera = null;
        }

        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }

        mUVCCameraViewR = null;
        mUVCCameraViewL = null;
        super.onDestroy();
    }


    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View view) {
            switch (view.getId()) {
                case R.id.camera_view_L:
                    final int n = mUSBMonitor.getDeviceCount();
                    if (n > 1)
                        CameraDialog.showDialog(CameraMainActivity.this, mUSBMonitor);
                    break;
                case R.id.camera_view_R:
                    break;
            }
        }
    };

    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {

        @Override
        public void onAttach(final UsbDevice device) {

            final int n = mUSBMonitor.getDeviceCount();
            UsbDevice usbDevice = null;
            if (DEBUG) Log.v(TAG, ">>>> onAttach getDeviceCount:" + n);
            if (n == 1) {
                usbDevice = mUSBMonitor.getDeviceList().get(0);
            } else if (n > 1) {
                for (UsbDevice deviceInfo : mUSBMonitor.getDeviceList()) {
                    if (deviceInfo.getProductId() == 0x0112) {
                        usbDevice = deviceInfo;
                        break;
                    }
                }
            }

            if (usbDevice == null) {
                CameraDialog.showDialog(CameraMainActivity.this, mUSBMonitor);
            } else {
                mUSBMonitor.requestPermission(usbDevice);
            }
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            if (mUVCCamera != null) return;
            if (DEBUG) Log.v(TAG, ">>>> onConnect UsbDevice:" + device);
            if (DEBUG) Log.v(TAG, ">>>> onConnect UsbControlBlock:" + ctrlBlock);
            if (DEBUG) Log.v(TAG, ">>>> onConnect createNew:" + createNew);
            prepareFrameBufferColorFile();
            prepareFrameBufferDepthFile();
            mUVCCamera = new UVCCamera();
            EXECUTER.execute(new Runnable() {
                @Override
                public void run() {
                    mUVCCamera.open(ctrlBlock);

                    mProductVersion = mUVCCamera.getProductVersion();

                    getZDTableValue();
                    setMeasureCallback();
                    mUVCCamera.setCameraVideoMode(UVCCamera.CameraMode.Rectify_Color_8Bit);

                    checkIRValue();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mIRSeekBar.setMax(mIRValueMax);
                            mIRSeekBar.setProgress(mIRValueCurrent);
                            setIRIcon();
                        }
                    });

                    mSupportedSize = mUVCCamera.getSupportedSize();

                    if (DEBUG) Log.i(TAG, "mSupportedSize" + mSupportedSize);
                    if (mSupportedSize != null && !mSupportedSize.isEmpty()) {
                        updatePreviewSizeSetting();

                    }
                    try {
                        if (DEBUG) Log.d(TAG, "set color uvccamera");
                        //mUVCCamera.setPreviewSize(Color_W, Color_H, UVCCamera.FRAME_FORMAT_MJPEG, UVCCamera.CAMERA_COLOR);
                        mUVCCamera.setPreviewSize(Color_W, Color_H, 1, 30, UVCCamera.FRAME_FORMAT_MJPEG, 0, UVCCamera.CAMERA_COLOR);
                        if (mLeftPreviewSurface != null) {
                            mLeftPreviewSurface.release();
                            mLeftPreviewSurface = null;
                        }
                        final SurfaceTexture surfaceTexture_l = mUVCCameraViewL.getSurfaceTexture();
                        if (surfaceTexture_l != null)
                            mLeftPreviewSurface = new Surface(surfaceTexture_l);
                        mUVCCamera.setPreviewDisplay(mLeftPreviewSurface, UVCCamera.CAMERA_COLOR);
                        //mUVCCamera.startCapture(mLeftPreviewSurface, UVCCamera.CAMERA_COLOR);
                        mUVCCamera.startPreview(UVCCamera.CAMERA_COLOR);

                    } catch (Exception e) {
                        Log.e(TAG, "set color uvccamera exception:" + e.toString());
                        return;
                    }
                    if (mUVCCamera != null && !mOnlyColor) {
                        try {
                            if (DEBUG) Log.d(TAG, "set depth uvccamera");
                            //mUVCCamera.setPreviewSize(Depth_W, Depth_H, UVCCamera.FRAME_FORMAT_YUYV, UVCCamera.CAMERA_DEPTH);
                            mUVCCamera.setPreviewSize(Depth_W, Depth_H, 1, 30, UVCCamera.FRAME_FORMAT_YUYV, 0, UVCCamera.CAMERA_DEPTH);
                            if (mRightPreviewSurface != null) {
                                mRightPreviewSurface.release();
                                mRightPreviewSurface = null;
                            }
                            final SurfaceTexture surfaceTexture_r = mUVCCameraViewR.getSurfaceTexture();
                            if (surfaceTexture_r != null)
                                mRightPreviewSurface = new Surface(surfaceTexture_r);
                            mUVCCamera.setPreviewDisplay(mRightPreviewSurface, UVCCamera.CAMERA_DEPTH);
                            //mUVCCamera.startCapture(mRightPreviewSurface, UVCCamera.CAMERA_DEPTH);
                            mUVCCamera.startPreview(UVCCamera.CAMERA_DEPTH);
                        } catch (Exception e) {
                            Log.e(TAG, "set depth uvccamera exception:" + e.toString());
                            return;
                        }
                    } else {
                        if (DEBUG) Log.e(TAG, "color only");
                    }
                   // mUVCCamera.setFrameCallback(mDepthCubeIFrameCallback, UVCCamera.PIXEL_FORMAT_RAW, UVCCamera.CAMERA_DEPTH);
                }
            });

        }

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            if (DEBUG) Log.v(TAG, "onDisconnect");

            mShowMeasure = false;
            setMeasure();
            mProductVersion = "";
            mIRValueMax = 0;

            if (mUVCCamera != null && device.equals(mUVCCamera.getDevice())) {
                if (mUVCCamera != null) {
                    mUVCCamera.stopPreview(UVCCamera.CAMERA_COLOR);
                    mUVCCamera.stopPreview(UVCCamera.CAMERA_DEPTH);
                }
                if (mLeftPreviewSurface != null) {
                    mLeftPreviewSurface.release();
                    mLeftPreviewSurface = null;
                }
                if (mRightPreviewSurface != null) {
                    mRightPreviewSurface.release();
                    mRightPreviewSurface = null;
                }
                mUVCCamera.close();
                mUVCCamera.destroy();
                mUVCCamera = null;
            }
        }

        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(CameraMainActivity.this, R.string.usb_device_detached, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel() {
            if (DEBUG) Log.v(TAG, "onCancel:");
        }
    };

    /*
     * ByteBuffer frame is YUYV
     * */
    private final IFrameCallback mColorIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(ByteBuffer frame) {
            //if (DEBUG) Log.v(TAG, "onFrame color callback frame:" + frame);
            showColorFrameRate(frame);

            int capacity = frame.capacity();
            int limit = frame.limit();

            byte[] buffer = new byte[capacity];
            for (int offset = 0; offset < limit; offset++) {
                buffer[offset] = frame.get(offset);
            }

            if (mColorCount < 10 && mFileOutputStreamColor != null) {
                try {
                    Log.i(TAG, "saveFrameBufferColor...");
                    mFileOutputStreamColor.write(buffer);
                    mFileOutputStreamColor.flush();
                } catch (Exception e) {
                    Log.e(TAG, "saveFrameBufferColor Exception:" + e.toString());
                }
                mColorCount++;
            }
        }
    };

    /*
     * ByteBuffer frame is YUYV
     * */
    private final IFrameCallback mDepthIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(ByteBuffer frame) {
            //if (DEBUG) Log.v(TAG, "onFrame depth callback frame:" + frame);
            int capacity = frame.capacity();
            int limit = frame.limit();

            byte[] buffer = new byte[capacity];
            for (int offset = 0; offset < limit; offset++) {
                buffer[offset] = frame.get(offset);
            }

            if (mDepthCount < 10 && mFileOutputStreamDepth != null) {
                try {
                    Log.i(TAG, "saveFrameBufferDepth...");
                    mFileOutputStreamDepth.write(buffer);
                    mFileOutputStreamDepth.flush();
                } catch (Exception e) {
                    Log.e(TAG, "saveFrameBufferDepth Exception:" + e.toString());
                }
                mDepthCount++;
            }
            setCameraSensorStatus();
        }
    };

    private void prepareFrameBufferColorFile() {
        try {
            File file = new File(mInternal_sd_path, "merged_color.yuv");
            if (file.exists())
                file.delete();
            file.setReadable(true, false);
            file.setWritable(true, false);
            mColorCount = 0;
            mFileOutputStreamColor = new FileOutputStream(file);
        } catch (Exception e) {
            Log.e(TAG, "prepareFrameBufferColorFile Exception:" + e.toString());
        }
    }

    private void prepareFrameBufferDepthFile() {
        try {
            File file = new File(Environment.getExternalStorageDirectory(), "merged_depth.yuv");
            if (file.exists())
                file.delete();
            file.setReadable(true, false);
            file.setWritable(true, false);
            mDepthCount = 0;
            mFileOutputStreamDepth = new FileOutputStream(file);
        } catch (Exception e) {
            Log.e(TAG, "prepareFrameBufferDepthFile Exception:" + e.toString());
        }
    }

    private void updatePreviewSizeSetting() {
        List<Size> supportedSizeListColor = UVCCamera.getSupportedSizeList(mSupportedSize, -1, 1);
        List<Size> supportedSizeListDepth = UVCCamera.getSupportedSizeList(mSupportedSize, -1, 2);
        // Check if the size is available
        // if not, set the first supported size as default
        if (isSizeSupported(Color_W, Color_H, supportedSizeListColor) == false) {
            if (!supportedSizeListColor.isEmpty()) {
                Color_W = supportedSizeListColor.get(0).width;
                Color_H = supportedSizeListColor.get(0).height;
            }
        }
        if (isSizeSupported(Depth_W, Depth_H, supportedSizeListDepth) == false) {
            if (!supportedSizeListDepth.isEmpty()) {
                Depth_W = supportedSizeListDepth.get(0).width;
                Depth_H = supportedSizeListDepth.get(0).height;
            }
        }
        //Set preference
        AppSettings appSettings = AppSettings.getInstance(mContext);
        appSettings.put(AppSettings.SUPPORTED_SIZE, mSupportedSize);
        appSettings.put(AppSettings.COLOR_W, Color_W);
        appSettings.put(AppSettings.COLOR_H, Color_H);
        appSettings.put(AppSettings.DEPTH_W, Depth_W);
        appSettings.put(AppSettings.Depth_H, Depth_H);
        appSettings.saveAll();
    }

    private boolean isSizeSupported(int width, int height, List<Size> supportedSizeList) {
        for (int i = 0; i < supportedSizeList.size(); i++) {
            Size sz = supportedSizeList.get(i);
            if (width == sz.width && height == sz.height) {
                if (DEBUG)
                    Log.i(TAG, String.format("isSizeSupported found: %d (%d,%d) interfaceNumber:%d", i, sz.width, sz.height, sz.interfaceNumber));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenu = menu;
        mOptionsMenu.add(0, MENU_SETTINGS, 1, R.string.btn_settings)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        updateOptionsMenu();
        return true;
    }

    private void updateOptionsMenu() {
        if (mOptionsMenu == null) return;
        mOptionsMenu.findItem(MENU_SETTINGS).setVisible(true);
    }

    private final OnClickListener mSettingsListener = new OnClickListener() {
        public void onClick(View v) {
            final Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setClass(mContext, SettingsMainActivity.class);
            mContext.startActivity(intent);
        }
    };

    private final OnClickListener mCubeListener = new OnClickListener() {
        public void onClick(View v) {
            mShowCube = !mShowCube;
            if ( mShowCube == true) cube_volume_label = !cube_volume_label;
            setCube();
           // Button = true ;
        }
    };

    private final OnClickListener mMeasureListener = new OnClickListener() {
        public void onClick(View v) {

//            if (mUVCCamera != null && mSwitchTwoWindow) {
            mShowMeasure = !mShowMeasure;
            setMeasure();
//            }
        }
    };


    public void checkViewOrientation() {

        AppSettings appSettings = AppSettings.getInstance(mContext);

        mReverse = appSettings.get(AppSettings.REVERSE, mReverse);
        mLandscape = appSettings.get(AppSettings.LANDSCAPE, mLandscape);
        mFlip = appSettings.get(AppSettings.FLIP, mFlip);

        if (mLandscape) {
            if (mReverse) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        } else {
            if (mReverse) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }

        if (mFlip) {
            mUVCCameraViewL.setScaleX(-1);
            mUVCCameraViewR.setScaleX(-1);
        } else {
            mUVCCameraViewL.setScaleX(1);
            mUVCCameraViewR.setScaleX(1);
        }
    }

    private void initAll() {

        mUVCCameraViewL = (UVCCameraTextureView) findViewById(R.id.camera_view_L);
        mUVCCameraViewL.setOnClickListener(mOnClickListener);
        mUVCCameraViewR = (UVCCameraTextureView) findViewById(R.id.camera_view_R);
        mUVCCameraViewR.setOnClickListener(mOnClickListener);


        mTextViewIR = (TextView) findViewById(R.id.text_ir);
        mTextViewIR.setOnClickListener(mIROnClickListener);
        mTextViewIR.setOnLongClickListener(mIROnLongClickListener);
        mTextViewIR.setTextColor(Color.GRAY);
        mTextViewIR.setAlpha(0.7f);

        mIRSeekBar = (SeekBar) findViewById(R.id.seekbar_IR_value);
        mIRSeekBar.setOnSeekBarChangeListener(mIRValueListener);
        mIRSeekBar.setVisibility(View.GONE);

        mText_IRValue = (TextView) findViewById(R.id.textview_IR_value);
        mText_IRValue.setVisibility(View.GONE);

        mImageViewOne = (ImageView) findViewById(R.id.image_one);
        mImageViewOne.setVisibility(View.GONE);

        mImageViewTwo = (ImageView) findViewById(R.id.image_two);
        mImageViewTwo.setVisibility(View.GONE);

        mImageViewThree = (ImageView) findViewById(R.id.image_three);
        mImageViewThree.setImageResource(R.drawable.cancel);
        mImageViewThree.setVisibility(View.GONE);

        mImageViewFour = (ImageView) findViewById(R.id.image_four);
        mImageViewFour.setImageResource(R.drawable.measure);
        mImageViewFour.setVisibility(View.GONE);
        mImageViewFour.setOnClickListener(mMeasureListener);

        mImageViewFive = (ImageView) findViewById(R.id.image_five);
        mImageViewFive.setOnClickListener(mSettingsListener);

        mImageViewSix = (ImageView) findViewById(R.id.image_six);
        mImageViewSix.setOnClickListener(mCubeListener);

        mMainLayoutTop = (LinearLayout) findViewById(R.id.main_layout_top);
        mRelativeLayout_L = (RelativeLayout) findViewById(R.id.camera_layout_L);
        mRelativeLayout_R = (RelativeLayout) findViewById(R.id.camera_layout_R);

        mImageViewMeasureSpot1 = (ImageView) findViewById(R.id.measure_spot_1);
        mImageViewMeasureSpot1.setVisibility(View.INVISIBLE);
        mImageViewMeasureSpot2 = (ImageView) findViewById(R.id.measure_spot_2);
        mImageViewMeasureSpot2.setVisibility(View.INVISIBLE);
        if (mMoveMeasureSpot) {
            mImageViewMeasureSpot1.setOnTouchListener(mMeasureSpotTouchListener);
            mImageViewMeasureSpot2.setOnTouchListener(mMeasureSpotTouchListener);
        }

        mTextViewMeasure = (TextView) findViewById(R.id.tvMeasure);
        mTextViewMeasure.setVisibility(View.INVISIBLE);

        mTextViewCube = (TextView) findViewById(R.id.tvCube);
        mTextViewCube.setVisibility(View.INVISIBLE);



    }

    public void setCameraSensorStatus() {

        AppSettings appSettings = AppSettings.getInstance(mContext);
        if (mCameraSensorChange) {
            mCameraSensorChange = false;
            appSettings.put(AppSettings.CAMERA_SENSOR_CHANGE, mCameraSensorChange);
            appSettings.saveAll();
            if (mUVCCamera != null) {
                if (mAEStatus) {
                    int ret_enableAE = mUVCCamera.setEnableAE();
                    Log.i(TAG, "uvc camera enable AE ret:" + ret_enableAE);
                } else {
                    int ret_diableAE = mUVCCamera.setDisableAE();
                    Log.i(TAG, "uvc camera diable AE:" + ret_diableAE);
                }
                if (mAWBStatus) {
                    int ret_enableAWB = mUVCCamera.setEnableAWB();
                    Log.i(TAG, "uvc camera enable AWB ret:" + ret_enableAWB);
                } else {
                    int ret_diableAWB = mUVCCamera.setDisableAWB();
                    Log.i(TAG, "uvc camera diable AWB ret:" + ret_diableAWB);
                }
            } else {
                Log.i(TAG, "uvc camera do not connect");
                Toast.makeText(CameraMainActivity.this, "uvc camera do not connect", Toast.LENGTH_SHORT).show();
            }
        } else {
            return;
        }
    }

    public void showColorFrameRate(final ByteBuffer frame) {
        runOnUiThread(new Runnable() {
            public void run() {

            }
        });
    }

    private final SeekBar.OnSeekBarChangeListener mIRValueListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int progress = seekBar.getProgress();
            Log.i(TAG, "seekbarprogress:" + progress);

            mIRValueCurrent = progress;
            setIRValue(mIRValueCurrent);
            setIRIcon();
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            mText_IRValue.setText(progress + "/" + seekBar.getMax());
        }
    };

    private final OnClickListener mIROnClickListener = new OnClickListener() {
        public void onClick(View v) {

            if (mUVCCamera != null && mHasIR) {

                mIRSwitch = !mIRSwitch;
                if (mIRSwitch) {
                    if (mIRValueCurrent == 0) {
                        mIRValueCurrent = mIRValueDefault;
                    }
                    setIRValue(mIRValueCurrent);
                } else {
                    setIRValue(0);
                    if (mIRSeekBar.isShown()) {
                        mIRSeekBar.setVisibility(View.GONE);
                        mText_IRValue.setVisibility(View.GONE);
                    }
                }

                setIRIcon();
            }

        }
    };

    private final View.OnLongClickListener mIROnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {

            if (mUVCCamera != null && mHasIR && mIRSwitch) {

                if (mIRSeekBar.isShown()) {
                    mIRSeekBar.setVisibility(View.GONE);
                    mText_IRValue.setVisibility(View.GONE);
                    mText_IRValue.setOnClickListener(null);
                } else {
                    mIRSeekBar.setVisibility(View.VISIBLE);
                    mIRSeekBar.setProgress(mIRValueCurrent);
                    mText_IRValue.setText(mIRValueCurrent + "/" + mIRSeekBar.getMax());
                    mText_IRValue.setVisibility(View.VISIBLE);
                    mText_IRValue.setOnClickListener(mIRValueOnClickListener);
                }
            }

            return true;
        }
    };

    private final OnClickListener mIRValueOnClickListener = new OnClickListener() {
        public void onClick(View v) {

            if (mUVCCamera != null && mHasIR && mIRSwitch) {

                int IRMaxValue = getIRMaxValue();
                showSetIRMaxValueDialog(IRMaxValue);
            }

        }
    };

    private void showSetIRMaxValueDialog(final int max) {

        final LayoutInflater mLayoutInflater = LayoutInflater.from(this);
        final View mView = mLayoutInflater.inflate(R.layout.dialog, null);
        final EditText mEditTextMaxValue = (EditText) mView.findViewById(R.id.editText_intent_event);
        mEditTextMaxValue.setInputType(InputType.TYPE_CLASS_NUMBER);
        AlertDialog.Builder alert_builder = new AlertDialog.Builder(this);
        alert_builder.setView(mView);

        mEditTextMaxValue.setText(String.valueOf(mIRValueMax));

        alert_builder.setTitle("Change IR Max");
        alert_builder.setPositiveButton(getString(R.string.dlg_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                final String valueStr = mEditTextMaxValue.getText().toString();
                if (!TextUtils.isEmpty(valueStr)) {

                    mIRValueMax = Integer.valueOf(valueStr);
                    if (mIRValueMax > max) {
                        mIRValueMax = max;
                        Toast.makeText(CameraMainActivity.this, "Value is large than device limit, the max value will change to " + max, Toast.LENGTH_SHORT);
                    }
                    mEditTextMaxValue.setText(String.valueOf(mIRValueMax));

                    if (mIRValueCurrent > mIRValueMax) {
                        mIRValueCurrent = mIRValueMax;
                        setIRValue(mIRValueCurrent);
                    }

                    mIRSeekBar.setMax(mIRValueMax);
                    mIRSeekBar.setProgress(mIRValueCurrent);
                }
                dialog.dismiss();
            }
        })
                .setNegativeButton(getString(R.string.dlg_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        AlertDialog alertDialog = alert_builder.create();
        alertDialog.show();
    }

    private void setIRValue(int value) {
        if (mUVCCamera != null) {
            mUVCCamera.setIRCurrentValue(value);
        }
    }

    private void setCube() {

        if (mShowCube) {
            mTextViewCube.setVisibility(View.VISIBLE);
            setCubeCallback();
            // mTextViewCube.setText(Depth_W+"");
        } else {
            mTextViewCube.setVisibility(View.INVISIBLE);
        }
        //mTextViewCube.setText("display");



    }

    private void setMeasure() {

        if (mShowMeasure) {
            mImageViewMeasureSpot1.setVisibility(View.VISIBLE);
            mImageViewMeasureSpot2.setVisibility(View.VISIBLE);
            mTextViewMeasure.setVisibility(View.VISIBLE);
        } else {
            mImageViewMeasureSpot1.setVisibility(View.INVISIBLE);
            mImageViewMeasureSpot2.setVisibility(View.VISIBLE);
            mTextViewMeasure.setVisibility(View.INVISIBLE);
        }

        setMeasureCallback();
    }

    private void setCubeCallback() {

        if (mUVCCamera != null) {
            if (mShowCube) {
                mTextViewCube.setText("cube volume measuring...");
                cube_volume_label = true;
                mUVCCamera.setFrameCallback(mDepthCubeIFrameCallback, UVCCamera.PIXEL_FORMAT_RAW, UVCCamera.CAMERA_DEPTH);
                //mUVCCamera.setFrameCallback(mDepthCubeIFrameCallbackCAO, UVCCamera.PIXEL_FORMAT_RAW, UVCCamera.CAMERA_DEPTH);
            } else {
                //mTextViewCube.setText(""+0);
                cube_volume_label = false;
                mUVCCamera.setFrameCallback(null, UVCCamera.PIXEL_FORMAT_RAW, UVCCamera.CAMERA_DEPTH);
            }
        } else {
            mTextViewCube.setText("无深度相机数据");
        }
    }

    private void setMeasureCallback() {

        if (mUVCCamera != null) {
            if (mShowMeasure) {
                //mTextViewMeasure.setText("waiting1...");
                mUVCCamera.setFrameCallback(mDepthMeasureIFrameCallback, UVCCamera.PIXEL_FORMAT_RAW, UVCCamera.CAMERA_DEPTH);
            } else {
                mUVCCamera.setFrameCallback(null, UVCCamera.PIXEL_FORMAT_RAW, UVCCamera.CAMERA_DEPTH);
            }
        }
    }

    private int getMeasureSpotIndex(View measureSpot) {
        return getRealY(measureSpot) * Depth_W * 2 + getRealX(measureSpot);
    }

    private int getRealX(View measureSpot) {
        int centerX = (measureSpot.getLeft() + measureSpot.getRight()) / 2;
        return centerX * Depth_W * 2 / ((View) measureSpot.getParent()).getWidth();
    }

    private int getRealY(View measureSpot) {
        int centerY = (measureSpot.getTop() + measureSpot.getBottom()) / 2;
        return centerY * Depth_H / ((View) measureSpot.getParent()).getHeight();
    }

    private double getDistance() {
        double h = Math.abs(getRealX(mImageViewMeasureSpot1) - getRealX(mImageViewMeasureSpot2));
        double B = h / 1280 * 67;
        double Z = zCenterValue;
        double X = Math.tan(Math.toRadians(B / 2)) * Z * 2;

        return Math.round(X);
    }

    private float mLastX, mLastY;
    private final View.OnTouchListener mMeasureSpotTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            int x = (int) event.getX();
            int y = (int) event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mLastX = x;
                    mLastY = y;

                    break;

                case MotionEvent.ACTION_MOVE:
                    int offX = (int) (x - mLastX);
                    int offY = (int) (y - mLastY);

                    int left = v.getLeft() + offX;
                    int right = v.getRight() + offX;
                    int top = v.getTop() + offY;
                    int bottom = v.getBottom() + offY;

                    int centerX = (left + right) / 2;
                    int centerY = (top + bottom) / 2;

                    int parentMaxX = ((View) v.getParent()).getWidth();
                    int parentMaxY = ((View) v.getParent()).getHeight();

                    if (centerX >= 0 && centerX <= parentMaxX && centerY >= 0 && centerY <= parentMaxY) {
                        v.layout(v.getLeft() + offX, v.getTop() + offY, v.getRight() + offX, v.getBottom() + offY);
                    }

                    break;
            }

            getMeasureSpotIndex(v);

            return true;
        }
    };

    private int zCenterValue;

    /*
     * ByteBuffer frame is YUYV
     * */

    /*

    void saveBmp(Bitmap bmp){
    File dir ;
    File file ;
    String subdir = new String();
    FileOutputStream outStream = null ;
    subdir = "/aaa/bRgb" ;
    dir = new File(Environment.getExternalStorageDirectory() + subdir) ;
    if (!dir.exists()){
        dir.mkdirs() ;
    }

    file = new File(Environment.getExternalStorageDirectory() + subdir, timeSigned()) ;
    try{
        outStream = new FileOutputStream(file) ;
        bmp.compress(Bitmap.CompressFormat.JPEG,100,outStream);
        outStream.close() ;
    }catch (IOException e){
        e.printStackTrace();
    }
     */

    private final IFrameCallback mDepthCubeIFrameCallbackCAO = new IFrameCallback() {
        @Override
        public void onFrame(ByteBuffer byteBuffer) {

            final ByteBuffer frame = byteBuffer ;

            if(Button){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        saveYUV(frame) ;
                    }
                }).start();
                Button = false ;
            }

        }
    };


    private static int frame_num = 0;
    double[][] volume_relative = new double[5][4];
   // private String string_x;
    private String string_view;

    private final IFrameCallback mDepthCubeIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(ByteBuffer byteBuffer) {

           // double[][] volume_relative = new double[5][4];

            //mTextViewCube.setText("next...");
            if(cube_volume_label == true)
            {
                //mTextViewCube.setText("true");

                int[] pixels = new int[Depth_H * Depth_W * 2];
                for (int index_1 = 0; index_1 < Depth_H * Depth_W * 2; index_1++) {
                    pixels[index_1] = getZValue(byteBuffer.get(index_1));
                }

                byte[] yuyv_frame = new byte[Depth_H * Depth_W * 2];
                for (int index_2 = 0; index_2 < Depth_H * Depth_W * 2; index_2++) {
                    yuyv_frame[index_2] = byteBuffer.get(index_2);
                }

                String string_x = cube_volume(yuyv_frame,Depth_H * Depth_W,pixels,Depth_W,Depth_H);
                //String string_x = "3,4,5,6";
                String[] str_s = string_x.split(",");
                volume_relative[frame_num][0] = Double.valueOf(str_s[0]).doubleValue();
                volume_relative[frame_num][1] = Double.valueOf(str_s[1]).doubleValue();
                volume_relative[frame_num][2] = Double.valueOf(str_s[2]).doubleValue();
                volume_relative[frame_num][3] = Double.valueOf(str_s[3]).doubleValue();

                // mTextViewCube.setText(Integer.toString(frame_num));
                // mTextViewCube.setText(cube_volume(yuyv_frame,Depth_H * Depth_W,pixels,Depth_W,Depth_H));

                frame_num = frame_num + 1;
                if(frame_num > 4)
                {

                    int []mean_4 = {0,0,0,0,0};
                    for(int i = 0 ;i<4;i++)
                    {
                        mean_4[i] = (int)((volume_relative[0][i] + volume_relative[1][i] + volume_relative[2][i] +
                                    volume_relative[3][i] + volume_relative[4][i])/5*10);
                    }
                    double var = 0;
                    for(int i = 0; i<5; i++)
                    {
                        var = var + (volume_relative[i][0] - mean_4[0]) * (volume_relative[i][0] - mean_4[0]);
                    }
                    var = var / 5;
                    if(var > 3)
                    {
                        frame_num = 0;cube_volume_label = true;
                    }
                    else
                    {
                        string_view = "length: "+Integer.toString(mean_4[0]/10)+'.'+ Integer.toString(mean_4[0]%10)+
                                "cm width: "+Integer.toString(mean_4[1]/10)+'.'+ Integer.toString(mean_4[1]%10)+
                                "cm height: "+Integer.toString(mean_4[2]/10)+'.'+ Integer.toString(mean_4[2]%10)+
                                "cm volume: "+Integer.toString(mean_4[3]/10)+'.'+ Integer.toString(mean_4[3]%10);

                        mTextViewCube.setText(string_view);

                        frame_num = 0;
                        cube_volume_label = false;
                    }

                    //mTextViewCube.setText("false");
                    //mTextViewCube.setText(cube_volume(yuyv_frame,Depth_H * Depth_W,pixels,Depth_W,Depth_H));
                    //mTextViewCube.setText(Integer.toString(frame_num));
                }
            }
            else
            {
                //mTextViewCube.setText("false");
                mTextViewCube.setText(string_view);
            }


            /*
            try {
                File dir0 ;
                File file ;
                String subdir = new String();
                FileOutputStream outStream = null ;
                subdir = "/aaa/bRgb" ;
                dir0 = new File(Environment.getExternalStorageDirectory() + subdir) ;
                if (!dir0.exists()){
                dir0.mkdirs() ;
                }

                //file = new File(Environment.getExternalStorageDirectory() + subdir, timeSigned()) ;
                file = new File(dir0 + "depth.txt");
                if (!file.exists()) {
                file.createNewFile();
                }

                FileWriter out = new FileWriter(file);  //文件写入流

                //将数组中的数据写入到文件中。每行各数据之间TAB间隔
                int num = 0;
                for(int i=0;i<Depth_H;i++){
                    for(int j=0;j<Depth_W * 2;j++){
                        out.write(pixels[num]+" ");
                        num++;
                    }
                    out.write("\r\n");
                }
                    out.close();

                }catch (Exception e1) {
                    mTextViewCube.setText(e1.getMessage());
                }


            try {

                File dir ;
                File file1 ;
                String subdir = new String();
                FileOutputStream outStream = null ;
                subdir = "/aaa/bRgb" ;
                dir = new File(Environment.getExternalStorageDirectory() + subdir) ;
                if (!dir.exists()){
                dir.mkdirs() ;
                }

                //file = new File(Environment.getExternalStorageDirectory() + subdir, timeSigned()) ;
                file1 = new File(dir + "frame.txt");
                if (!file1.exists()) {
                file1.createNewFile();
                }

                FileWriter out1 = new FileWriter(file1);  //文件写入流

                //将数组中的数据写入到文件中。每行各数据之间TAB间隔
                int num = 0;
                for(int i=0;i<Depth_H;i++){
                    for(int j=0;j<Depth_W * 2;j++){
                        out1.write(yuyv_frame[num]+" ");
                        num++;
                    }
                    out1.write("\r\n");
                }
                out1.close();

            }catch (Exception e2) {
                mTextViewCube.setText(e2.getMessage());
            }
            */
        }
    };


    private final IFrameCallback mDepthMeasureIFrameCallback = new IFrameCallback() {

        @Override
        public void onFrame(ByteBuffer frame) {
            //mTextViewMeasure.setText("waiting2...");

            final int index1 = getMeasureSpotIndex(mImageViewMeasureSpot1);
            final int index2 = getMeasureSpotIndex(mImageViewMeasureSpot2);
            final int zValue1 = getZValue(frame.get(index1));
            final int zValue2 = getZValue(frame.get(index2));
            final int zCenterValue = (zValue1 + zValue2) / 2;
            CameraMainActivity.this.zCenterValue = zCenterValue;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // mTextViewMeasure.setText("waiting3...");
                    getDistance();
                    StringBuilder builder = new StringBuilder();
                    builder.append("Depth:" + zCenterValue / 10 + "cm   Length:" + getDistance() / 10 + "cm");
                    mTextViewMeasure.setText(builder.toString());
                }
            });

        }
    };

    private int getZValue(byte valueByte) {

        int zValue = 0;
        if (mZDBuffer != null) {
            int dValue = valueByte & 0xff;

            if (mProductVersion.equals(PRODUCT_VERSION_EX8037) ||
                    mProductVersion.equals(PRODUCT_VERSION_EX8036)) {
                dValue *= 8;
            }
            zValue = mZDBuffer[dValue];
        }

        return zValue;
    }

    ;

    private void getZDTableValue() {

        if (mProductVersion.equals(PRODUCT_VERSION_EX8037) ||
                mProductVersion.equals(PRODUCT_VERSION_EX8036)) {
            if (Depth_H >= 720) {
                mZDBuffer = mUVCCamera.getZDTableValue(0);
            } else if (Depth_H >= 480) {
                mZDBuffer = mUVCCamera.getZDTableValue(1);
            }
        } else if (mProductVersion.equals(PRODUCT_VERSION_EX8029)) {
            if (Depth_H >= 480) {
                mZDBuffer = mUVCCamera.getZDTableValue(0);
            } else if (Depth_H >= 240) {
                mZDBuffer = mUVCCamera.getZDTableValue(1);
            }
        } else {
            mZDBuffer = mUVCCamera.getZDTableValue();
        }
    }

    ;
    String timeSigned(){

        Calendar CD = Calendar.getInstance() ;
        int YY = CD.get(Calendar.YEAR) ;
        int MM = CD.get(Calendar.MONTH) + 1 ;
        int DD = CD.get(Calendar.DATE) ;
        int HH = CD.get(Calendar.HOUR_OF_DAY) ;
        int NN = CD.get(Calendar.MINUTE) ;
        int SS = CD.get(Calendar.SECOND) ;
        int MI = CD.get(Calendar.MILLISECOND) ;

        String ret = "" + YY
                + String.format("%02d", MM)
                + String.format("%02d", DD)
                + String.format("%02d", HH)
                + String.format("%02d", NN)
                + String.format("%02d", SS)
                + String.format("%04d", MI);
        return ret ;
    }


     void saveYUV( ByteBuffer frame){
         File dir ;
         File file = null;
         String subdir = new String();
         FileOutputStream outStream = null ;
         subdir = "/aaa/bYU" ;

         int capacity = frame.capacity();
         int limit = frame.limit();

         byte[] buffer = new byte[capacity];
         for (int offset = 0; offset < limit; offset++) {
             buffer[offset] = frame.get(offset);
         }
         dir = new File(Environment.getExternalStorageDirectory() + subdir) ;
         if (!dir.exists()){
             dir.mkdirs() ;
         }

         try{
             file = new File(dir.getCanonicalPath() + "/yuyv" + timeSigned() +".yuv") ;
         }catch (Exception e){
             e.getMessage() ;
         }


         try{
             outStream = new FileOutputStream(file) ;
             outStream.write(buffer);
             outStream.flush();
             outStream.close() ;
         }catch (IOException e){
             e.printStackTrace();
         }
     }
}


