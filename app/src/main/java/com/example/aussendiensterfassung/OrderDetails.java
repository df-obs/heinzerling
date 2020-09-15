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
import java.util.Objects;

public class OrderDetails extends AppCompatActivity {

    // List of all orders / elevators that are related to the main order
    protected String elevatorObjectIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        // Define layout
        final LinearLayout layoutElevators = findViewById(R.id.order_details_elevators_layout);

        // Catch OrderID and define OrderID and order ParseObject
        Intent intent = getIntent();
        String orderObjectId = intent.getStringExtra("orderObjectId");

        ParseQuery<ParseObject> innerQuery = ParseQuery.getQuery("Auftrag");
        innerQuery.fromLocalDatastore();
        innerQuery.whereEqualTo("objectId", orderObjectId);
        ParseQuery<ParseObject> elevatorQuery = ParseQuery.getQuery("Einzelauftrag");
        elevatorQuery.fromLocalDatastore();
        elevatorQuery.whereMatchesQuery("Gesamtauftrag", innerQuery);
        elevatorQuery.include("Aufzug");

        ParseQuery<ParseObject> orderQuery = ParseQuery.getQuery("Auftrag");
        orderQuery.fromLocalDatastore();
        orderQuery.whereEqualTo("objectId", orderObjectId);
        orderQuery.include("Kunde");

        // Get all orders that are related to the main order
        elevatorQuery.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    // Clear view
                    layoutElevators.removeAllViewsInLayout();

                    // Create a button for every "single order" / elevator
                    for (int i=0; i<resultList.size(); i++) {
                        ParseObject orderElevator = resultList.get(i);

                        String valueElevatorId        = Objects.requireNonNull(orderElevator.getParseObject("Aufzug")).getString("Nummer");
                        String valueOrderId           = String.valueOf(orderElevator.getInt("Nummer"));
                        String valueOrderType         = orderElevator.getString("Typ");
                        final String elevatorObjectId = orderElevator.getObjectId();

                        boolean orderLocked = orderElevator.getBoolean("Gesperrt");

                        Button buttonElevator = new Button(getApplicationContext());

                        buttonElevator.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
                        buttonElevator.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                        buttonElevator.setText(String.format("%s: %s\n%s: %s\n%s", getString(R.string.elevator_id), valueElevatorId, getString(R.string.order_id), valueOrderId, valueOrderType));

                        if (orderLocked) {
                            buttonElevator.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // Order already locked / signed >> Switch to locked order view
                                    Intent switchToLockedOrders = new Intent(getApplicationContext(), LockedOrders.class);
                                    switchToLockedOrders.putExtra("orderObjectId", elevatorObjectId);
                                    startActivity(switchToLockedOrders);
                                }
                            });
                        } else {
                            buttonElevator.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent switchToShowOrder = new Intent(getApplicationContext(), SingleOrder.class);
                                    switchToShowOrder.putExtra("orderObjectId", elevatorObjectId);
                                    startActivity(switchToShowOrder);
                                }
                            });
                        }

                        if (valueOrderType.equals("STÖRUNG"))
                            buttonElevator.setBackgroundColor(0xFFF44336);
                        else if (i%2 == 0)
                            buttonElevator.setBackgroundColor(0xFF80D8FF);
                        else
                            buttonElevator.setBackgroundColor(0xFF82B1FF);

                        layoutElevators.addView(buttonElevator);

                        // Add elevator to the list
                        if (i==0) {
                            elevatorObjectIds = elevatorObjectId;
                        } else {
                            elevatorObjectIds = elevatorObjectIds.concat(",").concat(elevatorObjectId);
                        }
                    }

                    // Create button to sign all orders
                    Button buttonSign = new Button(getApplicationContext());

                    buttonSign.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                    buttonSign.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                    buttonSign.setText(getString(R.string.sign_all));
                    buttonSign.setBackgroundColor(0xFF9C27B0);
                    buttonSign.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent switchToSignOrder = new Intent(getApplicationContext(), SignOrder.class);
                            switchToSignOrder.putExtra("orderObjectId", elevatorObjectIds);
                            startActivity(switchToSignOrder);
                        }
                    });
                    layoutElevators.addView(buttonSign);
                } else {
                    Log.d("Order Details", "Error: " + e.getMessage());
                }
            }
        });

        // Set headline
        orderQuery.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    TextView textHeadline = findViewById(R.id.order_details_headline);
                    ParseObject order = resultList.get(0);
                    textHeadline.setText(Objects.requireNonNull(order.getParseObject("Kunde")).getString("Name"));
                } else {
                    Log.d("Order Details", "Error: " + e.getMessage());
                }
            }
        });
    }
}