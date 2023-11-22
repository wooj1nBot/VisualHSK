package com.release.visualhsk.dto;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.FirebaseFirestore;
import com.release.visualhsk.Setting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jxl.Sheet;

public class Level {

    public static final int WORD_STATE_NOT_TEST = 0;
    public static final int WORD_STATE_RIGHT = 1;
    public static final int WORD_STATE_WRONG = 2;

    public int level;
    public int count;
    public int current_pos;


    public Map<String, Integer> word_read; //단어를 몇번 외웠냐
    public Map<String, Date> test_date; //언제 단어를 외웠냐
    public Map<String, Date> read_date; //언제 단어를 외웠냐


    public Map<String, Integer> word_state; //단어 틀/맞

    public Map<String, Integer> right_count; //단어 틀/맞
    public Map<String, Integer> right_sum_count; //단어 틀/맞
    public Map<String, Boolean> word_star; //단어 별표


    public Level(int level, int count){
        this.level = level;
        this.count = count;
        this.current_pos = 1;

        this.word_read = new HashMap<>();
        this.test_date = new HashMap<>();
        this.read_date = new HashMap<>();
        this.word_state = new HashMap<>();
        this.word_star = new HashMap<>();
        this.right_count = new HashMap<>();
        this.right_sum_count = new HashMap<>();

    }

    public Level(){}


    @Exclude
    public void addWordCount(int pos){
        if (word_read == null) word_read = new HashMap<>();
        if (!word_read.containsKey(String.valueOf(pos))) {
            word_read.put(String.valueOf(pos), 0);
        }
        word_read.put(String.valueOf(pos), word_read.get(String.valueOf(pos))+1);
    }

    @Exclude
    public void readWord(int pos, Context context){
        if (word_read == null) word_read = new HashMap<>();
        if (read_date == null) read_date = new HashMap<>();

        addWordCount(pos);

        Date date = new Date(System.currentTimeMillis());
        read_date.put(String.valueOf(pos), date);

        this.current_pos = pos;

        saveCurrent(context);
    }





    @Exclude
    public void setWordStar(int pos, Context context){
        if (word_star == null) word_star = new HashMap<>();
        word_star.put(String.valueOf(pos), !getWordStar(pos));
        saveCurrent(context);
    }

    @Exclude
    public boolean getWordStar(int pos){
        if (word_star == null || !word_star.containsKey(String.valueOf(pos))) return false;
        return word_star.get(String.valueOf(pos));
    }

    @Exclude
    public void setWordState(int pos, int state,  Context context){
        if (word_state == null) word_state = new HashMap<>();
        if (right_count == null) right_count = new HashMap<>();
        if (right_sum_count == null) right_sum_count = new HashMap<>();
        if (test_date == null) test_date = new HashMap<>();


        if (state == WORD_STATE_WRONG){
            right_count.put(String.valueOf(pos), 0);
            right_sum_count.put(String.valueOf(pos), 0);
            word_state.put(String.valueOf(pos), WORD_STATE_WRONG);
        }else{
            if (!test_date.containsKey(String.valueOf(pos))){
                right_count.put(String.valueOf(pos), 1);
            }else{

                int cnt = 0;
                if (right_count.containsKey(String.valueOf(pos))){
                    cnt = right_count.get(String.valueOf(pos));
                }

                int dif = getDiffDays(test_date.get(String.valueOf(pos)));

                if (dif == 1){
                    cnt++;
                    right_count.put(String.valueOf(pos), cnt);
                }else if(dif > 1){
                    if (cnt < Setting.getCompleteCount(context)){
                        right_count.put(String.valueOf(pos), 1);
                    }
                }
            }

            int cnt = 0;
            if (right_sum_count.containsKey(String.valueOf(pos))) {
                cnt = right_sum_count.get(String.valueOf(pos));
            }
            cnt++;
            right_sum_count.put(String.valueOf(pos), cnt);

            if (word_state.containsKey(String.valueOf(pos))){
                if (word_state.get(String.valueOf(pos)) == WORD_STATE_WRONG){
                    if (cnt >= Setting.getRightCount(context)){
                        word_state.put(String.valueOf(pos), WORD_STATE_RIGHT);
                    }
                }
            }else{
                word_state.put(String.valueOf(pos), WORD_STATE_RIGHT);
            }
        }

        Date date = new Date(System.currentTimeMillis());
        test_date.put(String.valueOf(pos), date);
        saveCurrent(context);
    }

    public int getDiffDays(Date last){
        Calendar c = Calendar.getInstance();
        c.setTime(last);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 1);

