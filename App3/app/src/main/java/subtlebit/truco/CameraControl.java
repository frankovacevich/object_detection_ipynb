package subtlebit.truco;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.io.IOException;

public class CameraControl{

    public final int MY_CAMERA_REQUEST_CODE = 102;

    private FrameLayout cameraPreviewLayout;    // The layout were the camera will be displayed
    private ImageSurfaceView imageSurfaceView;  // The SurfaceView that will handle the display
    private MainActivity mainActivity;
    private Camera camera;

    CameraControl(){

    }

    // =============================================================================================
    // INITIALIZE, RESTART AND CLOSE CAMERA
    // =============================================================================================
    void Initialize(MainActivity mainActivity_, FrameLayout frameLayout_){
        mainActivity = mainActivity_;
        cameraPreviewLayout = frameLayout_;

        if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(mainActivity, new String[] {Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        else
            Setup();
    }

    public void Setup(){
        if(camera!=null) return;

        camera = Camera.open();
        imageSurfaceView = new ImageSurfaceView(mainActivity, camera);
        cameraPreviewLayout.addView(imageSurfaceView);
        imageSurfaceView = new ImageSurfaceView(mainActivity, camera);

        // Set correct orientation of the camera
        if (mainActivity.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            camera.setDisplayOrientation(90);
        } else {
            camera.setDisplayOrientation(0);
        }
    }

    public void releaseCamera(){
        camera.release();
        camera = null;
    }

    public void restartPreview(){
        camera.stopPreview();
        camera.startPreview();
    }

    // =============================================================================================
    // TAKE PICTURE
    // =============================================================================================

    void takePictureAsync(Camera.PictureCallback pictureCallback) {
        camera.takePicture(null, null, pictureCallback);
    }

    // =============================================================================================
    // BITMAP HANDLING
    // =============================================================================================
    private Bitmap scaleDownBitmapImage(Bitmap bitmap, int newWidth, int newHeight){
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        return resizedBitmap;
    }
}

//
//
// This is a helper class for the Camera Control
//
class ImageSurfaceView  extends SurfaceView implements SurfaceHolder.Callback {
    private Camera camera;
    private SurfaceHolder surfaceHolder;

    public ImageSurfaceView (Context context, Camera camera) {
        super(context);
        this.camera = camera;
        this.surfaceHolder = getHolder();
        this.surfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            this.camera.setPreviewDisplay(holder);
            this.camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        this.camera.stopPreview();
        this.camera.release();
    }
}

