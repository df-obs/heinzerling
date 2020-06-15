package com.example.aussendiensterfassung;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.prolificinteractive.materialcalendarview.*;
import com.parse.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class OrderCalendar extends AppCompatActivity {

    ParseObject userObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_calendar);

        MaterialCalendarView calendarView = findViewById(R.id.calendar_calendar);
        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                Calendar calendar = new Calendar.Builder().setDate(date.getYear(), date.getMonth()-1, date.getDay()).build();
                changeDate(calendar);
            }
        });

        // Set calendar to current date
        calendarView.setCurrentDate(CalendarDay.today());
        calendarView.setDateSelected(CalendarDay.today(), true);
        changeDate(Calendar.getInstance());
    }

    // Returns a list of all orders at a specific date
    public void changeDate(Calendar calendar) {
        // Clear time values
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Set dates of today and tomorrow for comparison
        final Date today = calendar.getTime();
        final Date tomorrow = new Date(today.getTime()+86400000);

        DateFormat dateFormat = DateFormat.getDateInstance();
        String strToday = dateFormat.format(today);

        // Set headline
        TextView viewHeadline = findViewById(R.id.calendar_headline);
        viewHeadline.setText(String.format("%s %s", getString(R.string.orders_at), strToday));

        // Get user
        final SharedPreferences pref = getSharedPreferences("CONFIG", 0);
        String userId = pref.getString("USER", "NOT_FOUND");
        ParseQuery<ParseObject> queryEmployeeList = ParseQuery.getQuery("Monteur");
        queryEmployeeList.whereEqualTo("objectId", userId);
        queryEmployeeList.fromLocalDatastore();
        queryEmployeeList.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    if (resultList.size() > 0) {
                        ParseObject employee = resultList.get(0);
                        userObject = employee;
                        List <ParseObject> tempList;
                        ArrayList<String> possibleOrders = new ArrayList<>();

                        // Get possible orders
                        ParseQuery<ParseObject> innerQuery = ParseQuery.getQuery("MonteurAuftrag");
                        innerQuery.whereEqualTo("Monteur", employee);
                        innerQuery.include("Auftrag");
                        innerQuery.include("Auftrag.Gesamtauftrag");
                        innerQuery.fromLocalDatastore();
                        try {
                             tempList = innerQuery.find();
                             for (int i = 0; i < tempList.size(); i++) {
                                 ParseObject possibleCombination = tempList.get(i);
                                 String possibleOrder = Objects.requireNonNull(Objects.requireNonNull(possibleCombination.getParseObject("Auftrag")).getParseObject("Gesamtauftrag")).getObjectId();
                                 possibleOrders.add(possibleOrder);
                             }
                        } catch (ParseException ex) {
                            ex.printStackTrace();
                        }

                        // Get filtered and sorted orders
                        ParseQuery<ParseObject> query = ParseQuery.getQuery("Auftrag");
                        query.fromLocalDatastore();
                        query.include("Kunde");
                        query.whereContainedIn("objectId", possibleOrders);
                        query.whereGreaterThanOrEqualTo("Datum", today);
                        query.whereLessThanOrEqualTo("Datum", tomorrow);
                        query.orderByAscending("Datum");
                        query.findInBackground(new FindCallback<ParseObject>() {
                            public void done(List<ParseObject> resultList, ParseException e) {
                                if (e == null) {
                                    showOrders(resultList);
                                } else {
                                    Log.d("Calendar orders", "Error: " + e.getMessage());
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    // Show orders below calendar
    public void showOrders(List<ParseObject> resultList) {
        // Define and clear layout
        LinearLayout layout = findViewById(R.id.calendar_layout);
        layout.removeAllViewsInLayout();

        // Get orders and create a button for each order
        for (int i=0; i<resultList.size(); i++) {
            // Get fields
            ParseObject order = resultList.get(i);
            Date date = order.getDate("Datum");
            String customer = Objects.requireNonNull(order.getParseObject("Kunde")).getString("Name");
            final String orderObjectId = order.getObjectId();

            DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
            String strTime = timeFormat.format(date);

            // Create button
            Button buttonOrder = new Button(this);
            buttonOrder.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
            buttonOrder.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            buttonOrder.setText(String.format("%s\n%s %s", customer, strTime, getString(R.string.clock)));
            buttonOrder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent switchToOrderDetails = new Intent(getApplicationContext(), OrderDetails.class);
                    switchToOrderDetails.putExtra("orderObjectId", orderObjectId);
                    startActivity(switchToOrderDetails);
                }
            });

            if (i%2 == 0) {
                buttonOrder.setBackgroundColor(0xFF80D8FF);
            } else
                buttonOrder.setBackgroundColor(0xFF82B1FF);

            layout.addView(buttonOrder);
        }
    }
}