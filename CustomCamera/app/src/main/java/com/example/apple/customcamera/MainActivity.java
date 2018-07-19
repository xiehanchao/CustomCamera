package com.example.apple.customcamera;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.view.OrientationEventListener.ORIENTATION_UNKNOWN;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "xieyaoyan";
    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mMediaRecorder;
    private boolean isRecording = false;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private int mId;
    private String mPath;
    private File pictureFile;
    private int current = -1;
    private Camera.Parameters parameters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isRecording) {
                            // stop recording and release camera
                            mMediaRecorder.stop();  // stop the recording
                            releaseMediaRecorder(); // release the MediaRecorder object
                            mCamera.lock();         // take camera access back from MediaRecorder

                            // inform the user that recording has stopped
                            ((Button)v).setText("Capture");
                            isRecording = false;
                        } else {
                            // initialize video camera
                            if (prepareVideoRecorder(mCamera)) {
                                // Camera is available and unlocked, MediaRecorder is prepared,
                                // now you can start recording
                                mMediaRecorder.start();

                                // inform the user that recording has started
                                ((Button)v).setText("Stop");
                                Toast.makeText(MainActivity.this, "录制文件保存在:"+mPath, Toast.LENGTH_LONG).show();
                                isRecording = true;
                            } else {
                                // prepare didn't work, release the camera
                                releaseMediaRecorder();
                                // inform user
                            }
                        }
                    }
                }
        );


        findViewById(R.id.PICCAPTURE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(null, null, mPicture);
            }
        });

    }
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            System.out.println("pictureFile.getAbsolutePath() = " + pictureFile.getAbsolutePath());
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions: " );
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();

                int bitmapDegree = getBitmapDegree(pictureFile.getAbsolutePath());
                Bitmap bitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());

                Bitmap newBitMap = rotateBitmapByDegree(bitmap, bitmapDegree);
