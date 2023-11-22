package com.release.visualhsk;

import static com.release.visualhsk.WordActivity.WORD_MODE_DEFAULT;
import static com.release.visualhsk.WordActivity.WORD_MODE_REVIEW;
import static com.release.visualhsk.WordActivity.WORD_MODE_STAR;
import static com.release.visualhsk.WordActivity.WORD_MODE_WRONG;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

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
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

public class PopupReceiver extends BroadcastReceiver {


    TextView tv_word, tv_sound, tv_count, tv_right, tv_wrong;
    TextView[] answers;

    ImageView iv_tts, iv_state;
    CardView cd_back, cd_skip;
    ProgressBar progressBar;
    Word word;
    User user;

    TextToSpeech tts;

    long startTime = 0;

    private Timer appTimer;

    boolean isMean;

    ArrayList<Word> words;

    int right;
    int wrong;
    int pos;
    int count;

    ArrayList<Word> testSet;
    Handler handler;

    Context context;

    Dialog mDialog;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        this.context = context;
        getData(context);
    }

    public void showTestDialog(Context context){
        startTime = System.currentTimeMillis();

        mDialog = new Dialog(context);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(R.layout.word_test_dialog);
        mDialog.setCancelable(false);

        handler = new Handler();
        tv_word = mDialog.findViewById(R.id.word);
        tv_sound = mDialog.findViewById(R.id.word2);
        tv_count = mDialog.findViewById(R.id.count);
        tv_right = mDialog.findViewById(R.id.right);
        tv_wrong = mDialog.findViewById(R.id.wrong);

        iv_tts = mDialog.findViewById(R.id.tts);
        iv_state = mDialog.findViewById(R.id.state);

        answers = new TextView[4];
        answers[0] = mDialog.findViewById(R.id.a1);
        answers[1] = mDialog.findViewById(R.id.a2);
        answers[2] = mDialog.findViewById(R.id.a3);
        answers[3] = mDialog.findViewById(R.id.a4);

        cd_back = mDialog.findViewById(R.id.cd_back);
        cd_skip = mDialog.findViewById(R.id.skip);
        progressBar = mDialog.findViewById(R.id.progressBar2);

        isMean = true;

        if (!isMean){
            tv_word.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 26);
        }

        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
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

        int LAYOUT_FLAG;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }
        mDialog.getWindow().setType(LAYOUT_FLAG);

        mDialog.show();


        Window window = mDialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    public void getData(Context context){

        right = 0;
        wrong = 0;
        pos = 1;
        words = new ArrayList<>();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(User.getId(context)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult().exists()){
                    user = task.getResult().toObject(User.class);
                    if (user.levels != null){
                        for(String key : user.levels.keySet()){
                            Level level = user.levels.get(key);
                            words.addAll(level.getReviewPopupDataSet(context, getSheet(context, level.level)));
                        }
                    }
                    Log.d("wordlog", String.valueOf(words.size()));
                    if (!words.isEmpty()){
                        showTestDialog(context);
                        getTestSet(pos, isMean);
                    }
                }

            }
        });

    }

    public Sheet getSheet(Context context, int level){
        AssetManager manager = context.getResources().getAssets();
        try {
            String file = String.format("hsk%d.xls", level);
            InputStream inputStream = manager.open("res/" + file);
            WorkbookSettings setting = new WorkbookSettings();
            setting.setEncoding("Cp1252");
            Workbook wb = Workbook.getWorkbook(inputStream, setting);
            return wb.getSheet(0);
        } catch (IOException | BiffException e) {
            return null;
        }
    }

    public void getTestSet(int pos, boolean isMean){

        testSet = new ArrayList<>();

        this.word = words.get(pos-1);
        Level level = user.levels.get(String.valueOf(word.level));
        Sheet sheet = getSheet(context, word.level);
        ArrayList<Integer> randoms = new ArrayList<>();

        for (int i = 0; i < 3; i++){
            int r = new Random().nextInt(level.count-1) + 1;
            while (randoms.contains(r) || r == pos){
                r = new Random().nextInt(level.count-1) + 1;
            }
            randoms.add(r);
        }

        testSet.add(word);

        for (int i = 0; i < 3; i++){
            int r = randoms.get(i);
            String w = sheet.getCell(0, r-1).getContents();
            String voice = sheet.getCell(1, r-1).getContents();
            String mean = sheet.getCell(2, r-1).getContents();
            Word word = new Word(level.level, r, w, voice, mean);
            testSet.add(word);
        }

        Collections.shuffle(testSet);

        word.saveWord(context);

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
        startTimer();
    }

    public void startTimer(){
        int t = Setting.getTimerSec(context);
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

    public void choice(int i){
        appTimer.cancel();
        iv_state.setVisibility(View.VISIBLE);
        iv_state.setAlpha(0.9f);

        if (i == -1 || this.word != testSet.get(i)){
            user.levels.get(String.valueOf(word.level)).setWordState(word.pos, Level.WORD_STATE_WRONG, context);
            wrong++;
            iv_state.setImageResource(R.drawable.close_48px);
            iv_state.setImageTintList(ColorStateList.valueOf(Color.parseColor("#EF5350")));
        }else{
            user.levels.get(String.valueOf(word.level)).setWordState(word.pos, Level.WORD_STATE_RIGHT, context);
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
                        if (pos == words.size()){
                            showFinishDialog();
                        }else{
                            getTestSet(++pos, isMean);
                        }
                    }
                }
            }).start();
        }


    }

    public void showDialog(){
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.test_end_dialog);
        dialog.setCancelable(true);


        TextView tv_right = dialog.findViewById(R.id.right);
        TextView tv_wrong = dialog.findViewById(R.id.wrong);
        CardView btn_cancel = dialog.findViewById(R.id.cancel);
        CardView btn_ok = dialog.findViewById(R.id.ok);

        tv_right.setText(right + "단어");
        tv_wrong.setText(wrong + "단어");

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                destroy();
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        int LAYOUT_FLAG;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }
        dialog.getWindow().setType(LAYOUT_FLAG);


        dialog.show();
        Window window = dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    public void destroy(){
        if (appTimer != null) {
            appTimer.cancel();
        }
        user.addStudyTime(context, System.currentTimeMillis()-startTime);
        mDialog.dismiss();
    }


    public void showFinishDialog(){
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.test_result_dialog);
        dialog.setCancelable(false);


        TextView tv_right = dialog.findViewById(R.id.right);
        TextView tv_wrong = dialog.findViewById(R.id.wrong);
        TextView tv_title = dialog.findViewById(R.id.tv_level);

        TextView pro_title = dialog.findViewById(R.id.pro_title);
        TextView pro_value = dialog.findViewById(R.id.pro_value);

        CircularProgressIndicator indicator = dialog.findViewById(R.id.circular_progress);
        CardView btn_ok = dialog.findViewById(R.id.ok);

        indicator.setProgress(((float) right / (right + wrong)) * 100, 100);

        tv_title.setText("복습 단어");

        pro_title.setText("정답률");
        pro_value.setText((int) indicator.getProgress() + "%");

        tv_right.setText(right + "단어");
        tv_wrong.setText(wrong + "단어");

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                destroy();
            }
        });

        int LAYOUT_FLAG;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }
        dialog.getWindow().setType(LAYOUT_FLAG);


        dialog.show();
        Window window = dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }
}