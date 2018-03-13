package com.matain.situationdetector;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.Arrays;

public class CameraActivity extends AppCompatActivity {
    private final boolean DBG = true;
    private final String LOG_TAG = CameraActivity.class.getSimpleName();
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceViewHolder;
    private ImageReader mImageReader;
    private CameraManager mCameraManager;
    private Handler mCameraHandler;
    private Handler mMainHandler;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mCaptureSession;
    /*camera device id*/
    private String mCameraId = Integer.toString(CameraCharacteristics.LENS_FACING_FRONT);

    private CameraDevice mCameraDevice;
    /*camera state call back*/
    private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            logd("CameraStateCallback camera opened .... ");
            mCameraDevice = cameraDevice;
            //TODO take preview
            try{
                takePreview();
            }catch (CameraAccessException e){
                logd("take preview exception e:" + e);
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            logd("CameraStateCallback camera closed .... ");
            //TODO close camera
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            logd("CameraStateCallback camera error .... ");
            Toast.makeText(CameraActivity.this, "摄像头开启失败", Toast.LENGTH_SHORT).show();
        }
    };

    private BaseLoaderCallback mOpenCVLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status){
                case BaseLoaderCallback.SUCCESS:
                    Log.d(LOG_TAG, "openCV lib load success ...");
                    break;
                default:
                    Log.d(LOG_TAG, "openCV lib load fail ...");
                    break;
            }
        }
        @Override
        public void onPackageInstall(int operation, InstallCallbackInterface callback) {
            Log.d(LOG_TAG,"onPackageInstall do nothing ....");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        mSurfaceView = (SurfaceView) findViewById(R.id.camera_surfaceview);
    }
    @Override
    public void onResume(){
        super.onResume();
        if(!OpenCVLoader.initDebug()){
           Log.d(LOG_TAG, "external openCV lib not found, using openCV manager for initialization ...");
           OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mOpenCVLoaderCallback);
        }else {
            Log.d(LOG_TAG, "internal openCV lib found, using it.");
            mOpenCVLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }
        initSurfaceView();
    }

    void initSurfaceView(){
        mSurfaceViewHolder = mSurfaceView.getHolder();
        mSurfaceViewHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                logd("surfaceCreated");
                initCameraAndPreview();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                logd("surfaceChanged");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                logd("surfaceDestroyed");
                if(mCameraDevice != null){
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
            }
        });
    }

    void initCameraAndPreview(){
        logd("init camera and preview");
        HandlerThread handlerThread = new HandlerThread("camera");
        handlerThread.start();
        mCameraHandler = new Handler(handlerThread.getLooper());
        mMainHandler = new Handler(getMainLooper());
        mImageReader = ImageReader.newInstance(mSurfaceView.getWidth(), mSurfaceView.getHeight(), ImageFormat.JPEG,7);
        try {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA);
            }
            mCameraManager.openCamera(mCameraId, mCameraStateCallback,mCameraHandler);
        }catch (CameraAccessException e){
            loge("exception when open camera e;" + e);
        }catch (SecurityException e){
            loge("exception when open camera e;" + e);
        }
    }

    private ImageReader.OnImageAvailableListener mImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            logd("onImageAvailable");
        }
    };

    private void takePreview() throws CameraAccessException{
        mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mPreviewBuilder.addTarget(mSurfaceViewHolder.getSurface());
        mCameraDevice.createCaptureSession(Arrays.asList(mSurfaceViewHolder.getSurface(), mImageReader.getSurface()),mCaptureSessionStateCallback, mMainHandler);
    }

    private CameraCaptureSession.StateCallback mCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            mCaptureSession = cameraCaptureSession;
            try{
                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null,mMainHandler);
            }catch (CameraAccessException e){
                logd("CameraCaptureSession.StateCallback onConfigured exception: " + e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            logd("CameraCaptureSession.StateCallback onConfigureFailed ...");
        }
    };

    private void logd(String msg){
        if(DBG){
            Log.d(LOG_TAG, msg);
        }
    }

    private void loge(String msg){
        Log.e(LOG_TAG, msg);
    }
}
