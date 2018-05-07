package com.victor.zbar.module;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;
import android.view.TextureView;
import android.widget.Toast;

import com.victor.zbar.interfaces.OnQrScanListener;

import java.io.IOException;

import zbar.victor.com.library.R;
import zbar.victor.com.library.core.CameraScanner;
import zbar.victor.com.library.core.GraphicDecoder;
import zbar.victor.com.library.core.NewCameraScanner;
import zbar.victor.com.library.core.OldCameraScanner;
import zbar.victor.com.library.core.ZBarDecoder;
import zbar.victor.com.library.view.AdjustTextureView;
import zbar.victor.com.library.view.ScannerFrameView;

/**
 * Created by victor on 2017/11/20.
 */

public class ZbarScanHelper implements CameraScanner.CameraListener,
        TextureView.SurfaceTextureListener, GraphicDecoder.DecodeListener{
    private String TAG = "ZbarScanHelper";
    private static final long VIBRATE_DURATION = 200L;

    public Activity mActivity;
    private AdjustTextureView mTextureView;
    private ScannerFrameView mScannerFrameView;
    private CameraScanner mCameraScanner;
    private GraphicDecoder mGraphicDecoder;
    private MediaPlayer mediaPlayer;
    private boolean playBeep;
    private static final float BEEP_VOLUME = 0.10f;
    private boolean vibrate;
    private OnQrScanListener mOnQrScanListener;

    private int mCount = 0;
    private String mResult = null;

    public ZbarScanHelper(Activity activity,AdjustTextureView adjustTextureView,ScannerFrameView scannerFrameView, OnQrScanListener listener) {
        mActivity = activity;
        mTextureView = adjustTextureView;
        mScannerFrameView = scannerFrameView;
        mOnQrScanListener = listener;
        init();
    }

    private void init () {
        boolean isOldApi = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            isOldApi = true;
        }
        mCameraScanner = isOldApi ? OldCameraScanner.getInstance() : NewCameraScanner.getInstance();
        mCameraScanner.setCameraListener(this);
        mTextureView.setSurfaceTextureListener(this);
    }


    private void initBeepSound() {
        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            mActivity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(beepListener);

            AssetFileDescriptor file = mActivity.getResources().openRawResourceFd(
                    R.raw.beep);
            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
                file.close();
                mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                mediaPlayer.prepare();
            } catch (IOException e) {
                mediaPlayer = null;
            }
        }
    }

    public void onResume() {
        Log.d(TAG, "xxxxxxxxxxxxxxxxxxxonResume");
        playBeep = true;
        final AudioManager audioService = (AudioManager) mActivity.getSystemService(mActivity.AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false;
        }
        initBeepSound();
        vibrate = true;

        Log.d(TAG, getClass().getName() + ".onRestart()");
        if (mTextureView.isAvailable()) {
            //部分机型转到后台不会走onSurfaceTextureDestroyed()，因此isAvailable()一直为true，转到前台后不会再调用onSurfaceTextureAvailable()
            //因此需要手动开启相机
            mCameraScanner.setSurfaceTexture(mTextureView.getSurfaceTexture());
            mCameraScanner.setPreviewSize(mTextureView.getWidth(), mTextureView.getHeight());
            mCameraScanner.openCamera(mActivity.getApplicationContext());
        }
    }

    public void onPause () {
        mCameraScanner.closeCamera();
    }

    public void onDestroy () {
        Log.d(TAG, getClass().getName() + ".onDestroy()");
        mCameraScanner.setGraphicDecoder(null);
        if (mGraphicDecoder != null) {
            mGraphicDecoder.setDecodeListener(null);
            mGraphicDecoder.detach();
        }
        mCameraScanner.detach();
    }

    @SuppressLint("MissingPermission")
    private void playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }
        if (vibrate) {
            Vibrator vibrator = (Vibrator) mActivity.getSystemService(mActivity.VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    public void restartPreview(int delayMs) {
        mGraphicDecoder.startDecodeDelay(delayMs);
    }

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private final MediaPlayer.OnCompletionListener beepListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mCameraScanner.setSurfaceTexture(surface);
        mCameraScanner.setPreviewSize(width, height);
        mCameraScanner.openCamera(mActivity.getApplicationContext());
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, getClass().getName() + ".onSurfaceTextureSizeChanged() width = " + width + " , height = " + height);
        // TODO 当View大小发生变化时，要进行调整。
//        mTextureView.setImageFrameMatrix();
//        mCameraScanner.setPreviewSize(width, height);
//        mCameraScanner.setFrameRect(mScannerFrameView.getLeft(), mScannerFrameView.getTop(), mScannerFrameView.getRight(), mScannerFrameView.getBottom());
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG, getClass().getName() + ".onSurfaceTextureDestroyed()");
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void decodeSuccess(int type, int quality, String result) {
        if (result.equals(mResult)) {
            if (++mCount > 3) {//连续四次相同则显示结果（主要过滤脏数据，也可以根据条码类型自定义规则）
                if (quality < 10) {
                    mGraphicDecoder.stopDecode();
                    playBeepSoundAndVibrate();
                    if (mOnQrScanListener != null) {
                        mOnQrScanListener.OnQrScan(true, result);
                    }
                } else if (quality < 100) {
                    mGraphicDecoder.stopDecode();
                    playBeepSoundAndVibrate();
                    if (mOnQrScanListener != null) {
                        mOnQrScanListener.OnQrScan(true, result);
                    }
                } else {
                    playBeepSoundAndVibrate();
                    mGraphicDecoder.stopDecode();
                    if (mOnQrScanListener != null) {
                        mOnQrScanListener.OnQrScan(true, result);
                    }
                }
            }
        } else {
            mCount = 1;
            mResult = result;
            Log.e(TAG, getClass().getName() + ".decodeSuccess() -> "+mResult);
        }
    }

    @Override
    public void openCameraSuccess(int surfaceWidth, int surfaceHeight, int surfaceDegree) {
        Log.e(TAG, getClass().getName() + ".openCameraSuccess() frameWidth = " + surfaceWidth + " , frameHeight = " + surfaceHeight + " , frameDegree = " + surfaceDegree);
        mTextureView.setImageFrameMatrix(surfaceWidth, surfaceHeight, surfaceDegree);
        if (mGraphicDecoder == null) {
            mGraphicDecoder = new ZBarDecoder();
            mGraphicDecoder.setDecodeListener(this);
        }
        //该区域坐标为相对于父容器的左上角顶点。
        //TODO 应考虑TextureView与ScannerFrameView的Margin与padding的情况
        mCameraScanner.setFrameRect(mScannerFrameView.getLeft(), mScannerFrameView.getTop(), mScannerFrameView.getRight(), mScannerFrameView.getBottom());
        mCameraScanner.setGraphicDecoder(mGraphicDecoder);
    }

    @Override
    public void openCameraError() {
        if (mOnQrScanListener != null) {
            mOnQrScanListener.OnQrScan(true, "open camera error!");
        }
    }

    @Override
    public void noCameraPermission() {
        if (mOnQrScanListener != null) {
            mOnQrScanListener.OnQrScan(true, "no camera permission!");
        }
    }

    @Override
    public void cameraDisconnected() {
        if (mOnQrScanListener != null) {
            mOnQrScanListener.OnQrScan(true, "camera disconnected!");
        }
    }
}
