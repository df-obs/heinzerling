package com.example.aussendiensterfassung;

import android.content.Intent;
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


import java.util.Date;
import java.util.List;

public class Kalendar extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_kalendar);

        MaterialCalendarView calendarView = findViewById(R.id.weekCalendar);

        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(MaterialCalendarView widget, CalendarDay date, boolean selected) {
                Date datum = new Date(date.getYear()-1900, date.getMonth()-1, date.getDay());
                changeDate(datum);
            }
        });

        calendarView.setCurrentDate(CalendarDay.today());
        calendarView.setDateSelected(CalendarDay.today(), true);

        changeDate(new Date(CalendarDay.today().getYear()-1900, CalendarDay.today().getMonth()-1, CalendarDay.today().getDay()));
    }

    public void changeDate(final Date today) {
        final Date tomorrow = new Date(today.getTime()+86400000);

        TextView headlineAuftraege = findViewById(R.id.headlineAuftraege);
        headlineAuftraege.setText("Aufträge am " + today.getDate() + "." + (today.getMonth()+1) + "." + (today.getYear()+1900));

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Auftrag");
        query.fromLocalDatastore();
        query.whereGreaterThanOrEqualTo("Datum", today);
        query.whereLessThanOrEqualTo("Datum", tomorrow);
        query.orderByAscending("Datum");
        query.include("Kunde");

        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> auftragsListe, ParseException e) {
                if (e == null) {
                         zeigeAuftraege(auftragsListe);
                } else {
                    Log.d("Auftraege", "Error: " + e.getMessage());
                }
            }
        });
    }

    public void zeigeAuftraege(List<ParseObject> auftragsListe) {
        LinearLayout layout = findViewById(R.id.kalenderLayout);
        layout.removeAllViewsInLayout();

        for (int i=0; i<auftragsListe.size(); i++) {
            ParseObject auftrag = auftragsListe.get(i);
            Date date = (Date) auftrag.get("Datum");
            String auftraggeber = (String) auftrag.getParseObject("Kunde").get("Name");
            String typ = (String) auftrag.get("Typ");
            String minutes = date.getMinutes() + "";
            final String auftragsnummer = auftrag.getObjectId();

            if (date.getMinutes() < 10) {
                minutes = "0" + date.getMinutes();
            }

            Button button = new Button(this);

            button.setId(i);
            button.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            button.setText(auftraggeber + "\n" + typ + "\n" + date.getHours() + ":" + minutes + " Uhr");

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openAuftragDetails(v, auftragsnummer);
                }
            });

            switch (typ) {
                case "Wartung":
                    button.setBackgroundColor(0xFF4CAF50);
                    break;
                case "Festpreis":
                    button.setBackgroundColor(0xFF2196F3);
                    break;
                case "Störung":
                    button.setBackgroundColor(0xFFFF5722);
                    break;
            }

            layout.addView(button);
        }
    }

    public void openAuftragDetails(View view, String auftrag) {
        Intent startAuftragDetailsActivity = new Intent(this, OrderDetails.class);
        startAuftragDetailsActivity.putExtra("orderObjectId", auftrag);
        startActivity(startAuftragDetailsActivity);
    }
}
