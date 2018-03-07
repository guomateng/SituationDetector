package com.matain.situationdetector;

import android.hardware.camera2.CameraManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.OpenCVLoader;

public class CameraActivity extends AppCompatActivity {
    private final String LOG_TAG = CameraActivity.class.getSimpleName();
    private CameraManager mCameraManager;
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
    }
}
