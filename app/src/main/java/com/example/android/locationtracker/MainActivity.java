package com.example.android.locationtracker;

import android.Manifest;
import android.content.Intent;
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
import android.widget.TextView;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends Activity implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mRequestingLocationUpdates;

    private static int UPDATE_INTERVAL = 7000;
    private static int FASTEST_INTERVAL = 5000;
    private TextView lblLocation;
    private Button btnShowLocation, btnStartLocationUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lblLocation = (TextView) findViewById(R.id.lblLocation);
        btnShowLocation = (Button) findViewById(R.id.btnShowLocation);
        btnStartLocationUpdates = (Button) findViewById(R.id.btnLocationUpdates);
        if (checkPlayServices()) {
            buildGoogleApiClient();
        }
        createLocationRequest();

        btnShowLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayLocation();
            }
        });

        btnStartLocationUpdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLocationUpdates();
            }
        });

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

    protected boolean checkPlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }

            return false;
        }
        return true;
    }

    protected synchronized void buildGoogleApiClient() {
        //accessing Google APIs
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                                              .addConnectionCallbacks(this)
                                              .addOnConnectionFailedListener(this)
                                              .addApi(LocationServices.API)
                                              .build();
    }

    protected void createLocationRequest() {
        mRequestingLocationUpdates = new LocationRequest();
        mRequestingLocationUpdates.setInterval(UPDATE_INTERVAL);
        mRequestingLocationUpdates.setFastestInterval(FASTEST_INTERVAL);
        mRequestingLocationUpdates.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

    }

    private void displayLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();
            lblLocation.setText(latitude + "," + longitude);
        } else {
            lblLocation.setText("Couldn't get location. Make sure location is enabled.");
        }
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if(mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mRequestingLocationUpdates, this);
        }
    }

    protected void stopLocationUpdates () {
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }


    public void onLocationChanged (Location location) {
        double newLatitude = location.getLatitude();
        double newLongitude = location.getLongitude();
        Log.d("DONNA", newLatitude + " " + newLongitude);
        lblLocation.setText(newLatitude+" "+newLongitude);
        this.makeRequest(newLatitude, newLongitude);
    }

    @Override
    protected void onStart(){
        super.onStart();
        if(mGoogleApiClient != null){
            mGoogleApiClient.connect();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mGoogleApiClient != null)
            mGoogleApiClient.disconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected (Bundle arg0) {
        checkPlayServices();
    }


}
