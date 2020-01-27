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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        // Catch OrderID and define OrderID and order ParseObject
        Intent intent = getIntent();
        String orderObjectId = intent.getStringExtra("orderObjectId");

        ParseQuery<ParseObject> innerQuery = ParseQuery.getQuery("Auftrag");
        innerQuery.whereEqualTo("objectId", orderObjectId);
        ParseQuery<ParseObject> elevatorQuery = ParseQuery.getQuery("Einzelauftrag");
        elevatorQuery.whereMatchesQuery("Gesamtauftrag", innerQuery);
        elevatorQuery.include("Aufzug");

        ParseQuery<ParseObject> orderQuery = ParseQuery.getQuery("Auftrag");
        orderQuery.whereEqualTo("objectId", orderObjectId);
        orderQuery.include("Kunde");

        elevatorQuery.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    LinearLayout layoutElevators = findViewById(R.id.order_details_elevators_layout);
                    layoutElevators.removeAllViewsInLayout();

                    for (int i=0; i<resultList.size(); i++) {
                        ParseObject orderElevator = resultList.get(i);

                        String valueElevatorId        = String.valueOf(Objects.requireNonNull(orderElevator.getParseObject("Aufzug")).getInt("Nummer"));
                        String valueOrderId           = String.valueOf(orderElevator.getInt("Nummer"));
                        final String elevatorObjectId = orderElevator.getObjectId();

                        Button buttonElevator = new Button(getApplicationContext());

                        buttonElevator.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
                        buttonElevator.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                        buttonElevator.setText(String.format("%s: %s\n%s: %s", getString(R.string.elevator_id), valueElevatorId, getString(R.string.order_id), valueOrderId));

                        buttonElevator.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent switchToShowOrder = new Intent(getApplicationContext(), Auftragsanzeige.class);
                                switchToShowOrder.putExtra("orderObjectId", elevatorObjectId);
                                startActivity(switchToShowOrder);
                            }
                        });

                        if (i%2 == 0) {
                            buttonElevator.setBackgroundColor(0xFF80D8FF);
                        } else
                            buttonElevator.setBackgroundColor(0xFF82B1FF);

                        layoutElevators.addView(buttonElevator);
                    }
                } else {
                    Log.d("Order Details", "Error: " + e.getMessage());
                }
            }
        });

        orderQuery.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> resultList, ParseException e) {
                if (e == null) {
                    TextView textHeadline = findViewById(R.id.order_details_headline);
                    ParseObject order = resultList.get(0);
                    textHeadline.setText(String.format("%s\n%s", order.get("Typ"), Objects.requireNonNull(order.getParseObject("Kunde")).getString("Name")));
                } else {
                    Log.d("Order Details", "Error: " + e.getMessage());
                }
            }
        });
    }
}
