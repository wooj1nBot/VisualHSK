package com.release.visualhsk;

import static com.release.visualhsk.WordActivity.WORD_MODE_REVIEW;
import static com.release.visualhsk.WordActivity.WORD_MODE_STAR;
import static com.release.visualhsk.WordActivity.WORD_MODE_WRONG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.jaygoo.widget.RangeSeekBar;
import com.release.visualhsk.dto.Level;
import com.release.visualhsk.dto.User;
import com.release.visualhsk.dto.Word;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

public class WordListActivity extends AppCompatActivity {

    TextView tv_level;
    RecyclerView rc;
    ListViewAdapter adapter;
    int current;
    int mode;

    ArrayList<Word> words;
    Sheet sheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_list);
        tv_level = findViewById(R.id.tv_title2);
        rc = findViewById(R.id.rc);
        rc.setLayoutManager(new LinearLayoutManager(this));

        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Intent intent = getIntent();

        current = intent.getIntExtra("current", 1);
        mode = intent.getIntExtra("mode", WordActivity.WORD_MODE_DEFAULT);
        getLevel(intent.getIntExtra("level", 1));
    }




    public void getLevel(int level_pos){
        tv_level.setText("HSK " + level_pos + "급");
        AssetManager manager = getResources().getAssets();
        words = new ArrayList<>();

        try {

            String file = String.format("hsk%d.xls", level_pos);
            InputStream inputStream = manager.open("res/" + file);
            WorkbookSettings setting = new WorkbookSettings();
            setting.setEncoding("Cp1252");

            Workbook wb = Workbook.getWorkbook(inputStream, setting);
            sheet = wb.getSheet(0);
            int rowTotal = sheet.getColumn(0).length;

            Level level = new Level(level_pos, rowTotal);

            if (mode == WordActivity.WORD_MODE_DEFAULT) {
                for (int i = 0; i < rowTotal; i++) {
                    String w = sheet.getCell(0, i).getContents();
                    String voice = sheet.getCell(1, i).getContents();
                    String mean = sheet.getCell(2, i).getContents();
                    Word word = new Word(level_pos, i + 1, w, voice, mean);
                    words.add(word);
                }
            }

            adapter = new ListViewAdapter(level, words);
            rc.setAdapter(adapter);
            rc.scrollToPosition(current-5);

        } catch (IOException | BiffException e) {

        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(User.getId(this)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult().exists()){
                    User user = task.getResult().toObject(User.class);
                    if (user.levels != null && user.levels.containsKey(String.valueOf(level_pos))){
                        Level level = user.levels.get(String.valueOf(level_pos));
                        adapter.level = level;
                        if (mode != WordActivity.WORD_MODE_DEFAULT){
                            if (mode == WORD_MODE_WRONG){
                                words.addAll(level.getWrongDataSet(sheet, WordListActivity.this));
                            }
                            if (mode == WORD_MODE_REVIEW){
                                words.addAll(level.getReviewDataSet(sheet));
                            }
                            if (mode == WORD_MODE_STAR){
                                words.addAll(level.getStarDataSet(sheet));
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        });







    }


    class ListViewAdapter extends RecyclerView.Adapter<ListViewAdapter.ViewHolder> {

        Context context;
        Level level;
        ArrayList<Word> words;


        public ListViewAdapter(Level level, ArrayList<Word> words){
            this.level = level;
            this.words = words;
        }


        @NonNull
        @Override
        public ListViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            context = parent.getContext();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) ;
            View view = inflater.inflate(R.layout.word_list_litem, parent, false);
            return new ListViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ListViewAdapter.ViewHolder holder, int position) {
             Word word = words.get(position);
             holder.tv_word.setText(word.word);
             holder.tv_voice.setText(String.format("[ %s ]", word.voice));
             holder.tv_mean.setText(word.mean);
             holder.tv_pos.setText(String.format("%03d", word.pos));

             if (level.getWordState(word.pos) == Level.WORD_STATE_NOT_TEST){
                 holder.iv_test.setVisibility(View.GONE);
             }
             if (level.getWordState(word.pos) == Level.WORD_STATE_RIGHT){
                holder.iv_test.setVisibility(View.VISIBLE);
                holder.iv_test.setImageResource(R.drawable.right);
             }
             if (level.getWordState(word.pos) == Level.WORD_STATE_WRONG){
                 holder.iv_test.setVisibility(View.VISIBLE);
                 holder.iv_test.setImageResource(R.drawable.wong);
             }

             if (level.getIsComplete(word.pos, context)){
                 holder.iv_check.setVisibility(View.VISIBLE);
             }else{
                 holder.iv_check.setVisibility(View.INVISIBLE);
             }

             if (word.pos == current){
                 holder.itemView.setBackgroundColor(Color.parseColor("#FFF9D4"));
             }else{
                 holder.itemView.setBackgroundColor(Color.TRANSPARENT);
             }

             if (level.getWordStar(word.pos)){
                 holder.iv_star.setVisibility(View.VISIBLE);
             }else{
                 holder.iv_star.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return words.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            TextView tv_pos;
            TextView tv_word;
            TextView tv_mean;
            TextView tv_voice;
            ImageView iv_test;
            ImageView iv_star;

            ImageView iv_check;

            ViewHolder(View itemView) {
                super(itemView);

                tv_pos = itemView.findViewById(R.id.position);
                tv_word = itemView.findViewById(R.id.word);
                tv_mean = itemView.findViewById(R.id.mean);
                tv_voice = itemView.findViewById(R.id.voice);
                iv_test = itemView.findViewById(R.id.iv);
                iv_star = itemView.findViewById(R.id.star);
                iv_check = itemView.findViewById(R.id.imageView10);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = getAdapterPosition() ;
                        if (pos != RecyclerView.NO_POSITION) {
                            // TODO : use pos.
                            Intent intent = new Intent();
                            intent.putExtra("pos", pos+1);
                            setResult(1, intent);
                            finish();
                        }
                    }
                });
            }
        }
    }
}