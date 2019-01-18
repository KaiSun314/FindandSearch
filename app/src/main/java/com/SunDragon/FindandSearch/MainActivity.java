package com.SunDragon.FindandSearch;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    final ArrayList<String> paths = new ArrayList<>();
    boolean deleteClicked = false;
    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_PERMISSIONS = 2;
    int delPosition = -1;

    final String breaks = "!!!!!";
    StringBuilder saved = new StringBuilder();
    String[] splitString;

    Intent intent;
    String mCurrentPhotoPath;

    boolean[] toBeDeleted;

    int width, height;

    boolean deleteState = false;

    AlertDialog alertDialog;

    String newName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_PERMISSIONS);
            }
        } else {

        }

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.RECORD_AUDIO)) {

            } else {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},REQUEST_PERMISSIONS);
            }
        } else {

        }

        if(savedInstanceState != null) {
            mCurrentPhotoPath = savedInstanceState.getString("filePath");
            deleteClicked = savedInstanceState.getBoolean("deleteClicked");
            toBeDeleted = savedInstanceState.getBooleanArray("toBeDeleted");
        }

        if(deleteClicked) {
            Toolbar toolbarMain = (Toolbar) findViewById(R.id.toolbarMain);
            toolbarMain.setVisibility(View.GONE);

            Toolbar toolbarDelete = (Toolbar) findViewById(R.id.toolbarDelete);
            toolbarDelete.setVisibility(View.VISIBLE);
        }
        else {
            Toolbar toolbarMain = (Toolbar) findViewById(R.id.toolbarMain);
            toolbarMain.setVisibility(View.VISIBLE);

            Toolbar toolbarDelete = (Toolbar) findViewById(R.id.toolbarDelete);
            toolbarDelete.setVisibility(View.GONE);
        }

        try {
            InputStream inputStream = openFileInput("photoList");
            InputStreamReader reader = new InputStreamReader(inputStream);

            int data = 0;
            while(true) {
                try {
                    data = reader.read();
                    if(data == -1) break;
                    char current = (char) data;
                    saved.append(current);
                }
                catch(IOException e) {
                    data = -1;
                    e.printStackTrace();
                }
            }

            inputStream.close();
            reader.close();
        }
        catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        if(! deleteClicked) {
            try {
                intent = getIntent();
                String before = (String) intent.getStringExtra("filePath");
                delPosition = intent.getIntExtra("position", -1);

                intent.removeExtra("filePath");
                intent.removeExtra("position");

                if(before != null && ! before.isEmpty()) {
                    File file = new File("photolist");
                    file.delete();
                    saved = saved.append(before+breaks);
                    if(delPosition != -1) {
                        delPosition++;
                    }
                    FileOutputStream outputStream;
                    try {
                        outputStream = openFileOutput("photoList", MODE_PRIVATE);
                        outputStream.write(saved.toString().getBytes());
                        outputStream.close();
                    }
                    catch(FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ArrayList<Integer> toDeletePositions = new ArrayList<>();
        splitString = saved.toString().split(breaks);
        for(int i=splitString.length-1; i>=0; i--) {
            if (!splitString[i].isEmpty()) {
                File file = new File(splitString[i]);
                if(! file.exists()) {
                    toDeletePositions.add(i);
                }
                paths.add(splitString[i]);
            }
        }

        if(savedInstanceState == null) {
            toBeDeleted = new boolean[paths.size()];
        }

        if(delPosition != -1) {
            toDeletePositions.add(delPosition);
        }

        deletePhoto(toDeletePositions);

        if(savedInstanceState != null) {
            deleteState = savedInstanceState.getBoolean("deleteState", false);
            if(deleteState) {
                toDelete();
            }
        }
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {

        private Context context;
        private List<String> data;

        public RecyclerViewAdapter(Context context, List<String> data) {
            this.context = context;
            this.data = data;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            LayoutInflater mInflater = LayoutInflater.from(context);
            view = mInflater.inflate(R.layout.cardview_photo,parent,false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, final int position) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = displayMetrics.widthPixels;
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                holder.photo.getLayoutParams().width = width/3-15;
                holder.photo.getLayoutParams().height = width/3-15;
            }
            else {
                holder.photo.getLayoutParams().width = width/5-15;
                holder.photo.getLayoutParams().height = width/5-15;
            }
            if(! toBeDeleted[position]) holder.border.setBackgroundColor(ContextCompat.getColor(context,R.color.transparent));
            else holder.border.setBackgroundColor(ContextCompat.getColor(context,R.color.BlueBorder));
            Glide.with(context).load(data.get(position)).into(holder.photo);

            holder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (deleteClicked) {
                        toBeDeleted[position] = ! toBeDeleted[position];
                        if(toBeDeleted[position]) holder.border.setBackgroundColor(ContextCompat.getColor(context,R.color.BlueBorder));
                        else holder.border.setBackgroundColor(ContextCompat.getColor(context,R.color.transparent));
                    } else {
                        Intent intent = new Intent(context, findSearchActivity.class);
                        intent.putExtra("filePath", data.get(position));
                        intent.putExtra("new", false);
                        intent.putExtra("position", position);
                        intent.putExtra("original", data.get(position));
                        View view = (View) findViewById(R.id.screen);
                        int width = view.getWidth();
                        int height = view.getHeight();
                        intent.putExtra("width", width);
                        intent.putExtra("height", height);

                        startActivity(intent);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder {

            ImageView photo;
            LinearLayout border;
            CardView cardView;

            public MyViewHolder(View itemView) {
                super(itemView);

                photo = (ImageView) itemView.findViewById(R.id.photo_id);
                border = (LinearLayout) itemView.findViewById(R.id.border);
                cardView = (CardView) itemView.findViewById(R.id.cardview_id);
            }
        }
    }

    public void deletePhoto(ArrayList<Integer> positions) {
        for(int i=0; i<positions.size(); i++) {
            File newFile = new File (paths.get(positions.get(i)));
            newFile.delete();
        }

        Collections.sort(positions);
        for(int i=0; i<positions.size(); i++) {
            paths.remove(positions.get(i)-i);
        }

        RecyclerView screen = (RecyclerView) findViewById(R.id.recyclerview_id);
        RecyclerViewAdapter myAdapter = new RecyclerViewAdapter(this,paths);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            screen.setLayoutManager(new GridLayoutManager(this, 3));
        }
        else {
            screen.setLayoutManager(new GridLayoutManager(this, 5));
        }
        screen.setAdapter(myAdapter);

        saved = new StringBuilder();
        for(int i=paths.size()-1; i>=0; i--) {
            saved = saved.append(paths.get(i)+breaks);
        }

        File file = new File("photolist");
        file.delete();
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput("photoList", MODE_PRIVATE);
            outputStream.write(saved.toString().getBytes());
            outputStream.close();
        }
        catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("filePath", mCurrentPhotoPath);
        outState.putBoolean("deleteClicked", deleteClicked);
        outState.putBooleanArray("toBeDeleted", toBeDeleted);
        outState.putInt("width", width);
        outState.putInt("height", height);
        outState.putBoolean("deleteState", deleteState);
        outState.putString("newName", newName);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mCurrentPhotoPath = savedInstanceState.getString("filePath");
        deleteClicked = savedInstanceState.getBoolean("deleteClicked");
        toBeDeleted = savedInstanceState.getBooleanArray("toBeDeleted");
        width = savedInstanceState.getInt("width");
        height = savedInstanceState.getInt("height");
        deleteState = savedInstanceState.getBoolean("deleteState");
        newName = savedInstanceState.getString("newName");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            Intent intent = new Intent(this, findSearchActivity.class);
            mCurrentPhotoPath = newName;
            intent.putExtra("filePath", mCurrentPhotoPath);
            intent.putExtra("new", true);
            intent.putExtra("width", width);
            intent.putExtra("height", height);

            startActivity(intent);
        }
    }

    public void addMain(View view) {
        TakePicture takePicture = new TakePicture(this,this);
        try {
            File imageFile = takePicture.createImageFile();
            newName = imageFile.getAbsolutePath();
            View v = (View) findViewById(R.id.screen);
            width = v.getWidth();
            height = v.getHeight();
            takePicture.dispatchTakePictureIntent(imageFile);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void selectMain(View view) {
        deleteClicked = true;

        Toolbar toolbarMain = (Toolbar) findViewById(R.id.toolbarMain);
        toolbarMain.setVisibility(View.GONE);

        Toolbar toolbarDelete = (Toolbar) findViewById(R.id.toolbarDelete);
        toolbarDelete.setVisibility(View.VISIBLE);
    }

    public void cancelDelete(View view) {
        deleteClicked = false;
        for(int i=0; i<paths.size(); i++) {
            toBeDeleted[i] = false;
        }

        Toolbar toolbarMain = (Toolbar) findViewById(R.id.toolbarMain);
        toolbarMain.setVisibility(View.VISIBLE);

        Toolbar toolbarDelete = (Toolbar) findViewById(R.id.toolbarDelete);
        toolbarDelete.setVisibility(View.GONE);

        RecyclerView screen = (RecyclerView) findViewById(R.id.recyclerview_id);
        RecyclerViewAdapter myAdapter = new RecyclerViewAdapter(this,paths);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            screen.setLayoutManager(new GridLayoutManager(this, 3));
        }
        else {
            screen.setLayoutManager(new GridLayoutManager(this, 5));
        }
        screen.setAdapter(myAdapter);
    }

    public void deleteDelete(View view) {
        deleteState = true;
        toDelete();
    }

    public void toDelete() {
        final ArrayList<Integer> toDeletePositions = new ArrayList<>();
        for(int i=0; i<paths.size(); i++) {
            if(toBeDeleted[i]) {
                toDeletePositions.add(i);
            }
        }

        if(toDeletePositions.size() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("These photos will be deleted from Find and Search on this device").setTitle("Delete photos");
            int size = toDeletePositions.size();
            builder.setPositiveButton("Delete " + (size==1?"":size+" ") + "Photo" + (size==1?"":"s"), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    deleteClicked = false;
                    for(int i=0; i<paths.size(); i++) {
                        toBeDeleted[i] = false;
                    }
                    deletePhoto(toDeletePositions);

                    Toolbar toolbarMain = (Toolbar) findViewById(R.id.toolbarMain);
                    toolbarMain.setVisibility(View.VISIBLE);

                    Toolbar toolbarDelete = (Toolbar) findViewById(R.id.toolbarDelete);
                    toolbarDelete.setVisibility(View.GONE);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                }
            });
            alertDialog = builder.create();
            alertDialog.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        ArrayList<Integer> toBeDeleted = new ArrayList<>();
        for(int i=0; i<paths.size(); i++) {
            File file = new File(paths.get(i));
            if(! file.exists()) {
                toBeDeleted.add(i);
            }
        }

        if(toBeDeleted.size() > 0) {
            deletePhoto(toBeDeleted);
        }
    }

    @Override
    public void onBackPressed() {

    }

    public void deleteCache() {
        try {
            deleteDir(getCacheDir());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean deleteDir(File file) {
        if (file != null && file.isDirectory()) {
            for (String child : file.list()) {
                boolean deleted = deleteDir(new File(file, child));
                if (! deleted) return false;
            }
            return file.delete();
        } else if (file != null && file.isFile()) {
            return file.delete();
        } else {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        deleteCache();
    }
}
