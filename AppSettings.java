package com.esp.uvc.usbcamera;

import android.content.Context;

/**
 * Created by GrayJao on 2017/11/2.
 */

public class AppSettings extends SharedPrefManager {

    private static volatile AppSettings ourInstance;
    private static final String SETTINGS_PRF = "settings_prf";

    /**
     * Keys
     */
    public final static String WINDOW_TYPE = "mWindowType";
    public final static String ONLY_COLOR = "mOnlyColor";
    public final static String REVERSE = "mReverse";
    public final static String LANDSCAPE = "mLandscape";
    public final static String FLIP = "mFlip";

    public final static String ONLY_COLOR_FRAME_RATE = "mOnlyColorFrameRate";
    public final static String ETRON_COLOR_FRAME_RETE = "mEtronColorFrameRate";
    public final static String ETRON_DEPTH_FRAME_RETE = "mEtronDepthFrameRate";

    public final static String COLOR_W = "Color_W";
    public final static String COLOR_H = "Color_H";
    public final static String DEPTH_W = "Depth_W";
    public final static String Depth_H = "Depth_H";

    public final static String ONLY_COLOR_POSITION = "mOnlyColorPosition";
    public final static String ETRON_POSITION = "mEtronPosition";
    public final static String DEPTH_POSITION = "mDepthPosition";

    public final static String FISH_POSITION = "mFishPosition";
    public final static String FISH_RECORD_FRAME = "mFishRecordFrame";

    public final static String SUPPORTED_SIZE = "mSupportedSize";

    public final static String CURRENT = "mCurrent";
    public final static String AWB_STATUS = "mAWBStatus";
    public final static String AE_STATUS = "mAEStatus";
    public final static String CAMERA_SENSOR_CHANGE = "mCameraSensorChange";

    /**
     * Default values
     */
    private final static int WINDOWS_TYPE_DEFAULT = 1;
    private final static boolean ONLY_COLOR_DEFAULT = false;
    private final static boolean REVERSE_DEFAULT = true;
    private final static boolean LANDSCAPE_DEFAULT = false;
    private final static boolean FLIP_DEFAULT = true;

    private final static int Color_W_DEFAULT = 0;
    private final static int Color_H_DEFAULT = 0;
    private final static int Depth_W_DEFAULT = 0;
    private final static int Depth_H_DEFAULT = 0;

    private final static int ONLY_COLOR_FRAME_RETE_DEFAULT = 30;
    private final static int ETRON_COLOR_FRAME_RETE_DEFAULT = 30;
    private final static int ETRON_DEPTH_FRAME_RETE_DEFAULT = 30;

    private final static int ONLY_COLOR_POSITION_DEFAULT = 0;
    private final static int ETRON_POSITION_DEFAULT = 0;
    private final static int DEPTH_POSITION_DEFAULT = 0;

    private final static int FISH_POSITION_DEFAULT = 0;
    private final static int FISH_RECORD_FRAME_DEFAULT = 60;


    public static AppSettings getInstance(Context context) {

        if (ourInstance == null) {
            synchronized (AppSettings.class) {
                if (ourInstance == null)
                    ourInstance = new AppSettings(context);
            }
        }
        return ourInstance;
    }

    private AppSettings(Context context) {
        super(context, SETTINGS_PRF);
    }

    @Override
    void initDefaultValue() {

        if (!contains(WINDOW_TYPE)) {
            put(WINDOW_TYPE, WINDOWS_TYPE_DEFAULT);
        }
        if (!contains(ONLY_COLOR)) {
            put(ONLY_COLOR, ONLY_COLOR_DEFAULT);
        }
        if (!contains(REVERSE)) {
            put(REVERSE, REVERSE_DEFAULT);
        }
        if (!contains(LANDSCAPE)) {
            put(LANDSCAPE, LANDSCAPE_DEFAULT);
        }
        if (!contains(FLIP)) {
            put(FLIP, FLIP_DEFAULT);
        }
    }

}
