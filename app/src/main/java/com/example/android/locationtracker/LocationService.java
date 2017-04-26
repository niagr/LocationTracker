package com.example.android.locationtracker;

import android.Manifest;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

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
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by hi-258 on 27/4/17.
 */

public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mRequestingLocationUpdates;

    private static int UPDATE_INTERVAL = 7000;
    private static int FASTEST_INTERVAL = 5000;

    public LocationService () {
        super();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onHandleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    protected void onHandleIntent(Intent intent) {
        Log.d("nish", "hello from LocationService");
        bringToForeground();
        String operation = intent.getExtras().getString(Constants.OPERATION);
        if (operation.equals(Constants.startLocationUpdates)) {
            if (checkPlayServices()) {
                if (mGoogleApiClient == null)
                    buildGoogleApiClient();
                if(mGoogleApiClient != null){
                    mGoogleApiClient.connect();
                    return;
                } else {
                    Log.d("nish", "Google API Client was null.");
                }
            } else {
                Log.d("nish", "Google Play Services is unavailable. Exiting Service");
            }
        } else if (operation.equals(Constants.stopLocationUpdates)) {
            Log.d("nish", "Stopping activity");
            stopSelf();
        } else {
            Log.d("nish", "Could not recognise operation, exiting service");
        }

        stopSelf();

    }

    void bringToForeground () {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Constants.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Location Tracker")
                .setTicker("Location Tracker")
                .setContentText("Location updates are being sent")
                .setSmallIcon(R.drawable.marker)
                .setContentIntent(pendingIntent)
                .setOngoing(true).build();
        startForeground(Constants.FOREGROUND_SERVICE, notification);
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

    private void getLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.d("nish", "Shit");
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();
            Log.d("nish", "location: " + latitude + "," + longitude);
        } else {
            Log.d("nish", "Couldn't get location. Make sure location is enabled.");
        }
    }

    protected boolean checkPlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                // TODO: FIX THIS!!!
//                googleApiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
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
        createLocationRequest();
        startLocationUpdates();
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

    @Override
    public void onDestroy() {
        Log.d("nish", "Destorying locationService");
        stopLocationUpdates();
        super.onDestroy();
    }

    public void onLocationChanged (Location location) {
        double newLatitude = location.getLatitude();
        double newLongitude = location.getLongitude();
        Log.d("nish", newLatitude + " " + newLongitude);
        // TODO: FIX THIS!!!!
//        lblLocation.setText(newLatitude+" "+newLongitude);
        this.makeRequest(newLatitude, newLongitude);
    }

}
