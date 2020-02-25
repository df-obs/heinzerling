package com.example.aussendiensterfassung;

import android.content.Intent;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class OrderCalendar extends AppCompatActivity {

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

        // Get orders
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Auftrag");
        query.fromLocalDatastore();
        query.whereGreaterThanOrEqualTo("Datum", today);
        query.whereLessThanOrEqualTo("Datum", tomorrow);
        query.orderByAscending("Datum");
        query.include("Kunde");

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
            String type = order.getString("Typ");
            final String orderObjectId = order.getObjectId();

            DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
            String strTime = timeFormat.format(date);

            // Create button
            Button buttonOrder = new Button(this);
            buttonOrder.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
            buttonOrder.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            buttonOrder.setText(String.format("%s\n%s\n%s %s", customer, type, strTime, getString(R.string.clock)));
            buttonOrder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent switchToOrderDetails = new Intent(getApplicationContext(), OrderDetails.class);
                    switchToOrderDetails.putExtra("orderObjectId", orderObjectId);
                    startActivity(switchToOrderDetails);
                }
            });

            // Change colour according to order type
            if (type != null) {
                switch (type) {
                    case "Wartung":
                        buttonOrder.setBackgroundColor(0xFF4CAF50);
                        break;
                    case "Festpreis":
                        buttonOrder.setBackgroundColor(0xFF2196F3);
                        break;
                    case "Störung":
                        buttonOrder.setBackgroundColor(0xFFFF5722);
                        break;
                }
            }

            layout.addView(buttonOrder);
        }
    }
}