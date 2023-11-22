package com.release.visualhsk;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.jaygoo.widget.RangeSeekBar;
import com.release.visualhsk.dto.Level;
import com.release.visualhsk.dto.User;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;


public class TrainningFragment extends Fragment {
    RecyclerView rc;

    User user;

    public TrainningFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_trainning, container, false);


        rc = view.findViewById(R.id.rc);
        rc.setLayoutManager(new LinearLayoutManager(getContext()));

        getData();
        return view;
    }


    public void getData(){

        AssetManager manager = getResources().getAssets();
        String[] list;

        try {
            list = manager.list("res");
            ArrayList<String> folders = new ArrayList<>(Arrays.asList(list));
            folders.sort(String::compareTo);
            ArrayList<Level> progress = new ArrayList<>();
            ArrayList<Level> complete = new ArrayList<>();

            for (int i = 1; i <= folders.size(); i++) {

                InputStream inputStream = manager.open("res/" + folders.get(i-1));
                Workbook wb = Workbook.getWorkbook(inputStream);
                Sheet sheet = wb.getSheet(0);
                int rowTotal = sheet.getColumn(0).length;
                Level level = new Level(i, rowTotal);
                progress.add(level);
                inputStream.close();
            }

            LevelAdapter adapter = new LevelAdapter(complete, progress, 0);
            rc.setAdapter(adapter);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(User.getId(getActivity())).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful() && task.getResult().exists()){
                        user = task.getResult().toObject(User.class);

                        if (user.levels != null) {

                            for (String key : user.levels.keySet()) {
                                int k = Integer.parseInt(key);
                                Level level = user.levels.get(key);
                                if (level.getWordSafePercent(getActivity()) == 100){
                                    complete.add(level);
                                }
                                progress.set(k-1, level);
                            }

                            for (Level level : complete){
                                progress.removeIf(level1 -> level.level == level1.level);
                            }

                            adapter.all_time = user.study_time;

                            adapter.notifyDataSetChanged();
                        }
                    }
                }
            });



        } catch (IOException | BiffException e) {
            throw new RuntimeException(e);
        }


    }


    class LevelAdapter extends RecyclerView.Adapter<LevelAdapter.ViewHolder> {

        Context context;
        ArrayList<Level> complete;
        ArrayList<Level> progress;

        long all_time;
        int sum_count;
        int review_count;

        public LevelAdapter(ArrayList<Level> complete, ArrayList<Level> progress, long all_time){
            this.complete = complete;
            this.progress = progress;
            this.all_time = all_time;

            ArrayList<Level> levels = new ArrayList<>(complete);
            levels.addAll(progress);
            for (Level level : levels){
                sum_count += level.count;
                review_count += level.getReviewWordCount();
            }

        }


        @NonNull
        @Override
        public LevelAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            context = parent.getContext();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) ;
            View view = inflater.inflate(R.layout.history_item_layout, parent, false);
            return new LevelAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LevelAdapter.ViewHolder holder, int position) {

            Level level;

            if (complete.size() <= position){
                level = progress.get(position-complete.size());
                if (position-complete.size() == 0){
                    holder.tv_title.setText("진행 중인 급수");
                    holder.tv_title.setVisibility(View.VISIBLE);
                    if (complete.size() == 0){
                        holder.cd.setVisibility(View.VISIBLE);
                    }else{
                        holder.cd.setVisibility(View.GONE);
                    }
                }else{
                    holder.tv_title.setVisibility(View.GONE);
                    holder.cd.setVisibility(View.GONE);
                }
                holder.iv_safe.setVisibility(View.GONE);
            }else{
                level = complete.get(position);
                if (position == 0){
                    holder.tv_title.setText("완료한 급수");
                    holder.tv_title.setVisibility(View.VISIBLE);
                    holder.cd.setVisibility(View.VISIBLE);
                }else{
                    holder.tv_title.setVisibility(View.GONE);
                    holder.cd.setVisibility(View.GONE);
                }
                holder.iv_safe.setVisibility(View.VISIBLE);
            }

            if (level.getWordSafePercent(getContext()) == 100){
                holder.l1.setVisibility(View.GONE);
                holder.l2.setVisibility(View.GONE);
            }else{
                holder.l1.setVisibility(View.VISIBLE);
                holder.l2.setVisibility(View.VISIBLE);

            }
            holder.indicator.setProgress((int) ((float) review_count / sum_count) / 100f, 100);
            holder.pro_value.setText((int) holder.indicator.getProgress() + "%");
            holder.tv_progress.setText(String.format("%d / %d", review_count, sum_count));

            long diff = all_time;

            long diffHor = (diff) / 3600000; //시 차이
            diff %= 3600000;
            long diffMin = (diff) / 60000; //분 차이
            diff %= 60000;
            long diffSec = (diff) / 1000; //초 차이

            String time = String.format("%02d : %02d : %02d", diffHor, diffMin, diffSec);

            holder.tv_time.setText(time);

            holder.tv_complete.setText(level.getWordSafeCount(context) + " / " + level.count);
            holder.tv_safe.setText(level.getWordSafePercent(context) + " %");
            holder.tv_level.setText(level.level + "급");

            if (holder.tv_bar.getTag().equals("collapse")){
                holder.detail.setVisibility(View.GONE);
                holder.iv_bar.setImageResource(R.drawable.arrow_down);
                holder.tv_bar.setText("세부정보");
            }else{
                holder.detail.setVisibility(View.VISIBLE);
                holder.iv_bar.setImageResource(R.drawable.arrow_forward_ios);
                holder.tv_bar.setText("접기");
            }

            holder.bar.setTag(holder);

            holder.bar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ViewHolder holder = (ViewHolder) view.getTag();
                    if (holder.tv_bar.getTag().equals("collapse")){
                        holder.detail.setVisibility(View.VISIBLE);
                        holder.iv_bar.setImageResource(R.drawable.arrow_forward_ios);
                        holder.tv_bar.setText("접기");
                        holder.tv_bar.setTag("expand");
                        holder.ed1.post(new Runnable() {
                            @Override
                            public void run() {
                                holder.ed1.setFocusableInTouchMode(true);
                                holder.ed1.requestFocus();
                            }
                        });
                    }else{
                        holder.detail.setVisibility(View.GONE);
                        holder.iv_bar.setImageResource(R.drawable.arrow_down);
                        holder.tv_bar.setText("세부정보");
                        holder.tv_bar.setTag("collapse");
                    }
                }
            });

            holder.tv_right.setText(level.getRightWordCount() + " 단어");
            holder.tv_wrong.setText(level.getWrongWordCount() + " 단어");

            holder.ed1.setText(String.valueOf(90));
            holder.ed1.setImeOptions(EditorInfo.IME_ACTION_DONE);
            holder.ed1.addTextChangedListener(new TextChangeListener(holder, level));
            holder.ed2.setText(String.valueOf(Setting.getCompleteCount(getContext())));
            holder.ed2.setEnabled(false);
            holder.tv1.setText(String.format("%% 안심율까지 %d 단어 남았습니다.", level.getWordSafeCount(90, getContext())));
            holder.tv2.setText(String.format("단어씩 외우면,  %d 일이 걸립니다.", level.getWordSafeDays(90, Setting.getCompleteCount(getContext()), getContext())));
        }

        @Override
        public int getItemCount() {
            return complete.size() + progress.size();
        }

        public class TextChangeListener implements TextWatcher{

            ViewHolder viewHolder;
            Level level;

            TextChangeListener(ViewHolder viewHolder, Level level){
                this.viewHolder = viewHolder;
                this.level = level;
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().length() > 0) {
                    int p = Integer.parseInt(editable.toString());
                    viewHolder.tv1.setText(String.format("%% 안심율까지 %d 단어 남았습니다.", level.getWordSafeCount(p, getContext())));
                    viewHolder.tv2.setText(String.format("단어씩 외우면,  %d 일이 걸립니다.", level.getWordSafeDays(p, Setting.getCompleteCount(getContext()), getContext())));
                }
            }
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            TextView tv_level;
            TextView tv_safe;
            TextView tv_complete, tv_right, tv_wrong, tv1, tv2, tv_bar, tv_title, pro_value;
            EditText ed1, ed2;

            LinearLayout detail, bar, l1, l2;
            ImageView iv_safe, iv_bar;

            CardView cd;
            CircularProgressIndicator indicator;
            TextView tv_progress, tv_time;


            ViewHolder(View itemView) {
                super(itemView);
                tv_title = itemView.findViewById(R.id.textView5);
                tv_level = itemView.findViewById(R.id.level);
                tv_safe = itemView.findViewById(R.id.safe);
                tv_complete = itemView.findViewById(R.id.complete);
                tv_right = itemView.findViewById(R.id.right);
                tv_wrong = itemView.findViewById(R.id.wrong);
                pro_value = itemView.findViewById(R.id.pro_value);
                tv1 = itemView.findViewById(R.id.tv1);
                tv2 = itemView.findViewById(R.id.tv2);
                l1 = itemView.findViewById(R.id.l1);
                l2 = itemView.findViewById(R.id.l2);
                ed1 = itemView.findViewById(R.id.ed1);
                ed2 = itemView.findViewById(R.id.ed2);
                tv_bar = itemView.findViewById(R.id.textView8);
                detail = itemView.findViewById(R.id.detail);
                bar = itemView.findViewById(R.id.bar);
                iv_safe = itemView.findViewById(R.id.imageView5);
                iv_bar = itemView.findViewById(R.id.imageView6);

                cd = itemView.findViewById(R.id.cd);
                indicator = itemView.findViewById(R.id.circular_progress);
                tv_progress = itemView.findViewById(R.id.progress);
                tv_time = itemView.findViewById(R.id.time);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = getAdapterPosition() ;
                        if (pos != RecyclerView.NO_POSITION) {
                            // TODO : use pos.

                        }
                    }
                });
            }
        }
    }


}