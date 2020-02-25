package com.example.aussendiensterfassung;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.*;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openCalendar(View view) {
        Intent startCalendarActivity = new Intent(this, OrderCalendar.class);
        startActivity(startCalendarActivity);
    }

    public void openLockedOrders(View view) {
        Intent startLockedOrdersActivity = new Intent(this, LockedOrders.class);
        startActivity(startLockedOrdersActivity);
    }
}
