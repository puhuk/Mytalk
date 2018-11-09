package com.example.oc.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText id;
    private EditText password;

    private Button login;
    private Button signup;
    private FirebaseRemoteConfig FirebaseRemoteConfig;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        firebaseAuth=FirebaseAuth.getInstance();
        firebaseAuth.signOut();

        String splash_background=FirebaseRemoteConfig.getString(getString(R.string.rc_color));
        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.LOLLIPOP){
            getWindow().setStatusBarColor(Color.parseColor(splash_background));
        }

        id=(EditText)findViewById(R.id.loginActivity_edittext_id);
        password=(EditText)findViewById(R.id.loginActivity_edittext_password);

        login=(Button)findViewById(R.id.loginActivity_button_login);
        signup =(Button)findViewById(R.id.loginActivity_button_signup);

        login.setBackgroundColor(Color.parseColor(splash_background));
        signup.setBackgroundColor(Color.parseColor(splash_background));



        login.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                loginEvent();
                startActivity(new Intent(LoginActivity.this,MainActivity.class));
                finish();

            }
        });

        signup.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent=new Intent(LoginActivity.this,SignupActivity.class);
                startActivity(intent);
                finish();
                /* startActivity(new Intent(LoginActivity.this,SignupActivity.class)); */

            }
        });

        //로그인 인터페이스 리스너
        authStateListener=new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user=firebaseAuth.getCurrentUser();
                if(user!=null){
                    //로그인
                    Intent intent=new Intent(LoginActivity.this,MainActivity.class);
                    startActivity(intent);
                    finish();
                }else{
                    //로그인 실패 시
                }
            }
        };
    }
    void loginEvent(){
        firebaseAuth.signInWithEmailAndPassword(id.getText().toString(),password.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {


                if (!task.isSuccessful()) {
                    // there was an error
                    if (password.length() < 6) {
                        password.setError(getString(R.string.minimum_password));
                    } else {
                        Toast.makeText(LoginActivity.this, getString(R.string.auth_failed), Toast.LENGTH_LONG).show();
                    }
                } else {
                    String uid=FirebaseAuth.getInstance().getCurrentUser().getUid();
                    String token= FirebaseInstanceId.getInstance().getToken();
                    Map<String,Object> map=new HashMap<>();
                    map.put("pushToken",token);

                    //Log.e("hello", (String)FirebaseAuth.getInstance().getCurrentUser().getDisplayName());

                    FirebaseDatabase.getInstance().getReference().child("users").child(uid).updateChildren(map);
                    //Log.e("hello",(String)FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    @Override
    protected void onStart(){
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseAuth.removeAuthStateListener(authStateListener);
    }
}
