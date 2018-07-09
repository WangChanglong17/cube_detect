package com.esp.uvc.usbcamera;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.esp.android.usb.camera.core.Size;
import com.esp.android.usb.camera.core.USBMonitor;
import com.esp.android.usb.camera.core.UVCCamera;
import com.esp.uvc.R;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class PreviewImageSettings extends AppCompatActivity {
    private static final boolean DEBUG = true;
    private static final String TAG = "PreviewImageSettings";
    private static final String KEY_ONLY_COLOR_PREVIEW_CHECKED = "only_color_preview_checked";
    private static final String KEY_ONLY_COLOR_PREVIEW_FRAME_SIZE = "only_color_preview_frame_size";
    private static final String KEY_ONLY_COLOR_PREVIEW_FRAME_RATE = "only_color_preview_frame_rate";
    private static final String KEY_ETRON_PREVIEW_FRAME_SIZE = "etron_preview_frame_size";
    private static final String KEY_DEPTH_PREVIEW_FRAME_SIZE = "depth_preview_frame_size";
    private static final String KEY_ETRON_COLOR_PREVIEW_FRAME_RATE = "etron_color_preview_frame_rate";
    private static final String KEY_ETRON_DEPTH_PREVIEW_FRAME_RATE = "etron_depth_preview_frame_rate";
    private static final String KEY_SWTICH_VIEW_WINDOW = "swtich_view_window";
    private static final String KEY_REVERSE_VIEW_WINDOW_CHECKED = "reverse_view_window_checked";
    private static final String KEY_LANDSCAPE_VIEW_WINDOW_CHECKED = "landscape_view_window_checked";
    private static final String KEY_FLIP_VIEW_WINDOW_CHECKED = "flip_view_window_checked";
    private static final String KEY_ONLY_COLOR_SET_FISH_FRAME = "only_color_set_fish_frame";
    private static final String KEY_TMP_FIX_FISH_LINE = "tmp_fix_fish_line";

    @Override
    public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         getFragmentManager().beginTransaction()
             .replace(android.R.id.content, new SettingsFragment())
             .commit();

    }

    public static class SettingsFragment extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener {


        /*private CharSequence singleList_only_color[] = {
            "640*360",
            "640*480",
            "1280*480",
            "1280*720",
            "2560*720",
            "2560*960"
        };
        private CharSequence singleList_etron_preview[] = {
            "Color640*360_Depth320*360",
            "Color640*480_Depth320*480",
            "Color640*400_Depth320*400",
            "Color320*240_Depth160*240",
            "Color640*240_Depth160*240",
            "Color800*600_Depth320*480",
            "Color1280*720_Depth640*720",
            "Color1280*720_Depth320*480"
        };*/
        private final CharSequence singleList_fish_frame[] = {
                "50",
                "100",
                "150",
                "200",
                "250",
                "300"
        };
        private final CharSequence singleList_window[] = { "Show one window", "Show two windows"};

        private CharSequence mEntriesColor[];
        private CharSequence mEntriesDepth[];
        List<Size> mSupportedSizeListColor;
        List<Size> mSupportedSizeListDepth;
        private static final int CORE_POOL_SIZE = 1;
        private static final int MAX_POOL_SIZE = 4;
        private static final int KEEP_ALIVE_TIME = 10;
        private static ThreadPoolExecutor EXECUTER
                = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        private USBMonitor mUSBMonitor = null;
        private UVCCamera mUVCCamera = null;
        private UsbDevice mUsbDevice = null;
        private String mSupportedSize;
        private int mWindowType = 1;
        private boolean mOnlyColor = false;
        private boolean mReverse = true;
        private boolean mLandscape = false;
        private boolean mFlip = true;
        private int mOnlyColorPosition = 0;
        private int mEtronPosition = 0;
        private int mDepthPosition = 0;
        private int mOnlyColorFrameRate = 30;
        private int mEtronColorFrameRate = 30;
        private int mEtronDepthFrameRate = 30;
        private int Color_W = 0;
        private int Color_H = 0;
        private int Depth_W = 0;
        private int Depth_H = 0;
        private int mFishPosition = 0;
        private int mFishRecordFrame = 60;
        private int FishFixLine = 0;

        private CheckBoxPreference mCheckBoxPreference_OnlyColorPreviewChecked;
        private CheckBoxPreference mCheckBoxPreference_ReverseViewWindowChecked;
        private CheckBoxPreference mCheckBoxPreference_LandscapeViewWindowChecked;
        private CheckBoxPreference mCheckBoxPreference_FlipViewWindowChecked;
        private ListPreference mListPreference_OnlyColorPreviewFrameSize;
        private ListPreference mListPreference_EtronPreviewFrameSize;
        private ListPreference mListPreference_DepthPreviewFrameSize;
        private ListPreference mListPreference_SwtichViewWindow;
        private ListPreference mListPreference_SetFishFrame;
        private EditTextPreference mEditTextPreference_OnlyColorPreviewFrameRate;
        private EditTextPreference mEditTextPreference_EtronColorPreviewFrameRate;
        private EditTextPreference mEditTextPreference_EtronDepthPreviewFrameSizeRate;
        private EditTextPreference mEditTextTempFix;
        private Context mContext = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preview_image_settings);
            this.mContext = getActivity();
            mUSBMonitor = new USBMonitor(mContext, mOnDeviceConnectListener);
            if (mUSBMonitor == null) {
                Log.d(TAG, "Error!! can not get USBMonitor " );
                return;
            }
            initUI();
        }

        @Override
        public void onResume() {
            super.onResume();
            if (mUSBMonitor != null)
                mUSBMonitor.register();

            if (mUVCCamera != null) {
                mUVCCamera.destroy();
                mUVCCamera = null;
            }

            readData();
            updateResolutionSetting();
            updateUI();
        }

        @Override
        public void onPause() {
            if (mUVCCamera != null) {
                mUVCCamera.destroy();
                mUVCCamera = null;
            }

            if (mUSBMonitor != null)
                mUSBMonitor.unregister();

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

            super.onDestroy();
        }

        private void updateResolutionSetting(){
            mSupportedSizeListColor=UVCCamera.getSupportedSizeList(mSupportedSize,6,1);//Get MJPEG only since usb3 is not supported
            mSupportedSizeListDepth=UVCCamera.getSupportedSizeList(mSupportedSize,-1,2);
            mEntriesColor=listToEntries(mSupportedSizeListColor);
            mEntriesDepth=listToEntries(mSupportedSizeListDepth);
            mListPreference_OnlyColorPreviewFrameSize.setEntries(mEntriesColor);
            mListPreference_OnlyColorPreviewFrameSize.setEntryValues(mEntriesColor);
            mListPreference_EtronPreviewFrameSize.setEntries(mEntriesColor);
            mListPreference_EtronPreviewFrameSize.setEntryValues(mEntriesColor);
            mListPreference_DepthPreviewFrameSize.setEntries(mEntriesDepth);
            mListPreference_DepthPreviewFrameSize.setEntryValues(mEntriesDepth);

            if(mEntriesColor.length==0){
                mOnlyColorPosition=-1;
                mEtronPosition=-1;
            }
            else if(mEtronPosition >= mEntriesColor.length || mOnlyColorPosition >= mEntriesColor.length){
                mOnlyColorPosition=0;
                mEtronPosition=0;
            }

            if(mEntriesDepth.length==0){
                mDepthPosition=-1;
            }
            else if(mDepthPosition >= mEntriesDepth.length){
                mDepthPosition=0;
            }
        }

        private void initUI() {
            mCheckBoxPreference_OnlyColorPreviewChecked
                = (CheckBoxPreference) findPreference(KEY_ONLY_COLOR_PREVIEW_CHECKED);
            mCheckBoxPreference_ReverseViewWindowChecked
                = (CheckBoxPreference) findPreference(KEY_REVERSE_VIEW_WINDOW_CHECKED);
            mCheckBoxPreference_LandscapeViewWindowChecked
                = (CheckBoxPreference) findPreference(KEY_LANDSCAPE_VIEW_WINDOW_CHECKED);
            mCheckBoxPreference_FlipViewWindowChecked
                = (CheckBoxPreference) findPreference(KEY_FLIP_VIEW_WINDOW_CHECKED);
            mListPreference_OnlyColorPreviewFrameSize
                = (ListPreference) findPreference(KEY_ONLY_COLOR_PREVIEW_FRAME_SIZE);
            mListPreference_OnlyColorPreviewFrameSize.setOnPreferenceChangeListener(SettingsFragment.this);
            mListPreference_EtronPreviewFrameSize        = (ListPreference) findPreference(KEY_ETRON_PREVIEW_FRAME_SIZE);
            mListPreference_EtronPreviewFrameSize.setOnPreferenceChangeListener(SettingsFragment.this);
            mListPreference_DepthPreviewFrameSize        = (ListPreference) findPreference(KEY_DEPTH_PREVIEW_FRAME_SIZE);
            mListPreference_DepthPreviewFrameSize.setOnPreferenceChangeListener(SettingsFragment.this);
            mListPreference_SwtichViewWindow             = (ListPreference) findPreference(KEY_SWTICH_VIEW_WINDOW);
            mListPreference_SwtichViewWindow.setOnPreferenceChangeListener(SettingsFragment.this);
            mListPreference_SetFishFrame                 = (ListPreference) findPreference(KEY_ONLY_COLOR_SET_FISH_FRAME);
            mListPreference_SetFishFrame.setOnPreferenceChangeListener(SettingsFragment.this);
            mEditTextPreference_OnlyColorPreviewFrameRate= (EditTextPreference) findPreference(KEY_ONLY_COLOR_PREVIEW_FRAME_RATE);
            mEditTextPreference_OnlyColorPreviewFrameRate.setOnPreferenceChangeListener(SettingsFragment.this);
            mEditTextPreference_EtronColorPreviewFrameRate = (EditTextPreference) findPreference(KEY_ETRON_COLOR_PREVIEW_FRAME_RATE);
            mEditTextPreference_EtronColorPreviewFrameRate.setOnPreferenceChangeListener(SettingsFragment.this);
            mEditTextPreference_EtronDepthPreviewFrameSizeRate= (EditTextPreference) findPreference(KEY_ETRON_DEPTH_PREVIEW_FRAME_RATE);
            mEditTextPreference_EtronDepthPreviewFrameSizeRate.setOnPreferenceChangeListener(SettingsFragment.this);
            mEditTextTempFix = (EditTextPreference) findPreference(KEY_TMP_FIX_FISH_LINE);
            mEditTextTempFix.setOnPreferenceChangeListener(SettingsFragment.this);
            buildUI();
        }

        public void buildUI() {
            mListPreference_OnlyColorPreviewFrameSize.setEntries(mEntriesColor);
            mListPreference_OnlyColorPreviewFrameSize.setEntryValues(mEntriesColor);
            mListPreference_EtronPreviewFrameSize.setEntries(mEntriesColor);
            mListPreference_EtronPreviewFrameSize.setEntryValues(mEntriesColor);
            mListPreference_DepthPreviewFrameSize.setEntries(mEntriesDepth);
            mListPreference_DepthPreviewFrameSize.setEntryValues(mEntriesDepth);
            mListPreference_SwtichViewWindow.setEntries(singleList_window);
            mListPreference_SwtichViewWindow.setEntryValues(singleList_window);
            mListPreference_SetFishFrame.setEntries(singleList_fish_frame);
            mListPreference_SetFishFrame.setEntryValues(singleList_fish_frame);
        }

        public void updateUI() {
            if(mOnlyColorPosition >-1){
                mListPreference_OnlyColorPreviewFrameSize.setValueIndex(mOnlyColorPosition);
                mListPreference_OnlyColorPreviewFrameSize.setSummary(mEntriesColor[mOnlyColorPosition]);
            }
            if(mEtronPosition >-1){
                mListPreference_EtronPreviewFrameSize.setValueIndex(mEtronPosition);
                mListPreference_EtronPreviewFrameSize.setSummary(mEntriesColor[mEtronPosition]);
            }
            if(mDepthPosition>-1){
                mListPreference_DepthPreviewFrameSize.setValueIndex(mDepthPosition);
                mListPreference_DepthPreviewFrameSize.setSummary(mEntriesDepth[mDepthPosition]);
            }
            mListPreference_OnlyColorPreviewFrameSize.setEnabled(mOnlyColor);
            mListPreference_EtronPreviewFrameSize.setEnabled(!mOnlyColor);
            mListPreference_DepthPreviewFrameSize.setEnabled(!mOnlyColor);
            mListPreference_SetFishFrame.setValueIndex(mFishPosition);
            mListPreference_SetFishFrame.setSummary(singleList_fish_frame[mFishPosition]);
            mListPreference_SetFishFrame.setEnabled(mOnlyColor);
            mEditTextPreference_OnlyColorPreviewFrameRate.setEnabled(mOnlyColor);
            mEditTextPreference_OnlyColorPreviewFrameRate.setSummary(String.valueOf(mOnlyColorFrameRate));
            mEditTextPreference_EtronColorPreviewFrameRate.setEnabled(!mOnlyColor);
            mEditTextPreference_EtronColorPreviewFrameRate.setSummary(String.valueOf(mEtronColorFrameRate));
            mEditTextPreference_EtronDepthPreviewFrameSizeRate.setEnabled(!mOnlyColor);
            mEditTextPreference_EtronDepthPreviewFrameSizeRate.setSummary(String.valueOf(mEtronDepthFrameRate));
            if (FishFixLine > 0) {
                mEditTextTempFix.setSummary(String.valueOf(FishFixLine));
            }
            mListPreference_SwtichViewWindow.setValueIndex(mWindowType);
            mListPreference_SwtichViewWindow.setSummary(singleList_window[mWindowType]);
            mCheckBoxPreference_OnlyColorPreviewChecked.setChecked(mOnlyColor);
            mCheckBoxPreference_ReverseViewWindowChecked.setChecked(mReverse);
            mCheckBoxPreference_LandscapeViewWindowChecked.setChecked(mLandscape);
            mCheckBoxPreference_FlipViewWindowChecked.setChecked(mFlip);
        }

        private void readData() {

            AppSettings appSettings = AppSettings.getInstance(mContext);
            mOnlyColor = appSettings.get(AppSettings.ONLY_COLOR, mOnlyColor);
            mOnlyColorPosition = appSettings.get(AppSettings.ONLY_COLOR_POSITION, mOnlyColorPosition);
            mEtronPosition = appSettings.get(AppSettings.ETRON_POSITION, mEtronPosition);
            mDepthPosition = appSettings.get(AppSettings.DEPTH_POSITION, mDepthPosition);
            mFishPosition = appSettings.get(AppSettings.FISH_POSITION, mFishPosition);
            mFishRecordFrame = appSettings.get(AppSettings.FISH_RECORD_FRAME, mFishRecordFrame);
            mOnlyColorFrameRate = appSettings.get(AppSettings.ONLY_COLOR_FRAME_RATE, mOnlyColorFrameRate);
            mEtronColorFrameRate = appSettings.get(AppSettings.ETRON_COLOR_FRAME_RETE,mEtronColorFrameRate);
            mEtronDepthFrameRate = appSettings.get(AppSettings.ETRON_DEPTH_FRAME_RETE, mEtronDepthFrameRate);
            mWindowType = appSettings.get(AppSettings.WINDOW_TYPE, mWindowType);
            mReverse = appSettings.get(AppSettings.REVERSE, mReverse);
            mLandscape = appSettings.get(AppSettings.LANDSCAPE, mLandscape);
            mFlip = appSettings.get(AppSettings.FLIP, mFlip);
            mSupportedSize = appSettings.get(AppSettings.SUPPORTED_SIZE,"");
        }

        public void writeData() {

            AppSettings appSettings = AppSettings.getInstance(mContext);

            if (mOnlyColor) {
                if(mOnlyColorPosition >-1) {
                    Color_W = mSupportedSizeListColor.get(mOnlyColorPosition).width;
                    Color_H = mSupportedSizeListColor.get(mOnlyColorPosition).height;
                    Depth_W = 0;
                    Depth_H = 0;
                }
            } else {
                if(mEtronPosition >-1) {
                    Color_W = mSupportedSizeListColor.get(mEtronPosition).width;
                    Color_H = mSupportedSizeListColor.get(mEtronPosition).height;
                }
                if(mDepthPosition >-1) {
                    Depth_W = mSupportedSizeListDepth.get(mDepthPosition).width;
                    Depth_H = mSupportedSizeListDepth.get(mDepthPosition).height;
                }
            }

            if (mFishPosition == 0) {
                mFishRecordFrame = 50;
            } else if (mFishPosition == 1) {
                mFishRecordFrame = 100;
            } else if (mFishPosition == 2) {
                mFishRecordFrame = 150;
            } else if (mFishPosition == 3) {
                mFishRecordFrame = 200;
            } else if (mFishPosition == 4) {
                mFishRecordFrame = 250;
            } else if (mFishPosition == 5) {
                mFishRecordFrame = 300;
            }

            appSettings.put(AppSettings.ONLY_COLOR, mOnlyColor);
            appSettings.put(AppSettings.REVERSE, mReverse);
            appSettings.put(AppSettings.LANDSCAPE, mLandscape);
            appSettings.put(AppSettings.FLIP, mFlip);
            appSettings.put(AppSettings.COLOR_W, Color_W);
            appSettings.put(AppSettings.COLOR_H, Color_H);
            appSettings.put(AppSettings.DEPTH_W, Depth_W);
            appSettings.put(AppSettings.Depth_H, Depth_H);
            appSettings.put(AppSettings.ONLY_COLOR_POSITION, mOnlyColorPosition);
            appSettings.put(AppSettings.ETRON_POSITION, mEtronPosition);
            appSettings.put(AppSettings.DEPTH_POSITION,mDepthPosition);
            appSettings.put(AppSettings.WINDOW_TYPE, mWindowType);
            appSettings.put(AppSettings.ONLY_COLOR_FRAME_RATE, mOnlyColorFrameRate);
            appSettings.put(AppSettings.ETRON_COLOR_FRAME_RETE, mEtronColorFrameRate);
            appSettings.put(AppSettings.ETRON_DEPTH_FRAME_RETE, mEtronDepthFrameRate);
            appSettings.put(AppSettings.FISH_POSITION, mFishPosition);
            appSettings.put(AppSettings.FISH_RECORD_FRAME, mFishRecordFrame);
            appSettings.put(AppSettings.SUPPORTED_SIZE, mSupportedSize);
            appSettings.saveAll();

            if (FishFixLine > 0) {
                Log.d(TAG, "FishFixLine:" + FishFixLine);
                FileWriter fwrite = null;
                try {
                    File file = new File("/storage/sdcard0/Android/data/com.etron.usbcamera/.tmp_fix.raw");
                    fwrite = new FileWriter(file);
                    fwrite.write(new Integer(FishFixLine).toString());
                    fwrite.flush();
                } catch(Exception e) {
                    Log.e(TAG, "write tmp fix Exception:" + e.toString());
                }

                try {
                    if (fwrite != null)
                        fwrite.close();
                } catch(IOException io) {
                    Log.e(TAG, "IOException:" + io.toString());
                }
            }
        }

        public boolean onPreferenceChange(Preference preference, Object objValue) {
            final String key = preference.getKey();
            if (KEY_ONLY_COLOR_PREVIEW_FRAME_SIZE.equals(key)) {
                mOnlyColorPosition = mListPreference_OnlyColorPreviewFrameSize.findIndexOfValue((String) objValue);
            } else if (KEY_ETRON_PREVIEW_FRAME_SIZE.equals(key)) {
                mEtronPosition = mListPreference_EtronPreviewFrameSize.findIndexOfValue((String) objValue);
            } else if (KEY_DEPTH_PREVIEW_FRAME_SIZE.equals(key)) {
                mDepthPosition = mListPreference_DepthPreviewFrameSize.findIndexOfValue((String) objValue);
            } else if (KEY_SWTICH_VIEW_WINDOW.equals(key)) {
                mWindowType = mListPreference_SwtichViewWindow.findIndexOfValue((String) objValue);
            } if (KEY_ONLY_COLOR_PREVIEW_FRAME_RATE.equals(key)) {
                mOnlyColorFrameRate = Integer.valueOf((String) objValue);
            } else if (KEY_ETRON_COLOR_PREVIEW_FRAME_RATE.equals(key)) {
                mEtronColorFrameRate = Integer.valueOf((String) objValue);
            } else if (KEY_ETRON_DEPTH_PREVIEW_FRAME_RATE.equals(key)) {
                mEtronDepthFrameRate =Integer.valueOf((String) objValue);
            } else if (KEY_ONLY_COLOR_SET_FISH_FRAME.equals(key)) {
                mFishPosition = mListPreference_SetFishFrame.findIndexOfValue((String) objValue);
            } else if (KEY_TMP_FIX_FISH_LINE.equals(key)) {
                FishFixLine = Integer.valueOf((String) objValue);
            }
            updateUI();
            writeData();
            return true;
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            if (preference == mCheckBoxPreference_OnlyColorPreviewChecked) {
                mOnlyColor = mCheckBoxPreference_OnlyColorPreviewChecked.isChecked();
            } else if (preference == mCheckBoxPreference_ReverseViewWindowChecked) {
                mReverse = mCheckBoxPreference_ReverseViewWindowChecked.isChecked();
            } else if (preference == mCheckBoxPreference_LandscapeViewWindowChecked) {
                mLandscape = mCheckBoxPreference_LandscapeViewWindowChecked.isChecked();
            } else if (preference == mCheckBoxPreference_FlipViewWindowChecked) {
                mFlip = mCheckBoxPreference_FlipViewWindowChecked.isChecked();
            }
            updateUI();
            writeData();
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        private CharSequence[] listToEntries(List<Size> list){
            CharSequence[] entries=new CharSequence[list.size()];
            List<String> listString=new ArrayList<String>();
            for(int i=0;i<list.size();i++){
                String type="";
                if(list.get(i).type==6)type="MJPEG";
                if(list.get(i).type==4)type="YUV";
                listString.add(String.format("%dx%d",list.get(i).width,list.get(i).height ));
                entries[i]=String.format("%dx%d",list.get(i).width,list.get(i).height );
                //Log.i(TAG,String.format("entries[%d]=%s",i,entries[i].toString()));
            }
            return entries;
        }
        private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
            @Override
            public void onAttach(final UsbDevice device) {
                if (device != null) {
                    mUsbDevice = device;
                } else {
                    final int n = mUSBMonitor.getDeviceCount();
                    if (DEBUG) Log.v(TAG, ">>>> onAttach getDeviceCount:" + n);
                    if (n ==1) {
                        mUsbDevice = mUSBMonitor.getDeviceList().get(0);
                    }
                }
                if (DEBUG) Log.v(TAG, ">>>> onAttach UsbDevice:" + mUsbDevice );
                if (mUsbDevice != null) {
                    mUSBMonitor.requestPermission(mUsbDevice);
                }
            }

            @Override
            public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
                if (mUVCCamera != null) return;
                if (DEBUG) Log.v(TAG, ">>>> onConnect UsbDevice:" + device);
                if (DEBUG) Log.v(TAG, ">>>> onConnect UsbControlBlock:" + ctrlBlock);
                if (DEBUG) Log.v(TAG, ">>>> onConnect createNew:" + createNew);
                if(DEBUG)Log.d(TAG, ">>>> getVenderId:" + ctrlBlock.getVenderId());
                if(DEBUG)Log.d(TAG, ">>>> getProductId:" + ctrlBlock.getProductId());
                if(DEBUG)Log.d(TAG, ">>>> getFileDescriptor:" + ctrlBlock.getFileDescriptor());
                final UVCCamera camera = new  UVCCamera();
                EXECUTER.execute(new Runnable() {
                    @Override
                    public void run() {
                        int ret = -1;
                        try {
                            ret = camera.open(ctrlBlock);
                            Log.i(TAG, "open uvccamera ret:" + ret);
                        } catch (Exception e) {
                            Log.e(TAG, "open uvccamera exception:" + e.toString());
                            return;
                        }
                        if (ret == UVCCamera.EYS_OK && mUVCCamera == null) {
                            mUVCCamera = camera;
                            mSupportedSize = mUVCCamera.getSupportedSize();
                            mSupportedSizeListDepth = mUVCCamera.getSupportedSizeList(-1,2);

                            updateResolutionSetting();
                            writeData();

                        }
                        else{
                            Log.e(TAG, "open uvccamera ret:" + ret);
                        }
                    }
                });
            }

            @Override
            public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
                if (DEBUG) Log.v(TAG, "onDisconnect");
                if (mUVCCamera != null && device.equals(mUVCCamera.getDevice())) {
                    mUVCCamera.close();
                    mUVCCamera.destroy();
                    mUVCCamera = null;
                }
            }

            @Override
            public void onDettach(final UsbDevice device) {
                Toast.makeText(mContext, R.string.usb_device_detached, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                if (DEBUG) Log.v(TAG, "onCancel:");
            }
        };
    }

}