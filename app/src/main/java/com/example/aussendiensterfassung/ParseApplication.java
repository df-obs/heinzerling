package com.example.aussendiensterfassung;

import android.app.Application;
import android.util.Log;

import com.parse.*;

import java.util.List;

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

        try {
            ParseObject.unpinAll();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String[] tables = {"Ansprechpartner", "Artikel", "ArtikelAuftrag", "Auftrag", "Aufzug", "Einzelauftrag", "Kunde", "Monteur", "MonteurAuftrag"};

        for (String table : tables) {
            pinData(table);
        }
    }

    public void pinData(String table) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(table);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    try {
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
