package com.matain.situationdetector;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.OpenCVLoader;

public class CameraActivity extends AppCompatActivity {
    private final boolean DBG = true;
    private final String LOG_TAG = CameraActivity.class.getSimpleName();
    private SurfaceView mSurfaceView;
    private CameraManager mCameraManager;
    private Handler mHandler;
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
            takePreview();
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
           Log.d(LOG_TAG, "internal openCV lib not found, using openCV manager for initialization ...");
           OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mOpenCVLoaderCallback);
        }else {
            Log.d(LOG_TAG, "internal openCV lib found, using it.");
            mOpenCVLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }
        initCameraAndPreview();
    }

    void initCameraAndPreview(){
        logd("init camera and preview");
        HandlerThread handlerThread = new HandlerThread("camera");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        try {
            mCameraManager.openCamera(mCameraId, mCameraStateCallback,mHandler);
        }catch (CameraAccessException e){
            loge("exception when open camera e;" + e);
        }catch (SecurityException e){
            loge("exception when open camera e;" + e);
        }
    }

    private void takePreview(){

    }

    private void logd(String msg){
        if(DBG){
            Log.d(LOG_TAG, msg);
        }
    }

    private void loge(String msg){
        Log.e(LOG_TAG, msg);
    }
}
