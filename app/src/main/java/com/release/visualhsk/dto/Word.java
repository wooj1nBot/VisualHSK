package com.release.visualhsk.dto;

import android.content.Context;

import com.google.firebase.firestore.FirebaseFirestore;

public class Word {

    public int level;
    public int pos;

    public String word;
    public String voice;
    public String mean;

    public Word(int level, int pos, String word, String voice, String mean){
        this.level = level;
        this.pos = pos;
        this.word = word;
        this.voice = voice;
        this.mean = mean;
    }

    public Word(){}

    public int getLevel() {
        return level;
    }

    public int getPos() {
        return pos;
    }

    public String getMean() {
        return mean;
    }

    public String getVoice() {
        return voice;
    }

    public String getWord() {
        return word;
    }

    public void saveWord(Context context){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(User.getId(context)).update(
                "current_word", this
        );
    }
}
