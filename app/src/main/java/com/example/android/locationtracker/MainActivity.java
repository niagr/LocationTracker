package com.example.android.locationtracker;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.app.Activity;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mRequestingLocationUpdates;

    private static int UPDATE_INTERVAL = 7000;
    private static int FASTEST_INTERVAL = 5000;
    private TextView lblTripId, lblTripIdOld;
    private EditText txtRouteId;
    private Button btnShowLocation, btnStartLocationUpdates, btnStopLocationUpdates;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String tripId = intent.getExtras().getString(Constants.TRIP_ID_KEY);
            boolean old = intent.getExtras().getBoolean(Constants.TRIP_ID_OLD);
            Toast.makeText(MainActivity.this, "TRIP ID: " + tripId, Toast.LENGTH_LONG).show();
            lblTripId.setText(tripId);
            lblTripIdOld.setText(old ? "Recovered from previous session" : "New trip ID");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lblTripId = (TextView) findViewById(R.id.lblTripId);
        lblTripIdOld = (TextView) findViewById(R.id.lblTripIdOld);
        btnStartLocationUpdates = (Button) findViewById(R.id.btnLocationUpdates);
        btnStopLocationUpdates = (Button) findViewById(R.id.btnStopLocationUpdates);
        txtRouteId = (EditText)findViewById(R.id.txtRouteId);


        btnStartLocationUpdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String routeId = txtRouteId.getText().toString();
                if (routeId.length() == 0) {
                    Toast.makeText(MainActivity.this, "Please enter a route ID first.", Toast.LENGTH_LONG).show();
                    return;
                }
                Intent locationServiceIntent = new Intent(MainActivity.this, LocationService.class);
                locationServiceIntent.putExtra(Constants.OPERATION, Constants.startLocationUpdates);
                locationServiceIntent.putExtra(Constants.ROUTE_ID_KEY, routeId);
                startService(locationServiceIntent);
            }
        });

        btnStopLocationUpdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent locationServiceIntent = new Intent(MainActivity.this, LocationService.class);
                stopService(locationServiceIntent);
            }
        });

        registerServiceBroadcastReceiver();

    }

    void registerServiceBroadcastReceiver () {
        Log.d("nish", "Registering receiver");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.SERVICE_BROADCAST_ACTION);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }


    void makeRequest (final double latitude, final double longitude) {

        SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss yyyy-MM-dd");
        final String dateStr = df.format(new Date());

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest strReq = new StringRequest(
                Request.Method.POST,
                "http://8bf548ae.ngrok.io/receive/",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("nish", response);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("nish", error.toString());
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("latitude", Double.toString(latitude));
                params.put("longitude", Double.toString(longitude));
                params.put("time", dateStr);
                return params;
            }
        };
        queue.add(strReq);
    }

}
