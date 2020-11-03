package com.example.aussendiensterfassung;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.parse.*;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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

        pref = getSharedPreferences("CONFIG", 0);

        // Tables that should be kept in LocalDatastore
        tables = new String[]{"Ansprechpartner", "Artikel", "ArtikelAuftrag", "Auftrag", "Aufzug", "Kunde", "Monteur", "MonteurAuftrag", "Einzelauftrag"};

        // Period of update timer
        period = 30000;

        // Fetch offline Data every period seconds

        /* disabled temporarily because of performance issues
        final Timer timer = new Timer();
        final TimerTask fetchOfflineData = new TimerTask() {
            @Override
            public void run() {
                ParseQuery<ParseObject> test = ParseQuery.getQuery("Einzelauftrag"); // Test connection
                test.findInBackground(new FindCallback<ParseObject>() {
                    public void done(List<ParseObject> resultList, ParseException e) {
                        if (e == null) { // no error -> connection ok
                            Log.i("Pin Data", "Established connection, will now refresh data.");

                            int syncCounter = pref.getInt("SYNC", 0);
                            ed = pref.edit();

                            if (syncCounter == 0) {
                                try {
                                    ParseObject.unpinAll();
                                } catch (ParseException ex) {
                                    ex.printStackTrace();
                                }
                                pinData(0, true);
                            } else {
                                pinData(0, false);
                            }

                            syncCounter = pref.getInt("SYNC", 0);
                            syncCounter++;
                            ed.putInt("SYNC", syncCounter);
                            ed.apply();
                        } else {
                            Log.e("Pin Data", "Error: " + e.getMessage());
                        }
                    }
                });
            }
        };
        timer.schedule(fetchOfflineData,0, period);
        */

        final Timer timer = new Timer();
        final TimerTask showNotifications = new TimerTask() {
            @Override
            public void run() {
                checkForNewOrders();
            }
        };
        timer.schedule(showNotifications,0, period);
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
                    Log.i("Pin Data", "Fetched and pinned table " + tablesList.get(sequence) + ", number of objects: " + resultList.size());

                    if(tablesList.get(sequence).equals("Einzelauftrag") && resultList.size() > 0) {
                        showNotification(resultList, lastUpdate, fullSync);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                pinData(sequence+1, fullSync);
            }
        }).start();
    }

    public void checkForNewOrders() {
        String userSql = pref.getString("USERSQL", "NOT_FOUND");
        Long lastUpdateMs = pref.getLong("LASTUPDATE", 0);
        Date lastUpdate = new Date(lastUpdateMs);
        Date currentUpdate = new Date();

        ed = pref.edit();
        ed.putLong("LASTUPDATE", currentUpdate.getTime());
        ed.apply();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Einzelauftrag");
        query.whereContains("Monteure", Objects.requireNonNull(userSql));
        query.whereNotEqualTo("Gesperrt", true);
        query.whereGreaterThan("updatedAt", lastUpdate);

        try {
            List<ParseObject> resultList = query.find();
            showNotification(resultList, lastUpdate, false);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void showNotification(List<ParseObject> resultList, Date lastUpdate, boolean fullSync) {
        // Filter for current user
        String userSql = pref.getString("USERSQL", "NOT_FOUND");

        ArrayList<ParseObject> orderList = new ArrayList<>();

        for (ParseObject order : resultList) {
            String employees = order.getString("Monteure");
            String[] employeeList = Objects.requireNonNull(employees).split("; ");

            for (String employee : employeeList) {
                if (employee.equals(userSql) && (Objects.requireNonNull(order.getDate("sqlUpdatedAt")).after(lastUpdate) || fullSync)) {
                    orderList.add(order);
                }
            }
        }

        if (orderList.size() == 0) return;

        int orderQty = orderList.size();

        DateFormat dateFormat = DateFormat.getDateInstance();
        ArrayList<String> datesList = new ArrayList<>();

        for (ParseObject order : orderList) {
            String strDate = dateFormat.format(order.getDate("DatumMitUhrzeit"));

            if (!datesList.contains(strDate)) {
                datesList.add(strDate);
            }
        }

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(orderQty);
        stringBuilder.append(" ");
        if (orderQty > 1) {
            stringBuilder.append(getString(R.string.new_plural));
            stringBuilder.append(" ");
            stringBuilder.append(getString(R.string.orders));
        }
        else {
            stringBuilder.append(getString(R.string.new_single));
            stringBuilder.append(" ");
            stringBuilder.append(getString(R.string.title_auftrag));
        }
        stringBuilder.append(" ");
        stringBuilder.append(getString(R.string.on));
        stringBuilder.append(" ");

        int maxDates = 5;

        for (int i = 0; i < datesList.size(); i++) {
            if (i < maxDates) {
                if (i > 0) stringBuilder.append(", ");
                stringBuilder.append(datesList.get(i));
            } else if (i == maxDates) {
                stringBuilder.append(" ");
                stringBuilder.append(getString(R.string.and));
                stringBuilder.append(" ");
                stringBuilder.append(datesList.size()-maxDates);
                stringBuilder.append(" ");
                stringBuilder.append(getString(R.string.others));
            }
        }

        Intent intent = new Intent(this, OrderCalendar.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "heinzerling_ch_1")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.new_orders))
                .setContentText(getString(R.string.new_orders_long))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(stringBuilder))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(pref.getInt("SYNC", 0), builder.build());
    }
}