        long diffSec = (start.getTimeInMillis() - c.getTimeInMillis()) / 1000; //초 차이
        long diffDays = diffSec / (24*60*60); //일자수 차이
        return (int) (diffDays);
    }

    @Exclude
    public int getRightWordCount(){
        if (right_sum_count == null) return WORD_STATE_NOT_TEST;
        int sum = 0;
        for (String key : right_sum_count.keySet()){
            if (right_sum_count.get(key) > 0) sum++;
        }
        return sum;
    }

    @Exclude
    public int getWrongWordCount(){
        if (right_sum_count == null) return WORD_STATE_NOT_TEST;
        int sum = 0;
        for (String key : right_sum_count.keySet()){
            if (right_sum_count.get(key) == 0) sum++;
        }
        return sum;
    }

    @Exclude
    public int getWrongTestSetCount(Context context){
        if (word_state == null) return WORD_STATE_NOT_TEST;

        int sum = 0;
        for (String key : word_state.keySet()){

            if (word_state.get(key) == WORD_STATE_WRONG){
                int cnt = 0;
                if (right_sum_count != null && right_sum_count.containsKey(key)) {
                    cnt = right_sum_count.get(key);
                }
                if (cnt >= Setting.getRightCount(context)){
                    word_state.put(key, WORD_STATE_RIGHT);
                }
            }

            if (word_state.get(key) == WORD_STATE_WRONG){
                sum++;
            }
        }
        return sum;
    }

    @Exclude
    public int getWordState(int pos){
        if (right_sum_count == null) return WORD_STATE_NOT_TEST;
        if (!right_sum_count.containsKey(String.valueOf(pos))) return WORD_STATE_NOT_TEST;
        return right_sum_count.get(String.valueOf(pos)) > 0 ? WORD_STATE_RIGHT : WORD_STATE_WRONG;
    }

    @Exclude
    public boolean getIsComplete(int pos, Context context){
        if (right_count == null) return false;
        if (!right_count.containsKey(String.valueOf(pos))) return false;
        return right_count.get(String.valueOf(pos)) >= Setting.getCompleteCount(context);
    }

    @Exclude
    public int getWordCount(){
        if (word_read == null || word_read.isEmpty()) return 0;
        int sum = 0;
        for (String key : word_read.keySet()){
            if (word_read.get(key) >= 1) {
                sum++;
            }
        }
        return sum;
    }


    @Exclude
    public int getWordSafeCount(Context context){
        if (right_count == null || right_count.isEmpty()) return 0;
        int sum = 0;
        for (String key : right_count.keySet()){
            if (getIsComplete(Integer.parseInt(key), context)) {
                sum++;
            }
        }
        return sum;
    }

    @Exclude
    public int getWordSafePercent(Context context){
        if (right_count == null || right_count.isEmpty()) return 0;
        return (int) (((float) getWordSafeCount(context)) / count * 100f);
    }

    @Exclude
    public int getWordSafeCount(int percent, Context context){
        if (right_count == null || right_count.isEmpty()) return count;
        return Math.max(0, percent-getWordSafePercent(context)) * count / 100;
    }

    @Exclude
    public int getWordSafeDays(int percent, int repeat, Context context){
        if (right_count == null || right_count.isEmpty()) return count / repeat;
        return getWordSafeCount(percent, context) / repeat;
    }

    @Exclude
    public ArrayList<Word> getWrongDataSet(Sheet sheet, Context context){
        ArrayList<Word> arrayList = new ArrayList<>();
        if (word_state == null || word_state.isEmpty()) return arrayList;
        for (String key : word_state.keySet()){

            if (word_state.get(key) == WORD_STATE_WRONG){
                int cnt = 0;
                if (right_sum_count != null && right_sum_count.containsKey(key)) {
                    cnt = right_sum_count.get(key);
                }
                if (cnt >= Setting.getRightCount(context)){
                    word_state.put(key, WORD_STATE_RIGHT);
                }
            }

            if (word_state.get(key) == WORD_STATE_WRONG){
                int pos = Integer.parseInt(key);
                String w = sheet.getCell(0, pos-1).getContents();
                String voice = sheet.getCell(1, pos-1).getContents();
                String mean = sheet.getCell(2, pos-1).getContents();
                Word word = new Word(level, pos, w, voice, mean);
                arrayList.add(word);
            }
        }

        arrayList.sort(new Comparator<Word>() {
            @Override
            public int compare(Word word, Word t1) {
                return word.pos - t1.pos;
            }
        });

        return arrayList;
    }

    @Exclude
    public ArrayList<Word> getReviewAllDataSet(Sheet sheet){
        ArrayList<Word> arrayList = new ArrayList<>();
        if (read_date == null || read_date.isEmpty()) return arrayList;

        for (String key : read_date.keySet()){
            int pos = Integer.parseInt(key);
            String w = sheet.getCell(0, pos-1).getContents();
            String voice = sheet.getCell(1, pos-1).getContents();
            String mean = sheet.getCell(2, pos-1).getContents();
            Word word = new Word(level, pos, w, voice, mean);
            arrayList.add(word);
        }

        arrayList.sort(new Comparator<Word>() {
            @Override
            public int compare(Word word, Word t1) {
                return word.pos - t1.pos;
            }
        });

        return arrayList;
    }

    @Exclude
    public ArrayList<Word> getReviewDataSet(Sheet sheet){
        ArrayList<Word> arrayList = new ArrayList<>();
        if (read_date == null || read_date.isEmpty()) return arrayList;

        for (String key : read_date.keySet()){
            int pos = Integer.parseInt(key);
            String w = sheet.getCell(0, pos-1).getContents();
            String voice = sheet.getCell(1, pos-1).getContents();
            String mean = sheet.getCell(2, pos-1).getContents();
            Word word = new Word(level, pos, w, voice, mean);
            arrayList.add(word);
        }

        arrayList.sort(new Comparator<Word>() {
            @Override
            public int compare(Word word, Word t1) {
                return word.pos - t1.pos;
            }
        });

        return arrayList;
    }

    @Exclude
    public ArrayList<Word> getReviewPopupDataSet(Context context, Sheet sheet){
        ArrayList<Word> arrayList = new ArrayList<>();
        if (read_date == null || read_date.isEmpty()) return arrayList;

        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 1);

        Calendar c = Calendar.getInstance();

        for (String key : read_date.keySet()){
            if (getIsComplete(Integer.parseInt(key), context)) continue;

            Date date = read_date.get(key);
            c.setTime(date);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);

            long diffSec = (start.getTimeInMillis() - c.getTimeInMillis()) / 1000; //초 차이
            long diffDays = diffSec / (24*60*60); //일자수 차이
            int dif = (int) (diffDays);

            if (dif <= 1){
                int pos = Integer.parseInt(key);
                String w = sheet.getCell(0, pos-1).getContents();
                String voice = sheet.getCell(1, pos-1).getContents();
                String mean = sheet.getCell(2, pos-1).getContents();
                Word word = new Word(level, pos, w, voice, mean);
                arrayList.add(word);
            }
        }

        arrayList.sort(new Comparator<Word>() {
            @Override
            public int compare(Word word, Word t1) {
                return word.pos - t1.pos;
            }
        });

        return arrayList;
    }



    @Exclude
    public ArrayList<Word> getStarDataSet(Sheet sheet){
        ArrayList<Word> arrayList = new ArrayList<>();
        if (word_star == null || word_star.isEmpty()) return arrayList;

        for (String key : word_star.keySet()){
            if (word_star.get(key)){
                int pos = Integer.parseInt(key);
                String w = sheet.getCell(0, pos-1).getContents();
                String voice = sheet.getCell(1, pos-1).getContents();
                String mean = sheet.getCell(2, pos-1).getContents();
                Word word = new Word(level, pos, w, voice, mean);
                arrayList.add(word);
            }
        }

        arrayList.sort(new Comparator<Word>() {
            @Override
            public int compare(Word word, Word t1) {
                return word.pos - t1.pos;
            }
        });

        return arrayList;
    }

    @Exclude
    public int getReviewAllWordCount(){
        if (read_date == null || read_date.isEmpty()) return 0;
        return read_date.size();
    }

    @Exclude
    public int getReviewWordCount(){
        if (read_date == null || read_date.isEmpty()) return 0;
        return read_date.size();
    }

    @Exclude
    public int getWordStarCount(){
        if (word_star == null) return 0;
        int sum = 0;
        for (String key : word_star.keySet()){
            if (word_star.get(key)){
                sum++;
            }
        }
        return sum;
    }

    public Map<String, Integer> getRight_sum_count() {
        return right_sum_count;
    }

    public Map<String, Date> getRead_date() {
        return read_date;
    }

    public Map<String, Date> getTest_date() {
        return test_date;
    }

    public Map<String, Integer> getRight_count() {
        return right_count;
    }

    public Map<String, Integer> getWord_read() {
        return word_read;
    }

    public int getCount() {
        return count;
    }

    public int getCurrent_pos() {
        return current_pos;
    }

    public int getLevel() {
        return level;
    }

    public Map<String, Boolean> getWord_star() {
        return word_star;
    }

    public Map<String, Integer> getWord_state() {
        return word_state;
    }

    @Exclude
    public void saveCurrent(Context context){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(User.getId(context)).update(
                "levels." + level, Level.this
        );
    }

}
