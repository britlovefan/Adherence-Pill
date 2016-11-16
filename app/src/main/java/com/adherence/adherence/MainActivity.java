package com.adherence.adherence;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button conformButton = (Button) this.findViewById(R.id.conformButton);
        Button cancelButton= (Button) this.findViewById(R.id.cancelButton);
        OnClickListener ocl = new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Login(arg0);
            }
        };
        conformButton.setOnClickListener(ocl);
        cancelButton.setOnClickListener(ocl);

        Intent intent = new Intent();
        intent.setClass(MainActivity.this, NextActivity.class);
        MainActivity.this.startActivity(intent);

        final Button button = (Button) findViewById(R.id.button_send);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Date now = new Date();
                String timestamp = new SimpleDateFormat("yyyy/MM/dd").format(now);
                ParseObject testObject = new ParseObject("TestXZ");
                testObject.put("TIME", timestamp);
                testObject.put("NAME", "ARYAN");
                testObject.saveEventually();
                Toast.makeText(getApplicationContext(), "Click button", Toast.LENGTH_LONG).show();
            }
        });

        final Button button1 = (Button) findViewById(R.id.Jarandice);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Date now = new Date();
                String timestamp = new SimpleDateFormat("yyyy/MM/dd").format(now);
                ParseObject testObject = new ParseObject("TestXZ");
                testObject.put("TIME", timestamp);
                testObject.put("NAME", "Jarandice");
                testObject.saveEventually();
                Toast.makeText(getApplicationContext(), "Click button", Toast.LENGTH_LONG).show();
            }
        });

        final Button button2 = (Button) findViewById(R.id.Truvada);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Date now = new Date();
                String timestamp = new SimpleDateFormat("yyyy/MM/dd").format(now);
                ParseObject testObject = new ParseObject("TestXZ");
                testObject.put("TIME", timestamp);
                testObject.put("NAME", "Truvada");
                testObject.saveEventually();
                Toast.makeText(getApplicationContext(), "Click button", Toast.LENGTH_LONG).show();
            }
        });

        final Button button3 = (Button) findViewById(R.id.Asprin);
        button3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Date now = new Date();
                String timestamp = new SimpleDateFormat("yyyy/MM/dd").format(now);
                ParseObject testObject = new ParseObject("TestXZ");
                testObject.put("TIME", timestamp);
                testObject.put("NAME", "Asprin");
                testObject.saveEventually();
                Toast.makeText(getApplicationContext(), "Click button", Toast.LENGTH_LONG).show();
            }
        });

        final Button button4 = (Button) findViewById(R.id.Lipitor);
        button4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Date now = new Date();
                String timestamp = new SimpleDateFormat("yyyy/MM/dd").format(now);
                ParseObject testObject = new ParseObject("TestXZ");
                testObject.put("TIME", timestamp);
                testObject.put("NAME", "Lipitor");
                testObject.saveEventually();
                Toast.makeText(getApplicationContext(), "Click button", Toast.LENGTH_LONG).show();
            }
        });


    }

    public void Login(View arg0){
        EditText name=(EditText)findViewById(R.id.username);
        EditText pwd=(EditText)findViewById(R.id.pwd);
        Button bt=(Button)findViewById(arg0.getId());
        String text=bt.getText().toString();
        if(text.equals("login")){
            String name1=name.getText().toString();
            String pwd1=pwd.getText().toString();
            loginparse(name1, pwd1);
        }
        else{
            name.setText("");
            pwd.setText("");
        }
    }

    private void loginparse(String name, String password) {
        if (isNetworkConnected()) {
            ParseUser.logInInBackground(name, password, new
                    com.parse.LogInCallback() {
                        public void done(ParseUser user, ParseException e) {
                            if (user != null) {
                                // These lines are saving the user's log-in status
                                ParseObject saveuser = new ParseObject("saveUser");
                                saveuser.put("user", user);
                                saveuser.pinInBackground("user");
                                // Go to the next page after logged in
                                Intent intent = new Intent();
                                intent.setClass(MainActivity.this, NextActivity.class);
                                MainActivity.this.startActivity(intent);
                            } else {
                                Context context = getApplicationContext();
                                CharSequence text = "Wrong password or username, please input again";
                                int duration = Toast.LENGTH_SHORT;
                                Toast toast = Toast.makeText(context, text, duration);
                                toast.show();
                            }
                        }
                    });
        } else {
            Context context = getApplicationContext();
            CharSequence text = "Network connection not available, please try again.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            // There are no active networks.
            return false;
        } else
            return true;
    }
}
