package com.example.oc.myapplication;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.example.oc.myapplication.fragment.AccountFragment;
import com.example.oc.myapplication.fragment.ChatFragment;
import com.example.oc.myapplication.fragment.PeopleFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //uid=FirebaseAuth.getInstance().getCurrentUser().getUid();
        //passPushTokenToServer();

        setContentView(R.layout.activity_main);
        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.mainactivity_bottomnavigationview);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch(item.getItemId()){
                    case R.id.action_people:
                        getSupportFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout,new PeopleFragment()).commit();
                        return true;
                    case R.id.action_chat:
                        getSupportFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout,new ChatFragment()).commit();
                        return true;
                    case R.id.action_account:
                        getSupportFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout,new AccountFragment()).commit();
                        return true;
                }
                return false;
            }
        });
        passPushTokenToServer();

    }
    void passPushTokenToServer(){
        //Log.e("hello","uid : "+uid);
        if(FirebaseAuth.getInstance().getCurrentUser()!=null){
            uid=FirebaseAuth.getInstance().getCurrentUser().getUid();
            String token= FirebaseInstanceId.getInstance().getToken();
            Log.e("hello", token);
            Map<String,Object> map=new HashMap<>();
            map.put("pushToken",token);

            FirebaseDatabase.getInstance().getReference().child("users").child(uid).updateChildren(map);
        }else{
            Log.e("hello", "token is null");
        }



    }
}
