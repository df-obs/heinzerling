package com.example.aussendiensterfassung;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    protected ArrayList<String> employeeNameList;
    protected ArrayList<String> employeeIdList;
    protected String mUserId;
    protected String mUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get user
        final SharedPreferences pref = getSharedPreferences("CONFIG", 0);
        mUserId = pref.getString("USER", "NOT_FOUND");
        mUserName = "";

        // Set AutoFill for user
        employeeNameList = new ArrayList<>();
        employeeIdList = new ArrayList<>();
        ParseQuery<ParseObject> queryEmployeeList = ParseQuery.getQuery("Monteur");
        queryEmployeeList.fromLocalDatastore();
        queryEmployeeList.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    for (int i = 0; i < resultList.size(); i++) {
                        ParseObject employee = resultList.get(i);
                        employeeNameList.add(employee.getString("Name"));
                        employeeIdList.add(employee.getObjectId());

                        if (mUserId.equals(employee.getObjectId())) {
                            mUserName = employee.getString("Name");
                        }
                    }

                    if (mUserId.equals("NOT_FOUND")) {
                        mUserId = employeeIdList.get(0);
                        mUserName = employeeNameList.get(0);

                        SharedPreferences.Editor ed = pref.edit();
                        ed.putString("USER", mUserId);
                        ed.apply();
                    }

                    AutoCompleteTextView fieldName = findViewById(R.id.main_input_name);
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_dropdown_item_1line, employeeNameList);
                    fieldName.setThreshold(1);
                    fieldName.setAdapter(arrayAdapter);
                    fieldName.setText(mUserName);
                }
            }
        });
    }

    public void setUser(View view) {
        AutoCompleteTextView fieldName = findViewById(R.id.main_input_name);
        String userName = fieldName.getText().toString();
        String userId = "";

        for (int i = 0; i < employeeIdList.size(); i++) {
            if (employeeNameList.get(i).equals(userName)) {
                userId = employeeIdList.get(i);
            }
        }

        if (!userId.equals("")) {
            SharedPreferences pref = getSharedPreferences("CONFIG", 0);
            SharedPreferences.Editor ed = pref.edit();
            ed.putString("USER", userId);
            ed.apply();
            Toast.makeText(getApplicationContext(), getString(R.string.saved), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.error_saving_wrong_employee), Toast.LENGTH_SHORT).show();
        }
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
