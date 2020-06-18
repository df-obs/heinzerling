package com.example.aussendiensterfassung;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.parse.*;
import com.trick2live.parser.rtf.exception.PlainTextExtractorException;
import com.trick2live.parser.rtf.exception.UnsupportedMimeTypeException;
import com.trick2live.parser.rtf.parser.PlainTextExtractor;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static android.view.View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION;

public class LockedOrders extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locked_orders);

        // Define layout
        final LinearLayout layoutOrders = findViewById(R.id.locked_orders_orders_layout);

        // Catch OrderID and define OrderID (if existing)
        String orderObjectId;
        Intent intent = getIntent();
        if (intent.hasExtra("orderObjectId")) {
            orderObjectId = intent.getStringExtra("orderObjectId");
            showLockedOrder(orderObjectId);
        }

        // Get user
        final SharedPreferences pref = getSharedPreferences("CONFIG", 0);
        final String userId = pref.getString("USER", "NOT_FOUND");
        ParseQuery<ParseObject> queryEmployeeList = ParseQuery.getQuery("Monteur");
        queryEmployeeList.whereEqualTo("objectId", userId);
        queryEmployeeList.fromLocalDatastore();
        queryEmployeeList.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    if (resultList.size() > 0) {
                        ParseObject employee = resultList.get(0);
                        final ParseObject userObject = employee;
                        String userSql = employee.getString("sqlRef");

                        // Get all locked (= signed) orders
                        ParseQuery<ParseObject> orderQuery = ParseQuery.getQuery("Einzelauftrag");
                        orderQuery.fromLocalDatastore();
                        orderQuery.whereEqualTo("Gesperrt", true);
                        orderQuery.whereContains("Monteure", userSql);
                        orderQuery.include("Gesamtauftrag");
                        orderQuery.include("Gesamtauftrag.Kunde");
                        orderQuery.orderByDescending("updatedAt");
                        orderQuery.findInBackground(new FindCallback<ParseObject>() {
                            public void done(List<ParseObject> resultList, ParseException e) {
                                if (e == null) {
                                    int count = 0;

                                    // Clear view
                                    layoutOrders.removeAllViewsInLayout();

                                    // Create a button for every order
                                    for (int i=0; i<resultList.size(); i++) {
                                        ParseObject order = resultList.get(i);

                                        // Get order data
                                        final String valueOrderId = String.valueOf(order.getInt("Nummer"));
                                        final String valueObjectId = order.getObjectId();
                                        final String valueCustomer = Objects.requireNonNull(Objects.requireNonNull(order.getParseObject("Gesamtauftrag")).getParseObject("Kunde")).getString("Name");

                                        // Create and style button
                                        Button buttonOrder = new Button(getApplicationContext());
                                        buttonOrder.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
                                        buttonOrder.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                                        buttonOrder.setText(String.format("%s: %s\n%s", getString(R.string.order_id), valueOrderId, valueCustomer));
                                        buttonOrder.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                showLockedOrder(valueObjectId);
                                            }
                                        });

                                        if (count % 2 == 0) {
                                            buttonOrder.setBackgroundColor(0xFF80D8FF);
                                        } else {
                                            buttonOrder.setBackgroundColor(0xFF82B1FF);
                                        }

                                        count++;

                                        layoutOrders.addView(buttonOrder);
                                    }
                                } else {
                                    Log.d("Locked Orders", "Error: " + e.getMessage());
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    // Show a single order with all details
    protected void showLockedOrder(String orderObjectId) {
        // Hide orders overview
        LinearLayout layoutOrders = findViewById(R.id.locked_orders_layout);
        layoutOrders.setVisibility(View.GONE);

        // Show detail view
        ScrollView layoutDetails = findViewById(R.id.locked_orders_details_layout);
        layoutDetails.setVisibility(View.VISIBLE);
        
        // Get order
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Einzelauftrag");
        query.fromLocalDatastore();
        query.whereEqualTo("objectId", orderObjectId);
        query.include("Aufzug");
        query.include("Aufzug.Kunde");
        query.include("Gesamtauftrag");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    ParseObject finalOrder = resultList.get(0);
                    DateFormat dateFormat = DateFormat.getDateInstance();
                    DateFormat timeFormat = DateFormat.getDateTimeInstance();

                    // Get and parse database contents
                    int valueOrderId = finalOrder.getInt("Nummer");
                    Date valueDate = Objects.requireNonNull(finalOrder.getParseObject("Gesamtauftrag")).getDate("Datum");
                    String valueStrDate = dateFormat.format(valueDate);
                    String valueCustomer = Objects.requireNonNull(Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getParseObject("Kunde")).getString("Name");
                    String valueCustomerStreet = Objects.requireNonNull(Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getParseObject("Kunde")).getString("Strasse");
                    String valueCustomerCity = Objects.requireNonNull(Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getParseObject("Kunde")).getString("PLZ") + " " + Objects.requireNonNull(Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getParseObject("Kunde")).getString("Ort");
                    String valueElevatorId = Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getString("Nummer");
                    String valueElevatorStreet = Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getString("Strasse");
                    String valueElevatorCity = Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getString("PLZ") + " " + Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getString("Ort");
                    String valueKeySafe = Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getString("Schluesseldepot");
                    if (valueKeySafe == null || valueKeySafe.equals("") || valueKeySafe.equals(" ")) {
                        valueKeySafe = getString(R.string.not_known);
                    }
                    Date valueLastMaintenance = Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getDate("LetzteWartung");
                    String valueStrLastMaintenance = getString(R.string.not_known);
                    if (valueLastMaintenance != null) {
                        valueStrLastMaintenance = dateFormat.format(valueLastMaintenance);
                    }
                    String valueWork = finalOrder.getString("Arbeiten");
                    String valueRemarks = finalOrder.getString("Bemerkungen");
                    Date valueUpdated = finalOrder.getUpdatedAt();
                    boolean valueShowTimestamp = finalOrder.getBoolean("Zeitstempel");
                    String valueStrUpdated = timeFormat.format(valueUpdated);

                    // Convert RTF
                    PlainTextExtractor rtfExtractor = new PlainTextExtractor();
                    try {
                        if (valueWork != null) {
                            if (valueWork.startsWith("{"))
                                valueWork = rtfExtractor.extract(valueWork, "application/rtf");
                        }
                        if (valueRemarks != null) {
                            if (valueRemarks.startsWith("{"))
                                valueRemarks = rtfExtractor.extract(valueRemarks, "application/rtf");
                        }
                    } catch (UnsupportedMimeTypeException | PlainTextExtractorException ex) {
                        ex.printStackTrace();
                    }

                    // Get and print images (signatures)
                    String stringSignatureEmployee = finalOrder.getString("Unterschrift_Monteur");
                    String stringSignatureCustomer = finalOrder.getString("Unterschrift_Kunde");
                    ImageView viewSignatureEmployee = findViewById(R.id.locked_orders_details_signature_employee);
                    ImageView viewSignatureCustomer = findViewById(R.id.locked_orders_details_signature_customer);
                    printBase64Image(Objects.requireNonNull(stringSignatureEmployee), viewSignatureEmployee);
                    printBase64Image(Objects.requireNonNull(stringSignatureCustomer), viewSignatureCustomer);

                    // Print headline
                    TextView textHeadline = findViewById(R.id.locked_orders_details_headline);
                    textHeadline.setText(String.format("%s: %s", getString(R.string.order_number), valueOrderId));

                    // Print order data
                    TextView viewData = findViewById(R.id.locked_orders_details_text_data);
                    viewData.setText(String.format("%s: %s\n\n%s\n%s\n%s\n\n%s: %s\n\n%s:\n%s\n%s\n\n%s: %s\n", getString(R.string.date), valueStrDate, valueCustomer, valueCustomerStreet, valueCustomerCity, getString(R.string.elevator_id), valueElevatorId, getString(R.string.location), valueElevatorStreet, valueElevatorCity, getString(R.string.key_safe), valueKeySafe));

                    // Print data of last maintenance
                    TextView viewLastMaintenance = findViewById(R.id.locked_orders_details_text_lastmaintenance);
                    viewLastMaintenance.setText(String.format("\n%s: %s", getString(R.string.last_maintenance), valueStrLastMaintenance));

                    // Print work
                    TextView viewWork = findViewById(R.id.locked_orders_details_text_work);
                    if (valueWork != null) {
                        if (!valueWork.matches("")) {
                            viewWork.setVisibility(View.VISIBLE);
                            viewWork.setText(String.format("\n%s: %s", getString(R.string.performed_work), valueWork));
                        } else {
                            viewWork.setVisibility(View.GONE);
                        }
                    } else {
                        viewWork.setVisibility(View.GONE);
                    }

                    // Print remarks
                    TextView viewRemarks = findViewById(R.id.locked_orders_details_text_remarks);
                    if (valueRemarks != null) {
                        if (!valueRemarks.matches("")) {
                            viewRemarks.setVisibility(View.VISIBLE);
                            viewRemarks.setText(String.format("\n%s: %s\n", getString(R.string.remarks), valueRemarks));
                        } else {
                            viewRemarks.setVisibility(View.VISIBLE);
                            viewRemarks.setText("\n");
                        }
                    } else {
                        viewRemarks.setVisibility(View.VISIBLE);
                        viewRemarks.setText("\n");
                    }

                    // Print signature date (it timestamp option is set)
                    TextView viewSignatureDate = findViewById(R.id.locked_orders_details_signature_date);
                    if (valueShowTimestamp) {
                        viewSignatureDate.setVisibility(View.VISIBLE);
                        viewSignatureDate.setText(String.format("%s %s\n", getString(R.string.signed_at), valueStrUpdated));
                    } else {
                        viewSignatureDate.setVisibility(View.GONE);
                    }

                    // Get positions (mechanics + material)
                    getPositions(finalOrder);
                } else {
                    Log.d("GetFinalOverviewQuery", "Error: " + e.getMessage());
                }
            }
        });
    }

    // Show positions on the final order overview
    protected void getPositions(ParseObject order) {
        // Delete old lines
        ArrayList<View> dynamicRows = new ArrayList<>();
        View rootView = findViewById(R.id.locked_orders_details_layout);
        rootView.findViewsWithText(dynamicRows, "dynamicRow", FIND_VIEWS_WITH_CONTENT_DESCRIPTION);

        for (int i=0; i<dynamicRows.size(); i++) {
            View obsoleteRow = dynamicRows.get(i);
            ((ViewGroup)obsoleteRow.getParent()).removeView(obsoleteRow);
        }

        // Get material
        ParseQuery<ParseObject> queryMaterial = ParseQuery.getQuery("ArtikelAuftrag");
        queryMaterial.fromLocalDatastore();
        queryMaterial.whereEqualTo("Auftrag", order);
        queryMaterial.include("Artikel");

        queryMaterial.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    // Define position table
                    TableLayout tablePositions = findViewById(R.id.locked_orders_details_table_material);

                    // Parse and print the single positions
                    for (int i=0; i<resultList.size(); i++) {
                        ParseObject materialPosition = resultList.get(i);

                        // Get field contents out of the database
                        int pos = i + 1;
                        double quantity = materialPosition.getDouble("Anzahl");
                        String unit = Objects.requireNonNull(materialPosition.getParseObject("Artikel")).getString("Einheit");
                        String name = Objects.requireNonNull(materialPosition.getParseObject("Artikel")).getString("Name");

                        // Create new table row
                        TableRow row = new TableRow(getApplicationContext());
                        row.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));
                        row.setContentDescription("dynamicRow");

                        // Print position
                        TextView textPosition = new TextView(getApplicationContext());
                        textPosition.setText(String.valueOf(pos));
                        textPosition.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        row.addView(textPosition);

                        // Print quantity
                        TextView textQuantity = new TextView(getApplicationContext());
                        DecimalFormat f = new DecimalFormat("0.00");
                        textQuantity.setText(f.format(quantity));
                        textQuantity.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        row.addView(textQuantity);

                        // Print unit
                        TextView textUnit = new TextView(getApplicationContext());
                        textUnit.setText(unit);
                        textUnit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        row.addView(textUnit);

                        // Print name
                        TextView textName = new TextView(getApplicationContext());
                        textName.setText(name);
                        textName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        row.addView(textName);

                        // Attach table row to table
                        tablePositions.addView(row);
                    }
                } else {
                    Log.d("GetFinalMaterialQuery", "Error: " + e.getMessage());
                }
            }
        });

        // Get mechanics
        ParseQuery<ParseObject> queryMechanics = ParseQuery.getQuery("MonteurAuftrag");
        queryMechanics.fromLocalDatastore();
        queryMechanics.whereEqualTo("Auftrag", order);
        queryMechanics.include("Monteur");

        queryMechanics.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    // Define position table
                    TableLayout tablePositions = findViewById(R.id.locked_orders_details_table_mechanics);

                    if (resultList.size() > 0) {
                        tablePositions.setVisibility(View.VISIBLE);
                    } else {
                        tablePositions.setVisibility(View.GONE);
                    }

                    // Parse and print the single positions
                    for (int i=0; i<resultList.size(); i++) {
                        ParseObject mechanicPosition = resultList.get(i);

                        // Get field contents out of the database
                        int pos = i + 1;
                        double quantity = mechanicPosition.getDouble("Stunden");
                        String name = Objects.requireNonNull(mechanicPosition.getParseObject("Monteur")).getString("Name");

                        // Create new table row
                        TableRow row = new TableRow(getApplicationContext());
                        row.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));
                        row.setContentDescription("dynamicRow");

                        // Print position
                        TextView textPosition = new TextView(getApplicationContext());
                        textPosition.setText(String.valueOf(pos));
                        textPosition.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        row.addView(textPosition);

                        // Print quantity
                        TextView textQuantity = new TextView(getApplicationContext());
                        DecimalFormat f = new DecimalFormat("0.00");
                        textQuantity.setText(String.format ("%s %s", f.format(quantity), getString(R.string.hour_short)));
                        textQuantity.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        row.addView(textQuantity);

                        // Print name
                        TextView textName = new TextView(getApplicationContext());
                        textName.setText(name);
                        textName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        row.addView(textName);

                        // Attach table row to table
                        tablePositions.addView(row);
                    }
                } else {
                    Log.d("GetFinalMechanicsQuery", "Error: " + e.getMessage());
                }
            }
        });
    }

    // Close detail view and switch to overview
    public void closeDetails() {
        // Hide detail view
        ScrollView layoutDetails = findViewById(R.id.locked_orders_details_layout);
        layoutDetails.setVisibility(View.GONE);

        // Show orders overview
        LinearLayout layoutOrders = findViewById(R.id.locked_orders_layout);
        layoutOrders.setVisibility(View.VISIBLE);
    }

    // Handle Back button
    public void onBackPressed() {
        ScrollView layoutDetails = findViewById(R.id.locked_orders_details_layout);
        if (layoutDetails.getVisibility() == View.VISIBLE) {
            closeDetails();
        } else {
            Intent switchToMain = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(switchToMain);
        }
    }

    // Decode and Base64-Images
    protected void printBase64Image(String stringImg, ImageView imageView)
    {
        byte[] byteStream = Base64.decode(stringImg, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(byteStream, 0, byteStream.length);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        imageView.setAdjustViewBounds(true);
        imageView.setImageBitmap(bitmap);
    }
}