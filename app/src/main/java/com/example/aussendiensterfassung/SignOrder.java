package com.example.aussendiensterfassung;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.print.PdfConverter;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class SignOrder extends AppCompatActivity {

    protected String[] orderObjectIds;
    protected List<Integer> orderIds;

    protected Bitmap bitmapSignatureEmployee;
    protected Bitmap bitmapSignatureCustomer;

    protected boolean booleanSaveTimestamp;
    protected boolean booleanSendCustomerMail;
    protected String inputMailAdress;

    protected CaptureSignatureView viewSignatureEmployee;
    protected CaptureSignatureView viewSignatureCustomer;
    protected FloatingActionButton fabSave;
    protected LinearLayout layoutOptions;
    protected TextView viewHeadline;

    protected String stringSignatureEmployee;
    protected String stringSignatureCustomer;

    protected int step;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_order);
        step = 0;

        // Catch OrderIDs (String separated by ",") and split to an array
        Intent intent = getIntent();
        String orderObjectId = intent.getStringExtra("orderObjectId");
        orderObjectIds = orderObjectId.split(Pattern.quote( "," ));

        // Define headline
        viewHeadline = findViewById(R.id.sign_order_headline);

        // Define layout for employee signature
        final LinearLayout layoutSignatureEmployee = findViewById(R.id.sign_order_layout_employee);
        viewSignatureEmployee = new CaptureSignatureView(this, null);
        layoutSignatureEmployee.addView(viewSignatureEmployee, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

        // Define layout for customer signature
        final LinearLayout layoutSignatureCustomer = findViewById(R.id.sign_order_layout_customer);
        viewSignatureCustomer = new CaptureSignatureView(this, null);
        layoutSignatureCustomer.addView(viewSignatureCustomer, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

        // Define layout for options
        layoutOptions = findViewById(R.id.sign_order_layout_options);
        final CheckBox fieldTimestamp = findViewById(R.id.sign_order_input_timestamp);
        final CheckBox fieldCustomerMail = findViewById(R.id.sign_order_input_mail);
        final EditText fieldMailAdress  = findViewById(R.id.sign_order_input_mailadress);

        // Initialize button for saving
        fabSave = findViewById(R.id.sign_order_fab_save);
        fabSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (step) {
                    case 0: // save employee signature and move to customer signature
                        step = 1;
                        bitmapSignatureEmployee = viewSignatureEmployee.getBitmap();
                        viewHeadline.setText(getString(R.string.signature_customer));
                        layoutSignatureEmployee.setVisibility(View.GONE);
                        layoutSignatureCustomer.setVisibility(View.VISIBLE);
                        break;
                    case 1: // save customer signature and move to options
                        step = 2;
                        bitmapSignatureCustomer = viewSignatureCustomer.getBitmap();
                        viewHeadline.setText(getString(R.string.options));
                        layoutSignatureCustomer.setVisibility(View.GONE);
                        layoutOptions.setVisibility(View.VISIBLE);
                        break;
                    case 2: // save and lock order
                        booleanSaveTimestamp = fieldTimestamp.isChecked();
                        booleanSendCustomerMail = fieldCustomerMail.isChecked();
                        inputMailAdress = fieldMailAdress.getText().toString();
                        saveSignatures();
                        break;
                }
            }
        });

        // Get order IDs
        orderIds = new ArrayList<>();
        ParseQuery<ParseObject> queryOrders = ParseQuery.getQuery("Einzelauftrag");
        queryOrders.fromLocalDatastore();
        queryOrders.whereContainedIn("objectId", Arrays.asList(orderObjectIds));
        queryOrders.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    for (int i = 0; i < resultList.size(); i++) {
                        ParseObject orderObject = resultList.get(i);
                        int orderId = orderObject.getInt("Nummer");
                        orderIds.add(orderId);
                    }
                } else {
                    Log.d("SignOrder", "Error: " + e.getMessage());
                }
            }
        });
    }

    // Save Signatures to DB
    protected void saveSignatures() {
        // Encode signatures
        stringSignatureEmployee = encodeImage(bitmapSignatureEmployee);
        stringSignatureCustomer = encodeImage(bitmapSignatureCustomer);

        // Update the DB lines with signatures and lock the orders
        ParseQuery<ParseObject> querySignature = ParseQuery.getQuery("Einzelauftrag");
        querySignature.fromLocalDatastore();
        querySignature.whereContainedIn("objectId", Arrays.asList(orderObjectIds));
        querySignature.whereNotEqualTo("Gesperrt", true);
        querySignature.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    for (int i = 0; i < resultList.size(); i++) {
                        ParseObject orderObject = resultList.get(i);
                        orderObject.put("Unterschrift_Monteur", stringSignatureEmployee);
                        orderObject.put("Unterschrift_Kunde", stringSignatureCustomer);
                        orderObject.put("Zeitstempel", booleanSaveTimestamp);
                        orderObject.put("Gesperrt", true);
                        orderObject.saveEventually();
                    }
                } else {
                    Log.d("SaveSignaturesQuery", "Error: " + e.getMessage());
                }
            }
        });

        // Create attachments and send mail
        createAttachments();
    }

    // Send a mail to office and customer with pdf-exports of the order
    protected void sendMail() {
        // Define mail recipients
        String officeMail    = getString(R.string.office_mail);
        String customerMail  = inputMailAdress;

        // Define order string
        StringBuilder orders = new StringBuilder();

        for (int i=0; i<orderIds.size(); i++) {
            if (i != 0) {
                orders.append(", ");
            }
            orders.append(orderIds.get(i));
        }

        // Define mail subject
        String subject;
        if (orderIds.size() == 1) {
            subject = String.format("%s %s %s", getString(R.string.order_confirmation), getString(R.string.for_order), orders);
        } else {
            subject = String.format("%s %s %s", getString(R.string.order_confirmations), getString(R.string.for_orders), orders);
        }

        // Define mail body
        String body = String.format("%s\n\n%s %s.\n\n%s\n\n%s", getString(R.string.mail_salutation), getString(R.string.mail_confirmation_body), subject, getString(R.string.mail_ending), getString(R.string.company_name));

        // Create mail intent
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("text/xml");

        // Add data to intent
        if (booleanSendCustomerMail) {
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{customerMail});
            emailIntent.putExtra(android.content.Intent.EXTRA_CC, new String[]{officeMail});
        } else {
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{officeMail});
        }
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);

        // Add attachments
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ArrayList<Uri> uris = new ArrayList<>();

        for (int i = 0; i < orderIds.size(); i++) {
            File folder = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "ABs");
            File attachment = new File(folder, getString(R.string.order_confirmation_short) + "_" + orderIds.get(i) + ".pdf");
            uris.add(FileProvider.getUriForFile(this,BuildConfig.APPLICATION_ID + ".provider", attachment));
        }

        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

        // Change view so the user sees that everything is finished
        viewHeadline.setText(getString(R.string.saved));
        ProgressBar progressBar = findViewById(R.id.sign_order_progress_bar);
        TextView textFinshed = findViewById(R.id.sign_order_text_finished);
        Button buttonIntent = findViewById(R.id.sign_order_button_intent);

        progressBar.setVisibility(View.GONE);
        textFinshed.setVisibility(View.VISIBLE);
        buttonIntent.setVisibility(View.VISIBLE);

        // Start intent
        this.startActivity(emailIntent);
    }

    // When everything is finished, switch to locked orders activity
    public void switchToLockedOrders(View v) {
        Intent switchToLockedOrders = new Intent(getApplicationContext(), LockedOrders.class);
        if (orderObjectIds.length==1) {
            switchToLockedOrders.putExtra("orderObjectId", orderObjectIds[0]);
        }
        startActivity(switchToLockedOrders);
    }

    // Create attachments and send mail when finished
    public void createAttachments() {
        // Show a progress bar because pdf creation will take a while
        LinearLayout layoutWaiting = findViewById(R.id.sign_order_layout_waiting);
        layoutOptions.setVisibility(View.GONE);
        fabSave.hide();
        layoutWaiting.setVisibility(View.VISIBLE);
        viewHeadline.setText(getString(R.string.creating_pdf));

        // Create PDFs
        PdfConverter converter = PdfConverter.getInstance();
        converter.setListener(new PdfConverter.Listener() {
            @Override
            public void onFinishing() {
                sendMail();
            }
        });

        ArrayList<File> files = new ArrayList<>();
        ArrayList<String> htmlStrings = new ArrayList<>();

        File folder = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "ABs");

        if (!folder.exists()) {
            folder.mkdir();
        }

        for (int i=0; i<orderIds.size(); i++) {
            File file = new File(folder, getString(R.string.order_confirmation_short) + "_" + orderIds.get(i) + ".pdf");
            String htmlString = orderAsHtml(orderObjectIds[i]);

            files.add(file);
            htmlStrings.add(htmlString);
        }

        converter.convertMultiple(getApplicationContext(), htmlStrings, files);
    }

    protected String orderAsHtml(String orderObjectId) {
        ////////////////////////////////////////////////////////////////////////////////////////
        // Get order data                                                                     //
        ////////////////////////////////////////////////////////////////////////////////////////

        ParseObject finalOrder = null;
        int valueOrderId = 0;
        Date valueDate;
        String valueStrDate = "";
        String valueOrderType = "";
        String valueCustomer = "";
        int valueCustomerId = 0;
        String valueCustomerStreet = "";
        String valueCustomerCity = "";
        String valueElevatorId = "";
        String valueElevatorStreet = "";
        String valueElevatorCity = "";
        String valueKeySafe = "";
        String valueWork = "";
        String valueRemarks = "";
        Date valueUpdated;
        boolean valueWorkDone = true;
        String valueStrUpdated = "";

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Einzelauftrag");
        query.fromLocalDatastore();
        query.whereEqualTo("objectId", orderObjectId);
        query.include("Aufzug");
        query.include("Aufzug.Kunde");
        query.include("Gesamtauftrag");
        try {
            List<ParseObject> resultList = query.find();
            finalOrder = resultList.get(0);
            DateFormat dateFormat = DateFormat.getDateInstance();
            DateFormat timeFormat = DateFormat.getDateTimeInstance();

            // Get and parse database contents
            valueOrderId = finalOrder.getInt("Nummer");
            valueDate = Objects.requireNonNull(finalOrder.getParseObject("Gesamtauftrag")).getDate("Datum");
            valueOrderType = Objects.requireNonNull(finalOrder.getParseObject("Gesamtauftrag")).getString("Typ");
            valueStrDate = dateFormat.format(valueDate);
            valueCustomer = Objects.requireNonNull(Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getParseObject("Kunde")).getString("Name");
            valueCustomerId = Objects.requireNonNull(Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getParseObject("Kunde")).getInt("Kundennummer");
            valueCustomerStreet = Objects.requireNonNull(Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getParseObject("Kunde")).getString("Strasse");
            valueCustomerCity = Objects.requireNonNull(Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getParseObject("Kunde")).getInt("PLZ") + " " + Objects.requireNonNull(Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getParseObject("Kunde")).getString("Ort");
            valueElevatorId = Integer.toString(Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getInt("Nummer"));
            valueElevatorStreet = Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getString("Strasse");
            valueElevatorCity = Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getInt("PLZ") + " " + Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getString("Ort");
            valueKeySafe = Objects.requireNonNull(finalOrder.getParseObject("Aufzug")).getString("Schluesseldepot");
            valueWork = finalOrder.getString("Arbeiten");
            valueRemarks = finalOrder.getString("Bemerkungen");
            valueUpdated = new Date();
            valueWorkDone = finalOrder.getBoolean("Abgeschlossen");
            valueStrUpdated = timeFormat.format(valueUpdated);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        ////////////////////////////////////////////////////////////////////////////////////////
        // Get positions                                                                      //
        ////////////////////////////////////////////////////////////////////////////////////////

        ArrayList<String> materialQuantity = getPositions(finalOrder, "Anzahl");
        ArrayList<String> materialUnit     = getPositions(finalOrder, "Einheit");
        ArrayList<String> materialName     = getPositions(finalOrder, "Name");

        ArrayList<String> mechanicQuantity = getMechanics(finalOrder, "Anzahl");
        ArrayList<String> mechanicCategory = getMechanics(finalOrder, "Kategorie");
        ArrayList<String> mechanicName     = getMechanics(finalOrder, "Name");

        ////////////////////////////////////////////////////////////////////////////////////////
        // Build HTML                                                                         //
        ////////////////////////////////////////////////////////////////////////////////////////

        StringBuilder html = new StringBuilder();

        // Add header
        html.append("<table cellpadding=\"5\" cellspacing=\"0\" style=\"width: 100%;\">");
        html.append(String.format("<tr><td></td><td style=\"text-align: right\"><img src=\"data:image/png;base64, %s\"><br><br></td></tr>", getString(R.string.company_logo)));
        html.append(String.format("<tr><td><u><span style=\"font-size: x-small\">%s - %s - %s</span></u><br><br>%s<br>%s<br>%s<br></td><td style=\"text-align: right\"><b>%s %s<br>%s: %s</b><br><br>%s: %s<br>%s: %s</td></tr>", getString(R.string.company_name), getString(R.string.company_adress), getString(R.string.company_city), valueCustomer, valueCustomerStreet, valueCustomerCity, getString(R.string.order_number), valueOrderId, getString(R.string.type), valueOrderType, getString(R.string.customer_id), valueCustomerId, getString(R.string.date), valueStrDate));
        html.append(String.format("<tr><td><br><br>%s: %s<br><br>%s:<br>%s<br>%s</td><td style=\"text-align: right\"><br><br><br>%s:<br>%s</td></tr>", getString(R.string.elevator_id), valueElevatorId, getString(R.string.location), valueElevatorStreet, valueElevatorCity, getString(R.string.key_safe), valueKeySafe));
        html.append("</table>");

        // Add product list
        if (materialQuantity.size() > 0) {
            html.append("<br>");
            html.append("<table cellpadding=\"5\" cellspacing=\"0\" style=\"width: 100%;\" border=\"0\">");
            html.append(String.format("<tr style=\"background-color: #cccccc; padding:5px;\"><td style=\"padding:5px; width:%s;\"><b>%s</b></td><td style=\"width:%s;\"><b>%s</b></td><td style=\"width:%s;\"><b>%s</b></td><td style=\"width:%s;\"><b>%s</b></td></tr>", "7%", getString(R.string.position_short), "10%", getString(R.string.quantity), "10%", getString(R.string.unit), "73%", getString(R.string.description)));

            for (int i = 0; i < materialQuantity.size(); i++) {
                String col0 = String.valueOf(i+1);
                String col1 = materialQuantity.get(i);
                String col2 = materialUnit.get(i);
                String col3 = materialName.get(i);

                html.append(String.format("<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>", col0, col1, col2, col3));
            }
            html.append("</table>");
        }

        // Add mechanics list
        if (mechanicQuantity.size() > 0) {
            html.append("<br>");
            html.append("<table cellpadding=\"5\" cellspacing=\"0\" style=\"width: 100%;\" border=\"0\">");
            html.append(String.format("<tr style=\"background-color: #cccccc; padding:5px;\"><td style=\"padding:5px; width:%s;\"><b>%s</b></td><td style=\"width:%s;\"><b>%s</b></td><td style=\"width:%s;\"><b>%s</b></td><td style=\"width:%s;\"><b>%s</b></td></tr>", "7%", getString(R.string.position_short), "15%", getString(R.string.duration), "63%", getString(R.string.mechanic), "15%", getString(R.string.category_short)));

            for (int i = 0; i < mechanicQuantity.size(); i++) {
                String col0 = String.valueOf(i+1);
                String col1 = mechanicQuantity.get(i);
                String col2 = mechanicName.get(i);
                String col3 = mechanicCategory.get(i);

                html.append(String.format("<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>", col0, col1, col2, col3));
            }
            html.append("</table>");
        }

        // Add performed work
        if (valueWork != null) {
            if (!valueWork.equals("")) {
                html.append(String.format("<br><b>%s:</b><br>", getString(R.string.performed_work)));
                html.append(valueWork.replaceAll("\n", "<br>"));
            }
        }

        // Add remarks
        if (valueRemarks != null) {
            if (!valueRemarks.equals("")) {
                html.append(String.format("<br><br><b>%s:</b><br>", getString(R.string.remarks)));
                html.append(valueRemarks.replaceAll("\n", "<br>"));
            }
        }

        // Work done?
        html.append("<br><br>");

        if (valueWorkDone) {
            html.append(String.format("%s <b>%s</b>.", getString(R.string.work), getString(R.string.done)));
        } else {
            html.append(String.format("%s <b>%s</b>.", getString(R.string.work), getString(R.string.not_done)));
        }

        // Signatures
        html.append("<br><br><table cellpadding=\"5\" cellspacing=\"0\" style=\"width: 100%;\">");
        html.append(String.format("<tr><td><img src=\"data:image/png;base64, %s\" width=\"200\" height=\"80\"></td><td><img src=\"data:image/png;base64, %s\" width=\"200\" height=\"80\"></td></tr>", stringSignatureEmployee, stringSignatureCustomer));
        html.append(String.format("<tr><td>%s</td><td>%s</td></tr>", getString(R.string.signature_employee), getString(R.string.signature_customer)));

        if (booleanSaveTimestamp) {
            html.append(String.format("<tr><td>%s %s</td><td></td></tr>", getString(R.string.signed_at), valueStrUpdated));
        }

        html.append("</table>");

        return html.toString();
    }

    // Get article details by column
    protected ArrayList<String> getPositions(ParseObject order, String column) {
        ArrayList<String> articleList = new ArrayList<>();

        ParseQuery<ParseObject> queryMaterial = ParseQuery.getQuery("ArtikelAuftrag");
        queryMaterial.fromLocalDatastore();
        queryMaterial.whereEqualTo("Auftrag", order);
        queryMaterial.include("Artikel");
        try {
            List<ParseObject> resultList = queryMaterial.find();

            // Parse and print the single positions
            for (int i = 0; i < resultList.size(); i++) {
                ParseObject materialPosition = resultList.get(i);

                // Get field contents out of the database
                switch (column) {
                    case "Anzahl": // Quantity
                        double quantity = materialPosition.getDouble("Anzahl");
                        DecimalFormat f = new DecimalFormat("0.00");
                        String strQuantity = f.format(quantity);
                        articleList.add(strQuantity);
                        break;

                    case "Einheit":
                        String unit = Objects.requireNonNull(materialPosition.getParseObject("Artikel")).getString("Einheit");
                        articleList.add(unit);
                        break;

                    case "Name":
                        String name = Objects.requireNonNull(materialPosition.getParseObject("Artikel")).getString("Name");
                        articleList.add(name);
                        break;
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return articleList;
    }

    // Get mechanic details by column
    protected ArrayList<String> getMechanics(ParseObject order, String column) {
        ArrayList<String> mechanicsList = new ArrayList<>();

        // Get mechanics
        ParseQuery<ParseObject> queryMechanics = ParseQuery.getQuery("MonteurAuftrag");
        queryMechanics.fromLocalDatastore();
        queryMechanics.whereEqualTo("Auftrag", order);
        queryMechanics.include("Monteur");
        try {
            List<ParseObject> resultList = queryMechanics.find();

            // Parse and print the single positions
            for (int i=0; i<resultList.size(); i++) {
                ParseObject mechanicPosition = resultList.get(i);
                DecimalFormat f = new DecimalFormat("0.00");

                switch(column) {
                    case "Anzahl":
                        double quantity = mechanicPosition.getDouble("Stunden");
                        String strQuantity = String.format ("%s %s", f.format(quantity), getString(R.string.hour_short));
                        mechanicsList.add(strQuantity);
                        break;

                    case "Kategorie":
                        double category = mechanicPosition.getDouble("Kategorie");
                        String strCategory = f.format(category);
                        mechanicsList.add(strCategory);
                        break;

                    case "Name":
                        String name = Objects.requireNonNull(mechanicPosition.getParseObject("Monteur")).getString("Name");
                        mechanicsList.add(name);
                        break;
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return mechanicsList;
    }

    // Resizes and Base64-encodes an image
    protected String encodeImage(Bitmap img) {
        // Convert ByteArray to Bitmap and resize
        Bitmap bitmap =  Bitmap.createScaledBitmap(img, 800, 320, false);

        // Convert Bitmap to ByteArray and compress it
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 95, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        // Return Base64-encoded String
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
}