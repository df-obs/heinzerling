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

import com.parse.*;

import java.util.List;

public class Auftragdetails extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auftragdetails);

        Intent intent = getIntent();
        String auftragsnummer = intent.getStringExtra("auftrag");

        ParseQuery<ParseObject> innerQuery = ParseQuery.getQuery("Auftrag");
        innerQuery.whereEqualTo("objectId", auftragsnummer);
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Einzelauftrag");
        query.whereMatchesQuery("Gesamtauftrag", innerQuery);
        query.include("Aufzug");

        ParseQuery<ParseObject> auftragsQuery = ParseQuery.getQuery("Auftrag");
        auftragsQuery.whereEqualTo("objectId", auftragsnummer);
        auftragsQuery.include("Kunde");

        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> aufzugsListe, ParseException e) {
                if (e == null) {
                    zeigeAufzuege(aufzugsListe);
                } else {
                    Log.d("Aufzuege", "Error: " + e.getMessage());
                }
            }
        });

        auftragsQuery.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> auftrag, ParseException e) {
                if (e == null) {
                    zeigeHeadline(auftrag);
                } else {
                    Log.d("Auftraege", "Error: " + e.getMessage());
                }
            }
        });
    }

    public void zeigeHeadline(List<ParseObject> auftragsListe) {
        TextView headline = findViewById(R.id.headlineDetails);
        ParseObject auftrag = auftragsListe.get(0);
        headline.setText(auftrag.get("Typ") + "\n" + auftrag.getParseObject("Kunde").get("Name"));
    }

    public void zeigeAufzuege(List<ParseObject> auftragsListe) {
        LinearLayout layout = findViewById(R.id.aufzuegeLayout);
        layout.removeAllViewsInLayout();

        for (int i=0; i<auftragsListe.size(); i++) {
            ParseObject auftrag = auftragsListe.get(i);

            String aufzugsnummer = auftrag.getParseObject("Aufzug").get("Nummer") + "";
            String auftragsnummer = auftrag.get("Nummer") + "";
            final String auftragsId = auftrag.getObjectId();

            Button button = new Button(this);

            button.setId(i);
            button.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            button.setText("Aufzugsnummer: " + aufzugsnummer + "\n" + "Auftragsnummer: " + auftragsnummer);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openAufzug(v, auftragsId);
                }
            });

            if (i%2 == 0) {
                button.setBackgroundColor(0xFF80D8FF);
            } else
                button.setBackgroundColor(0xFF82B1FF);

            layout.addView(button);
        }
    }

    public void openAufzug(View view, String auftragsId) {
        Intent startAuftragActivity = new Intent(this, Auftragsanzeige.class);
        startAuftragActivity.putExtra("auftrag", auftragsId);
        startActivity(startAuftragActivity);
    }
}
