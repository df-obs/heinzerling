package com.example.aussendiensterfassung;

import android.app.Application;
import android.util.Log;

import com.parse.*;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ParseApplication extends Application {
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
        final String[] tables = {"Ansprechpartner", "Artikel", "ArtikelAuftrag", "Auftrag", "Aufzug", "Einzelauftrag", "Kunde", "Monteur", "MonteurAuftrag"};

        // Fetch offline Data every 10 seconds
        TimerTask fetchOfflineData = new TimerTask() {
            @Override
            public void run() {
                ParseQuery<ParseObject> test = ParseQuery.getQuery("Einzelauftrag");
                test.findInBackground(new FindCallback<ParseObject>() {
                    public void done(List<ParseObject> resultList, ParseException e) {
                        if (e == null) {
                            Log.i("Pin Data", "Established connection, will now refresh data.");
                            for (String table : tables) {
                                try {
                                    ParseObject.unpinAll();
                                } catch (ParseException ex) {
                                    ex.printStackTrace();
                                }
                                pinData(table);
                            }
                        } else {
                            Log.e("Pin Data", "Error: " + e.getMessage());
                        }
                    }
                });
            }
        };
        Timer timer = new Timer();
        timer.schedule(fetchOfflineData,0, 10000);
    }

    public void pinData(final String table) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(table);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    try {
                        Log.i("Pin Data", "Fetched and pinned table " + table);
                        ParseObject.pinAll(resultList);
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    Log.e("Pin Data", "Error: " + e.getMessage());
                }
            }
        });
    }
}
