package com.release.visualhsk.dto;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kakao.sdk.user.UserApiClient;
import com.nhn.android.naverlogin.OAuthLogin;
import com.release.visualhsk.R;
import com.release.visualhsk.Setting;

import java.util.HashMap;
import java.util.Random;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class User {

    public String uid;
    public long study_time;
    public Word current_word;
    public HashMap<String, Level> levels;

    public User(String uid){
        this.uid = uid;
        this.levels = new HashMap<>();
    }

    public User(){}


    public void changeLevel(Level level){
        levels.put(String.valueOf(level.level), level);
    }


    public String getUid() {
        return uid;
    }

    public HashMap<String, Level> getLevels() {
        return levels;
    }

    public long getStudy_time() {
        return study_time;
    }

    public Word getCurrent_word() {
        return current_word;
    }

    @Exclude
    public static String getId(Context context){
        SharedPreferences preferences = context.getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        return preferences.getString("uid", "");
    }

    @Exclude
    public static void saveId(Context context, String uid){
        SharedPreferences preferences = context.getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("uid", uid);
        editor.apply();
    }

    @Exclude
    public void saveUser(Context context){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(User.getId(context)).set(this);
    }

    @Exclude
    public void addStudyTime(Context context, long time){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(User.getId(context))
                .update("study_time", study_time + time);
    }

    @Exclude
    public static void logout(Context context){
        SharedPreferences.Editor editor = context.getSharedPreferences("UserInfo", Context.MODE_PRIVATE).edit();
        editor.remove("uid");
        editor.apply();

        logoutKakao();
        logoutGoogle(context);
        FirebaseAuth.getInstance().signOut();
        logoutNaver(context);
        Setting.removeAllSetting(context);
    }


    @Exclude
    private static void logoutNaver(Context context){
        OAuthLogin mOAuthLoginModule = OAuthLogin.getInstance();
        mOAuthLoginModule.init(
                context
                , context.getString(R.string.naver_client_id)
                ,context.getString(R.string.naver_client_secret)
                ,context.getString(R.string.naver_client_name)
                //,OAUTH_CALLBACK_INTENT
                // SDK 4.1.4 버전부터는 OAUTH_CALLBACK_INTENT변수를 사용하지 않습니다.
        );
        mOAuthLoginModule.logoutAndDeleteToken(context);
    }

    @Exclude
    private static void logoutKakao(){ UserApiClient.getInstance().logout(new Function1<Throwable, Unit>() { @Override public Unit invoke(Throwable throwable) { return null; } }); }

    @Exclude
    private static void logoutGoogle(Context context){
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
        mGoogleSignInClient.signOut();
    }
}
