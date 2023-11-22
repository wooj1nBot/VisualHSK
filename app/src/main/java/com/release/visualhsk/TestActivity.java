package com.release.visualhsk;

import static com.release.visualhsk.WordActivity.WORD_MODE_DEFAULT;
import static com.release.visualhsk.WordActivity.WORD_MODE_REVIEW;
import static com.release.visualhsk.WordActivity.WORD_MODE_STAR;
import static com.release.visualhsk.WordActivity.WORD_MODE_WRONG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.release.visualhsk.dto.Level;
import com.release.visualhsk.dto.User;
import com.release.visualhsk.dto.Word;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

public class TestActivity extends AppCompatActivity {

    TextView tv_level, tv_word, tv_sound, tv_count, tv_right, tv_wrong, tv_sum, tv_left;
    TextView[] answers;

    ImageView iv_tts, iv_back, iv_state, iv_check;
    CardView cd_back, cd_skip;
    ProgressBar progressBar;

    Level level;
    Word word;
    User user;

    Sheet sheet;

    TextToSpeech tts;

    long startTime = 0;

    private Timer appTimer;

    boolean isMean;

    int mode;

    ArrayList<Word> words;

    int right;
    int wrong;
    int pos;
    int count;

    ArrayList<Word> testSet;
    Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        handler = new Handler();
        tv_level = findViewById(R.id.tv_title);
        tv_word = findViewById(R.id.word);
        tv_sound = findViewById(R.id.word2);
        tv_count = findViewById(R.id.count);
        tv_right = findViewById(R.id.right);
        tv_wrong = findViewById(R.id.wrong);
        tv_sum = findViewById(R.id.count2);
        tv_left = findViewById(R.id.count3);

        iv_tts = findViewById(R.id.tts);
        iv_back = findViewById(R.id.back);
        iv_state = findViewById(R.id.state);
        iv_check = findViewById(R.id.check);

        answers = new TextView[4];
        answers[0] = findViewById(R.id.a1);
        answers[1] = findViewById(R.id.a2);
        answers[2] = findViewById(R.id.a3);
        answers[3] = findViewById(R.id.a4);

        cd_back = findViewById(R.id.cd_back);
        cd_skip = findViewById(R.id.skip);
        progressBar = findViewById(R.id.progressBar2);

        Intent intent = getIntent();
        int l = intent.getIntExtra("level", 1);
        mode = intent.getIntExtra("mode", WORD_MODE_DEFAULT);
        isMean = intent.getBooleanExtra("type", true);

