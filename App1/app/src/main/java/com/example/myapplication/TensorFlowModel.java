package com.example.myapplication;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

// =============================================================================================
// TENSORFLOW MODEL
// This class loads a tensorflow graph (.pb) from the assets and performs an object recognition
// routine.
//
// Functions:
// - Initialize:
//   void Initialize()
//   void InitializeAsync()
// - Run:
//   void Run(Bitmap bitmap)
//   void RunAsync(Bitmap bitmap, Runnable runnable)
//   List<ClassifierOutputItem> getResult()
// - Bitmap Handling:
//   Bitmap DrawBoxOnBitmap(Bitmap)
//   byte[] BitmapToArray(Bitmap)
//   Bitmap ResizeAndCropBitmap(Bitmap)
// =============================================================================================

public class TensorFlowModel {
    int IMG_WIDTH = 700;
    int IMG_HEIGHT = 700;
    int NUM_CLASSES = 3;
    int MAX_DETECTION = 100;
    float DETECTION_THRESHOLD = 0.3f;
    int NUM_BOXES_PER_BLOCK = 5;
    int BLOCK_SIZE = 32;

    String MODEL_PATH = "file:///android_asset/frozen_inference_graph.pb";
    String LABELS_PATH = "labels.txt";
    String TEST_BITMAP_PATH = "test.jpg";

    private List<String> LABELS = new ArrayList<>();
    private TensorFlowInferenceInterface tensorFlowInferenceInterface;
    private AssetManager assetManager;
    private Bitmap testbitmap;
    private List<ClassifierOutputItem> RunResult;

    MainActivity mainActivity;

    // =============================================================================================
    // INITIALIZE MODEL
    // =============================================================================================
    TensorFlowModel(){ }

    public void InitializeAsync(AssetManager assetManager_){
        assetManager = assetManager_;
        InitializeAsyncTask IAT = new InitializeAsyncTask(assetManager);
        IAT.execute();
    }

    private class InitializeAsyncTask extends AsyncTask<Void, Void, Void>{
        private AssetManager assetManager;
        InitializeAsyncTask(AssetManager assetManager_){
            assetManager = assetManager_;
        }

        @Override
        protected Void doInBackground(Void ... params){
            Initialize(assetManager);
            return null;
        }
    }

    public void Initialize(AssetManager assetManager_){
        assetManager = assetManager_;
        tensorFlowInferenceInterface = new TensorFlowInferenceInterface(assetManager,MODEL_PATH);
        try{
            InputStreamReader ISR = new InputStreamReader(assetManager.open(LABELS_PATH));
            BufferedReader BR = new BufferedReader(ISR);
            String line = "";
            while((line = BR.readLine()) != null){
                LABELS.add(line);
                NUM_CLASSES += 1;
            }
            BR.close();
            ISR.close();

            InputStream IS = assetManager.open(TEST_BITMAP_PATH);
            testbitmap = BitmapFactory.decodeStream(IS);
        } catch (Exception ex){
            Log.i("ERROR",ex.getMessage().toString());
        }
    }

    // =============================================================================================
    // RUN
    // =============================================================================================
    public void RunAsync(Bitmap bitmap, Runnable runnable){
        RunAsyncTask RAT = new RunAsyncTask(bitmap,runnable);
        RAT.execute();
    }

    private class RunAsyncTask extends AsyncTask<Void, Void, Void>{
        private Bitmap bitmap;
        private Runnable runnable;
        RunAsyncTask(Bitmap bitmap_, Runnable runnable_){
            bitmap = bitmap_;
            runnable = runnable_;
        }

        @Override
        protected Void doInBackground(Void ... params){
            Run(bitmap);
            return null;
        }

        @Override
        protected void onPostExecute(Void v){
            runnable.run();
        }
    }

    List<ClassifierOutputItem> getResult(){
        return RunResult;
    }

    private static final double[] ANCHORS = {
            1.08, 1.19,
            3.42, 4.41,
            6.63, 11.38,
            9.42, 5.11,
            16.62, 10.52
    };

