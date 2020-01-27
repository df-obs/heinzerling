package com.example.aussendiensterfassung;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

public class Auftragsanzeige extends AppCompatActivity {

    LinearLayout layoutAuftragAnzeige;
    LinearLayout layoutAnsprechpartnerAnzeige;
    ImageButton callButton;
    BottomNavigationView navView;
    String objectAdress;
    String auftragsnummer;
    ParseObject kundeObj;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    layoutAnsprechpartnerAnzeige.setVisibility(View.GONE);
                    layoutAuftragAnzeige.setVisibility(View.VISIBLE);
                    return true;
                case R.id.navigation_navi:
                    Intent mapsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(objectAdress));
                    startActivity(mapsIntent);
                    return true;
                case R.id.navigation_contacts:
                    layoutAuftragAnzeige.setVisibility(View.GONE);
                    layoutAnsprechpartnerAnzeige.setVisibility(View.VISIBLE);
                    return true;
                case R.id.navigation_edit:
                    editAuftrag(findViewById(R.id.edit_order_navigation));
                    return true;
            }
            return false;
        }
    };

    public void editAuftrag(View view) {
        Intent startAuftragEdit = new Intent(this, EditOrder.class);
        startAuftragEdit.putExtra("orderObjectId", auftragsnummer);
        startActivity(startAuftragEdit);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auftragsanzeige);

        Intent intent = getIntent();
        auftragsnummer = intent.getStringExtra("orderObjectId");

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Einzelauftrag");
        query.whereEqualTo("objectId", auftragsnummer);
        query.include("Aufzug");
        query.include("Aufzug.Kunde");
        query.include("Gesamtauftrag");

        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> einzelauftrag, ParseException e) {
                if (e == null) {
                    zeigeAuftrag(einzelauftrag);
                } else {
                    Log.d("Aufzuege", "Error: " + e.getMessage());
                }
            }
        });

        navView = findViewById(R.id.nav_auftrag);
        layoutAuftragAnzeige = findViewById(R.id.layoutAuftragAnzeige);
        layoutAnsprechpartnerAnzeige = findViewById(R.id.layoutAnsprechpartnerAnzeige);

        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        rufeArtikelAb();
    }

    protected void rufeArtikelAb() {
        ParseQuery<ParseObject> innerQuery = ParseQuery.getQuery("Einzelauftrag");
        innerQuery.whereEqualTo("objectId", auftragsnummer);
        ParseQuery<ParseObject> query = ParseQuery.getQuery("ArtikelAuftrag");
        query.whereMatchesQuery("Auftrag", innerQuery);
        query.whereEqualTo("Vorgegeben", true);
        query.include("Artikel");

        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> artikel, ParseException e) {
                if (e == null) {
                    zeigeArtikel(artikel);
                } else {
                    Log.d("Artikel", "Error: " + e.getMessage());
                }
            }
        });
    }

    protected void zeigeArtikel(List<ParseObject> artikelListe) {
        TableLayout table = findViewById(R.id.artikelTabelle);

        for (int i=0; i<artikelListe.size(); i++) {
            ParseObject artikel = artikelListe.get(i);

            int pos = i+1;
            double menge = Double.valueOf(artikel.get("Anzahl") + "");
            String einheit = artikel.getParseObject("Artikel").get("Einheit") + "";
            String beschreibung = artikel.getParseObject("Artikel").get("Name") + "";

            TableRow row = new TableRow(this);
            row.setLayoutParams(new TableLayout.LayoutParams( TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));

            TextView vPos = new TextView(this);
            vPos.setText(pos + "");
            vPos.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            row.addView(vPos);

            TextView vMenge = new TextView(this);
            DecimalFormat f = new DecimalFormat("0.00");
            vMenge.setText(f.format(menge));
            vMenge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            row.addView(vMenge);

            TextView vEinheit = new TextView(this);
            vEinheit.setText(einheit);
            vEinheit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            row.addView(vEinheit);

            TextView vName = new TextView(this);
            vName.setText(beschreibung);
            vName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            row.addView(vName);

            table.addView(row);
        }
    }

    protected void rufeAnsprechpartnerAb() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Ansprechpartner");
        query.whereEqualTo("Kunde", kundeObj);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> ansprechpartner, ParseException e) {
                if (e == null) {
                    zeigeAnsprechpartner(ansprechpartner);
                } else {
                    Log.d("Ansprechpartner", "Error: " + e.getMessage());
                }
            }
        });
    }

    protected void zeigeAnsprechpartner(List<ParseObject> personenListe) {
        TableLayout table = findViewById(R.id.ansprechpartnerTabelle);

        for (int i=0; i<personenListe.size(); i++) {
            ParseObject person = personenListe.get(i);

            String name = person.get("Name") + "";
            final String telefon = person.get("Telefon") + "";

            TableRow row = new TableRow(this);
            row.setLayoutParams(new TableLayout.LayoutParams( TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));
            row.setGravity(Gravity.CENTER_VERTICAL);

            ImageButton callButton = new ImageButton(this);
            callButton.setLayoutParams(new TableRow.LayoutParams( TableRow.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
            callButton.setImageResource(R.drawable.ic_contact_phone_black_48dp);
            row.addView(callButton);

            callButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent phoneIntent = new Intent();
                    phoneIntent.setAction(Intent.ACTION_DIAL);
                    phoneIntent.setData(Uri.parse("tel:" + telefon));
                    startActivity(phoneIntent);
                }
            });

            TextView vName = new TextView(this);
            vName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            vName.setTypeface(vName.getTypeface(), Typeface.BOLD);
            vName.setText(name + "\n" + telefon);
            row.addView(vName);

            table.addView(row);
        }
    }

    protected void zeigeAuftrag(List<ParseObject> auftragsListe) {
        ParseObject auftrag = auftragsListe.get(0);

        String auftragsnummer = auftrag.get("Nummer") + "";
        Date date = (Date) auftrag.getParseObject("Gesamtauftrag").get("Datum");
        String datum = date.getDate() + "." + (date.getMonth()+1) + "." + (date.getYear()+1900);
        String kunde = (String) auftrag.getParseObject("Aufzug").getParseObject("Kunde").get("Name");
        kundeObj = auftrag.getParseObject("Aufzug").getParseObject("Kunde");
        String kd_strasse = (String) auftrag.getParseObject("Aufzug").getParseObject("Kunde").get("Strasse");
        String kd_ort = auftrag.getParseObject("Aufzug").getParseObject("Kunde").get("PLZ") + " " + auftrag.getParseObject("Aufzug").getParseObject("Kunde").get("Ort");
        String aufzugsnummer = auftrag.getParseObject("Aufzug").get("Nummer") + "";
        String az_strasse = (String) auftrag.getParseObject("Aufzug").get("Strasse");
        String az_ort = auftrag.getParseObject("Aufzug").get("PLZ") + " " + auftrag.getParseObject("Aufzug").get("Ort");
        String schluesseldepot = (String) auftrag.getParseObject("Aufzug").get("Schluesseldepot");
        Date dateLast = (Date) auftrag.getParseObject("Aufzug").get("LetzteWartung");
        String letzteWartung = dateLast.getDate() + "." + (dateLast.getMonth()+1) + "." + (dateLast.getYear()+1900);

        TextView viewAuftragsnummer = findViewById(R.id.auftragsNr);
        viewAuftragsnummer.setText("Auftrag Nummer " + auftragsnummer);

        TextView viewDaten = findViewById(R.id.auftragsDaten);
        viewDaten.setText("Datum: "+ datum + "\n\n" + kunde + "\n" + kd_strasse + "\n" + kd_ort + "\n\nAufzugnummer: "+ aufzugsnummer + "\n\n" + "Standort:\n" + az_strasse + "\n" + az_ort + "\n\nSchlüsseldepot: " + schluesseldepot + "\n");

        TextView viewLetzteWartung = findViewById(R.id.letzteWartung);
        viewLetzteWartung.setText("\nLetzte Wartung: " + letzteWartung);

        TextView headlineAnsprechpartner = findViewById(R.id.auftragsNrKontakte);
        headlineAnsprechpartner.setText("Auftrag Nummer " + auftragsnummer + "\nAnsprechpartner");

        objectAdress = az_strasse + " " + az_ort;
        objectAdress = objectAdress.replace(" ", "+");
        objectAdress = "https://www.google.com/maps/dir/?api=1&destination=" + objectAdress;

        rufeAnsprechpartnerAb();
    }
}
