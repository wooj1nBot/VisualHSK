package com.release.visualhsk;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.release.visualhsk.dto.User;

import org.json.JSONException;

public class MainActivity extends AppCompatActivity {

    TextView tv1, tv2, tv3;
    ImageView iv1, iv2, iv3;

    HomeFragment homeFragment;
    WrongFragment wrongFragment;
    TrainningFragment trainningFragment;
    FragmentManager manager;
    private long pressedTime;

    private final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 991;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (User.getId(MainActivity.this).equals("")){
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }else{
            if (Setting.getAlarmType(this) == Setting.ALARM_TYPE_TIME){
                Setting.addPopup(this);
            }
            if (Setting.getAlarmType(this) == Setting.ALARM_TYPE_START){
                Intent receiverIntent = new Intent(this, PopupReceiver.class);
                sendBroadcast(receiverIntent);
            }
        }

        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(/*context=*/ this);




        tv1 = findViewById(R.id.tv1);
        tv2 = findViewById(R.id.tv2);
        tv3 = findViewById(R.id.tv3);

        iv1 = findViewById(R.id.iv1);
        iv2 = findViewById(R.id.iv2);
        iv3 = findViewById(R.id.iv3);


        homeFragment = new HomeFragment();
        wrongFragment = new WrongFragment();
        trainningFragment = new TrainningFragment();



        manager = getSupportFragmentManager();
        manager.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);
                clear();
                if (f instanceof TrainningFragment) {
                    iv1.setImageTintList(ColorStateList.valueOf(Color.parseColor("#D6BA08")));
                    tv1.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                    tv1.setTextColor(Color.parseColor("#D6BA08"));
                }
                if (f instanceof HomeFragment) {
                    iv2.setImageTintList(ColorStateList.valueOf(Color.parseColor("#D6BA08")));
                    tv2.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                    tv2.setTextColor(Color.parseColor("#D6BA08"));
                }
                if (f instanceof WrongFragment) {
                    iv3.setImageTintList(ColorStateList.valueOf(Color.parseColor("#D6BA08")));
                    tv3.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                    tv3.setTextColor(Color.parseColor("#D6BA08"));
                }

            }
        }, true);

        if (!checkDrawPermission()){
            makeDialog();
        }


        initFragment();
    }

    public void onClick(View v){
        FragmentTransaction transaction = manager.beginTransaction();

        if (v.getId() == R.id.mypage) {
            transaction.replace(R.id.parent, trainningFragment);
            transaction.commitAllowingStateLoss();
        } else if (v.getId() == R.id.home) {
            transaction.replace(R.id.parent, homeFragment);
            transaction.commitAllowingStateLoss();
        } else if (v.getId() == R.id.premium) {
            transaction.replace(R.id.parent, wrongFragment);
            transaction.commitAllowingStateLoss();
        }
    }

    @Override
    public void onBackPressed() {
        FragmentTransaction transaction = manager.beginTransaction();
        if (manager.getBackStackEntryCount() > 1) {
            Fragment fragment = manager.findFragmentById(R.id.parent);
            if (fragment != null) {
                transaction.remove(fragment).commit();
                manager.popBackStack();
            }
        }else{
            if ( pressedTime == 0 ) {
                Toast.makeText(MainActivity.this, " 한 번 더 누르면 종료됩니다." , Toast.LENGTH_LONG).show();
                pressedTime = System.currentTimeMillis();
            }
            else {
                int seconds = (int) (System.currentTimeMillis() - pressedTime);

                if ( seconds > 2000 ) {
                    Toast.makeText(MainActivity.this, " 한 번 더 누르면 종료됩니다." , Toast.LENGTH_LONG).show();
                    pressedTime = 0 ;
                }
                else {
                    super.onBackPressed();
                    finish(); // app 종료 시키기
                }
            }
        }

    }

    public void makeDialog(){
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View mdialog = inflater.inflate(R.layout.permission_dialog, null);
        AlertDialog.Builder buider = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
        buider.setView(mdialog);
        buider.setCancelable(true);
        Dialog dialog = buider.create();
        MaterialCardView ok = mdialog.findViewById(R.id.ok);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPermission();
                dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                Setting.setAlarmType(MainActivity.this, Setting.ALARM_TYPE_NO);
            }
        });
        dialog.show();
        Window window = dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    public boolean checkDrawPermission() {
        if (Setting.getAlarmType(this) == Setting.ALARM_TYPE_NO) return true;
        return Settings.canDrawOverlays(this);
    }

    public void requestPermission(){
        Uri uri = Uri.parse("package:"+ getPackageName());
        try {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri), ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
        }catch (ActivityNotFoundException e){
            Toast.makeText(MainActivity.this, "This is an unsupported device.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if(!checkDrawPermission()) {
                Setting.setAlarmType(MainActivity.this, Setting.ALARM_TYPE_NO);
            }
        }
    }

    public void initFragment(){
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(R.id.parent, homeFragment);
        transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }

    public void clear(){
        iv1.setImageTintList(ColorStateList.valueOf(Color.parseColor("#808080")));
        tv1.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        tv1.setTextColor(Color.parseColor("#555555"));

        iv2.setImageTintList(ColorStateList.valueOf(Color.parseColor("#808080")));
        tv2.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        tv2.setTextColor(Color.parseColor("#555555"));

        iv3.setImageTintList(ColorStateList.valueOf(Color.parseColor("#808080")));
        tv3.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        tv3.setTextColor(Color.parseColor("#555555"));
    }
















}