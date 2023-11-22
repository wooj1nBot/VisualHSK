package com.release.visualhsk;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.release.visualhsk.dto.Level;
import com.release.visualhsk.dto.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.jaygoo.widget.RangeSeekBar;
import com.release.visualhsk.dto.Word;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;


public class HomeFragment extends Fragment {


    RecyclerView rc;
    User user;

    public TextView tv_level, tv_word, tv_voice, tv_review, tv_star, tv_wrong;
    public ImageView setting;
    public CardView cd1, cd2, cd3, cd4;
    public ArrayList<Level> levels;

    public HomeFragment() {
        // Required empty public constructor
    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_home, container, false);
        rc = view.findViewById(R.id.rc);

        FirebaseApp.initializeApp(/*context=*/ getContext());

        GridLayoutManager manager = new GridLayoutManager(getContext(), 2);
        rc.setLayoutManager(manager);
        rc.addItemDecoration(new SpacesItemDecoration(ConvertDPtoPX(getContext(), 20), 0));

        tv_level = view.findViewById(R.id.level);
        tv_word = view.findViewById(R.id.word);
        tv_voice = view.findViewById(R.id.word2);
        tv_review = view.findViewById(R.id.review);
        tv_star = view.findViewById(R.id.star);
        tv_wrong = view.findViewById(R.id.wrong);

        cd1 = view.findViewById(R.id.cd1);
        cd2 = view.findViewById(R.id.cd2);
        cd3 = view.findViewById(R.id.cd3);
        cd4 = view.findViewById(R.id.cd4);

