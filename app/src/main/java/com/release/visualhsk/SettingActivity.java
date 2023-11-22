package com.release.visualhsk;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.release.visualhsk.dto.User;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.Calendar;

public class SettingActivity extends AppCompatActivity {
    RadioButton r1, r2, r3;
    TimePicker picker;
    LinearLayout s1, s2, s3;
    TextView t1, t2, t3;
    NumberPicker rangePicker;
    TextView tv_done;
    SlidingUpPanelLayout layout;
    boolean isRequest = false;
    private final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 991;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        r1 = findViewById(R.id.r1);
        r2 = findViewById(R.id.r2);
        r3 = findViewById(R.id.r3);
        picker = findViewById(R.id.timepicker);
        s1 = findViewById(R.id.sp1);
        s2 = findViewById(R.id.sp2);
        s3 = findViewById(R.id.sp3);
        t1 = findViewById(R.id.sp_1);
        t2 = findViewById(R.id.sp_2);
        t3 = findViewById(R.id.sp_3);
        layout = findViewById(R.id.sliding_layout);

        rangePicker = findViewById(R.id.picker);
        tv_done = findViewById(R.id.done);

        CardView logout = findViewById(R.id.logout);

        ImageView back = findViewById(R.id.back);

        ColorStateList colorStateList = new ColorStateList(
                new int[][]
                        {
                                new int[]{-android.R.attr.state_checked}, // Disabled
                                new int[]{android.R.attr.state_checked}   // Enabled
                        },
                new int[]
                        {
                                Color.parseColor("#C2C2C2"), // disabled
                                Color.parseColor("#F3D409")   // enabled
                        }
        );

        r1.setButtonTintList(colorStateList); // set the color tint list
        r1.invalidate(); // Could not be necessary
        r2.setButtonTintList(colorStateList); // set the color tint list
        r2.invalidate(); // Could not be necessary
        r3.setButtonTintList(colorStateList); // set the color tint list
        r3.invalidate(); // Could not be necessary

        setSetting();

        layout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

        layout.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                User.logout(SettingActivity.this);
                Intent intent = new Intent(SettingActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }

    private void showActionsBottomSheet() {


    }

