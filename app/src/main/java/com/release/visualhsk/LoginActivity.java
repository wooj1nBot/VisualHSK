package com.release.visualhsk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kakao.sdk.auth.model.OAuthToken;
import com.kakao.sdk.user.UserApiClient;
import com.kakao.sdk.user.model.User;
import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;

import org.apache.log4j.chainsaw.Main;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class LoginActivity extends AppCompatActivity {

    GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    Function2<OAuthToken, Throwable, Unit> callback;

    int RC_SIGN_IN = 9001; //구글 로그인을 감지하기 위한 변수
    OAuthLogin mOAuthLoginModule;

    LoadingView loadingView;

    OAuthLoginHandler mOAuthLoginHandler;

    @SuppressLint("HandlerLeak")



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);



        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestProfile()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        callback = new Function2<OAuthToken, Throwable, Unit>() {
            @Override
            public Unit invoke(OAuthToken oAuthToken, Throwable throwable) {
                if (oAuthToken == null) {
                    Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    loadingView.stop();
                    return null;
                }
                handleKakaoLogin();
                return null;
            }
        };


        mOAuthLoginModule = OAuthLogin.getInstance();
        mOAuthLoginModule.init(
                LoginActivity.this
                ,getString(R.string.naver_client_id)
                ,getString(R.string.naver_client_secret)
                ,getString(R.string.naver_client_name)
                //,OAUTH_CALLBACK_INTENT
                // SDK 4.1.4 버전부터는 OAUTH_CALLBACK_INTENT변수를 사용하지 않습니다.
        );

        mOAuthLoginHandler = new OAuthLoginHandler() {


            @Override
            public void run(boolean success) {
                if (success) {
                    new RequestApiTask(LoginActivity.this, mOAuthLoginModule, loadingView).execute();
                } else {
                    Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    loadingView.stop();
                }
            };
        };


        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void handleKakaoLogin(){
        UserApiClient.getInstance().me(new Function2<User, Throwable, Unit>() {
            @Override
            public Unit invoke(User user, Throwable throwable) {
                //로그인 되었다면
                if(user != null){
                    long id = user.getId();
                    login(String.valueOf(id), loadingView);
                }
                // 안되어 있으면
                else{
                    loadingView.stop();
                    Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
                return null;
            }
        });
    }

    public void onClick(View v){

        loadingView = new LoadingView(this);
        loadingView.show("로그인 중...");

        if (v.getId() == R.id.google){
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        }
        if (v.getId() == R.id.kakao){
            if(UserApiClient.getInstance().isKakaoTalkLoginAvailable(LoginActivity.this)){
                //카카오톡이 설치되어 있을 경우 -> 앱에서 로그인
                UserApiClient.getInstance().loginWithKakaoTalk(LoginActivity.this, callback);
            }else{ //카카오톡이 설치되어 있지 않다면 -> 웹에서 로그인
                UserApiClient.getInstance().loginWithKakaoAccount(LoginActivity.this, callback);
            }
        }
        if (v.getId() == R.id.naver){
            mOAuthLoginModule.startOauthLoginActivity(LoginActivity.this, mOAuthLoginHandler);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    public class RequestApiTask extends AsyncTask<Void, Void, String> {
        private final Context mContext;
        private final OAuthLogin mOAuthLoginModule;
        private LoadingView loadingView;

        public RequestApiTask(Context mContext, OAuthLogin mOAuthLoginModule, LoadingView loadingView) {
            this.mContext = mContext;
            this.mOAuthLoginModule = mOAuthLoginModule;
            this.loadingView = loadingView;
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected String doInBackground(Void... params) {
            String url = "https://openapi.naver.com/v1/nid/me";
            String at = mOAuthLoginModule.getAccessToken(mContext);
            return mOAuthLoginModule.requestApi(mContext, at, url);
        }

        protected void onPostExecute(String content) {
            try {
                JSONObject loginResult = new JSONObject(content);
                if (loginResult.getString("resultcode").equals("00")){
                    JSONObject response = loginResult.getJSONObject("response");
                    String id = response.getString("id");
                    login(id, loadingView);
                }else{
                    loadingView.stop();
                    Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }

            } catch (JSONException e) {
                loadingView.stop();
                Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {

            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String token = account.getIdToken();

            AuthCredential credential = GoogleAuthProvider.getCredential(token, null);
            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            Log.d("login", task.getResult().toString());
                            if (!task.isSuccessful()) {
                                loadingView.stop();
                                Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            FirebaseUser user = mAuth.getCurrentUser();
                            login(user.getUid(), loadingView);
                        }
                    });
        } catch (ApiException e) {
            loadingView.stop();
            Log.d("login", e.toString());
            Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void login(String id, LoadingView loadingView){

        com.release.visualhsk.dto.User user = new com.release.visualhsk.dto.User(id);

        db.collection("users").document(id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult().exists()){
                    loadingView.stop();
                    com.release.visualhsk.dto.User.saveId(LoginActivity.this, id);
                    Toast.makeText(LoginActivity.this, "다시 오신 것을 환영합니다!", Toast.LENGTH_SHORT).show();
                    SharedPreferences preferences = getSharedPreferences("setting", MODE_PRIVATE);

                    if (!preferences.getBoolean("isIntro", false)){
                        Intent intent = new Intent(LoginActivity.this, IntroActivity.class);
                        startActivity(intent);
                        finish();
                    }else{
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }else{
                    db.collection("users").document(id).set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            loadingView.stop();
                            if (task.isSuccessful()){
                                com.release.visualhsk.dto.User.saveId(LoginActivity.this, id);
                                Toast.makeText(LoginActivity.this, "처음 오신 것을 환영합니다!", Toast.LENGTH_SHORT).show();
                                SharedPreferences preferences = getSharedPreferences("setting", MODE_PRIVATE);

                                if (!preferences.getBoolean("isIntro", false)){
                                    Intent intent = new Intent(LoginActivity.this, IntroActivity.class);
                                    startActivity(intent);
                                    finish();
                                }else{
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            }else{
                                Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }
}