        setting = view.findViewById(R.id.imageView4);
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), SettingActivity.class);
                startActivity(intent);
            }
        });

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

            GridViewAdapter adapter = new GridViewAdapter(levels);
            rc.setAdapter(adapter);

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
                                wrong += level.getWrongWordCount();
                            }
                            adapter.notifyDataSetChanged();

                            if (user.current_word != null) {
                                tv_level.setText(user.current_word.level + "급");
                                tv_word.setText(user.current_word.word);
                                tv_voice.setText(String.format("[ %s ]", user.current_word.voice));
                            }
                        }

                        tv_review.setText(String.format("%s개의 단어", review));
                        tv_star.setText(String.format("%s개의 단어", star));
                        tv_wrong.setText(String.format("%s개의 단어", wrong));

                        cd1.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(getContext(), WordActivity.class);
                                intent.putExtra("level", user.current_word.level);
                                getContext().startActivity(intent);
                            }
                        });

                        cd2.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                showLevelDialog(WordActivity.WORD_MODE_REVIEW);
                            }
                        });
                        cd3.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                showLevelDialog(WordActivity.WORD_MODE_STAR);
                            }
                        });

                        cd4.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                showLevelDialog(WordActivity.WORD_MODE_WRONG);
                            }
                        });
                    }
                }
            });



        } catch (IOException | BiffException e) {
            throw new RuntimeException(e);
        }


    }

    public static int ConvertDPtoPX(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public class SpacesItemDecoration extends RecyclerView.ItemDecoration {

        private int mSpacing;
        private int mTopSpacing;

        public SpacesItemDecoration(int spacing, int topSpacing) {
            this.mSpacing = spacing;
            this.mTopSpacing = topSpacing;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            // Column Index
            int index = ((GridLayoutManager.LayoutParams) view.getLayoutParams()).getSpanIndex();
            // Item 포지션
            int position = parent.getChildLayoutPosition(view);
            if (index == 0) {
                //좌측 Spacing 절반
                outRect.right = mSpacing/ 2;
            } else {
                //우측 Spacing 절반
                outRect.left = mSpacing/ 2;
            }
            // 상단 탑 Spacing 맨 위에 포지션 0, 1은 Spacing을 안 줍니다.
            if (position < 2) {
                outRect.top = 0;
            } else {
                outRect.top = mTopSpacing;
            }
        }
    }

    class GridViewAdapter extends RecyclerView.Adapter<GridViewAdapter.ViewHolder> {

        Context context;
        ArrayList<Level> levels;


        public GridViewAdapter(ArrayList<Level> levels){
            this.levels = levels;
        }


        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            context = parent.getContext();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) ;
            View view = inflater.inflate(R.layout.word_home_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Level level = levels.get(position);

            holder.tv_title.setText(String.format("HSK %d 급", level.level));
            holder.tv_count.setText(String.format("%d개의 단어", level.count));
            holder.seekBar.setRange(0, 100);
            holder.tv_complete.setText(String.format("%d%%", level.getWordSafePercent(context)));
            holder.seekBar.setProgress(level.getWordSafePercent(context));
            holder.seekBar.setEnabled(false);
        }

        @Override
        public int getItemCount() {
            return levels.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            TextView tv_title;
            TextView tv_count;
            TextView tv_complete;
            RangeSeekBar seekBar;


            ViewHolder(View itemView) {
                super(itemView);

                tv_title = itemView.findViewById(R.id.tv_title);
                tv_count = itemView.findViewById(R.id.tv_sub);
                tv_complete = itemView.findViewById(R.id.value);
                seekBar = itemView.findViewById(R.id.seekbar);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = getAdapterPosition() ;
                        if (pos != RecyclerView.NO_POSITION) {
                            // TODO : use pos.
                            showDialog(levels.get(pos), WordActivity.WORD_MODE_DEFAULT);
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

    public void showDialog(Level level, int mode){
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View mdialog = inflater.inflate(R.layout.word_dialog, null);
        AlertDialog.Builder buider = new AlertDialog.Builder(getContext(), android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
        buider.setView(mdialog);
        buider.setCancelable(true);
        Dialog dialog = buider.create();


        TextView tv_title = mdialog.findViewById(R.id.tv_title);
        TextView tv_count = mdialog.findViewById(R.id.tv_count);
        TextView tv_cancel = mdialog.findViewById(R.id.tv_cancel);
        CardView btn_word = mdialog.findViewById(R.id.btn_word);
        CardView btn_test = mdialog.findViewById(R.id.btn_test);

        if (mode == WordActivity.WORD_MODE_DEFAULT) {
            tv_title.setText(String.format("HSK %d급", level.level));
            tv_count.setText(String.format("%d / %d", level.getWordCount(), level.count));
        }
        if (mode == WordActivity.WORD_MODE_WRONG) {
            tv_title.setText("틀린 단어");
            tv_count.setVisibility(View.INVISIBLE);
        }
        if (mode == WordActivity.WORD_MODE_REVIEW) {
            tv_title.setText("복습 단어");
            tv_count.setVisibility(View.INVISIBLE);
        }
        if (mode == WordActivity.WORD_MODE_STAR) {
            tv_title.setText("별표 친 단어");
            tv_count.setVisibility(View.INVISIBLE);
        }

        btn_word.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(getContext(), WordActivity.class);
                intent.putExtra("level", level.level);
                intent.putExtra("mode", mode);
                getContext().startActivity(intent);
            }
        });

        btn_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                showTestDialog(level, mode);
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



    public void showLevelDialog(int mode){
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View mdialog = inflater.inflate(R.layout.select_level_dialog, null);
        AlertDialog.Builder buider = new AlertDialog.Builder(getContext(), android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
        buider.setView(mdialog);
        buider.setCancelable(true);
        Dialog dialog = buider.create();

        RecyclerView rc = mdialog.findViewById(R.id.rc);

        LevelViewAdapter adapter = new LevelViewAdapter(levels, mode);
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


        public LevelViewAdapter(ArrayList<Level> levels, int mode){
            this.levels = levels;
            this.mode = mode;
        }


        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            context = parent.getContext();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) ;
            View view = inflater.inflate(R.layout.level_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Level level = levels.get(position);
            holder.tv_title.setText(String.format("%d급 - ", level.level));
            int count = 0;
            if (mode == WordActivity.WORD_MODE_WRONG){
                count = level.getWrongWordCount();
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
                            showDialog(levels.get(pos), mode);
                        }
                    }
                });
            }
        }
    }
}