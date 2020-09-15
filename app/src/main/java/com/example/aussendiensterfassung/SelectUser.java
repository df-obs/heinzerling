package com.example.aussendiensterfassung;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
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

public class SelectUser extends AppCompatActivity {

    protected ArrayList<String> employeeNameList;
    protected ArrayList<String> employeeIdList;
    protected ArrayList<String> employeeSqlList;
    protected String mUserId;
    protected String mUserName;

    SharedPreferences pref;
    SharedPreferences.Editor ed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_user);

        // Get current user
        pref = getSharedPreferences("CONFIG", 0);
        mUserId = pref.getString("USER", "NOT_FOUND");
        mUserName = getString(R.string.no_user_selected);

        // Get a list of all employees (= users)
        employeeNameList = new ArrayList<>();
        employeeIdList = new ArrayList<>();
        employeeSqlList = new ArrayList<>();
        ParseQuery<ParseObject> queryEmployeeList = ParseQuery.getQuery("Monteur");
        queryEmployeeList.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    if (resultList.size() != 0) {
                        initUsers(resultList);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.user_connection_needed), Toast.LENGTH_SHORT).show();
                    mUserName = getString(R.string.user_no_connection);
                }
            }
        });
    }

    public void initUsers(@NonNull List<ParseObject> resultList) {
        for (int i = 0; i < resultList.size(); i++) {
            ParseObject employee = resultList.get(i);
            employeeNameList.add(employee.getString("Name"));
            employeeIdList.add(employee.getObjectId());
            employeeSqlList.add(employee.getString("sqlRef"));

            if (mUserId.equals(employee.getObjectId())) {
                mUserName = employee.getString("Name");
            }
        }

        if (mUserId.equals("NOT_FOUND")) {
            mUserName = getString(R.string.no_user_selected);
        }

        AutoCompleteTextView fieldName = findViewById(R.id.select_user_input_name);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_dropdown_item_1line, employeeNameList);
        fieldName.setThreshold(1);
        fieldName.setAdapter(arrayAdapter);
        fieldName.setText(mUserName);
    }

    public void setUser(View view) {
        AutoCompleteTextView fieldName = findViewById(R.id.select_user_input_name);
        String userName = fieldName.getText().toString();
        String userId = "";
        String userSql = "";

        for (int i = 0; i < employeeIdList.size(); i++) {
            if (employeeNameList.get(i).equals(userName)) {
                userId = employeeIdList.get(i);
                userSql = employeeSqlList.get(i);
            }
        }

        if (!userId.equals("")) {
            ed = pref.edit();
            ed.putString("USER", userId);
            ed.putString("USERNAME", userName);
            ed.putString("USERSQL", userSql);
            ed.apply();
            Toast.makeText(getApplicationContext(), getString(R.string.saved), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.error_saving_wrong_employee), Toast.LENGTH_SHORT).show();
        }
    }

    public void setManualSync(View v) {
        ed = pref.edit();
        ed.putInt("SYNC", 0);
        ed.apply();
    }

    public void backToMain(View view) {
        Intent backToMainActivity = new Intent(this, MainActivity.class);
        startActivity(backToMainActivity);
    }

    // Handle Back button
    public void onBackPressed() {
        Intent backToMainActivity = new Intent(this, MainActivity.class);
        startActivity(backToMainActivity);
    }
}
