package com.example.chatting;


import android.os.Message;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;



public class MessagingControl {
/*
    // This registration token comes from the client FCM SDKs.
    String registrationToken = "YOUR_REGISTRATION_TOKEN";

    HashMap<String, String> map = new HashMap<>();

    public HashMap<String, String> getMap() {
        map.put("token", MyApplication.prefs.getGlobalToken());
        return map;
    }

    // See documentation on defining a message payload.
    RemoteMessage message = new RemoteMessage.Builder("Msg").setData(map).build();
            *//*.putData("score", "850")
            .putData("time", "2:45")
            .setToken(registrationToken)
            .build();*//*

    // Send a message to the device corresponding to the provided
// registration token.
    *//*String response = FirebaseMessaging.getInstance().send(message);*//*
    FirebaseMessaging.getInstance.send(message);
// Response is a message ID string.
System.out.println("Successfully sent message: " + response);*/
}