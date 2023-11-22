package com.release.visualhsk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import me.relex.circleindicator.CircleIndicator3;

public class IntroActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        TextView skip = findViewById(R.id.skip);
        LinearLayout prev = findViewById(R.id.prev);
        LinearLayout next = findViewById(R.id.next);
        ViewPager2 pager2 = findViewById(R.id.viewpager);

        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = getSharedPreferences("setting", MODE_PRIVATE).edit();
                editor.putBoolean("isIntro", true);
                editor.apply();

                Intent intent = new Intent(IntroActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        prev.setVisibility(View.GONE);

        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pager2.setCurrentItem(pager2.getCurrentItem()-1);
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pager2.getCurrentItem() == 2){

                    SharedPreferences.Editor editor = getSharedPreferences("setting", MODE_PRIVATE).edit();
                    editor.putBoolean("isIntro", true);
                    editor.apply();

                    Intent intent = new Intent(IntroActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }else{
                    pager2.setCurrentItem(pager2.getCurrentItem()+1);
                }
            }
        });



        MyAdapter viewPager2Adapter = new MyAdapter(this);
        pager2.setAdapter(viewPager2Adapter);

        CircleIndicator3 mIndicator = findViewById(R.id.indicator);
        mIndicator.setViewPager(pager2);

        pager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                mIndicator.animatePageSelected(position);

                if (position == 0){
                    prev.setVisibility(View.GONE);
                    next.setVisibility(View.VISIBLE);
                } else if (position == 1) {
                    prev.setVisibility(View.VISIBLE);
                    next.setVisibility(View.VISIBLE);
                }else{
                    prev.setVisibility(View.VISIBLE);
                    next.setVisibility(View.VISIBLE);
                }

            }
        });

    }


    public static class PageFragment extends Fragment {
        // When requested, this adapter returns a DemoObjectFragment,
        // representing an object in the collection.
        RecyclerView rc;
        RelativeLayout add_tag;

        int[] layout = {R.layout.intro_page1, R.layout.intro_page2, R.layout.intro_page3};

        int pos;

        PageFragment(int pos){
            this.pos = pos;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return inflater.inflate(layout[pos], container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        }
    }

    public class MyAdapter extends FragmentStateAdapter {


        public MyAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return new PageFragment(position);
        }

        @Override
        public int getItemCount() {
            return 3;
        }

    }


}