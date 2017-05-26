package com.example.asus.androidlivephoto;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.PorterDuff;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;
import com.dropbox.client2.session.TokenPair;
import com.example.asus.androidlivephoto.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

public class ResultViewer extends Activity {
    ProgressDialog progressDialog;
    GifMovieView gifMovieView;
    String root = Environment.getExternalStorageDirectory().toString();
    File myDir = new File(root + "/req_images");
    Movie movie;
    private DropboxAPI dropboxApi;
    private boolean isUserLoggedIn;
    private Button loginButton,uploadbutton;
    private final static String DROPBOX_FILE_DIR = "/Gif_Files_New/";
    private final static String DROPBOX_NAME = "dropbox_prefs";
    private final static String ACCESS_KEY = "l381r8u4x91vtk1";
    private final static String ACCESS_SECRET = "qy05atb72icv0ad";
    private final static Session.AccessType ACCESS_TYPE = Session.AccessType.DROPBOX;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_result_viewer);
        loginButton=(Button) findViewById(R.id.dropboxlogin);
        uploadbutton=(Button)findViewById(R.id.dropboxupload);
        loggedIn(false);
        movie=Movie.decodeFile(myDir+"/PhotoGif"+getIntent().getExtras().getLong("counter")+".gif");
        gifMovieView=(GifMovieView)findViewById(R.id.gif1);
        gifMovieView.setPaused(!gifMovieView.isPaused());
        gifMovieView.setMovie(movie);
        addImageToGallery(myDir+"/PhotoGif"+getIntent().getExtras().getLong("counter")+".gif",this);

        AppKeyPair appKeyPair = new AppKeyPair(ACCESS_KEY, ACCESS_SECRET);
        AndroidAuthSession session;

        SharedPreferences prefs = getSharedPreferences(DROPBOX_NAME, 0);
        String key = prefs.getString(ACCESS_KEY, null);
        String secret = prefs.getString(ACCESS_SECRET, null);

        if(key != null && secret != null) {
            AccessTokenPair token = new AccessTokenPair(key, secret);
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE, token);
        } else {
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
        }

        dropboxApi = new DropboxAPI(session);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isUserLoggedIn){
                    dropboxApi.getSession().unlink();
                    loggedIn(false);
                } else {
                    ((AndroidAuthSession) dropboxApi.getSession())
                            .startAuthentication(ResultViewer.this);
                }

            }
        });
        uploadbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isUserLoggedIn){
                UploadFile uploadFile = new UploadFile(ResultViewer.this, dropboxApi, DROPBOX_FILE_DIR);
                uploadFile.execute();
                }
                else{
                    Toast.makeText(ResultViewer.this, "You have to login first", Toast.LENGTH_SHORT).show();
                }

            }
        });


    }
    protected void onResume() {
        super.onResume();

        AndroidAuthSession session = (AndroidAuthSession)dropboxApi.getSession();
        if(session.authenticationSuccessful()) {
            try {
                session.finishAuthentication();

                TokenPair tokens = session.getAccessTokenPair();
                SharedPreferences prefs = getSharedPreferences(DROPBOX_NAME, 0);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(ACCESS_KEY, tokens.key);
                editor.putString(ACCESS_SECRET, tokens.secret);
                editor.commit();

                loggedIn(true);
            } catch (IllegalStateException e) {
                Toast.makeText(this, "Error during Dropbox auth", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onGifClick(View view){
        GifMovieView gif=(GifMovieView)view;
        gif.setPaused(!gif.isPaused());
    }
    public static void addImageToGallery(final String filePath, final Context context) {

        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/gif");
        values.put(MediaStore.MediaColumns.DATA, filePath);

        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }
    public void loggedIn(boolean userLoggedIn) {
        isUserLoggedIn = userLoggedIn;
        //     uploadFileBtn.setEnabled(userLoggedIn);

        if(isUserLoggedIn){
            uploadbutton.getBackground().clearColorFilter();
        }
        else{
            uploadbutton.getBackground().setColorFilter(Color.GRAY,PorterDuff.Mode.MULTIPLY);
        }
        loginButton.setText(userLoggedIn ? "Log out" : "Log in Dropbox");
    }


    private class UploadFile extends AsyncTask<Void, Void, Boolean> {

        private DropboxAPI dropboxApi;
        private String path;
        private Context context;

        public UploadFile(Context context, DropboxAPI dropboxApi, String path) {
            super();
            this.dropboxApi = dropboxApi;
            this.path = path;
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog=new ProgressDialog(ResultViewer.this);
            progressDialog.setMessage("Uploading File...");
            progressDialog.setIndeterminate(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {



            try {
                File filePath = new File(myDir,"PhotoGif"+getIntent().getExtras().getLong("counter")+".gif");


                FileInputStream fileInputStream = new FileInputStream(filePath);
                dropboxApi.putFile(path + "PhotoGif"+getIntent().getExtras().getLong("counter")+".gif", fileInputStream,
                        filePath.length(), null, null);

                return true;
            } catch (IOException ioe) {

            } catch (DropboxException de) {
                // TODO: handle exception
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            progressDialog.dismiss();
            if(result) {
                Toast.makeText(context, "File has been uploaded!",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Error occured while processing the upload request",
                        Toast.LENGTH_LONG).show();
            }

        }
    }
}
