package com.release.visualhsk;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.release.visualhsk.dto.Level;
import com.release.visualhsk.dto.User;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;


public class WrongFragment extends Fragment {
    
    TextView tv_wrong;
    TextView tv_review;
    TextView tv_star;
    
    CardView cd_wrong_word, cd_wrong_test;
    CardView cd_review_word, cd_review_test;
    CardView cd_star_word, cd_star_test;
    public ArrayList<Level> levels;
    User user;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_wrong, container, false);
        
        tv_wrong = view.findViewById(R.id.tv_count1);
        tv_review = view.findViewById(R.id.tv_count2);
        tv_star = view.findViewById(R.id.tv_count3);
        
        tv_wrong.setText("0개의 단어");
        tv_review.setText("0개의 단어");
        tv_star.setText("0개의 단어");
        
        cd_wrong_word = view.findViewById(R.id.cd_word1);
        cd_review_word = view.findViewById(R.id.cd_word2);
        cd_star_word = view.findViewById(R.id.cd_word3);

        cd_wrong_test = view.findViewById(R.id.cd_test1);
        cd_review_test = view.findViewById(R.id.cd_test2);
        cd_star_test = view.findViewById(R.id.cd_test3);
        
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        getData();
    }

    public void getData(){

        AssetManager manager = getResources().getAssets();
        String[] list;

        try {

            list = manager.list("res");
            ArrayList<String> folders = new ArrayList<>(Arrays.asList(list));
            folders.sort(String::compareTo);
            levels = new ArrayList<>();

            for (int i = 1; i <= folders.size(); i++) {

                InputStream inputStream = manager.open("res/" + folders.get(i-1));
                Workbook wb = Workbook.getWorkbook(inputStream);
                Sheet sheet = wb.getSheet(0);
                int rowTotal = sheet.getColumn(0).length;
                Level level = new Level(i, rowTotal);
                levels.add(level);

                inputStream.close();
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(User.getId(getContext())).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful() && task.getResult().exists()){
                        user = task.getResult().toObject(User.class);

                        int review = 0;
                        int star = 0;
                        int wrong = 0;

                        if (user.levels != null) {

                            for (String key : user.levels.keySet()) {
                                int k = Integer.parseInt(key);
                                Level level = user.levels.get(key);

                                levels.set(k-1, level);
                                review += level.getReviewWordCount();
                                star += level.getWordStarCount();
                                wrong += level.getWrongTestSetCount(getActivity());
                            }
                        }

                        tv_review.setText(String.format("%s개의 단어", review));
                        tv_star.setText(String.format("%s개의 단어", star));
                        tv_wrong.setText(String.format("%s개의 단어", wrong));

                        cd_review_word.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                showLevelDialog(WordActivity.WORD_MODE_REVIEW, 0);
                            }
                        });

                        cd_star_word.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                showLevelDialog(WordActivity.WORD_MODE_STAR, 0);
                            }
                        });
                        cd_wrong_word.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                showLevelDialog(WordActivity.WORD_MODE_WRONG, 0);
                            }
                        });

                        cd_review_test.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                showLevelDialog(WordActivity.WORD_MODE_REVIEW, 1);
                            }
                        });

                        cd_star_test.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                showLevelDialog(WordActivity.WORD_MODE_STAR, 1);
                            }
                        });
                        cd_wrong_test.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                showLevelDialog(WordActivity.WORD_MODE_WRONG, 1);
                            }
                        });



                    }
                }
            });



        } catch (IOException | BiffException e) {
            throw new RuntimeException(e);
        }


    }


    public void showLevelDialog(int mode, int type){
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View mdialog = inflater.inflate(R.layout.select_level_dialog, null);
        AlertDialog.Builder buider = new AlertDialog.Builder(getContext(), android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
        buider.setView(mdialog);
        buider.setCancelable(true);
        Dialog dialog = buider.create();

        RecyclerView rc = mdialog.findViewById(R.id.rc);

        LevelViewAdapter adapter = new LevelViewAdapter(levels, mode, type);
        rc.setLayoutManager(new LinearLayoutManager(getContext()));
        rc.setAdapter(adapter);

        dialog.show();
        Window window = dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    class LevelViewAdapter extends RecyclerView.Adapter<LevelViewAdapter.ViewHolder> {

        Context context;
        ArrayList<Level> levels;
        int mode;
        int type;


        public LevelViewAdapter(ArrayList<Level> levels, int mode, int type){
            this.levels = levels;
            this.mode = mode;
            this.type = type;
        }


        @NonNull
        @Override
        public LevelViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            context = parent.getContext();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) ;
            View view = inflater.inflate(R.layout.level_item, parent, false);
            return new LevelViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LevelViewAdapter.ViewHolder holder, int position) {
            Level level = levels.get(position);
            holder.tv_title.setText(String.format("%d급 - ", level.level));
            int count = 0;
            if (mode == WordActivity.WORD_MODE_WRONG){
                count = level.getWrongTestSetCount(getActivity());
            }
            if (mode == WordActivity.WORD_MODE_REVIEW){
                count = level.getReviewWordCount();
            }
            if (mode == WordActivity.WORD_MODE_STAR){
                count = level.getWordStarCount();
            }
            if (count == 0){
                holder.itemView.setClickable(false);
            }else{
                holder.itemView.setBackgroundColor(Color.parseColor("#FFF9D4"));
            }
            holder.itemView.setTag(count);
            holder.tv_count.setText(String.format("%d개의 단어", count));
        }

        @Override
        public int getItemCount() {
            return levels.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            TextView tv_title;
            TextView tv_count;


            ViewHolder(View itemView) {
                super(itemView);

                tv_title = itemView.findViewById(R.id.textView10);
                tv_count = itemView.findViewById(R.id.textView7);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = getAdapterPosition() ;
                        if (pos != RecyclerView.NO_POSITION) {
                            // TODO : use pos.

                            if (type == 0){
                                Intent intent = new Intent(getContext(), WordActivity.class);
                                intent.putExtra("level", levels.get(pos).level);
                                intent.putExtra("mode", mode);
                                getContext().startActivity(intent);
                            }else{
                                showTestDialog(levels.get(pos), mode);
                            }
                        }
                    }
                });
            }
        }
    }

    public void showTestDialog(Level level, int mode){
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View mdialog = inflater.inflate(R.layout.test_dialog, null);
        AlertDialog.Builder buider = new AlertDialog.Builder(getContext(), android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
        buider.setView(mdialog);
        buider.setCancelable(true);
        Dialog dialog = buider.create();

        CardView btn_word = mdialog.findViewById(R.id.btn_word);
        CardView btn_test = mdialog.findViewById(R.id.btn_test);
        TextView tv_cancel = mdialog.findViewById(R.id.tv_cancel);

        btn_word.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(getContext(), TestActivity.class);
                intent.putExtra("level", level.level);
                intent.putExtra("mode", mode);
                intent.putExtra("type", true);
                getContext().startActivity(intent);
            }
        });

        btn_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                Intent intent = new Intent(getContext(), TestActivity.class);
                intent.putExtra("level", level.level);
                intent.putExtra("mode", mode);
                intent.putExtra("type", false);
                getContext().startActivity(intent);
            }
        });


        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });


        dialog.show();
        Window window = dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }
    
}