package com.SunDragon.FindandSearch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;

import java.io.File;

public class TakePicture {
    Context mContext;
    Activity mActivity;
    static int REQUEST_TAKE_PHOTO = 1;

    public TakePicture(Context context, Activity activity) {
        mContext = context;
        mActivity = activity;
    }

    protected File createImageFile() {
        File dir = new File(Environment.getExternalStorageDirectory()+"/Photos");
        dir.mkdirs();
        File imageFile = new File(dir, "."+Long.toString(System.currentTimeMillis())+".jpg");
        return imageFile;
    }
    protected void dispatchTakePictureIntent(File imageFile) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri photoURI = Uri.fromFile(imageFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        mActivity.startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
    }
}
