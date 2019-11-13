package com.example.myapplication;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TensorFlowLiteModel {

    int IMG_SIZE = 300;
    int NUM_CLASSES = 3;
    int BATCH_SIZE = 12;
    int NUM_DETECTIONS = 10;
    float IMAGE_MEAN = 128f;
    float IMAGE_STD = 128f;
    float DETECTION_THRESHOLD = 0.3f;
    int NUM_BOXES_PER_BLOCK = 5;
    int BLOCK_SIZE = 32;

    String MODEL_PATH = "graph.tflite";
    String LABELS_PATH = "labels.txt";
    String TEST_BITMAP_PATH = "test.jpg";

    private List<String> LABELS = new ArrayList<>();
    private Interpreter TensorFlowLiteInterpreter;
    private AssetManager assetManager;
    private Bitmap testbitmap;
    private List<ClassifierOutputItem> RunResult;

    MainActivity mainActivity;

    public void InitializeAsync(AssetManager assetManager_){
        assetManager = assetManager_;
        InitializeAsyncTask IAT = new InitializeAsyncTask(assetManager);
        IAT.execute();
    }

    private class InitializeAsyncTask extends AsyncTask<Void, Void, Void> {
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
        try {
            // LOAD MODEL
            assetManager = assetManager_;
            AssetFileDescriptor fileDescriptor = assetManager.openFd(MODEL_PATH);
            FileInputStream fileInputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = fileInputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            TensorFlowLiteInterpreter = new Interpreter(fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength));

            // READ LABELS
            InputStreamReader ISR = new InputStreamReader(assetManager.open(LABELS_PATH));
            BufferedReader BR = new BufferedReader(ISR);
            String line = "";
            while((line = BR.readLine()) != null){
                LABELS.add(line);
                NUM_CLASSES += 1;
            }
            BR.close();
            ISR.close();

            // LOAD TRAINING BITMAP
            InputStream IS = assetManager.open(TEST_BITMAP_PATH);
            testbitmap = BitmapFactory.decodeStream(IS);
        } catch (Exception ex){
            Log.i("FUCKING ERROR",ex.getMessage().toString());
        }
    }


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

    public void Run(Bitmap bitmap) {
        Bitmap new_bitmap = ResizeAndCropBitmap(bitmap);
        //Bitmap new_bitmap = ResizeAndCropBitmap(testbitmap);
        //ByteBuffer  byte_array = convertBitmapToByteBuffer(new_bitmap);

        Object[] inputArray = ConvertBitmapToObject(new_bitmap);

        float[][][] outputLocations = new float[1][NUM_DETECTIONS][4];
        float[][] outputClasses = new float[1][NUM_DETECTIONS];
        float[][] outputScores = new float[1][NUM_DETECTIONS];
        float[] numDetections = new float[1];
        Map<Integer, Object> result = new HashMap<>();
        result.put(0, outputLocations);
        result.put(1, outputClasses);
        result.put(2, outputScores);
        result.put(3, numDetections);

        long startTime = System.currentTimeMillis();
        //TensorFlowLiteInterpreter.run(float_array,result);
        TensorFlowLiteInterpreter.runForMultipleInputsOutputs(inputArray, result);
        long difference = System.currentTimeMillis() - startTime;
        Log.i("TENSORFLOW_TIME:", Long.toString(difference));

    }

    private Object[] ConvertBitmapToObject(Bitmap bitmap) {
        ByteBuffer imgData = ByteBuffer.allocateDirect(BATCH_SIZE * IMG_SIZE * IMG_SIZE);
        imgData.order(ByteOrder.nativeOrder());

        int[] intValues = new int[IMG_SIZE * IMG_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        imgData.rewind();
        for (int i = 0; i < IMG_SIZE; ++i) {
            for (int j = 0; j < IMG_SIZE; ++j) {
                int pixelValue = intValues[i * IMG_SIZE + j];
                if (false) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }

        Object[] result = new Object[1];
        result[0] = imgData;
        return result;
    }


    public Bitmap ResizeAndCropBitmap (Bitmap bitmap){
        // Resizes and crops a bitmap to get the correct size to feed
        // to the tensorflow image classifier.
        // The result is another bitmap with size W * H as follows

        int W = bitmap.getWidth();
        int H = bitmap.getHeight();

        float aspectRatio = IMG_SIZE * 1.0f / IMG_SIZE;

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
        bitmap = Bitmap.createScaledBitmap(bitmap, IMG_SIZE, IMG_SIZE, true);
        return bitmap;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BATCH_SIZE * IMG_SIZE * IMG_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[IMG_SIZE * IMG_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < IMG_SIZE; ++i) {
            for (int j = 0; j < IMG_SIZE; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.put((byte) ((val >> 16) & 0xFF));
                byteBuffer.put((byte) ((val >> 8) & 0xFF));
                byteBuffer.put((byte) (val & 0xFF));
            }
        }
        return byteBuffer;
    }

}
