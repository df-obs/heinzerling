package com.example.aussendiensterfassung;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class SignOrder extends AppCompatActivity {

    protected String[] orderObjectIds;

    protected byte[] byteSignatureEmployee;
    protected byte[] byteSignatureCustomer;

    protected CaptureSignatureView viewSignatureEmployee;
    protected CaptureSignatureView viewSignatureCustomer;

    protected boolean employeeSigSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_order);
        employeeSigSet = false;

        // Catch OrderIDs (String separated by ",") and split to an array
        Intent intent = getIntent();
        String orderObjectId = intent.getStringExtra("orderObjectId");
        orderObjectIds = orderObjectId.split(Pattern.quote( "," ));

        // Define headline
        final TextView viewHeadline = findViewById(R.id.sign_order_headline);

        // Define layout for employee signature
        final LinearLayout layoutSignatureEmployee = findViewById(R.id.sign_order_layout_employee);
        viewSignatureEmployee = new CaptureSignatureView(this, null);
        layoutSignatureEmployee.addView(viewSignatureEmployee, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

        // Define layout for customer signature
        final LinearLayout layoutSignatureCustomer = findViewById(R.id.sign_order_layout_customer);
        viewSignatureCustomer = new CaptureSignatureView(this, null);
        layoutSignatureCustomer.addView(viewSignatureCustomer, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

        // Initialize button for employee signature
        FloatingActionButton fabSave = findViewById(R.id.sign_order_fab_save);
        fabSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!employeeSigSet) {
                    employeeSigSet = true;
                    byteSignatureEmployee = viewSignatureEmployee.getBytes();
                    viewHeadline.setText(getString(R.string.signature_customer));
                    layoutSignatureEmployee.setVisibility(View.GONE);
                    layoutSignatureCustomer.setVisibility(View.VISIBLE);
                } else {
                    byteSignatureCustomer = viewSignatureCustomer.getBytes();
                    saveSignatures();
                }
            }
        });
    }

    // Save Signatures to DB
    protected void saveSignatures() {
        // Save signatures to ParseFiles
        final ParseFile fileSignatureEmployee = new ParseFile("signature_employee.bmp", byteSignatureEmployee);
        final ParseFile fileSignatureCustomer = new ParseFile("signature_customer.bmp", byteSignatureCustomer);
        fileSignatureEmployee.saveInBackground();
        fileSignatureCustomer.saveInBackground();

        // Update the DB lines with signatures and lock the orders
        ParseQuery querySignature = ParseQuery.getQuery("Einzelauftrag");
        querySignature.whereContainedIn("objectId", Arrays.asList(orderObjectIds));
        querySignature.whereNotEqualTo("Gesperrt", true);
        querySignature.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    for (int i = 0; i < resultList.size(); i++) {
                        ParseObject orderObject = resultList.get(i);
                        orderObject.put("Unterschrift_Monteur", fileSignatureEmployee);
                        orderObject.put("Unterschrift_Kunde", fileSignatureCustomer);
                        orderObject.put("Gesperrt", true);
                        orderObject.saveEventually();
                    }
                } else {
                    Log.d("SaveSignaturesQuery", "Error: " + e.getMessage());
                }
            }
        });

        // Switch to locked order view
        Intent switchToLockedOrders = new Intent(getApplicationContext(), LockedOrders.class);
        if (orderObjectIds.length==1) {
            switchToLockedOrders.putExtra("orderObjectId", orderObjectIds[0]);
        }
        startActivity(switchToLockedOrders);
    }
}