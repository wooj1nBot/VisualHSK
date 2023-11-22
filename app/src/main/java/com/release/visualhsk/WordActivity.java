package com.release.visualhsk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import com.release.visualhsk.dto.Level;
import com.release.visualhsk.dto.User;
import com.release.visualhsk.dto.Word;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

public class WordActivity extends AppCompatActivity {

    TextView tv_level, tv_word, tv_sound, tv_count, tv_time, tv_mean, tv_dialog_time, tv_read, tv_date;
    ImageView iv_star, iv_tts, iv_back, iv_check;
    CardView list, next, prev;

    public static final int WORD_MODE_DEFAULT = 0;
    public static final int WORD_MODE_WRONG = 1;
    public static final int WORD_MODE_REVIEW = 2;
    public static final int WORD_MODE_STAR = 3;

    Level level;
    Word word;
    User user;

    Sheet sheet;

    TextToSpeech tts;

    long startTime = 0;

    private Timer appTimer;

    String time;
    int pos;

    boolean isList;

    int mode;

    ArrayList<Word> words;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word);

        tv_level = findViewById(R.id.tv_title);
        tv_word = findViewById(R.id.word);
        tv_sound = findViewById(R.id.word2);
        tv_count = findViewById(R.id.count);
        tv_time = findViewById(R.id.time);
        tv_read = findViewById(R.id.read);
        tv_date = findViewById(R.id.count4);

        tv_mean = findViewById(R.id.mean);
        iv_star = findViewById(R.id.star);
        iv_tts = findViewById(R.id.tts);
        iv_back = findViewById(R.id.back);
        iv_check = findViewById(R.id.check);

        list = findViewById(R.id.list);
        next = findViewById(R.id.next);
        prev = findViewById(R.id.prev);

        Intent intent = getIntent();
        int l = intent.getIntExtra("level", 1);
        mode = intent.getIntExtra("mode", WORD_MODE_DEFAULT);
        pos = intent.getIntExtra("pos", 1);

        iv_star.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                level.setWordStar(word.pos, WordActivity.this);
                if(level.getWordStar(word.pos)){
                    iv_star.setImageResource(R.drawable.star_on);
                }else{
                    iv_star.setImageResource(R.drawable.star_off);
                }
            }
        });

        tts = new TextToSpeech(WordActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR){
                    tts.setLanguage(Locale.CHINA);
                }
            }
        });

        iv_tts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tts.speak(word.word, TextToSpeech.QUEUE_FLUSH, null);
            }
        });

        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (word.pos == 1){
                    getLevel(level.level-1, true, true);
                }else{
                    if (mode == WORD_MODE_DEFAULT) {
                        getWord(sheet, level.level, word.pos - 1);
                    }else{
                        getWord(words, --pos);
                    }
                }
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (word.pos == level.count){
                    pos = 1;
                    getLevel(level.level+1, true, false);
                }else{
                    if (mode == WORD_MODE_DEFAULT) {
                        getWord(sheet, level.level, word.pos + 1);
                    }else{
                        getWord(words, ++pos);
                    }
                }
            }
        });

        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });

        list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isList = true;
                Intent intent = new Intent(WordActivity.this, WordListActivity.class);
                intent.putExtra("level", level.level);
                intent.putExtra("current", word.pos);
                intent.putExtra("mode", mode);
                startActivityForResult(intent, 1112);
            }
        });
        startTime = System.currentTimeMillis();
        startTimer();

        getLevel(l, false, false);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isList){
            startTime = System.currentTimeMillis();
        }
        isList = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isList){
            user.addStudyTime(WordActivity.this, System.currentTimeMillis()-startTime);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            pos = data.getIntExtra("pos", 1);
            getLevel(level.level, true, false);
        }
    }

    public void getLevel(int level_pos, boolean isNew, boolean isPrev){

        if (mode == WORD_MODE_DEFAULT) {
            tv_level.setText("HSK " + level_pos + "급");
        }else{
            if (mode == WORD_MODE_WRONG){
                tv_level.setText("틀린 단어");
            }
            if (mode == WORD_MODE_REVIEW){
                tv_level.setText("복습 단어");
            }
            if (mode == WORD_MODE_STAR){
                tv_level.setText("별표 친 단어");
            }
        }

        AssetManager manager = getResources().getAssets();

        try {
            String file = String.format("hsk%d.xls", level_pos);
            InputStream inputStream = manager.open("res/" + file);
            WorkbookSettings setting = new WorkbookSettings();
            setting.setEncoding("Cp1252");

            Workbook wb = Workbook.getWorkbook(inputStream, setting);
            sheet = wb.getSheet(0);
            int rowTotal = sheet.getColumn(0).length;
            level = new Level(level_pos, rowTotal);
            if (isPrev){
                pos = rowTotal;
            }
        } catch (IOException | BiffException e) {

        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(User.getId(this)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult().exists()){
                    user = task.getResult().toObject(User.class);
                    if (user.levels != null && user.levels.containsKey(String.valueOf(level_pos))){
                        level = user.levels.get(String.valueOf(level_pos));
                    }

                }

                if (mode == WORD_MODE_DEFAULT) {
                    if (isNew) {
                        getWord(sheet, level.level, pos);
                    } else {
                        getWord(sheet, level.level, level.current_pos);
                    }
                }else{
                    if (mode == WORD_MODE_WRONG){
                        words = level.getWrongDataSet(sheet, WordActivity.this);
                    }
                    if (mode == WORD_MODE_REVIEW){
                        words = level.getReviewDataSet(sheet);
                    }
                    if (mode == WORD_MODE_STAR){
                        words = level.getStarDataSet(sheet);
                    }
                    getWord(words, pos);
                }

            }
        });

    }



    public void startTimer(){
        TimerTask purposeTask = new TimerTask() {
            @Override
            public void run() {
                long current = System.currentTimeMillis();
                long diff = current - startTime;

                long diffHor = (diff) / 3600000; //시 차이
                diff %= 3600000;
                long diffMin = (diff) / 60000; //분 차이
                diff %= 60000;
                long diffSec = (diff) / 1000; //초 차이

                time = String.format("%02d : %02d : %02d", diffHor, diffMin, diffSec);
                tv_time.setText(time);
                if (tv_dialog_time != null){
                    tv_dialog_time.setText(time);
                }
            }
        };

        appTimer = new Timer();
        appTimer.schedule(purposeTask, 0, 200);
    }

    public void stopTimer(){
        if(appTimer != null){
            appTimer.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        user.addStudyTime(WordActivity.this, System.currentTimeMillis()-startTime);
    }

    @Override
    public void onBackPressed() {
        showDialog();
    }

    public void getWord(ArrayList<Word> words, int pos){
        word = words.get(pos-1);
        word.saveWord(WordActivity.this);

        tv_word.setText(word.word);
        tv_sound.setText(String.format("[ %s ]", word.voice));

        String[] means = word.mean.split("\\|");
        StringBuilder m = new StringBuilder();
        for (int i = 1; i <= means.length; i++) {
            m.append(String.format("%d. %s\n", i, means[i - 1]));
        }

        tv_mean.setText(m);
        tv_count.setText(String.format("%d / %d", pos, words.size()));

        if (pos == words.size()){
            next.setCardBackgroundColor(Color.parseColor("#CACACA"));
            next.setClickable(false);
        }else{
            next.setCardBackgroundColor(Color.parseColor("#FFEA66"));
            next.setClickable(true);
        }

        if (pos == 1){
            prev.setCardBackgroundColor(Color.parseColor("#CACACA"));
            prev.setClickable(false);
        }else{
            prev.setCardBackgroundColor(Color.parseColor("#FFEA66"));
            prev.setClickable(true);
        }

        if(this.level.getWordStar(word.pos)){
            iv_star.setImageResource(R.drawable.star_on);
        }else{
            iv_star.setImageResource(R.drawable.star_off);
        }

        if (this.level.getIsComplete(word.pos, this)){
            iv_check.setVisibility(View.VISIBLE);
        }else{
            iv_check.setVisibility(View.GONE);
        }

        if (this.level.word_read != null){
            tv_read.setText(this.level.word_read.getOrDefault(String.valueOf(word.pos), 0) + "");
        }else{
            tv_read.setText(String.valueOf(0));
        }

        if (this.level.read_date.containsKey(String.valueOf(word.pos))){
            SimpleDateFormat format = new SimpleDateFormat("yyyy년 MM월 dd일");
            tv_date.setText(String.format("마지막으로 읽은 날짜\n%s", format.format(this.level.read_date.get(String.valueOf(word.pos)))));
        }else{
            tv_date.setVisibility(View.GONE);
        }

        this.level.readWord(word.pos, this);
    }

    public void getWord(Sheet sheet, int level, int pos){
        String w = sheet.getCell(0, pos-1).getContents();
        String voice = sheet.getCell(1, pos-1).getContents();
        String mean = sheet.getCell(2, pos-1).getContents();

        word = new Word(level, pos, w, voice, mean);
        word.saveWord(WordActivity.this);

        tv_word.setText(word.word);
        tv_sound.setText(String.format("[ %s ]", word.voice));

        String[] means = word.mean.split("\\|");
        StringBuilder m = new StringBuilder();
        for (int i = 1; i <= means.length; i++) {
            m.append(String.format("%d. %s\n", i, means[i - 1]));
        }

        tv_mean.setText(m);
        tv_count.setText(String.format("%d / %d", pos, this.level.count));

        AssetManager manager = getResources().getAssets();
        int count = 6;

        try {
            String[] list = manager.list("res");
            count = list.length;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (pos == this.level.count && level == count){
            next.setCardBackgroundColor(Color.parseColor("#CACACA"));
            next.setClickable(false);
        }else{
            next.setCardBackgroundColor(Color.parseColor("#FFEA66"));
            next.setClickable(true);
        }

        if (pos == 1 && level == 1){
            prev.setCardBackgroundColor(Color.parseColor("#CACACA"));
            prev.setClickable(false);
        }else{
            prev.setCardBackgroundColor(Color.parseColor("#FFEA66"));
            prev.setClickable(true);
        }

        if(this.level.getWordStar(word.pos)){
            iv_star.setImageResource(R.drawable.star_on);
        }else{
            iv_star.setImageResource(R.drawable.star_off);
        }

        if (this.level.getIsComplete(word.pos, this)){
            iv_check.setVisibility(View.VISIBLE);
        }else{
            iv_check.setVisibility(View.GONE);
        }

        if (this.level.word_read != null){
            tv_read.setText(this.level.word_read.getOrDefault(String.valueOf(word.pos), 0) + "");
        }else{
            tv_read.setText(String.valueOf(0));
        }

        if (this.level.read_date.containsKey(String.valueOf(word.pos))){
            SimpleDateFormat format = new SimpleDateFormat("yyyy년 MM월 dd일");
            tv_date.setText(String.format("마지막으로 읽은 날짜\n%s", format.format(this.level.read_date.get(String.valueOf(word.pos)))));
        }else{
            tv_date.setVisibility(View.GONE);
        }

        this.level.readWord(pos, this);

    }


    public void showDialog(){
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View mdialog = inflater.inflate(R.layout.trainning_end_dialog, null);
        AlertDialog.Builder buider = new AlertDialog.Builder(WordActivity.this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
        buider.setView(mdialog);
        buider.setCancelable(true);
        Dialog dialog = buider.create();


        tv_dialog_time = mdialog.findViewById(R.id.tv_time);
        CardView btn_cancel = mdialog.findViewById(R.id.cancel);
        CardView btn_ok = mdialog.findViewById(R.id.ok);

        long current = System.currentTimeMillis();
        long diff = current - startTime;

        long diffHor = (diff) / 3600000; //시 차이
        diff %= 3600000;
        long diffMin = (diff) / 60000; //분 차이
        diff %= 60000;
        long diffSec = (diff) / 1000; //초 차이

        String text = String.format("%02d : %02d : %02d", diffHor, diffMin, diffSec);

        tv_time.setText(text);

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                finish();
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });


        dialog.show();
        Window window = dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

}