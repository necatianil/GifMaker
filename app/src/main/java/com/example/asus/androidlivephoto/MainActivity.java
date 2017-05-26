package com.example.asus.androidlivephoto;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.MediaStore.Images.Media;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener,
        SurfaceHolder.Callback, Camera.PictureCallback {
    Intent intent;
    ProgressDialog progressDialog;
    SurfaceView cameraView;
    SurfaceHolder surfaceHolder;
    Camera camera;
    int currentCameraId = 0;
    ImageButton capturebutton;
    TextView countdownTextView;
    Handler timerUpdateHandler;
    boolean timelapseRunning = false;
    int currentTime = 0;
    int NUM_OF_PHOTOS;
    String root = Environment.getExternalStorageDirectory().toString();
    File myDir = new File(root + "/req_images");
    List<Bitmap> PhotoList;
   // final int SECONDS_BETWEEN_PHOTOS = 1;
    AnimatedGifEncoder encoder;
    ByteArrayOutputStream bos;
    Spinner speed_spinner,resolution_spinner;
    String spinnerselection,resolution_selection;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraView = (SurfaceView) this.findViewById(R.id.CameraView);
        surfaceHolder = cameraView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(this);
        PhotoList=new ArrayList<Bitmap>();
        speed_spinner=(Spinner)findViewById(R.id.speed_spinner);
        resolution_spinner=(Spinner)findViewById(R.id.resolution_spinner);
        ArrayAdapter<CharSequence> speed_adapter = ArrayAdapter.createFromResource(this,
                R.array.speed, android.R.layout.simple_spinner_item);
        speed_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        speed_spinner.setAdapter(new HintSpinnerAdapter(speed_adapter, R.layout.hint_row_item, this));

        ArrayAdapter<CharSequence> res_adapter = ArrayAdapter.createFromResource(this,
                R.array.resolution, android.R.layout.simple_spinner_item);
        res_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resolution_spinner.setAdapter(new HintSpinnerAdapter(res_adapter, R.layout.hint_row_item2, this));

        countdownTextView = (TextView) findViewById(R.id.CountDownTextView);
        capturebutton = (ImageButton) findViewById(R.id.CountDownButton);
        capturebutton.setOnClickListener(this);
        timerUpdateHandler = new Handler();
    }

    public void onClick(View v) {
        try{
        resolution_selection=resolution_spinner.getSelectedItem().toString();
        spinnerselection = speed_spinner.getSelectedItem().toString();

            if (!timelapseRunning) {
                timelapseRunning = true;
                timerUpdateHandler.post(timerUpdateTask);
                NUM_OF_PHOTOS = 5;
            } else {

            }
        }catch (Exception e){
            Toast.makeText(this, "Please select Resolution and Speed!", Toast.LENGTH_SHORT).show();
        }

    }

    private Runnable timerUpdateTask = new Runnable() {
        public void run() {
      //          if (currentTime < SECONDS_BETWEEN_PHOTOS) {
      //              currentTime++;
      //          } else {
      //              camera.takePicture(null, null, null, MainActivity.this);
      //              currentTime = 0;
//
      //          }
            if(NUM_OF_PHOTOS>0){
                currentTime++;
                countdownTextView.setText(""+currentTime);
                camera.takePicture(null, null, null, MainActivity.this);
                NUM_OF_PHOTOS--;
                timerUpdateHandler.postDelayed(timerUpdateTask, 2000);
            }
            else
            {
                timelapseRunning = false;
                timerUpdateHandler.removeCallbacks(timerUpdateTask);
                new gifmaker().execute();
            }

     //       timerUpdateHandler.postDelayed(timerUpdateTask, 1000);
       //     countdownTextView.setText("" + currentTime);
        }
    };

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        camera.startPreview();
    }

    public void surfaceCreated(SurfaceHolder holder) {

        camera = Camera.open();
        try {
            camera.setPreviewDisplay(holder);
            Camera.Parameters parameters = camera.getParameters();
            if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                //parameters.setPictureSize(640,480);
                parameters.set("orientation", "portrait");
                try {
                    resolution_selection=resolution_spinner.getSelectedItem().toString();
                    if(resolution_selection.equalsIgnoreCase("320x240")){
                        parameters.setPictureSize(320,240);
                    }
                    if(resolution_selection.equalsIgnoreCase("640x480")){
                        parameters.setPictureSize(640,480);
                    }
                    if(resolution_selection.equalsIgnoreCase("1280x960")){
                        parameters.setPictureSize(1280,960);
                    }
                    if(resolution_selection.equalsIgnoreCase("1920x1080")){
                        parameters.setPictureSize(1920,1080);
                    }
                }
                catch (Exception e){
                    Toast.makeText(this, "Please select resolution", Toast.LENGTH_SHORT).show();
                }
                camera.setDisplayOrientation(90);
            }
            camera.setParameters(parameters);
        } catch (IOException exception) {
            camera.release();
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
    }

    public void onPictureTaken(byte[] data, Camera camera) {
        Bitmap bitmap1;
        bitmap1= BitmapFactory.decodeByteArray(data,0,data.length);
        PhotoList.add(bitmap1);
        camera.startPreview();
    }



    public void SwapClick(View v) {

        camera.stopPreview();
        camera.release();
        if(currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
            currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        else {
            currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        camera = Camera.open(currentCameraId);
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(currentCameraId, info);
           int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
           int degrees = 0;
           switch (rotation) {
               case Surface.ROTATION_0: degrees = 0; break; //Natural orientation
               case Surface.ROTATION_90: degrees = 90; break; //Landscape left
               case Surface.ROTATION_180: degrees = 180; break;//Upside down
               case Surface.ROTATION_270: degrees = 270; break;//Landscape right
           }
           int rotate = (info.orientation - degrees + 360) % 360;

        //STEP #2: Set the 'rotation' parameter
        Camera.Parameters params = camera.getParameters();
        params.setRotation(rotate);
        try {
            resolution_selection=resolution_spinner.getSelectedItem().toString();
            if(resolution_selection.equalsIgnoreCase("320x240")){
                params.setPictureSize(320,240);
            }
            if(resolution_selection.equalsIgnoreCase("640x480")){
                params.setPictureSize(640,480);
            }
            if(resolution_selection.equalsIgnoreCase("1280x960")){
                params.setPictureSize(1280,960);
            }


        }
        catch (Exception e){
            Toast.makeText(this, "Please select resolution", Toast.LENGTH_SHORT).show();
        }


        try {
            camera.setPreviewDisplay(cameraView.getHolder());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        camera.setParameters(params);
        camera.setDisplayOrientation(90);
        camera.startPreview();
    }

    public void refresh(){
        bos = new ByteArrayOutputStream();
        encoder = new AnimatedGifEncoder();
        spinnerselection = speed_spinner.getSelectedItem().toString();
        if (spinnerselection.equalsIgnoreCase("x0.5")) {
            encoder.setDelay(1000);
        }
        if (spinnerselection.equalsIgnoreCase("x1")) {
            encoder.setDelay(500);
        }
        if (spinnerselection.equalsIgnoreCase("x2")) {
            encoder.setDelay(250);
        }
        if (spinnerselection.equalsIgnoreCase("x4")) {
            encoder.setDelay(125);

        }


        encoder.setRepeat(0);
        encoder.start(bos);
    }
    public void makegif(){
        Log.d("ad"," "+PhotoList.size());

        refresh();
        if(!myDir.exists()){
            myDir.mkdir();
        }
        for(int count=0;count<PhotoList.size();count++){
            encoder.addFrame(PhotoList.get(count));
            PhotoList.get(count).recycle();
    }
        long time=System.currentTimeMillis();
         try {
             encoder.finish();
             File filePath = new File(myDir, "PhotoGif"+time+".gif");
             FileOutputStream outputStream = new FileOutputStream(filePath);
             outputStream.write(bos.toByteArray());
             outputStream.close();
            // Toast.makeText(this, "Gif created", Toast.LENGTH_SHORT).show();
             PhotoList.clear();
         }catch (IOException e) {
             e.printStackTrace();
         }
        intent=new Intent(MainActivity.this,ResultViewer.class);
        intent.putExtra("counter",time);
    }


    private class gifmaker extends AsyncTask<Void,Void,Void>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            camera.stopPreview();
            progressDialog=new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Gif is creating " +
                    "Please Wait...");
            progressDialog.setIndeterminate(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            makegif();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            currentTime=0;
            countdownTextView.setText(""+currentTime);
            Toast.makeText(MainActivity.this, "Gif is created", Toast.LENGTH_SHORT).show();
            startActivity(intent);

        }
    }
}