    public void RunIfYolo(Bitmap bitmap){
        IMG_WIDTH = 416;
        IMG_HEIGHT = 416;
        NUM_CLASSES = 3;

        Bitmap new_bitmap = ResizeAndCropBitmap(bitmap);
        //new_bitmap = testbitmap; //ResizeAndCropBitmap(testbitmap);
        float[] float_array = BitmapToFloatArray(new_bitmap);

        final int gridWidth = (int) (new_bitmap.getWidth() / BLOCK_SIZE);
        final int gridHeight =(int) (new_bitmap.getHeight() / BLOCK_SIZE);

        long startTime = System.currentTimeMillis();
        tensorFlowInferenceInterface.feed("input", float_array,1L,IMG_HEIGHT, IMG_WIDTH, 3L );
        String[] OUTPUT_NAMES = {"output"};
        tensorFlowInferenceInterface.run(OUTPUT_NAMES);
        long difference = System.currentTimeMillis() - startTime;
        Log.i("TENSORFLOW_TIME:", Long.toString(difference));

        float[] output = new float[gridWidth * gridHeight * (NUM_CLASSES + 5) * NUM_BOXES_PER_BLOCK];
        tensorFlowInferenceInterface.fetch("output", output);

        List<ClassifierOutputItem> result = new ArrayList<>();
        for (int y = 0; y < gridHeight; ++y) {
            for (int x = 0; x < gridWidth; ++x) {
                for (int b = 0; b < NUM_BOXES_PER_BLOCK; ++b) {
                    final int offset =
                            (gridWidth * (NUM_BOXES_PER_BLOCK * (NUM_CLASSES + 5))) * y
                                    + (NUM_BOXES_PER_BLOCK * (NUM_CLASSES + 5)) * x
                                    + (NUM_CLASSES + 5) * b;

                    ClassifierOutputItem newOutputItem = new ClassifierOutputItem();

                    final float xPos = (x + expit(output[offset + 0])) * BLOCK_SIZE;
                    final float yPos = (y + expit(output[offset + 1])) * BLOCK_SIZE;

                    final float w = (float) (Math.exp(output[offset + 2]) * ANCHORS[2 * b + 0]) * BLOCK_SIZE;
                    final float h = (float) (Math.exp(output[offset + 3]) * ANCHORS[2 * b + 1]) * BLOCK_SIZE;

                    newOutputItem.xmin = (Math.max(0, xPos - w / 2)) / IMG_WIDTH;
                    newOutputItem.ymin = (Math.max(0, yPos - h / 2)) / IMG_HEIGHT;
                    newOutputItem.xmax = (Math.min(bitmap.getWidth() - 1, xPos + w / 2)) / IMG_WIDTH;
                    newOutputItem.ymax = (Math.min(bitmap.getHeight() - 1, yPos + h / 2)) / IMG_HEIGHT;

                    newOutputItem.confidence = expit(output[offset + 4]);

                    float maxClass = 0;

                    final float[] classes = new float[NUM_CLASSES];
                    for (int c = 0; c < NUM_CLASSES; ++c) {
                        classes[c] = output[offset + 5 + c];
                    }
                    softmax(classes);

                    for (int c = 0; c < NUM_CLASSES; ++c) {
                        if (classes[c] > maxClass) {
                            newOutputItem.class_number = c;
                            newOutputItem.class_name = LABELS.get((int) c);
                            maxClass = classes[c];
                        }
                    }

                    if (newOutputItem.confidence>DETECTION_THRESHOLD){
                        result.add(newOutputItem);
                    }

                }
            }
        }
        RunResult = result;
    }

    private float expit(final float x) {
        return (float) (1. / (1. + Math.exp(-x)));
    }

    private void softmax(final float[] vals) {
        float max = Float.NEGATIVE_INFINITY;
        for (final float val : vals) {
            max = Math.max(max, val);
        }
        float sum = 0.0f;
        for (int i = 0; i < vals.length; ++i) {
            vals[i] = (float) Math.exp(vals[i] - max);
            sum += vals[i];
        }
        for (int i = 0; i < vals.length; ++i) {
            vals[i] = vals[i] / sum;
        }
    }

