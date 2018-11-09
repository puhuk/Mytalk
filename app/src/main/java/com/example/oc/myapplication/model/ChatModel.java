package com.example.oc.myapplication.model;

import java.util.HashMap;
import java.util.Map;

public class ChatModel {

    //public String uid;
    //public String destinationUid;
    public Map<String, Boolean> users=new HashMap<>(); //채팅방 유저들
    public Map<String, Comment> comments=new HashMap<>(); // 채팅방 대화들
    public static class Comment{
        public String uid;
        public String message;
        public Object timestamp;
        public Map<String,Object> readUsers=new HashMap<>();
    }
}