    public void setSetting(){
        int alarm = Setting.getAlarmType(this);
        r1.setChecked(false);
        r2.setChecked(false);
        r3.setChecked(false);
        picker.setVisibility(View.GONE);
        picker.setEnabled(false);

        picker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker timePicker, int i, int i1) {
                Setting.setAlarmTime(SettingActivity.this, i, i1);
            }
        });

        if (alarm == Setting.ALARM_TYPE_START){
            r1.setChecked(true);
        }
        if (alarm == Setting.ALARM_TYPE_TIME){
            r2.setChecked(true);
            picker.setVisibility(View.VISIBLE);
            int[] time = Setting.getAlarmTime(this);
            picker.setHour(time[0]);
            picker.setMinute(time[1]);
            picker.setEnabled(true);
        }
        if (alarm == Setting.ALARM_TYPE_NO){
            r3.setChecked(true);
        }

        t1.setText(Setting.getRightCount(this) + "회");
        t2.setText(Setting.getCompleteCount(this) + "일");
        if (Setting.getTimerSec(this) == -1){
            t3.setText("없음");
        }else{
            t3.setText(Setting.getTimerSec(this) + "초");
        }

        s1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] arr = new String[Setting.SETTING_COMPLETE_TIMES.length];
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = Setting.SETTING_COMPLETE_TIMES[i] + "회";
                }
                rangePicker.setDisplayedValues(null);
                rangePicker.setMinValue(0);
                rangePicker.setMaxValue(arr.length-1);
                rangePicker.setDisplayedValues(arr);
                rangePicker.setValue(Setting.getRightIdx(SettingActivity.this));
                layout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);

                rangePicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                        Setting.setRightCount(SettingActivity.this, i1);
                        t1.setText(arr[i1]);
                    }
                });
            }
        });
        s2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] arr = new String[Setting.SETTING_COMPLETE_TIMES.length];
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = Setting.SETTING_COMPLETE_TIMES[i] + "일";
                }
                rangePicker.setDisplayedValues(null);
                rangePicker.setMinValue(0);
                rangePicker.setMaxValue(arr.length-1);
                rangePicker.setDisplayedValues(arr);
                rangePicker.setValue(Setting.getCompleteIdx(SettingActivity.this));
                layout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);

                rangePicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                        Setting.setCompleteCount(SettingActivity.this, i1);
                        t2.setText(arr[i1]);
                    }
                });
            }
        });
        s3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] arr = new String[Setting.SETTING_TEST_TIME.length];
                for (int i = 0; i < arr.length; i++) {
                    if (Setting.SETTING_TEST_TIME[i] == -1){
                        arr[i] = "없음";
                    }else{
                        arr[i] = Setting.SETTING_TEST_TIME[i] + "초";
                    }
                }
                rangePicker.setDisplayedValues(null);
                rangePicker.setMinValue(0);
                rangePicker.setMaxValue(arr.length-1);
                rangePicker.setDisplayedValues(arr);
                rangePicker.setValue(Setting.getTimerIdx(SettingActivity.this));
                layout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);

                rangePicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                        Setting.setTimerSec(SettingActivity.this, i1);
                        t3.setText(arr[i1]);
                    }
                });
            }
        });

        tv_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });

    }

    public void makeDialog(){
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View mdialog = inflater.inflate(R.layout.permission_dialog, null);
        AlertDialog.Builder buider = new AlertDialog.Builder(SettingActivity.this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
        buider.setView(mdialog);
        buider.setCancelable(true);
        Dialog dialog = buider.create();
        MaterialCardView ok = mdialog.findViewById(R.id.ok);
        isRequest = false;

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isRequest = true;
                requestPermission();
                dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (isRequest) return;

                r1.setChecked(false);
                r2.setChecked(false);
                r3.setChecked(true);
                picker.setVisibility(View.GONE);
                Setting.setAlarmType(SettingActivity.this, Setting.ALARM_TYPE_NO);
            }
        });
        dialog.show();
        Window window = dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    public boolean checkDrawPermission() {
        return Settings.canDrawOverlays(this);
    }

    public void requestPermission(){
        Uri uri = Uri.parse("package:"+ getPackageName());
        try {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri), ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
        }catch (ActivityNotFoundException e){
            Toast.makeText(SettingActivity.this, "This is an unsupported device.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if(!checkDrawPermission()) {
                r1.setChecked(false);
                r2.setChecked(false);
                r3.setChecked(true);
                picker.setVisibility(View.GONE);
                Setting.setAlarmType(SettingActivity.this, Setting.ALARM_TYPE_NO);
            }
        }
    }

    public void onClick(View v){
        r1.setChecked(false);
        r2.setChecked(false);
        r3.setChecked(false);
        ((RadioButton) v).setChecked(true);

        if (v.getId() == R.id.r2){
            picker.setVisibility(View.VISIBLE);
            int[] time = Setting.getAlarmTime(this);
            picker.setHour(time[0]);
            picker.setMinute(time[1]);
            picker.setEnabled(true);

            if (checkDrawPermission()){
                Setting.setAlarmType(this, Setting.ALARM_TYPE_TIME);
            }else{
                makeDialog();
            }
        }else{
            picker.setVisibility(View.GONE);
            if (v.getId() == R.id.r1){
                if (checkDrawPermission()){
                    Setting.setAlarmType(this, Setting.ALARM_TYPE_START);
                }else{
                    makeDialog();
                }
            }
            if (v.getId() == R.id.r3){
                Setting.setAlarmType(this, Setting.ALARM_TYPE_NO);
            }
        }
    }


}