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
import com.google.android.gms.tasks.OnSuccessListener;
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

public class MessageActivity extends AppCompatActivity {

    private String destinationUid;
    private Button button;
    private EditText editText;

    private String uid;
    private String chatRoomUid;
    private String userName;

    private RecyclerView recyclerView;

    private SimpleDateFormat simpleDataFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");

    private UserModel destinationUserModel;
    private DatabaseReference databaseReference;
    private ValueEventListener valueEventListener;
    int peopleCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid(); //채팅을 요구하는 아이디
        Log.e("hello", "uid is " + uid);
        userName=FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        //UserModel temp=FirebaseAuth.getInstance().getCurrentUser().getProviderData();
        //userName=FirebaseAuth.getInstance().getCurrentUser().getProviderData().getDisplayName();
        //for (UserInfo profile : FirebaseAuth.getInstance().getCurrentUser().getProviderData()) {
        //    userName = profile.getDisplayName();
        //}
        Log.e("hello", "usr name is " + userName);
        destinationUid = getIntent().getStringExtra("destinationUid"); //채팅을 당하는 아이디

        button = (Button) findViewById(R.id.messageActivity_button);
        editText = (EditText) findViewById(R.id.messageActivity_editText);

        recyclerView = (RecyclerView) findViewById(R.id.messageActivity_recyclerview);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChatModel chatModel = new ChatModel();
                //chatModel.uid= FirebaseAuth.getInstance().getCurrentUser().getUid();
                //chatModel.destinationUid=destinationUid;
                chatModel.users.put(uid, true);
                chatModel.users.put(destinationUid, true);

                if (chatRoomUid == null) {
                    button.setEnabled(false);
                    FirebaseDatabase.getInstance().getReference().child("chatrooms").push().setValue(chatModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            checkChatRoom();
                        }
                    });
                } else {
                    ChatModel.Comment comment = new ChatModel.Comment();
                    comment.uid = uid;
                    comment.message = editText.getText().toString();
                    comment.timestamp = ServerValue.TIMESTAMP;
                    FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments").push().setValue(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            sendGcm();
                            //String userName=FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
                            //Log.e("hello", userName);
                            editText.setText("");
                        }
                    });
                    //FirebaseDatabase.getInstance().getReference().child("chatrooms").push().setValue(comment);
                }
            }
        });
        checkChatRoom();
    }


    void sendGcm() {
        Gson gson = new Gson();

        //String userName=FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        //userName="admin";
        //Log.e("hello",userName);
        NotificationModel notificationModel = new NotificationModel();
        notificationModel.to = destinationUserModel.pushToken;
        //Log.e("hello",notificationModel.to);
        notificationModel.notification.title = userName;
        Log.e("hello", "userName : " + userName);
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


    void checkChatRoom() {
        //Log.e("hello",FirebaseDatabase.getInstance().getReference().child("chatrooms"));
        FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/" + uid).equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue()==null){
                    ChatModel newRoom=new ChatModel();
                    newRoom.users.put(uid,true);
                    newRoom.users.put(destinationUid,true);

                    FirebaseDatabase.getInstance().getReference().child("chatrooms").push().setValue(newRoom).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            checkChatRoom();
                        }
                    });
                    return;
                }

                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    ChatModel chatModel = item.getValue(ChatModel.class);
                    if (chatModel.users.containsKey(destinationUid) && chatModel.users.size() == 2) {
                        chatRoomUid = item.getKey();
                        button.setEnabled(true);
                        recyclerView.setLayoutManager(new LinearLayoutManager(MessageActivity.this));
                        recyclerView.setAdapter(new RecyclerViewAdapter());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        List<ChatModel.Comment> comments;
        UserModel userModel;

        public RecyclerViewAdapter() {
            comments = new ArrayList<>();
            FirebaseDatabase.getInstance().getReference().child("users").child(destinationUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    userModel = dataSnapshot.getValue(UserModel.class);
                    destinationUserModel = dataSnapshot.getValue(UserModel.class);
                    getMessageList();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }


        void getMessageList() {
            databaseReference = FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments");
            valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    comments.clear();
                    Map<String, Object> readUsersMap = new HashMap<>();
                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                        String key = item.getKey();
                        ChatModel.Comment comment_origin = item.getValue(ChatModel.Comment.class);
                        ChatModel.Comment comment_notify=item.getValue(ChatModel.Comment.class);
                        comment_notify.readUsers.put(uid, true);
                        readUsersMap.put(key, comment_notify);
                        comments.add(comment_origin);
                    }
                    //Log.e("hello",String.valueOf(comments));


                    FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments")
                            .updateChildren(readUsersMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            //메세지 갱신
                            notifyDataSetChanged();
                            //스크롤뷰
                            recyclerView.scrollToPosition(comments.size()-1);
                        }
                    });

                    /*
                    if (!comments.get(comments.size()-1).readUsers.containsKey(uid)) {
                        FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments")
                                .updateChildren(readUsersMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                //메세지 갱신
                                notifyDataSetChanged();
                                //스크롤뷰
                                recyclerView.scrollToPosition(comments.size()-1);
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


        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            MessageViewHolder messageViewHolder = ((MessageViewHolder) holder);

            //내가보낸 메세지
            if (comments.get(position).uid.equals(uid)) {
                messageViewHolder.textView_message.setText(comments.get(position).message);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.right_bubble);
                messageViewHolder.linearLayout_destination.setVisibility(View.INVISIBLE);
                messageViewHolder.linearLayout_main.setGravity(Gravity.RIGHT);
                setReadCounter(position, messageViewHolder.textView_readCounter_left);
                messageViewHolder.textView_readCounter_right.setVisibility(View.INVISIBLE);
                //상대방이 보낸 메세지
            } else {
                Glide.with(holder.itemView.getContext()).load(destinationUserModel.profileImageUrl).apply(new RequestOptions().circleCrop()).into(messageViewHolder.imageView_profile);
                messageViewHolder.textview_name.setText(destinationUserModel.userName);
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
            ((MessageViewHolder) holder).textView_message.setText(comments.get(position).message);

        }

        void setReadCounter(final int position, final TextView textView) {

            if(peopleCount==0){
                FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
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

        private class MessageViewHolder extends RecyclerView.ViewHolder {
            public TextView textView_message;
            public TextView textview_name;
            public ImageView imageView_profile;
            public LinearLayout linearLayout_destination;
            public LinearLayout linearLayout_main;
            public TextView textView_timestamp;
            public TextView textView_readCounter_left;
            public TextView textView_readCounter_right;

            public MessageViewHolder(View view) {
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

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if(valueEventListener!=null) {
            databaseReference.removeEventListener(valueEventListener);
        }
        finish();
        overridePendingTransition(R.anim.fromleft, R.anim.toright);
    }
}
