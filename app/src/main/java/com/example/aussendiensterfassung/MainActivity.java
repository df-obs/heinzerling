package com.example.aussendiensterfassung;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.Toast;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pref = getSharedPreferences("CONFIG", 0);

        Button selectUserButton = findViewById(R.id.main_button_user);
        if (isUserSelected()) {
            selectUserButton.setText(String.format("%s: %s\n\n%s", getString(R.string.selected_user), pref.getString("USERNAME", "NOT_FOUND"), getString(R.string.change_user)));
        } else {
            selectUserButton.setText(String.format("%s\n\n%s", getString(R.string.no_user_selected), getString(R.string.change_user)));
        }
    }

    public void openCalendar(View view) {
        if (isUserSelected()) {
            Intent startCalendarActivity = new Intent(this, OrderCalendar.class);
            startActivity(startCalendarActivity);
        }
    }

    public void openLockedOrders(View view) {
        if (isUserSelected()) {
            Intent startLockedOrdersActivity = new Intent(this, LockedOrders.class);
            startActivity(startLockedOrdersActivity);
        }
    }

    public void changeUser(View view) {
        Intent startUserActivity = new Intent(this, SelectUser.class);
        startActivity(startUserActivity);
    }

    public boolean isUserSelected() {
        String userId = pref.getString("USER", "NOT_FOUND");

        if (Objects.equals(userId, "NOT_FOUND")) {
            Toast.makeText(getApplicationContext(), getString(R.string.no_user_selected), Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }
}
