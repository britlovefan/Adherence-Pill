package com.adherence.adherence;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by weihanchu on 6/2/16.
 */
public class FirstActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);


        //? Trying to connect to the parse server
        Parse.enableLocalDatastore(this);
        Parse.initialize(new Parse.Configuration.Builder(this).applicationId("myAppId").server("http://129.105.36.93:5000/parse").build());
        ParseUser.enableAutomaticUser();
        //test
        Date now = new Date();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(now);
        ParseObject testObject = new ParseObject("TestXZ");
        //testObject.put("time", timestamp);
        testObject.put("name", "Christina");
        testObject.saveEventually();
        //test the timestamp and push it to server
        ParseObject testObject1 = new ParseObject("BottleUpdates");
        testObject1.put("timeStamp", timestamp);
        testObject1.saveEventually();
        //test the pill number actually works
        ParseObject testObject2 = new ParseObject("Bottle");
        testObject2.put("pillNumber", 6);
        testObject2.saveEventually();


        //Parse.initialize(this, "BDo39lSOtPuBwDfq0EBDgIjTzztIQE38Fuk03EcR", "6exCVtTYC6JhQP6gw1OFByyP2RRq5McznAsoQ3Gq");

        ParseUser.enableAutomaticUser();
        List<ParseObject> user = new ArrayList<>();
        try {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("saveUser");
            query.fromLocalDatastore();
            user = query.find();
        } catch (Exception e) {
        }
        if (user.size() > 0) {;
            Intent intent = new Intent();
            intent.setClass(FirstActivity.this, NextActivity.class);
            FirstActivity.this.startActivity(intent);
        }

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent();
                intent.setClass(FirstActivity.this, MainActivity2.class);
                FirstActivity.this.startActivity(intent);
            }
        }, 2000);

    }
}