        if (!isMean){
            tv_word.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 26);
        }

        tts = new TextToSpeech(TestActivity.this, new TextToSpeech.OnInitListener() {
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



        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });

        cd_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });

        startTime = System.currentTimeMillis();

        cd_skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appTimer.cancel();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        choice(-1);
                    }
                });
            }
        });


        getLevel(l);
    }

    @Override
    public void onBackPressed() {
        showDialog();
    }

    public void getLevel(int level_pos){

        right = 0;
        wrong = 0;
        pos = 1;
        tv_right.setText(right+"");
        tv_wrong.setText(wrong+"");

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
                    getTestSet(sheet, level.level, pos, isMean);
                }else{
                    if (mode == WORD_MODE_WRONG){
                        words = level.getWrongDataSet(sheet, TestActivity.this);
                    }
                    if (mode == WORD_MODE_REVIEW){
                        words = level.getReviewDataSet(sheet);
                    }
                    if (mode == WORD_MODE_STAR){
                        words = level.getStarDataSet(sheet);
                    }
                    getTestSet(pos, isMean);
                }

            }
        });

    }

    public void getTestSet(int pos, boolean isMean){

        testSet = new ArrayList<>();
        ArrayList<Integer> randoms = new ArrayList<>();

        for (int i = 0; i < 3; i++){
            int r = new Random().nextInt(level.count-1) + 1;
            while (randoms.contains(r) || r == pos){
                r = new Random().nextInt(level.count-1) + 1;
            }
            randoms.add(r);
        }

        testSet.add(words.get(pos-1));
        this.word = words.get(pos-1);
        this.level.readWord(word.pos, TestActivity.this);

        for (int i = 0; i < 3; i++){
            int r = randoms.get(i);
            String w = sheet.getCell(0, r-1).getContents();
            String voice = sheet.getCell(1, r-1).getContents();
            String mean = sheet.getCell(2, r-1).getContents();
            Word word = new Word(level.level, r, w, voice, mean);
            testSet.add(word);
        }

        Collections.shuffle(testSet);

        word.saveWord(TestActivity.this);

        if (isMean){
            tv_word.setText(word.word);
            tv_sound.setText(String.format("[ %s ]", word.voice));
        }else{
            StringBuilder m = new StringBuilder();
            String[] means = word.mean.split("\\|");
            for (int i = 1; i <= means.length; i++) {
                m.append(String.format("%d. %s\n", i, means[i - 1]));
            }
            tv_word.setText(m.toString().trim());
            tv_sound.setVisibility(View.GONE);
            iv_tts.setVisibility(View.GONE);
        }


        for (int j = 0; j < 4; j++) {
            Word word = testSet.get(j);
            StringBuilder m = new StringBuilder();
            if (isMean){
                String[] means = word.mean.split("\\|");
                for (int i = 1; i <= means.length; i++) {
                    m.append(String.format("%d. %s\n", i, means[i - 1]));
                }
            }else{
                m.append(word.word).append("\n").append(word.voice);
            }
            int finalJ = j;
            answers[j].getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int height = answers[finalJ].getHeight();
                    answers[finalJ].setWidth(height);
                }
            });
            answers[j].setText(m.toString().trim());
            answers[j].setTag(j);
            answers[j].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int j = (int) view.getTag();
                    choice(j);
                }
            });
        }

        tv_count.setText(String.format("%d / %d", pos, words.size()));

        if (this.level.getIsComplete(word.pos, this)){
            iv_check.setVisibility(View.VISIBLE);
        }else{
            iv_check.setVisibility(View.GONE);
        }

        if (this.level.right_count != null){
            int left = this.level.right_count.getOrDefault(String.valueOf(word.pos), 0);
            tv_sum.setText(String.format("누적 정답 횟수 %d회", left));
            if (left >= Setting.getCompleteCount(TestActivity.this)){
                tv_left.setText("복습 완료");
            }else{
                tv_left.setText(String.format("복습 완료까지 %d회 남음", Setting.getCompleteCount(TestActivity.this) - left));
            }
        }else{
            tv_sum.setText(String.format("누적 정답 횟수 %d회", 0));
            tv_left.setText(String.format("복습 완료까지 %d회 남음", Setting.getCompleteCount(TestActivity.this)));
        }

        startTimer();
    }

    public void getTestSet(Sheet sheet, int level, int pos, boolean isMean){

        testSet = new ArrayList<>();
        ArrayList<Integer> randoms = new ArrayList<>();
        randoms.add(pos);

        for (int i = 0; i < 3; i++){
            int r = new Random().nextInt(this.level.count-1) + 1;
            while (randoms.contains(r)){
                r = new Random().nextInt(this.level.count-1) + 1;
            }
            randoms.add(r);
        }

        for (int i = 0; i < 4; i++){
            int r = randoms.get(i);
            String w = sheet.getCell(0, r-1).getContents();
            String voice = sheet.getCell(1, r-1).getContents();
            String mean = sheet.getCell(2, r-1).getContents();

            Word word = new Word(level, r, w, voice, mean);
            Log.d("word", w + "," + voice + "," + mean);
            if (r == pos){
                this.word = word;
            }
            testSet.add(word);
        }
        Collections.shuffle(testSet);

        word.saveWord(TestActivity.this);
        this.level.readWord(word.pos, TestActivity.this);

        if (isMean){
            tv_word.setText(word.word);
            tv_sound.setText(String.format("[ %s ]", word.voice));
        }else{
            StringBuilder m = new StringBuilder();
            String[] means = word.mean.split("\\|");
            for (int i = 1; i <= means.length; i++) {
                m.append(String.format("%d. %s\n", i, means[i - 1]));
            }
            tv_word.setText(m.toString().trim());
            tv_sound.setVisibility(View.GONE);
            iv_tts.setVisibility(View.GONE);
        }


        for (int j = 0; j < 4; j++) {
            Word word = testSet.get(j);
            StringBuilder m = new StringBuilder();
            if (isMean){
                String[] means = word.mean.split("\\|");
                for (int i = 1; i <= means.length; i++) {
                    m.append(String.format("%d. %s\n", i, means[i - 1]));
                }
            }else{
                m.append(word.word).append("\n").append(word.voice);
            }
            int finalJ = j;
            answers[j].getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int height = answers[finalJ].getHeight();
                    answers[finalJ].setWidth(height);
                }
            });
            answers[j].setText(m.toString().trim());
            answers[j].setTag(j);
            answers[j].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int j = (int) view.getTag();
                    choice(j);
                }
            });
        }

        tv_count.setText(String.format("%d / %d", pos, this.level.count));

        if (this.level.getIsComplete(word.pos, this)){
            iv_check.setVisibility(View.VISIBLE);
        }else{
            iv_check.setVisibility(View.GONE);
        }

        if (this.level.right_count != null){
            int left = this.level.right_count.getOrDefault(String.valueOf(word.pos), 0);
            tv_sum.setText(String.format("누적 정답 횟수 %d회", left));
            if (left >= Setting.getCompleteCount(TestActivity.this)){
                tv_left.setText("복습 완료");
            }else{
                tv_left.setText(String.format("복습 완료까지 %d회 남음", Setting.getCompleteCount(TestActivity.this) - left));
            }
        }else{
            tv_sum.setText(String.format("누적 정답 횟수 %d회", 0));
            tv_left.setText(String.format("복습 완료까지 %d회 남음", Setting.getCompleteCount(TestActivity.this)));
        }

        startTimer();
    }


    public void choice(int i){
        appTimer.cancel();
        iv_state.setVisibility(View.VISIBLE);
        iv_state.setAlpha(0.9f);
        if (i == -1 || this.word != testSet.get(i)){
            level.setWordState(word.pos, Level.WORD_STATE_WRONG,TestActivity.this);
            wrong++;
            iv_state.setImageResource(R.drawable.close_48px);
            iv_state.setImageTintList(ColorStateList.valueOf(Color.parseColor("#EF5350")));
        }else{
            level.setWordState(word.pos, Level.WORD_STATE_RIGHT, TestActivity.this);
            right++;
            iv_state.setImageResource(R.drawable.circle2);
            iv_state.setImageTintList(ColorStateList.valueOf(Color.parseColor("#2196F3")));
        }

        tv_right.setText(right+"");
        tv_wrong.setText(wrong+"");

        iv_state.animate().alpha(0).setDuration(2000);

        for (int j = 0; j < 4; j++) {
            Word w = testSet.get(j);
            answers[j].setAlpha(0.3f);
            if (w == this.word){
                answers[j].setBackgroundResource(R.drawable.stroke_right);
            }else{
                answers[j].setBackgroundResource(R.drawable.stroke_wrong);
            }
            int finalJ = j;
            answers[j].setClickable(false);
            cd_skip.setClickable(false);
            answers[j].animate().alpha(1).setDuration(2000).withEndAction(new Runnable() {
                @Override
                public void run() {
                    answers[finalJ].setBackground(null);
                    answers[finalJ].setClickable(true);
                    cd_skip.setClickable(true);

                    if (finalJ == 0) {
                        if (mode == WORD_MODE_DEFAULT){
                            if (pos == level.count){
                                showFinishDialog();
                            }else{
                                getTestSet(sheet, level.level, ++pos, isMean);
                            }
                        }else{
                            if (pos == words.size()){
                                showFinishDialog();
                            }else{
                                getTestSet(++pos, isMean);
                            }
                        }

                    }
                }
            }).start();
        }


    }

    @Override
    protected void onStart() {
        super.onStart();
        startTime = System.currentTimeMillis();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (appTimer != null) {
            appTimer.cancel();
        }
        user.addStudyTime(TestActivity.this, System.currentTimeMillis()-startTime);
    }

    public void startTimer(){
        int t = Setting.getTimerSec(TestActivity.this);
        if (t != -1){
            int time = t * 1000;
            progressBar.setMax(300);
            progressBar.setProgress(0);
            count = 0;

            TimerTask purposeTask = new TimerTask() {
                @Override
                public void run() {
                    if (count >= time){
                        appTimer.cancel();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                choice(-1);
                            }
                        });
                    }
                    count += time / 300;
                    progressBar.setProgress(progressBar.getProgress()+1);
                }
            };
            appTimer = new Timer();
            appTimer.schedule(purposeTask, 0, time / 300);
        }
    }

    public void showDialog(){
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View mdialog = inflater.inflate(R.layout.test_end_dialog, null);
        AlertDialog.Builder buider = new AlertDialog.Builder(TestActivity.this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
        buider.setView(mdialog);
        buider.setCancelable(true);
        Dialog dialog = buider.create();


        TextView tv_right = mdialog.findViewById(R.id.right);
        TextView tv_wrong = mdialog.findViewById(R.id.wrong);
        CardView btn_cancel = mdialog.findViewById(R.id.cancel);
        CardView btn_ok = mdialog.findViewById(R.id.ok);

        tv_right.setText(right + "단어");
        tv_wrong.setText(wrong + "단어");

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


    public void showFinishDialog(){
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View mdialog = inflater.inflate(R.layout.test_result_dialog, null);
        AlertDialog.Builder buider = new AlertDialog.Builder(TestActivity.this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
        buider.setView(mdialog);
        buider.setCancelable(false);
        Dialog dialog = buider.create();


        TextView tv_right = mdialog.findViewById(R.id.right);
        TextView tv_wrong = mdialog.findViewById(R.id.wrong);
        TextView tv_title = mdialog.findViewById(R.id.tv_level);

        TextView pro_title = mdialog.findViewById(R.id.pro_title);
        TextView pro_value = mdialog.findViewById(R.id.pro_value);

        CircularProgressIndicator indicator = mdialog.findViewById(R.id.circular_progress);
        CardView btn_ok = mdialog.findViewById(R.id.ok);

        indicator.setProgress(((float) right / (right + wrong)) * 100, 100);


        if (mode == WORD_MODE_DEFAULT){
            tv_title.setText(String.format("HSK %d급", level.level));
        }
        if (mode == WORD_MODE_WRONG){
            tv_title.setText("틀린 단어");
        }
        if (mode == WORD_MODE_REVIEW){
            tv_title.setText("복습 단어");
        }
        if (mode == WORD_MODE_STAR){
            tv_title.setText("별표 친 단어");
        }

        pro_title.setText("정답률");
        pro_value.setText((int) indicator.getProgress() + "%");

        tv_right.setText(right + "단어");
        tv_wrong.setText(wrong + "단어");

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                finish();
            }
        });


        dialog.show();
        Window window = dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }




}