package com.release.visualhsk;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Calendar;

public class Setting {

    public static final int ALARM_TYPE_START = 0;
    public static final int ALARM_TYPE_TIME = 1;
    public static final int ALARM_TYPE_NO = 2;

    public static final int[] SETTING_COMPLETE_TIMES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    public static final int[] SETTING_TEST_TIME = {3, 5, 10, 15, 20, 25, 30, 35, 45, 50, -1};

    public static void removeAllSetting(Context context){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        preferences.edit().clear().apply();
    }

    public static int getAlarmType(Context context){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        return preferences.getInt("alarm", ALARM_TYPE_START);
    }

    public static void setAlarmType(Context context, int i){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("alarm", i);
        editor.apply();
    }

    public static int[] getAlarmTime(Context context){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        int hour = preferences.getInt("hour", 12);
        int minute = preferences.getInt("minute", 0);
        return new int[]{hour, minute};
    }

    public static void setAlarmTime(Context context, int h, int m){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("hour", h);
        editor.putInt("minute", m);
        editor.apply();
        setNotice(context, h, m);
    }



    public static int getCompleteCount(Context context){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        return SETTING_COMPLETE_TIMES[preferences.getInt("review", 4)];
    }

    public static int getCompleteIdx(Context context){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        return preferences.getInt("review", 4);
    }

    public static void setCompleteCount(Context context, int idx){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("review", idx);
        editor.apply();
    }

    public static int getRightCount(Context context){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        return SETTING_COMPLETE_TIMES[preferences.getInt("right", 4)];
    }
    public static int getRightIdx(Context context){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        return preferences.getInt("right", 4);
    }

    public static void setRightCount(Context context, int idx){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("right", idx);
        editor.apply();
    }

    public static int getTimerSec(Context context){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        return SETTING_TEST_TIME[preferences.getInt("timer", 3)];
    }

    public static int getTimerIdx(Context context){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        return preferences.getInt("timer", 3);
    }

    public static void setTimerSec(Context context, int idx){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("timer", idx);
        editor.apply();
    }

    public static void addPopup(Context context){
        int[] time = getAlarmTime(context);
        setNotice(context, time[0], time[1]);
    }

    public static void setNotice(Context context, int alarmHour, int alarmMinute) {
        removeAlarm(context);
        //알람을 수신할 수 있도록 하는 리시버로 인텐트 요청

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, alarmHour);
        calendar.set(Calendar.MINUTE, alarmMinute);

        AlarmManager mAlarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent receiverIntent = new Intent(context, PopupReceiver.class);
        PendingIntent mAlarmIntent = PendingIntent.getBroadcast(context, 997, receiverIntent, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        //알람시간 설정
        //param 1)알람의 타입
        //param 2)알람이 울려야 하는 시간(밀리초)을 나타낸다.
        //param 3)알람이 울릴 때 수행할 작업을 나타냄
        mAlarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, mAlarmIntent);
    }

    public static void removeAlarm(Context context){
        AlarmManager mAlarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent receiverIntent = new Intent(context, PopupReceiver.class);
        PendingIntent mAlarmIntent = PendingIntent.getBroadcast(context, 997, receiverIntent, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        mAlarmMgr.cancel(mAlarmIntent);
        mAlarmIntent.cancel();
    }







}