//                if (!bitmap.isRecycled()){
//                    bitmap.recycle();
//                }
                FileOutputStream fosB = new FileOutputStream(pictureFile);
                newBitMap.compress(Bitmap.CompressFormat.JPEG, 100, fosB);
                fosB.flush();
                fosB.close();

                System.out.println("XXXbitmapDegree = " + bitmapDegree);

            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }

    };
    /**
     * 将图片按照某个角度进行旋转
     *
     * @param bm
     *            需要旋转的图片
     * @param degree
     *            旋转角度
     * @return 旋转后的图片
     */
    public static Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
        Bitmap returnBm = null;

        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        matrix.postScale(-1, 1);
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }
    private int getBitmapDegree(String path) {
        int degree = 0;
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            ExifInterface exifInterface = new ExifInterface(path);
            // 获取图片的旋转信息
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }
    public void onOrientationChanged1(int orientation, Camera.Parameters parameters) {
        if (orientation == ORIENTATION_UNKNOWN) return;
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        int id = FindFrontCamera();
        android.hardware.Camera.getCameraInfo(id, info);
        orientation = (orientation + 45) / 90 * 90;
        int rotation = 0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (info.orientation - orientation + 360) % 360;
        } else {  // back-facing camera
            rotation = (info.orientation + orientation) % 360;
        }
        parameters.setRotation(rotation);
    }

    @Override
    protected void onResume() {
        super.onResume();
        myOrientationEventListener m = new myOrientationEventListener(this);
        m.enable();
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        mId = FindFrontCamera();
        try {
            mCamera = Camera.open(mId);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "摄像头正在使用", Toast.LENGTH_LONG).show();
            return;
        }
        mCamera.setFaceDetectionListener(new MyFaceDetectionListener());
        mPreview = new CameraPreview(MainActivity.this, mCamera, mId);
        FrameLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }

    private int orientation;

    class myOrientationEventListener extends OrientationEventListener{

        public myOrientationEventListener(Context context) {
            super(context);
        }

        public myOrientationEventListener(Context context, int rate) {
            super(context, rate);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            Log.d(TAG, "onOrientationChanged: " + orientation);
            if (orientation > 350 || orientation < 10) { //0度
                MainActivity.this.orientation = 0;
            } else if (orientation > 80 && orientation < 100) { //90度
                MainActivity.this.orientation = 90;
            } else if (orientation > 170 && orientation < 190) { //180度
                MainActivity.this.orientation = 180;
            } else if (orientation > 260 && orientation < 280) { //270度
                MainActivity.this.orientation = 270;
            } else {
                return;
            }
            if (MainActivity.this.orientation != current){
                current = MainActivity.this.orientation;
                onOrientationChanged1(current, parameters);
                mCamera.setParameters(parameters);
            }
        }
    }


    @TargetApi(9)
    private int FindFrontCamera(){
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number
        System.out.println("cameraCount = " + cameraCount);
        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo ); // get camerainfo
            if ( cameraInfo.facing ==Camera.CameraInfo.CAMERA_FACING_FRONT) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }


    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.release();        // release the camera for other applications
            mCamera.setFaceDetectionListener(null);
            mCamera = null;
        }
    }

    private void stopPreviewAndFreeCamera() {

        if (mCamera != null) {
            // Call stopPreview() to stop updating the preview surface.
            mCamera.stopPreview();

            // Important: Call release() to release the mCamera for use by other
            // applications. Applications should release the mCamera immediately
            // during onPause() and re-open() it during onResume()).
            mCamera.release();

            mCamera = null;
        }
    }


    private boolean prepareVideoRecorder(Camera mCamera){

        mMediaRecorder = new MediaRecorder();
        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(CamcorderProfile.get(mId, CamcorderProfile.QUALITY_1080P));

        // Step 4: Set output file
        mPath = getOutputMediaFile(MEDIA_TYPE_VIDEO).toString();
        System.out.println("path = " + mPath);
        mMediaRecorder.setOutputFile(mPath);

        // Step 5: Set the preview output
        Surface surface = mPreview.getHolder().getSurface();
        mMediaRecorder.setPreviewDisplay(surface);

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    class MyFaceDetectionListener implements Camera.FaceDetectionListener {

        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            if (faces.length > 0){
                Log.d("FaceDetection", "face detected: "+ faces.length +
                        " Face 1 Location X: " + faces[0].rect.centerX() +
                        "Y: " + faces[0].rect.centerY() );
            }
        }
    }



    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;
        private int mId;

        public CameraPreview(Context context, Camera camera, int id) {
            super(context);
            mCamera = camera;
            mId = id;
            int rotation = MainActivity.this.getWindowManager().getDefaultDisplay()
                    .getRotation();

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the mCamera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
                startFaceDetection();
            } catch (IOException e) {
                Log.d(TAG, "Error setting mCamera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
            // Surface will be destroyed when we return, so stop the preview.
            if (mCamera != null) {
                // Call stopPreview() to stop updating the preview surface.
               
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){// preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or reformatting changes here
            List<Camera.Size> supportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            for (Camera.Size size : supportedPreviewSizes) {
                System.out.println("Width = " + size.width + "  Height = " + size.height);
            }
            parameters = mCamera.getParameters();
            setCameraDisplayOrientation(MainActivity.this, mId, mCamera, parameters);

            parameters.setPreviewSize(1920, 1080);
            mCamera.setParameters(parameters);
            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (Exception e){
                Log.d(TAG, "Error starting mCamera preview: " + e.getMessage());
            }
        }

        public void setCameraDisplayOrientation(AppCompatActivity activity,
                                                int cameraId, Camera camera, Camera.Parameters parameters) {
            android.hardware.Camera.CameraInfo info =
                    new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(cameraId, info);
            int rotation = activity.getWindowManager().getDefaultDisplay()
                    .getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0: degrees = 0; break;
                case Surface.ROTATION_90: degrees = 90; break;
                case Surface.ROTATION_180: degrees = 180; break;
                case Surface.ROTATION_270: degrees = 270; break;
            }

            int result;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;  // compensate the mirror
            } else {  // back-facing
                result = (info.orientation - degrees + 360) % 360;
            }
            camera.setDisplayOrientation(result);
            System.out.println("XXXCameraPreview.setCameraDisplayOrientation   " + result);

        }
    }

    private void startFaceDetection() {
        // Try starting Face Detection
        Camera.Parameters params = mCamera.getParameters();

        // start face detection only *after* preview has started
        if (params.getMaxNumDetectedFaces() > 0){
            // camera supports face detection, so can start it:
            mCamera.startFaceDetection();
        }
    }
}
