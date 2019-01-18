package com.SunDragon.FindandSearch;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

public class findSearchActivity extends AppCompatActivity {

    String mCurrentPhotoPath;
    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_MICROPHONE = 0;
    int position = -1;
    String saved = "";
    String originalSaved = "";
    boolean retake = false;
    boolean newPhoto;
    int deletePosition = -1;
    ImageView mImageView;
    boolean listened = false;
    int prevx, prevy;
    int[] location = new int[2];
    long lastTouchTime;
    boolean finishedBackground = false;

    int[][] words;

    int state = 1;

    StringBuilder textInImage = new StringBuilder();
    StringBuilder[] blocks;
    StringBuilder[] lines;
    ArrayList<Integer> blockOrder = new ArrayList<>();
    ArrayList<ArrayList<Integer>> lineOrder = new ArrayList<>();
    ArrayList<ArrayList<Integer>> wordOrder = new ArrayList<>();
    int[] charToWordNum;

    ArrayList<TextView> highlighted = new ArrayList<>();
    Bitmap bitmap;

    int targetW, targetH;
    int width, height, newWidth, newHeight;

    boolean windowChanged = false;

    String newName = "";

    ArrayList<String> map = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_find_search);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        mCurrentPhotoPath = (String) intent.getStringExtra("filePath");
        newPhoto = (boolean) intent.getBooleanExtra("new", true);
        position = (int) intent.getIntExtra("position", -1);
        saved = (String) intent.getStringExtra("saved");
        originalSaved = (String) intent.getStringExtra("original");
        deletePosition = (int) intent.getIntExtra("deletePosition", -1);
        retake = (boolean) intent.getBooleanExtra("retake", false);
        width = (int) intent.getIntExtra("width", -1);
        height = (int) intent.getIntExtra("height", -1);

        EditText editText = (EditText) findViewById(R.id.editText);
        editText.addTextChangedListener(textWatcher);

        mImageView = (ImageView) findViewById(R.id.imageViewPhoto);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if(windowChanged) return;
        windowChanged = true;
        super.onWindowFocusChanged(hasFocus);

        View v = (View) findViewById(R.id.screen);
        newWidth = v.getWidth();
        newHeight = v.getHeight();

        // Get the dimensions of the View
        targetW = mImageView.getWidth()+(width==-1?0:-newWidth+width);
        targetH = mImageView.getHeight()+(height==-1?0:-newHeight+height);

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions), targetW, targetH, false);
        mImageView.setImageBitmap(bitmap);

        WordRecognizer wordRecognizer = new WordRecognizer();
        wordRecognizer.execute();
    }

    private class WordRecognizer extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            words = new int[targetH+1][targetW+1];
            for(int i=0; i<=targetH; i++) {
                for(int j=0; j<=targetW; j++) {
                    words[i][j] = -1;
                }
            }

            TextRecognizer detector = new TextRecognizer.Builder(findSearchActivity.this).build();
            if(detector.isOperational() && bitmap != null) {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<TextBlock> textBlocks = detector.detect(frame);
                int numBlocks = textBlocks.size();
                int numLines = 0;
                int cnt = 0;
                final ConstraintLayout screen = (ConstraintLayout) findViewById(R.id.screen);
                for (int index = 0; index < numBlocks; index++) {
                    TextBlock tBlock = textBlocks.valueAt(index);
                    for (Text line : tBlock.getComponents()) {
                        for (Text element : line.getComponents()) {
                            String word = element.getValue();
                            if(word == null || word.isEmpty()) continue;
                            map.add(word);
                            Rect boundingBox = element.getBoundingBox();
                            final int bottom = boundingBox.bottom;
                            final int top = boundingBox.top;
                            final int left = boundingBox.left;
                            final int right = boundingBox.right;
                            for (int i = Math.max(top, 0); i <= Math.min(bottom, targetH); i++) {
                                for (int j = Math.max(left, 0); j <= Math.min(right, targetW); j++) {
                                    words[i][j] = cnt;
                                }
                            }

                            final int cntCopy = cnt;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    highlighted.add(new TextView(findSearchActivity.this));
                                    highlighted.get(cntCopy).setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT));
                                    highlighted.get(cntCopy).setBackgroundColor(ContextCompat.getColor(findSearchActivity.this, R.color.Highlighter));
                                    highlighted.get(cntCopy).setY(top + mImageView.getTop());
                                    highlighted.get(cntCopy).setHeight(bottom - top);
                                    highlighted.get(cntCopy).setX(left + mImageView.getLeft());
                                    highlighted.get(cntCopy).setWidth(right - left);
                                    highlighted.get(cntCopy).setAlpha(0f);

                                    if (highlighted.get(cntCopy).getParent() != null) {
                                        ((ViewGroup) highlighted.get(cntCopy).getParent()).removeView(highlighted.get(cntCopy));
                                    }
                                    screen.addView(highlighted.get(cntCopy));
                                }
                            });

                            cnt++;
                        }
                        numLines++;
                    }
                }

                blocks = new StringBuilder[numBlocks];
                lines = new StringBuilder[numLines];
                cnt = 0;
                int lineNum = 0;
                for (int index = 0; index < numBlocks; index++) {
                    TextBlock tBlock = textBlocks.valueAt(index);

                    ArrayList<Pair<Integer, Integer>> linePos = new ArrayList<>();
                    for (Text line : tBlock.getComponents()) {
                        lines[lineNum] = new StringBuilder();
                        wordOrder.add(new ArrayList<Integer>());
                        for (Text element : line.getComponents()) {
                            String word = element.getValue();
                            if(word == null || word.isEmpty()) continue;
                            lines[lineNum].append(map.get(cnt) + " ");
                            wordOrder.get(lineNum).add(cnt);
                            cnt++;
                        }
                        linePos.add(new Pair<>(line.getBoundingBox().bottom + line.getBoundingBox().top, lineNum));
                        lineNum++;
                    }
                    Collections.sort(linePos, new Comparator<Pair<Integer, Integer>>() {
                        @Override
                        public int compare(final Pair<Integer, Integer> a, final Pair<Integer, Integer> b) {
                            if (a.first < b.first || (a.first == b.first && a.second < b.second)) {
                                return -1;
                            } else if (a.first == b.first && a.second == b.second) {
                                return 0;
                            } else {
                                return 1;
                            }
                        }
                    });

                    lineOrder.add(new ArrayList<Integer>());
                    blocks[index] = new StringBuilder();
                    for (int i = 0; i < linePos.size(); i++) {
                        blocks[index].append(lines[linePos.get(i).second]);
                        for (int j = 0; j < wordOrder.get(i + lineNum - linePos.size()).size(); j++) {
                            lineOrder.get(index).add(wordOrder.get(i + lineNum - linePos.size()).get(j));
                        }
                    }
                }

                int[] nxt = new int[numBlocks];
                int[] prev = new int[numBlocks];
                for (int i = 0; i < numBlocks; i++) {
                    nxt[i] = -1;
                    prev[i] = -1;
                }
                int[][] pos = new int[numBlocks][4];
                for (int i = 0; i < numBlocks; i++) {
                    TextBlock tBlock = textBlocks.valueAt(i);
                    pos[i][0] = tBlock.getBoundingBox().bottom;
                    pos[i][1] = tBlock.getBoundingBox().top;
                    pos[i][2] = tBlock.getBoundingBox().left;
                    pos[i][3] = tBlock.getBoundingBox().right;
                }
                for (int i = 0; i < numBlocks; i++) {
                    int minX = -1;
                    for (int j = 0; j < numBlocks; j++) {
                        if (pos[i][2] + pos[i][3] < pos[j][2] + pos[j][3] &&
                                (float) Math.min(pos[j][0] - pos[i][1], pos[i][0] - pos[j][1]) >= 0.6 * (float) Math.min(pos[i][0] - pos[i][1], pos[j][0] - pos[j][1])) {
                            if (minX == -1 || pos[j][2] + pos[j][3] < pos[minX][2] + pos[minX][3]) {
                                minX = j;
                            }
                        }
                    }
                    if (minX != -1) {
                        nxt[i] = minX;
                        prev[minX] = i;
                    }
                }
                ArrayList<Pair<Integer, Integer>> blockPos = new ArrayList<>();
                for (int i = 0; i < numBlocks; i++) {
                    if (prev[i] == -1) {
                        blockPos.add(new Pair<>(pos[i][0] + pos[i][1], i));
                    }
                }
                Collections.sort(blockPos, new Comparator<Pair<Integer, Integer>>() {
                    @Override
                    public int compare(final Pair<Integer, Integer> a, final Pair<Integer, Integer> b) {
                        if (a.first < b.first || (a.first == b.first && a.second < b.second)) {
                            return -1;
                        } else if (a.first == b.first && a.second == b.second) {
                            return 0;
                        } else {
                            return 1;
                        }
                    }
                });

                for (int i = 0; i < blockPos.size(); i++) {
                    int cur = blockPos.get(i).second;
                    while (cur != -1) {
                        textInImage.append(blocks[cur]);
                        for (int j = 0; j < lineOrder.get(cur).size(); j++) {
                            blockOrder.add(lineOrder.get(cur).get(j));
                        }
                        cur = nxt[cur];
                    }
                }

                charToWordNum = new int[textInImage.length()];
                int wordNum = 0;
                for (int i = 0; i < textInImage.length(); i++) {
                    charToWordNum[i] = wordNum;
                    if (textInImage.charAt(i) == ' ') {
                        wordNum++;
                    }
                }
            }

            finishedBackground = true;

            return null;
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if(finishedBackground) {
            long currentTime = System.currentTimeMillis();
            View v = (View) findViewById(R.id.screen);
            v.getLocationOnScreen(location);
            int x = (int) (event.getRawX()-mImageView.getLeft()-location[0]);
            int y = (int) (event.getRawY()-mImageView.getTop()-location[1]);
            String prevWord = "";
            if(prevx >= 0 && prevx <= mImageView.getWidth() && prevy >= 0 && prevy <= mImageView.getHeight() && words[prevy][prevx] != -1) {
                prevWord = map.get(words[prevy][prevx]);
            }
            EditText editText = (EditText) findViewById(R.id.editText);
            if(x >= 0 && x <= mImageView.getWidth() && y >= 0 && y <= mImageView.getHeight() && words[y][x] != -1 && (prevWord != map.get(words[y][x]) || currentTime-lastTouchTime > 1000)) {
                boolean empty = editText.getText().toString().isEmpty();
                editText.setText(editText.getText().toString().concat((empty?"":" ") + map.get(words[y][x])));
                editText.setSelection(editText.length());
            }
            prevx = x;
            prevy = y;
            lastTouchTime = currentTime;
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("filePath", mCurrentPhotoPath);
        outState.putString("saved", saved);
        outState.putString("original", originalSaved);
        EditText editText = (EditText) findViewById(R.id.editText);
        outState.putString("searchWord", editText.getText().toString());
        outState.putInt("position", position);
        outState.putInt("deletePosition", deletePosition);
        outState.putBoolean("retake", retake);
        outState.putInt("width", width);
        outState.putInt("height", height);
        outState.putInt("newWidth", newWidth);
        outState.putInt("newHeight", newHeight);
        outState.putBoolean("finishedBackground", finishedBackground);
        outState.putString("newName", newName);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mCurrentPhotoPath = savedInstanceState.getString("filePath");
        saved = savedInstanceState.getString("saved");
        originalSaved = savedInstanceState.getString("original");
        EditText editText = (EditText) findViewById(R.id.editText);
        editText.setText(savedInstanceState.getString("searchWord"));
        position = savedInstanceState.getInt("position");
        deletePosition = savedInstanceState.getInt("deletePosition");
        retake = savedInstanceState.getBoolean("retake");
        width = savedInstanceState.getInt("width");
        height = savedInstanceState.getInt("height");
        newWidth = savedInstanceState.getInt("newWidth");
        newHeight = savedInstanceState.getInt("newHeight");
        finishedBackground = savedInstanceState.getBoolean("finishedBackground");
        newName = savedInstanceState.getString("newName");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.save) {
            if(saved != null && ! saved.isEmpty()) {
                File newFile = new File (saved);
                newFile.delete();
            }
            saved = mCurrentPhotoPath;
        } else if (id == R.id.exit) {
            exitActivity();
        } else if (id == R.id.saveAndExit) {
            if(! retake) {
                if(newPhoto) {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("filePath", mCurrentPhotoPath);
                    startActivity(intent);
                }
                else {
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                }
            }
            else {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("position", deletePosition);
                intent.putExtra("filePath", mCurrentPhotoPath);
                startActivity(intent);
            }
        } else if (id == R.id.saveToGallery) {
            galleryAddPic task = new galleryAddPic();
            task.execute(mCurrentPhotoPath);
        } else if (id == R.id.retakePhoto) {
            TakePicture takePicture = new TakePicture(this,this);
            try {
                File imageFile = takePicture.createImageFile();
                newName = imageFile.getAbsolutePath();
                takePicture.dispatchTakePictureIntent(imageFile);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        } else if (id == R.id.deletePhoto) {
            if(! mCurrentPhotoPath.equals(originalSaved)) {
                File newFile = new File (mCurrentPhotoPath);
                newFile.delete();
            }
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("position", position);
            startActivity(intent);
        } else if (id == R.id.copyText) {
            EditText editText = (EditText) findViewById(R.id.editText);
            String text = editText.getText().toString();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(text, text);
            clipboard.setPrimaryClip(clip);
        }

        return super.onOptionsItemSelected(item);
    }

    public void find(View view) {
        Button findButton = (Button) findViewById(R.id.find);
        Button searchButton = (Button) findViewById(R.id.search);

        searchButton.setBackgroundColor(ContextCompat.getColor(this,R.color.lightGray));
        findButton.setBackgroundColor(ContextCompat.getColor(this,R.color.darkGray));

        state = 0;
        EditText editText = (EditText) findViewById(R.id.editText);
        KMPalgorithm(editText.getText().toString());
    }

    public void search(View view) {
        Button findButton = (Button) findViewById(R.id.find);
        Button searchButton = (Button) findViewById(R.id.search);

        findButton.setBackgroundColor(ContextCompat.getColor(this,R.color.lightGray));
        searchButton.setBackgroundColor(ContextCompat.getColor(this,R.color.darkGray));

        state = 1;
        KMPalgorithm("");
    }

    public void completeSearch(View view) {
        if(state == 1) {
            EditText userSearch = (EditText) findViewById(R.id.editText);

            Intent intent = new Intent(this, WebViewActivity.class);
            intent.putExtra("url", userSearch.getText().toString());

            startActivity(intent);
        }

    }

    public void microphone(View view) {
        ImageView microphoneButton = (ImageView) findViewById(R.id.microphone);
        microphoneButton.setImageResource(R.drawable.baseline_mic_black_18dp);

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent,REQUEST_MICROPHONE);
    }

    public void cancel(View view) {
        EditText editText = (EditText) findViewById(R.id.editText);
        editText.setText("",TextView.BufferType.EDITABLE);
    }

    public char f(char c) {
        if((int)c >= 65 && (int)c <= 90) {
            return (char)(c+32);
        }
        else {
            return c;
        }
    }

    private void KMPalgorithm(String s) {
        for(int i=0; i<highlighted.size(); i++) {
            highlighted.get(i).setAlpha(0f);
        }
        int M = s.length();
        if(M == 0) return;
        int[] lps = new int[M];

        int prev = 0;
        int cur = 1;
        lps[0] = 0;
        while(cur < M) {
            if(f(s.charAt(cur)) == f(s.charAt(prev))) {
                prev++;
                lps[cur] = prev;
                cur++;
            }
            else {
                if(prev == 0) {
                    lps[cur] = 0;
                    cur++;
                }
                else {
                    prev = lps[prev-1];
                }
            }
        }

        int N = textInImage.length();
        cur = 0;
        prev = 0;
        while(cur < N) {
            if(f(textInImage.charAt(cur)) == f(s.charAt(prev))) {
                cur++; prev++;
            }

            if(prev == M) {
                int begin = charToWordNum[cur-M];
                int end = charToWordNum[cur-1];
                for(int i=begin; i<=end; i++) {
                    highlighted.get(blockOrder.get(i)).setAlpha(0.3f);
                }
                prev = lps[prev-1];
            }
            else if(cur < N && f(textInImage.charAt(cur)) != f(s.charAt(prev))) {
                if(prev == 0) {
                    cur++;
                }
                else {
                    prev = lps[prev-1];
                }
            }
        }
    }

    private TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if(state == 0) {
                KMPalgorithm(s.toString());
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private class galleryAddPic extends AsyncTask<String,Void,Void> {

        @Override
        protected Void doInBackground(String... strings) {
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = 1;
            Bitmap bitmap = BitmapFactory.decodeFile(strings[0], bmOptions);

            File dir = new File(Environment.getExternalStorageDirectory()+"/Gallery");
            dir.mkdirs();
            File file=new File(dir,System.currentTimeMillis()+".jpg");
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG,100, fileOutputStream);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    Intent intent = new Intent(this, findSearchActivity.class);
                    deletePosition = position;
                    retake = true;
                    if(! mCurrentPhotoPath.equals(originalSaved) && ! mCurrentPhotoPath.equals(saved)) {
                        File newFile = new File (mCurrentPhotoPath);
                        newFile.delete();
                    }
                    mCurrentPhotoPath = newName;
                    intent.putExtra("filePath", mCurrentPhotoPath);
                    intent.putExtra("new", false);
                    intent.putExtra("saved", saved);
                    intent.putExtra("original", originalSaved);
                    intent.putExtra("position", position);
                    intent.putExtra("deletePosition", deletePosition);
                    intent.putExtra("retake", retake);
                    intent.putExtra("width", width);
                    intent.putExtra("height", height);

                    startActivity(intent);
                }
                break;
            case REQUEST_MICROPHONE:
                if(resultCode == RESULT_OK) {
                    List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    EditText editText = (EditText) findViewById(R.id.editText);
                    String current = editText.getText().toString();
                    boolean empty = current.isEmpty();
                    editText.setText(current.concat((empty?"":" ") + results.get(0)));
                    editText.setSelection(editText.length());
                }

                ImageView microphoneButton = (ImageView) findViewById(R.id.microphone);
                microphoneButton.setImageResource(R.drawable.baseline_mic_none_black_18dp);
                break;
        }

    }

    @Override
    public void onBackPressed() {
        exitActivity();
    }

    public void exitActivity() {
        if(! retake) {
            if(newPhoto) {
                if(saved != null && ! saved.isEmpty()) {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("filePath", saved);
                    startActivity(intent);
                }
                else {
                    File newFile = new File (mCurrentPhotoPath);
                    newFile.delete();
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                }
            }
            else {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
        }
        else {
            Intent intent = new Intent(this, MainActivity.class);
            if(! mCurrentPhotoPath.equals(saved)) {
                File newFile = new File (mCurrentPhotoPath);
                newFile.delete();
            }
            if(saved != null && ! saved.equals(originalSaved)) {
                intent.putExtra("position", deletePosition);
                intent.putExtra("filePath", saved);
            }
            startActivity(intent);
        }
    }
}