    public void Run(Bitmap bitmap){
        Bitmap new_bitmap = ResizeAndCropBitmap(bitmap);
        //new_bitmap = ResizeAndCropBitmap(testbitmap);
        byte[] byte_array = BitmapToArray(new_bitmap);

        long startTime = System.currentTimeMillis();
        tensorFlowInferenceInterface.feed("image_tensor:0", byte_array,1L,IMG_HEIGHT, IMG_WIDTH, 3L );
        String[] OUTPUT_NAMES = {"detection_boxes:0", "detection_scores:0", "detection_classes:0", "num_detections:0"};
        tensorFlowInferenceInterface.run(OUTPUT_NAMES);
        long difference = System.currentTimeMillis() - startTime;
        Log.i("TENSORFLOW_TIME:", Long.toString(difference));

        float[] result_size_array = {1};
        tensorFlowInferenceInterface.fetch("num_detections:0", result_size_array);
        int result_size = (int) result_size_array[0];

        List<ClassifierOutputItem> output = new ArrayList<>();
        if (result_size>0) {
            float[] result_boxes = new float[MAX_DETECTION * 4];
            float[] result_scores = new float[MAX_DETECTION];
            float[] result_classes = new float[MAX_DETECTION];
            tensorFlowInferenceInterface.fetch("detection_boxes:0", result_boxes);
            tensorFlowInferenceInterface.fetch("detection_scores:0", result_scores);
            tensorFlowInferenceInterface.fetch("detection_classes:0", result_classes);
            for (int i = 0; i < result_size; i++) {
                float resultConfidence = result_scores[i];
                if(result_scores[i] > DETECTION_THRESHOLD){
                    ClassifierOutputItem newOutputItem = new ClassifierOutputItem();
                    newOutputItem.class_number = (int) result_classes[i];
                    newOutputItem.class_name = LABELS.get((int) result_classes[i]);
                    newOutputItem.confidence = result_scores[i];
                    newOutputItem.ymin = (result_boxes[i*4 + 0]);
                    newOutputItem.xmin = (result_boxes[i*4 + 1]);
                    newOutputItem.ymax = (result_boxes[i*4 + 2]);
                    newOutputItem.xmax = (result_boxes[i*4 + 3]);
                    output.add(newOutputItem);
                }
            }
        }
        RunResult = output;
        return;
    }

    // =============================================================================================
    // BITMAP HANDLING
    // =============================================================================================
    public Bitmap DrawBoxOnBitmap(Bitmap bitmap, int xmin, int xmax, int ymin, int ymax, int color, int stroke){
        Bitmap nBitmap = bitmap.copy(bitmap.getConfig(), true);
        Canvas canvas = new Canvas(nBitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(stroke);

        canvas.drawLine(xmin,ymin,xmax,ymin,paint);
        canvas.drawLine(xmax,ymin,xmax,ymax,paint);
        canvas.drawLine(xmax,ymax,xmin,ymax,paint);
        canvas.drawLine(xmin,ymax,xmin,ymin,paint);

        return nBitmap;
    }

    private byte[] BitmapToArray(Bitmap bitmap){
        int[] intValues = new int[IMG_WIDTH * IMG_HEIGHT];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        byte[] byteValues = new byte[IMG_WIDTH * IMG_HEIGHT * 3];

        for (int i = 0; i < intValues.length; ++i) {
            byteValues[i * 3 + 2] = (byte) (intValues[i] & 0xFF);
            byteValues[i * 3 + 1] = (byte) ((intValues[i] >> 8) & 0xFF);
            byteValues[i * 3] = (byte) ((intValues[i] >> 16) & 0xFF);
        }

        return byteValues;
    }

    private float[] BitmapToFloatArray(Bitmap bitmap){
        int[] intValues = new int[IMG_WIDTH * IMG_HEIGHT];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        float[] floatValues = new float[IMG_WIDTH * IMG_HEIGHT * 3];

        for (int i = 0; i < intValues.length; ++i) {
            floatValues[i * 3 + 0] = ((intValues[i] >> 16) & 0xFF) / 255.0f;
            floatValues[i * 3 + 1] = ((intValues[i] >> 8) & 0xFF) / 255.0f;
            floatValues[i * 3 + 2] = (intValues[i] & 0xFF) / 255.0f;
        }
        return floatValues;
    }

    public Bitmap ResizeAndCropBitmap (Bitmap bitmap){
        // Resizes and crops a bitmap to get the correct size to feed
        // to the tensorflow image classifier.
        // The result is another bitmap with size W * H as follows

        int W = bitmap.getWidth();
        int H = bitmap.getHeight();

        float aspectRatio = IMG_WIDTH * 1.0f / IMG_HEIGHT;

        if(H>W){
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, W, H, matrix, true);
            W = bitmap.getWidth();
            H = bitmap.getHeight();
        }

        if(aspectRatio > W/H){
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, W, (int)(W/aspectRatio));
        } else {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, (int)(H*aspectRatio), H);
        }

        W = bitmap.getWidth();
        H = bitmap.getHeight();

        // 3) Scale image to final size (864x486)
        bitmap = Bitmap.createScaledBitmap(bitmap, IMG_WIDTH, IMG_HEIGHT, true);
        return bitmap;
    }

}

//
// The TensorFlowClassifier runs the model and gives as output a list
// of ClassifierOutputItem. Each item in this list corresponds
// to an object that was recognized in the input image.//
class ClassifierOutputItem{
    String class_name;
    int class_number;
    float xmin;
    float xmax;
    float ymin;
    float ymax;
    float confidence;
}