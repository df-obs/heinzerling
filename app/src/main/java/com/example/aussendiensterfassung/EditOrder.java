package com.example.aussendiensterfassung;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static android.view.View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION;

public class EditOrder extends AppCompatActivity {

    LinearLayout layoutMaterial;
    LinearLayout layoutMechanics;
    LinearLayout layoutServices;
    ScrollView layoutOverview;
    BottomNavigationView navView;

    ParseObject order;
    String      orderObjectId;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.edit_order_nav_material:
                    layoutMechanics.setVisibility(View.GONE);
                    layoutServices.setVisibility(View.GONE);
                    layoutOverview.setVisibility(View.GONE);
                    layoutMaterial.setVisibility(View.VISIBLE);
                    getMaterial();
                    return true;
                case R.id.edit_order_nav_mechanics:
                    layoutServices.setVisibility(View.GONE);
                    layoutOverview.setVisibility(View.GONE);
                    layoutMaterial.setVisibility(View.GONE);
                    layoutMechanics.setVisibility(View.VISIBLE);
                    getMechanics();
                    return true;
                case R.id.edit_order_nav_services:
                    layoutOverview.setVisibility(View.GONE);
                    layoutMaterial.setVisibility(View.GONE);
                    layoutMechanics.setVisibility(View.GONE);
                    layoutServices.setVisibility(View.VISIBLE);
                    getServices();
                    return true;
                case R.id.edit_order_nav_overview:
                    layoutMaterial.setVisibility(View.GONE);
                    layoutMechanics.setVisibility(View.GONE);
                    layoutServices.setVisibility(View.GONE);
                    layoutOverview.setVisibility(View.VISIBLE);
                    getOverview();
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_order);

        // Catch OrderID and define OrderID and order ParseObject
        Intent intent = getIntent();
        orderObjectId = intent.getStringExtra("orderObjectId");

        ParseQuery<ParseObject> queryOrder = ParseQuery.getQuery("Einzelauftrag");
        queryOrder.fromLocalDatastore();
        queryOrder.whereEqualTo("objectId", orderObjectId);
        queryOrder.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    order = resultList.get(0);
                    // Fill user input fields with existing data
                    getMaterial();
                } else {
                    Log.d("Edit Order", "Error: " + e.getMessage());
                }
            }
        });

        // Define layouts
        layoutMaterial = findViewById(R.id.edit_order_material_layout);
        layoutMechanics = findViewById(R.id.edit_order_mechanics_layout);
        layoutServices = findViewById(R.id.edit_order_services_layout);
        layoutOverview = findViewById(R.id.edit_order_overview_layout);
        navView = findViewById(R.id.edit_order_navigation);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Initialize buttons
        Button buttonSaveMaterial = findViewById(R.id.edit_order_material_button_save);
        buttonSaveMaterial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMaterial();
            }
        });

        Button buttonSaveMechanics = findViewById(R.id.edit_order_mechanics_button_save);
        buttonSaveMechanics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMechanics();
            }
        });

        Button buttonSaveServices = findViewById(R.id.edit_order_services_button_save);
        buttonSaveServices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveServices();
            }
        });

        Button buttonSignOrder = findViewById(R.id.edit_order_overview_button_sign);
        buttonSignOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent switchToSignOrder = new Intent(getApplicationContext(), SignOrder.class);
                switchToSignOrder.putExtra("orderObjectId", orderObjectId);
                startActivity(switchToSignOrder);
            }
        });
    }

    // Synchronize the material list: SERVER >> USER-INPUT-FIELDS
    protected void getMaterial() {
        // Generate article list for autocomplete
        final ArrayList<String> articleList = new ArrayList<>();

        ParseQuery<ParseObject> queryArticleList = ParseQuery.getQuery("Artikel");
        queryArticleList.fromLocalDatastore();
        queryArticleList.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    for (int i = 0; i < resultList.size(); i++) {
                        ParseObject article = resultList.get(i);
                        articleList.add(article.getString("Name"));
                    }
                }
            }
        });

        // Find previous attached articles
        ParseQuery<ParseObject> queryOld = ParseQuery.getQuery("ArtikelAuftrag");
        queryOld.fromLocalDatastore();
        queryOld.whereEqualTo("Auftrag", order);
        queryOld.whereEqualTo("Vorgegeben", false);
        queryOld.include("Artikel");
        queryOld.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    for (int i = 0; i < 8; i++) {
                        // Define fields
                        String inputQtyId = "edit_order_material_input_qty_" + i;
                        String inputArtId = "edit_order_material_input_art_" + i;
                        int inputQtyRes = getResources().getIdentifier(inputQtyId, "id", getPackageName());
                        int inputArtRes = getResources().getIdentifier(inputArtId, "id", getPackageName());

                        EditText fieldQuantity            = findViewById(inputQtyRes);
                        AutoCompleteTextView fieldArticle = findViewById(inputArtRes);

                        // Activate AutoComplete
                        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_dropdown_item_1line, articleList);
                        fieldArticle.setThreshold(1);
                        fieldArticle.setAdapter(arrayAdapter);

                        if (i < resultList.size()) {
                            // Define position object
                            ParseObject oldPosition = resultList.get(i);

                            // Define field contents
                            Double valueQuantity = oldPosition.getDouble("Anzahl");
                            String valueArticle = Objects.requireNonNull(oldPosition.getParseObject("Artikel")).getString("Name");

                            // Set field contents
                            DecimalFormat f = new DecimalFormat("0.00");
                            fieldQuantity.setText(f.format(valueQuantity));
                            fieldArticle.setText(valueArticle);
                        } else {
                            fieldQuantity.setText("");
                            fieldArticle.setText("");
                        }
                    }
                } else {
                    Log.d("AttachArticleQuery", "Error: " + e.getMessage());
                }
            }
        });
    }

    // Synchronize the material list: USER-INPUTS >> SERVER.
    protected void saveMaterial() {
        // Find and delete existing positions
        ParseQuery<ParseObject> queryOld = ParseQuery.getQuery("ArtikelAuftrag");
        queryOld.fromLocalDatastore();
        queryOld.whereEqualTo("Auftrag", order);
        queryOld.whereEqualTo("Vorgegeben", false);
        queryOld.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    for (int i=0; i<resultList.size(); i++) {
                        ParseObject oldPosition = resultList.get(i);
                        oldPosition.deleteEventually();
                    }
                } else {
                    Log.d("GetArticleQuery", "Error: " + e.getMessage());
                }
            }
        });

        // Save positions
        for (int i=0; i<8; i++) {
            // Get user inputs
            String inputQtyId = "edit_order_material_input_qty_" + i;
            String inputArtId = "edit_order_material_input_art_" + i;
            int inputQtyRes = getResources().getIdentifier(inputQtyId, "id", getPackageName());
            int inputArtRes = getResources().getIdentifier(inputArtId, "id", getPackageName());

            EditText fieldQuantity        = findViewById(inputQtyRes);
            EditText fieldArticle         = findViewById(inputArtRes);
            final String valueStrQuantity = fieldQuantity.getText().toString().replace(",", ".");
            final String valueArticle     = fieldArticle.getText().toString();
            final double valueQuantity;

            // Check if field is empty or quantity is 0 or quantity is not a number
            boolean entryOk = false;

            if (Objects.equals(valueStrQuantity, "") || !valueStrQuantity.matches("-?\\d+(\\.\\d+)?") || valueArticle.matches("")) {
                Log.d("AttachArticleCheck", "Info: Entry in line " + i + " is not correct, will not save article.");
            } else {
                if (Double.parseDouble(valueStrQuantity) > 0) {
                    entryOk = true;
                } else {
                    Log.d("AttachArticleCheck", "Info: Quantity in line " + i + " is 0, will not save article.");
                }
            }

            if (entryOk) { // User input is okay, proceed
                // Convert value quantity
                valueQuantity = Double.parseDouble(valueStrQuantity);

                // Check if article exists
                ParseQuery<ParseObject> query = ParseQuery.getQuery("Artikel");
                query.fromLocalDatastore();
                query.whereEqualTo("Name", valueArticle);
                query.findInBackground(new FindCallback<ParseObject>() {
                    public void done(List<ParseObject> resultList, ParseException e) {
                        if (e == null) {
                            // Article does not exist yet, create it
                            if (resultList.size() == 0) {
                                ParseObject newArticle = new ParseObject("Artikel");
                                newArticle.put("Name", valueArticle);
                                newArticle.put("Einheit", "stk");
                                newArticle.saveEventually();
                            }

                            // Attach article to order
                            ParseQuery<ParseObject> queryArticle = ParseQuery.getQuery("Artikel");
                            queryArticle.fromLocalDatastore();
                            queryArticle.whereEqualTo("Name", valueArticle);
                            queryArticle.findInBackground(new FindCallback<ParseObject>() {
                                public void done(List<ParseObject> resultList, ParseException e) {
                                    if (e == null) {
                                        if (resultList.size() == 0) {
                                            // No matching article > Error while creating in previous step
                                            Log.d("AttachArticleQuery", "Error: No matching article, even after creating it.");
                                        } else {
                                            // Get matching article and attach it to order
                                            ParseObject article = resultList.get(0);
                                            ParseObject newPosition = new ParseObject("ArtikelAuftrag");
                                            newPosition.put("Anzahl", valueQuantity);
                                            newPosition.put("Artikel", article);
                                            newPosition.put("Auftrag", order);
                                            newPosition.put("Vorgegeben", false);
                                            newPosition.saveEventually();
                                        }
                                    } else {
                                        Log.d("AttachArticleQuery", "Error: " + e.getMessage());
                                    }
                                }
                            });
                        } else {
                            Log.d("CreateArticleQuery", "Error: " + e.getMessage());
                        }
                    }
                });
            }
        }
    }

    // Synchronize the mechanics list: SERVER >> USER-INPUT-FIELDS
    protected void getMechanics() {
        // Generate employee list for autocomplete
        final ArrayList<String> employeeList = new ArrayList<>();

        ParseQuery<ParseObject> queryEmployeeList = ParseQuery.getQuery("Monteur");
        queryEmployeeList.fromLocalDatastore();
        queryEmployeeList.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    for (int i = 0; i < resultList.size(); i++) {
                        ParseObject employee = resultList.get(i);
                        employeeList.add(employee.getString("Name"));
                    }
                }
            }
        });

        // Find previous attached persons
        ParseQuery<ParseObject> queryOld = ParseQuery.getQuery("MonteurAuftrag");
        queryOld.fromLocalDatastore();
        queryOld.whereEqualTo("Auftrag", order);
        queryOld.whereEqualTo("Vorgegeben", false);
        queryOld.include("Monteur");
        queryOld.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    for (int i = 0; i < 8; i++) {
                        // Define fields
                        String inputQtyId  = "edit_order_mechanics_input_qty_" + i;
                        String inputNameId = "edit_order_mechanics_input_name_" + i;
                        String inputCatId  = "edit_order_mechanics_input_cat_" + i;
                        int inputQtyRes  = getResources().getIdentifier(inputQtyId, "id", getPackageName());
                        int inputNameRes = getResources().getIdentifier(inputNameId, "id", getPackageName());
                        int inputCatRes  = getResources().getIdentifier(inputCatId, "id", getPackageName());

                        EditText fieldQuantity         = findViewById(inputQtyRes);
                        AutoCompleteTextView fieldName = findViewById(inputNameRes);
                        EditText fieldCategory         = findViewById(inputCatRes);

                        // Activate AutoComplete
                        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_dropdown_item_1line, employeeList);
                        fieldName.setThreshold(1);
                        fieldName.setAdapter(arrayAdapter);

                        if (i < resultList.size()) {
                            // Define position object
                            ParseObject oldPosition = resultList.get(i);

                            // Define field contents
                            Double valueQuantity = oldPosition.getDouble("Stunden");
                            String valueName = Objects.requireNonNull(oldPosition.getParseObject("Monteur")).getString("Name");
                            Double valueCategory = oldPosition.getDouble("Kategorie");

                            // Set field contents
                            DecimalFormat f = new DecimalFormat("0.00");
                            fieldQuantity.setText(f.format(valueQuantity));
                            fieldName.setText(valueName);
                            fieldCategory.setText(f.format(valueCategory));
                        } else {
                            fieldQuantity.setText("");
                            fieldName.setText("");
                            fieldCategory.setText("");
                        }
                    }
                } else {
                    Log.d("GetMechanicQuery", "Error: " + e.getMessage());
                }
            }
        });
    }

    // Synchronize the mechanics list: USER-INPUTS >> SERVER.
    protected void saveMechanics() {
        // Find and delete existing positions
        ParseQuery<ParseObject> queryOld = ParseQuery.getQuery("MonteurAuftrag");
        queryOld.fromLocalDatastore();
        queryOld.whereEqualTo("Auftrag", order);
        queryOld.whereEqualTo("Vorgegeben", false);
        queryOld.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    for (int i=0; i<resultList.size(); i++) {
                        ParseObject oldPosition = resultList.get(i);
                        oldPosition.deleteEventually();
                    }
                } else {
                    Log.d("AttachMechanicsQuery", "Error: " + e.getMessage());
                }
            }
        });

        // Save positions
        for (int i=0; i<8; i++) {
            // Get user inputs
            String inputQtyId  = "edit_order_mechanics_input_qty_" + i;
            String inputNameId = "edit_order_mechanics_input_name_" + i;
            String inputCatId  = "edit_order_mechanics_input_cat_" + i;
            int inputQtyRes  = getResources().getIdentifier(inputQtyId, "id", getPackageName());
            int inputNameRes = getResources().getIdentifier(inputNameId, "id", getPackageName());
            int inputCatRes  = getResources().getIdentifier(inputCatId, "id", getPackageName());

            EditText fieldQuantity        = findViewById(inputQtyRes);
            EditText fieldName            = findViewById(inputNameRes);
            EditText fieldCategory        = findViewById(inputCatRes);
            final String valueStrQuantity = fieldQuantity.getText().toString().replace(",", ".");
            final String valueName        = fieldName.getText().toString();
            final String valueStrCategory = fieldCategory.getText().toString().replace(",", ".");
            final double valueQuantity;
            final double valueCategory;

            // Check if field is empty or quantity is 0 or quantity or category is not a number
            boolean entryOk = false;

            if (Objects.equals(valueStrQuantity, "") || !valueStrQuantity.matches("-?\\d+(\\.\\d+)?") || !valueStrCategory.matches("-?\\d+(\\.\\d+)?") || valueName.matches("")) {
                Log.d("AttachMechanicsCheck", "Info: Entry in line " + i + " is not correct, will not save mechanic.");
            } else {
                if (Double.parseDouble(valueStrQuantity) > 0) {
                    entryOk = true;
                } else {
                    Log.d("AttachMechanicsCheck", "Info: Quantity in line " + i + " is 0, will not save mechanic.");
                }
            }

            if (entryOk) { // User input is okay, proceed
                // Convert value quantity and category
                valueQuantity = Double.parseDouble(valueStrQuantity);
                valueCategory = Double.parseDouble(valueStrCategory);

                // Attach mechanic to order
                ParseQuery<ParseObject> queryMechanic = ParseQuery.getQuery("Monteur");
                queryMechanic.fromLocalDatastore();
                queryMechanic.whereEqualTo("Name", valueName);
                queryMechanic.findInBackground(new FindCallback<ParseObject>() {
                    public void done(List<ParseObject> resultList, ParseException e) {
                        if (e == null) {
                            if (resultList.size() == 0) {
                                // No matching mechanic, incorrect user input
                                Log.d("AttachMechanicQuery", "Error: No matching mechanic, will not save it.");
                            } else {
                                // Get matching mechanic and attach it to order
                                ParseObject mechanic = resultList.get(0);
                                ParseObject newPosition = new ParseObject("MonteurAuftrag");
                                newPosition.put("Stunden", valueQuantity);
                                newPosition.put("Monteur", mechanic);
                                newPosition.put("Auftrag", order);
                                newPosition.put("Kategorie", valueCategory);
                                newPosition.put("Vorgegeben", false);
                                newPosition.saveEventually();
                            }
                        } else {
                            Log.d("AttachMechanicQuery", "Error: " + e.getMessage());
                        }
                    }
                });
            }
        }
    }

    // Synchronize work and remarks: SERVER >> USER-INPUT-FIELDS
    protected void getServices() {
        // Find previous attached persons
        ParseQuery<ParseObject> queryOld = ParseQuery.getQuery("Einzelauftrag");
        queryOld.fromLocalDatastore();
        queryOld.whereEqualTo("objectId", orderObjectId);
        queryOld.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    // Define fields
                    EditText fieldWork     = findViewById(R.id.edit_order_services_input_work);
                    EditText fieldRemarks  = findViewById(R.id.edit_order_services_input_remarks);
                    CheckBox fieldFinished = findViewById(R.id.edit_order_services_input_finished);

                    // Define position object
                    ParseObject orderObject = resultList.get(0);

                    // Define field contents
                    String valueWork      = orderObject.getString("Arbeiten");
                    String valueRemarks   = orderObject.getString("Bemerkungen");
                    boolean valueFinished = orderObject.getBoolean("Abgeschlossen");

                    // Set field contents
                    fieldWork.setText(valueWork);
                    fieldRemarks.setText(valueRemarks);
                    fieldFinished.setChecked(valueFinished);
                } else {
                    Log.d("GetServicesQuery", "Error: " + e.getMessage());
                }
            }
        });
    }

    // Synchronize work and remarks: USER-INPUTS >> SERVER.
    protected void saveServices() {
        // Get user inputs
        EditText fieldWork     = findViewById(R.id.edit_order_services_input_work);
        EditText fieldRemarks  = findViewById(R.id.edit_order_services_input_remarks);
        CheckBox fieldFinished = findViewById(R.id.edit_order_services_input_finished);

        final String valueWork      = fieldWork.getText().toString();
        final String valueRemarks   = fieldRemarks.getText().toString();
        final Boolean valueFinished = fieldFinished.isChecked();

        // Save user inputs to database
        ParseQuery<ParseObject> queryOrder = ParseQuery.getQuery("Einzelauftrag");
        queryOrder.fromLocalDatastore();
        queryOrder.whereEqualTo("objectId", orderObjectId);
        queryOrder.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    ParseObject orderObject = resultList.get(0);
                    orderObject.put("Arbeiten", valueWork);
                    orderObject.put("Bemerkungen", valueRemarks);
                    orderObject.put("Abgeschlossen", valueFinished);
                    orderObject.saveEventually();
                } else {
                    Log.d("SaveServicesQuery", "Error: " + e.getMessage());
                }
            }
        });
    }

    // Show positions on the final order overview
    protected void getPositions() {
        // Delete old lines
        ArrayList<View> dynamicRows = new ArrayList<>();
        View rootView = findViewById(R.id.edit_order_overview_layout);
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
                    TableLayout tablePositions = findViewById(R.id.edit_order_overview_table_material);

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
                    TableLayout tablePositions = findViewById(R.id.edit_order_overview_table_mechanics);

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

    // Show the final order overview
    protected void getOverview() {
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

                    // Get and parse database contents
                    int valueOrderId = finalOrder.getInt("Nummer");
                    Date valueDate = Objects.requireNonNull(finalOrder.getParseObject("Gesamtauftrag")).getDate("Datum");
                    String valueStrDate = dateFormat.format(valueDate);
                    String valueCustomer = Objects.requireNonNull(Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getParseObject("Kunde")).getString("Name");
                    String valueCustomerStreet = Objects.requireNonNull(Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getParseObject("Kunde")).getString("Strasse");
                    String valueCustomerCity = Objects.requireNonNull(Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getParseObject("Kunde")).getInt("PLZ") + " " + Objects.requireNonNull(Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getParseObject("Kunde")).getString("Ort");
                    String valueElevatorId = Integer.toString(Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getInt("Nummer"));
                    String valueElevatorStreet = Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getString("Strasse");
                    String valueElevatorCity = Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getInt("PLZ") + " " + Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getString("Ort");
                    String valueKeySafe = Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getString("Schluesseldepot");
                    Date valueLastMaintenance = Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getDate("LetzteWartung");
                    String valueStrLastMaintenance = dateFormat.format(valueLastMaintenance);
                    String valueWork = finalOrder.getString("Arbeiten");
                    String valueRemarks = finalOrder.getString("Bemerkungen");

                    // Print headline
                    TextView textHeadline = findViewById(R.id.edit_order_overview_headline);
                    textHeadline.setText(String.format("%s: %s", getString(R.string.order_number), valueOrderId));

                    // Print order data
                    TextView viewData = findViewById(R.id.edit_order_overview_text_data);
                    viewData.setText(String.format("%s: %s\n\n%s\n%s\n%s\n\n%s: %s\n\n%s:\n%s\n%s\n\n%s: %s\n", getString(R.string.date), valueStrDate, valueCustomer, valueCustomerStreet, valueCustomerCity, getString(R.string.elevator_id), valueElevatorId, getString(R.string.location), valueElevatorStreet, valueElevatorCity, getString(R.string.key_safe), valueKeySafe));

                    // Print data of last maintenance
                    TextView viewLastMaintenance = findViewById(R.id.edit_order_overview_text_lastmaintenance);
                    viewLastMaintenance.setText(String.format("\n%s: %s", getString(R.string.last_maintenance), valueStrLastMaintenance));

                    // Print work
                    TextView viewWork = findViewById(R.id.edit_order_overview_text_work);
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
                    TextView viewRemarks = findViewById(R.id.edit_order_overview_text_remarks);
                    if (valueRemarks != null) {
                        if (!valueRemarks.matches("")) {
                            viewRemarks.setVisibility(View.VISIBLE);
                            viewRemarks.setText(String.format("\n%s: %s\n", getString(R.string.remarks), valueRemarks));
                        } else {
                            viewRemarks.setVisibility(View.GONE);
                        }
                    } else {
                        viewRemarks.setVisibility(View.GONE);
                    }
                } else {
                    Log.d("GetFinalOverviewQuery", "Error: " + e.getMessage());
                }
            }
        });

        // Get positions (mechanics + material)
        getPositions();
    }
}