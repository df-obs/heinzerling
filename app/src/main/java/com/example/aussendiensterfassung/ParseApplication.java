package com.example.aussendiensterfassung;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.parse.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ParseApplication extends Application {

    String[] tables;
    Date lastUpdate;
    int period;

    SharedPreferences pref;
    SharedPreferences.Editor ed;

    public void onCreate() {
        super.onCreate();

        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.parse_app_id))
                .enableLocalDataStore()
                .clientKey(getString(R.string.parse_client_key))
                .server(getString(R.string.parse_server_url))
                .build()
        );

        // Tables that should be kept in LocalDatastore
        tables = new String[]{"Ansprechpartner", "Artikel", "ArtikelAuftrag", "Auftrag", "Aufzug", "Einzelauftrag", "Kunde", "Monteur", "MonteurAuftrag"};

        // Period of update timer
        period = 20000;

        // Fetch offline Data every 20 seconds
        TimerTask fetchOfflineData = new TimerTask() {
            @Override
            public void run() {
                ParseQuery<ParseObject> test = ParseQuery.getQuery("Einzelauftrag"); // Test connection
                test.findInBackground(new FindCallback<ParseObject>() {
                    public void done(List<ParseObject> resultList, ParseException e) {
                        if (e == null) { // no error -> connection ok
                            Log.i("Pin Data", "Established connection, will now refresh data.");
                            pref = getSharedPreferences("CONFIG", 0);
                            int syncCounter = pref.getInt("SYNC", 0);
                            if (syncCounter < 5) {
                                pinData(0, true);
                            } else {
                                pinData(0, false);
                            }
                            syncCounter++;
                            ed = pref.edit();
                            ed.putInt("SYNC", syncCounter);
                            ed.apply();
                        } else {
                            Log.e("Pin Data", "Error: " + e.getMessage());
                        }
                    }
                });
            }
        };
        Timer timer = new Timer();
        timer.schedule(fetchOfflineData,0, period);
    }

    public void pinData(final int sequence, final boolean fullSync) {
        new Thread(new Runnable() {
            public void run() {
                ArrayList<String> tablesList = new ArrayList<>(Arrays.asList(tables));

                if (sequence > tablesList.size()-1) {
                    return;
                }

                lastUpdate = new Date();
                long dateMs = lastUpdate.getTime();
                dateMs = dateMs - period;
                lastUpdate.setTime(dateMs);

                ParseQuery<ParseObject> query = ParseQuery.getQuery(tablesList.get(sequence));
                if (!fullSync) {
                    query.whereGreaterThan("updatedAt", lastUpdate);
                }
                query.setLimit(5000);
                try {
                    List<ParseObject> resultList = query.find();
                    ParseObject.pinAllInBackground(tablesList.get(sequence), resultList);
                    Log.i("Pin Data", "Fetched and pinned table " + tablesList.get(sequence));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                pinData(sequence+1, fullSync);
            }
        }).start();
    }
}
