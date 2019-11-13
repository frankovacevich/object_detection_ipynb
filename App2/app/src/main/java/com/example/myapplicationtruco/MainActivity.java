package com.example.myapplicationtruco;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    CameraControl cameraControl;
    TensorFlowModel tensorFlowModel;
    Animations animationsControl;

    // =============================================================================================
    // OVERRIDE ACTIVITY METHODS
    // =============================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraControl = new CameraControl();
        cameraControl.Initialize(this,(FrameLayout) findViewById(R.id.camera_frame_layout));

        tensorFlowModel = new TensorFlowModel();
        tensorFlowModel.InitializeAsync(getAssets());

        animationsControl = new Animations(this);


    }

    @Override
    public void onDestroy(){
        cameraControl.releaseCamera();
        super.onDestroy();
    }

    @Override
    protected void onResume()
    {
        cameraControl.Setup();
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        cameraControl.releaseCamera();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        final int camera_request_code = 102;
        switch (requestCode) {
            case camera_request_code: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraControl.Setup();
                } else {
                    android.os.Process.killProcess(android.os.Process.myPid());
                    this.finish();
                }
                return;
            }
        }
    }

    // =============================================================================================
    // MANAGE VIEWS
    // =============================================================================================
    enum VIEWS {ReadyForPicture, Loading, ViewPicture, Settings}
    VIEWS currentView = VIEWS.ReadyForPicture;
    VIEWS previousVIew = null;
    void setView(VIEWS V) {
        previousVIew = currentView;
        currentView = V;

        FrameLayout frameCamera = ((FrameLayout) findViewById(R.id.camera_frame_layout));
        FrameLayout frameSettings = ((FrameLayout) findViewById(R.id.settings_frame_layout));
        ImageView frameView = ((ImageView) findViewById(R.id.image_frame_layout));
        ProgressBar frameLoading = ((ProgressBar) findViewById(R.id.loading_frame_layout));
        ImageView config_button = ((ImageView) findViewById(R.id.config_button));
        FloatingActionButton take_picture_button = ((FloatingActionButton) findViewById(R.id.take_picture_button));

        frameView.setVisibility(View.GONE);
        frameCamera.setVisibility(View.GONE);
        frameLoading.setVisibility(View.GONE);
        frameSettings.setVisibility(View.GONE);
        config_button.setVisibility(View.VISIBLE);
        take_picture_button.setVisibility(View.VISIBLE);

        switch (V) {
            case ViewPicture:
                frameView.setVisibility(View.VISIBLE);
                take_picture_button.setImageResource(R.drawable.ic_restart);
                break;

            case Loading:
                config_button.setVisibility(View.GONE);
                take_picture_button.setVisibility(View.GONE);
                frameLoading.setVisibility(View.VISIBLE);
                frameCamera.setVisibility(View.VISIBLE);
                break;

            case ReadyForPicture:
                frameCamera.setVisibility(View.VISIBLE);
                take_picture_button.setImageResource(R.drawable.ic_camera);
                break;

            case Settings:
                take_picture_button.setVisibility(View.GONE);
                frameCamera.setVisibility(View.VISIBLE);
                frameSettings.setVisibility(View.VISIBLE);
        }
    }

    // =============================================================================================
    // CLICK ON BUTTONS
    // =============================================================================================
    public void button_config_click(View v){
        if (currentView != VIEWS.Settings){
            setView(VIEWS.Settings);
            animationsControl.AnimateImageRotationClockwise((ImageView) findViewById(R.id.config_button));
            animationsControl.AnimateLayoutSlideFromTop((LinearLayout) findViewById(R.id.settings_menu));
            animationsControl.AnimateFrameLayoutFadeIn((FrameLayout) findViewById(R.id.settings_frame_layout));
            //animationsControl.AnimateButtonFadeIn((FloatingActionButton) findViewById(R.id.take_picture_button));
        }
        else{
            setView(previousVIew);
            animationsControl.AnimateImageRotationCounterclockwise((ImageView) findViewById(R.id.config_button));
            animationsControl.AnimateLayoutSlideFromBottom((LinearLayout) findViewById(R.id.settings_menu));
            animationsControl.AnimateFrameLayoutFadeOut((FrameLayout) findViewById(R.id.settings_frame_layout));
            //animationsControl.AnimateButtonFadeOut((FloatingActionButton) findViewById(R.id.take_picture_button));
        }
    }

    public void button_send_feedback_click(View v){
        OpenFeedbackDialog();
    }

    public void button_rate_app_click(View v){

    }



    public void button_camera_click(View v){
        if(currentView == VIEWS.ReadyForPicture){
            setView(VIEWS.Loading);
            cameraControl.takePictureAsync(new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] bytes, Camera camera) {
                    final Bitmap bitmap = rotateBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                    tensorFlowModel.RunAsync(bitmap, new Runnable() {
                        @Override
                        public void run() {
                            List<ClassifierOutputItem> result = tensorFlowModel.getResult();
                            Bitmap result_bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), false);

                            int W = result_bitmap.getWidth(); int H = result_bitmap.getHeight();

                            List<String> result_string_classes = new ArrayList<>();
                            for (ClassifierOutputItem r : result){
                                result_bitmap = tensorFlowModel.DrawBoxOnBitmap(result_bitmap,
                                        (int) (r.xmin * W),(int) (r.xmax * W), (int) (r.ymin * H),(int) (r.ymax * H),Color.GREEN,2);
                                result_string_classes.add(r.class_name);
                            }

                            ((TextView) findViewById(R.id.txtbox_1)).setText("");
                            ((TextView) findViewById(R.id.txtbox_2)).setText(Integer.toString(result.size()));
                            ((ImageView) findViewById(R.id.image_frame_layout)).setImageBitmap(result_bitmap);
                            ImageView image_view_top = (ImageView) findViewById(R.id.image_view_top);
                            Bitmap top_bitmap = getTopBitmap(result_string_classes, image_view_top.getWidth(),image_view_top.getHeight());
                            image_view_top.setImageBitmap(top_bitmap);
                            setView(VIEWS.ViewPicture);
                        }
                    });
                }
            });
        } else if(currentView == VIEWS.ViewPicture){
            cameraControl.restartPreview();
            setView(VIEWS.ReadyForPicture);
        }
    }

    // =============================================================================================
    // SOME OPERATIONS WITH BITMAPS
    // =============================================================================================
    private Bitmap rotateBitmap(Bitmap bitmap){
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void placeBitmap(Bitmap big_bitmap, Bitmap small_bitmap, int x, int y){
        Canvas canvas = new Canvas(big_bitmap);
        canvas.drawBitmap(big_bitmap, new Matrix(), null);
        canvas.drawBitmap(small_bitmap,x,y,new Paint());
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int newW, int newH){
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true);
    }

    private Bitmap getTopBitmap(List<String> result_string_classes, int W, int H){

        Bitmap top_bitmap = Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888);

        // Calculate dx (horizontal displacement of each card)
        int N = result_string_classes.size();
        float lw = (H * 40f / 59f) * 1.1f;
        int dx = (int) ((W-lw)/N);
        dx = (dx > lw) ? (int) lw : dx;

        int x = 0;
        for (String card : result_string_classes){
            try {
                InputStream IS = getAssets().open("smallcards/" + card + ".png");
                Bitmap card_bitmap = BitmapFactory.decodeStream(IS);
                card_bitmap = scaleBitmap(card_bitmap,(int) (H * 40.0 / 59.0), H);
                placeBitmap(top_bitmap,card_bitmap,x,0);
                x = x + dx;
            } catch(Exception ex){

            }
        }
        return top_bitmap;
    }

    // =============================================================================================
    // GET POINTS
    // =============================================================================================
    List<ScoreScheme> ScoreSchemesList = new ArrayList<>();
    class ScoreScheme{
        boolean showScore = true;
        Map<String, Integer> map = new HashMap<String, Integer>();
    }



    // =============================================================================================
    // DIALOGS
    // =============================================================================================
    void OpenFeedbackDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.feedback_dialog,null);
        dialogBuilder.setView(dialogView);
        final AlertDialog alertDialog = dialogBuilder.create();


        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                ((RadioButton) dialogView.findViewById(R.id.feedback_radiobutton_1)).setVisibility(View.VISIBLE);
                if(previousVIew!=VIEWS.ViewPicture){
                    alertDialog.findViewById(R.id.feedback_radiobutton_1).setVisibility(View.GONE);
                }

            }
        });

        alertDialog.setButton(Dialog.BUTTON_POSITIVE, "SEND", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Bitmap bitmap = null;
                String subj = getResources().getString(R.string.app_name) + " Feedback: ";
                if(((RadioButton) alertDialog.findViewById(R.id.feedback_radiobutton_1)).isChecked()){
                    subj += "Wrong label";
                    bitmap = ((BitmapDrawable) ((ImageView) findViewById(R.id.image_frame_layout)).getDrawable()).getBitmap();
                } else if(((RadioButton) alertDialog.findViewById(R.id.feedback_radiobutton_2)).isChecked()){
                    subj += "Feature not working";
                } else if(((RadioButton) alertDialog.findViewById(R.id.feedback_radiobutton_3)).isChecked()){
                    subj += "New idea";
                } else if(((RadioButton) alertDialog.findViewById(R.id.feedback_radiobutton_4)).isChecked()){
                    subj += "Other";
                }

                String msg = ((EditText) alertDialog.findViewById(R.id.feedback_edittext)).getText().toString();
                SendEmail ES = new SendEmail(subj, msg ,bitmap, MainActivity.this);
                ES.execute();
            }
        });

        alertDialog.show();
    }

    void EditSoresDialog(){

    }
}
