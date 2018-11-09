package com.example.oc.myapplication.chat;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.oc.myapplication.R;
import com.example.oc.myapplication.model.ChatModel;
import com.example.oc.myapplication.model.NotificationModel;
import com.example.oc.myapplication.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GroupMessageActivity extends AppCompatActivity {

    Map<String, UserModel> users=new HashMap<>();
    String destinationRoom;
    String uid;
    EditText editText;

    private DatabaseReference databaseReference;
    private ValueEventListener valueEventListener;
    private SimpleDateFormat simpleDataFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");

    private RecyclerView recyclerView;

    List<ChatModel.Comment> comments =new ArrayList<>();
    int peopleCount=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_message);
        destinationRoom=getIntent().getStringExtra("destinationRoom");
        uid= FirebaseAuth.getInstance().getCurrentUser().getUid();
        editText=(EditText)findViewById(R.id.groupMessageActivity_editText);

        FirebaseDatabase.getInstance().getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot item: dataSnapshot.getChildren()){
                    users.put(item.getKey(),item.getValue(UserModel.class));
                }
                init();
                recyclerView=(RecyclerView)findViewById(R.id.groupMessageActivity_recyclerview);
                recyclerView.setAdapter(new GroupMessageRecyclerViewAdapter());
                recyclerView.setLayoutManager(new LinearLayoutManager(GroupMessageActivity.this));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    void init(){
        Button button=(Button)findViewById(R.id.groupMessageActivity_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatModel.Comment comment = new ChatModel.Comment();
                comment.uid = uid;
                comment.message = editText.getText().toString();
                comment.timestamp = ServerValue.TIMESTAMP;
                FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("comments").push().setValue(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Map<String, Boolean> map=(Map<String,Boolean>)dataSnapshot.getValue();
                                for(String item:map.keySet()){
                                    if(item.equals(uid)){
                                        continue;
                                    }
                                    sendGcm(users.get(item).pushToken);
                                }
                                editText.setText("");
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                });
            }
        });
    }

    void sendGcm(String pushToken) {
        Gson gson = new Gson();

        String userName=FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        //userName="admin";
        //Log.e("hello",userName);
        NotificationModel notificationModel = new NotificationModel();
        notificationModel.to = pushToken;
        //Log.e("hello",notificationModel.to);
        notificationModel.notification.title = userName;
        notificationModel.notification.text = editText.getText().toString();
        notificationModel.data.title = userName;
        notificationModel.data.text = editText.getText().toString();

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; chareset=utf8"), gson.toJson(notificationModel));
        Request request = new Request.Builder()
                .header("Content-Type", "application/json")
                .addHeader("Authorization", "key=AAAA89Dl9q4:APA91bFtENm8V5qcG9ko0X2e2eZbyVb_OYZSqBT22ThZq1qFmD5zXL303BsuYv64e_uH811P3-iz-FH6Wd0pPlVTBykF716Jn06YzFkI5j0Lw6qjAF196Ek23ayM3ZGfGGS8gfQTcGwy")
                .url("https://gcm-http.googleapis.com/gcm/send")
                .post(requestBody)
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response respponse) throws IOException {

            }
        });
    }

    class GroupMessageRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        public GroupMessageRecyclerViewAdapter(){
            getMessageList();
        }

        void getMessageList() {
            databaseReference = FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("comments");
            valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    comments.clear();
                    Map<String, Object> readUsersMap = new HashMap<>();
                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                        String key = item.getKey();
                        ChatModel.Comment comment = item.getValue(ChatModel.Comment.class);
                        //ChatModel.Comment comment_notify=item.getValue(ChatModel.Comment.class);
                        comment.readUsers.put(uid, true);
                        readUsersMap.put(key, comment);
                        comments.add(comment);
                    }
                    Log.e("hello",String.valueOf(comments));

                    FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("comments")
                            .updateChildren(readUsersMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            //메세지 갱신
                            notifyDataSetChanged();
                            //스크롤뷰
                            recyclerView.scrollToPosition(comments.size() - 1);
                        }
                    });

                    /*
                    if (!comments.get(comments.size()-1).readUsers.containsKey(uid)) {
                        FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("comments")
                                .updateChildren(readUsersMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                //메세지 갱신
                                notifyDataSetChanged();
                                //스크롤뷰
                                recyclerView.scrollToPosition(comments.size() - 1);
                            }
                        });
                    }else{
                        //메세지 갱신
                        notifyDataSetChanged();
                        //스크롤뷰
                        recyclerView.scrollToPosition(comments.size()-1);
                    }
                    */

                }
                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view= LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_message,viewGroup,false);
            return new GroupMessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            GroupMessageViewHolder messageViewHolder = ((GroupMessageViewHolder) holder);

            //내가보낸 메세지
            if (comments.get(position).uid.equals(uid)) {
                messageViewHolder.textView_message.setText(comments.get(position).message);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.right_bubble);
                messageViewHolder.linearLayout_destination.setVisibility(View.INVISIBLE);
                messageViewHolder.linearLayout_main.setGravity(Gravity.RIGHT);
                //setReadCounter(position, messageViewHolder.textView_readCounter_left);
                messageViewHolder.textView_readCounter_right.setVisibility(View.INVISIBLE);
                //상대방이 보낸 메세지
            } else {
                Glide.with(holder.itemView.getContext()).load(users.get((comments.get(position).uid)).profileImageUrl).apply(new RequestOptions().circleCrop()).into(messageViewHolder.imageView_profile);
                messageViewHolder.textview_name.setText(users.get((comments.get(position).uid)).userName);
                messageViewHolder.linearLayout_destination.setVisibility(View.VISIBLE);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.left_bubble);
                messageViewHolder.textView_message.setText(comments.get(position).message);
                messageViewHolder.textView_message.setTextSize(25);
                messageViewHolder.linearLayout_main.setGravity(Gravity.LEFT);
                setReadCounter(position, messageViewHolder.textView_readCounter_right);
                messageViewHolder.textView_readCounter_left.setVisibility(View.INVISIBLE);
            }
            long unixTime = (long) comments.get(position).timestamp;
            Date date = new Date(unixTime);
            simpleDataFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            String time = simpleDataFormat.format(date);
            messageViewHolder.textView_timestamp.setText(time);
            //((GroupMessageViewHolder) holder).textView_message.setText(comments.get(position).message);


        }

        void setReadCounter(final int position, final TextView textView) {

            if(peopleCount==0){
                FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Map<String,Boolean> users=(Map<String,Boolean>) dataSnapshot.getValue();
                        peopleCount=users.size();
                        int count=peopleCount-comments.get(position).readUsers.size();
                        if(count>0){
                            textView.setVisibility(View.VISIBLE);
                            textView.setText(String.valueOf(count));
                        }else{
                            textView.setVisibility(View.INVISIBLE);
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }else{
                int count=peopleCount-comments.get(position).readUsers.size();
                if(count>0){
                    textView.setVisibility(View.VISIBLE);
                    textView.setText(String.valueOf(count));
                }else{
                    textView.setVisibility(View.INVISIBLE);
                }
            }
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        private class GroupMessageViewHolder extends RecyclerView.ViewHolder {
            public TextView textView_message;
            public TextView textview_name;
            public ImageView imageView_profile;
            public LinearLayout linearLayout_destination;
            public LinearLayout linearLayout_main;
            public TextView textView_timestamp;
            public TextView textView_readCounter_left;
            public TextView textView_readCounter_right;

            public GroupMessageViewHolder(View view) {
                super(view);
                textView_message = (TextView) view.findViewById(R.id.messageItem_textView_message);
                textview_name = (TextView) view.findViewById(R.id.messageItem_textview_name);
                imageView_profile = (ImageView) view.findViewById(R.id.messageItem_imageview_profile);
                linearLayout_destination = (LinearLayout) view.findViewById(R.id.messageItem_linearLayout_destination);
                linearLayout_main = (LinearLayout) view.findViewById(R.id.messageItem_linear_layout_main);
                textView_timestamp = (TextView) view.findViewById(R.id.messageItem_textView_timestamp);
                textView_readCounter_left = (TextView) view.findViewById(R.id.messageItem_textview_readCounter_left);
                textView_readCounter_right = (TextView) view.findViewById(R.id.messageItem_textview_readCounter_right);
            }
        }
    }
}
