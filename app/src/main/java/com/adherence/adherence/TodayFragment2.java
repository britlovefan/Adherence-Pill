package com.adherence.adherence;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class TodayFragment2 extends Fragment implements View.OnClickListener {
    static HashMap<String, HashMap<String, String>> patient = new HashMap<String, HashMap<String, String>>();
    private HashMap<String, HashMap<String, String>> display = new HashMap<String, HashMap<String, String>>();



    public Calendar c = Calendar.getInstance();
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static TodayFragment2 newInstance(int sectionNumber) {
        TodayFragment2 fragment = new TodayFragment2();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);

        return fragment;
    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment2, container, false);
        final Button upButton;
        upButton = (Button) view.findViewById(R.id.button_send);
        upButton.setOnClickListener(this);
        return view;

//        View rootView = inflater.inflate(R.layout.fragment2, container, false);
//        ProgressBar progress = (ProgressBar) rootView.findViewById(R.id.progressWheel);
//        progress.setSecondaryProgress(80);
//        try {
//            getDisplay("Sunday");
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//        addTimeViews(rootView, inflater);
//        return rootView;
    }
//
    @Override
    public void onClick(View v) {
        //do what you want to do when button is clicked
        switch (v.getId()) {
            case R.id.button_send:

//                ParseQuery<ParseObject> query=ParseQuery.getQuery("GameScore");
//                query.getInBackground("YsHLfJ7tUB", new GetCallback<ParseObject>() {
//
//                    @Override
//                    public void done(ParseObject arg0, ParseException arg1) {
//                        // TODO Auto-generated method stub
//
//                        if (arg1==null)
//                        {
//                            int score = arg0.getInt("score");
//                            playerName = arg0.getString("playerName");
//                            boolean cheatMode = arg0.getBoolean("cheatMode");
//                            tv.setText("Updating "+playerName);
//
//                        }
//                        else
//                        {
//                            Log.d("score", "Error: " + arg1.getMessage());
//                        }
//                    }

                Toast.makeText(getActivity(), "Click button", Toast.LENGTH_LONG).show();
                break;
        }
    }




    private void getDisplay(final String day) throws ParseException {
        if (isNetworkConnected()) {
//            Toast.makeText(getActivity(), "connect to network", Toast.LENGTH_LONG).show();
            ParseUser currentUser = ParseUser.getCurrentUser();
//            Toast.makeText(getActivity(), "currentUser: " + currentUser, Toast.LENGTH_LONG).show();
            ParseObject.unpinAllInBackground("schedules");
            ParseRelation exportContactRelation = currentUser.getRelation( "Prescription" );
            Toast.makeText(getActivity(), "exportContactRelation: " + exportContactRelation, Toast.LENGTH_LONG).show();
            try {
                List<ParseObject> prescriptions = exportContactRelation.getQuery().find();
//                Toast.makeText(getActivity(), "prescriptions: " + prescriptions, Toast.LENGTH_LONG).show();
                for (ParseObject prescription: prescriptions) {
                    ParseObject schedule = prescription.getParseObject("schedule");
                    schedule.fetchIfNeeded();
                    schedule.put("pillName", prescription.getString("pillName"));
                    schedule.pinInBackground("schedules");
                    JSONObject json = schedule.getJSONObject(day);
                    Iterator<String> iter = json.keys();
                    HashMap<String, String> time_pills = new HashMap<String, String>();
                    while (iter.hasNext()) {
                        String key = iter.next();
                        try {
                            String value = json.get(key).toString();
                            if (key.length() < 8)
                                key = "0" + key;
                            key.replace(" ", "");
                            time_pills.put(key, value);

                        } catch (JSONException e) {
                            // Something went wrong!
                        }
                    }
                    display.put(prescription.getString("pillName"), time_pills);
//                    ParseObject ppp = schedule.get(day);
//                    Log.v(TAG, String.valueOf(ppp));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            try {
                ParseQuery<ParseObject> query = ParseQuery.getQuery("Schedule");
                query.fromLocalDatastore();
                List<ParseObject> schedules = query.find();
                if (schedules.size() != 0) {
                    for (ParseObject schedule : schedules) {
                        JSONObject json = schedule.getJSONObject(day);
                        Iterator<String> iter = json.keys();
                        HashMap<String, String> time_pills = new HashMap<String, String>();
                        while (iter.hasNext()) {
                            String key = iter.next();
                            try {
                                String value = json.get(key).toString();
                                if (key.length() < 8)
                                    key = "0" + key;
                                key.replace(" ", "");
                                time_pills.put(key, value);

                            } catch (JSONException e1) {
                                // Something went wrong!
                            }
                        }
                        display.put(schedule.getString("pillName"), time_pills);
                    }
                } else {
                    Context context = getActivity().getApplicationContext();
                    CharSequence text = "Please check network connection.";
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
            } catch (Exception e) {
            }
        }
    }

    private void addTimeViews(View rootView, LayoutInflater timeViewInflater) {
        LinearLayout containerView = (LinearLayout) rootView.findViewById(R.id.today_container);
        int currenthour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) - 15;
        int currentminute = Calendar.getInstance().get(Calendar.MINUTE);

//        Toast.makeText(getActivity(), "display is: " + display, Toast.LENGTH_LONG).show();

        for(String key1:display.keySet()){
            System.out.println(key1);

            View timeView = timeViewInflater.inflate(R.layout.time_view, null);
            TextView timeStamp = (TextView) timeView.findViewById(R.id.time_stamp);
            timeStamp.setText(key1);
            LinearLayout pillListContainer = (LinearLayout) timeView.findViewById(R.id.time_pill_list);

            for(String key2:display.get(key1).keySet()){
                String timehour=key2.substring(0,2);
                String timeminute=key2.substring(3,5);
                String apm=key2.substring(key2.length()-2,key2.length());
                int hourtime=Integer.valueOf(timehour);
                int minutetime=Integer.valueOf(timeminute);
                if(apm.equals("am")&&(hourtime == 12)){hourtime -=12;}
                if(apm.equals("pm")&&(hourtime != 12)){hourtime+=12;}
                if(hourtime>currenthour || (hourtime == currenthour && minutetime>=currentminute)){
                    View  pillView = timeViewInflater.inflate(R.layout.pill_list_view, null);
                    TextView pillName = (TextView) pillView.findViewById(R.id.pill_name);
                    pillName.setText(key2);
                    TextView pillCount = (TextView) pillView.findViewById(R.id.pill_count);
                    pillCount.setText(display.get(key1).get(key2));
                    pillListContainer.addView(pillView);
                }
            }
            containerView.addView(timeView);
        }
    }
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            // There are no active networks.
            return false;
        } else
            return true;
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((NextActivity) context).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }
}