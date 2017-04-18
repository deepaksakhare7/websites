package job.com.searchnearbyplaces;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import job.com.searchnearbyplaces.model.Geometry;
import job.com.searchnearbyplaces.model.NearByPlaces;
import job.com.searchnearbyplaces.model.Result;
import job.com.searchnearbyplaces.model.route.Leg;
import job.com.searchnearbyplaces.model.route.Polyline;
import job.com.searchnearbyplaces.model.route.Route;
import job.com.searchnearbyplaces.model.route.RouteData;
import job.com.searchnearbyplaces.model.route.Step;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class DetailActivity extends FragmentActivity implements LocationListener {
    private String mGoogleKey;
    GoogleMap mGoogleMap;
    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    boolean canGetLocation = false;
    Location mLocation; // location
    double mLatitude; // latitude
    double mLongitude; // longitude

    private static final int PERMISSION_REQUEST_CODE = 200;
    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

    // Declaring a Location Manager
    protected LocationManager locationManager;
    private TextView mNameView, mAddressView, mPhoneView, mLatLongView;
    private MyApplication myApplication;
    private Context mContext;
    ArrayList<LatLng> mPoints;
    private Double latitude;
    private Double longitude;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        myApplication = MyApplication.getApp();

        mContext = this;

        Intent intent = getIntent();
        int position = Integer.parseInt(intent.getStringExtra("position"));

        mNameView = (TextView) findViewById(R.id.detail_name);
        mAddressView = (TextView) findViewById(R.id.detail_address);
        mPhoneView = (TextView) findViewById(R.id.detail_phone);
        mPhoneView.setText("NA");
        mLatLongView = (TextView) findViewById(R.id.detail_lat_long);

        Result result = myApplication.resultList.get(position);
        Geometry geometry = result.getGeometry();
        job.com.searchnearbyplaces.model.Location location = geometry.getLocation();

        latitude = location.getLat();
        longitude = location.getLng();

        String name = result.getName();
        mNameView.setText(name);

        String address = result.getFormattedAddress();
        mAddressView.setText(address);

        mLatLongView.setText(latitude + " , " + longitude);

        mGoogleKey = getResources().getString(R.string.places_key);

        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());
        if(status!= ConnectionResult.SUCCESS){ // Google Play Services are not available

            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();
        } else { //Play services are available
            mPoints = new ArrayList<>();
            SupportMapFragment fragment = ( SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.detail_map);

            fragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    mGoogleMap = googleMap;

                    getLocation();

                    LatLng startPoint = new LatLng(mLatitude, mLongitude);
                    LatLng dest = new LatLng(latitude, longitude);
                    drawMarker(startPoint);
                    drawMarker(dest);
                    getDirection(startPoint, dest);

                }
            });
        }

    }


    private void getDirection(LatLng origin,LatLng dest) {
        try {

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(Constant.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            RequestInterface requestInterface = retrofit.create(RequestInterface.class);

            LinkedHashMap<String,String> data=new LinkedHashMap<>();
            data.put("origin", origin.latitude+","+origin.longitude);
            data.put("destination", dest.latitude+","+dest.longitude);
            data.put("sensor", "false");

            Call<RouteData> response = requestInterface.getRoute(data);
            response.enqueue(new Callback<RouteData>() {
                @Override
                public void onResponse(Call<RouteData> call, Response<RouteData> response) {
                    if(response.isSuccessful()){
                        RouteData routeData = response.body();
                        ArrayList<LatLng> points = null;
                        PolylineOptions lineOptions = null;
                        List<Route> result = routeData.getRoutes();

                        for(int i=0;i<result.size();i++){
                            points = new ArrayList<LatLng>();
                            lineOptions = new PolylineOptions();
                            Route route = result.get(i);
                            List<Leg> listLegs = route.getLegs();
                            for(int j=0;j<listLegs.size();j++){
                                Leg leg = listLegs.get(j);
                                List<Step> listSteps = leg.getSteps();
                                for(int k = 0 ; k < listSteps.size() ; k++){
                                    List<LatLng> list = decodePoly(listSteps.get(k).getPolyline().getPoints().toString());
                                    for(int l=0;l<list.size();l++){
                                        LatLng position = new LatLng(list.get(l).latitude, list.get(l).longitude);
                                        points.add(position);
                                    }
                                }
                            }
                            lineOptions.addAll(points);
                            lineOptions.width(5);
                            lineOptions.color(Color.MAGENTA);
                        }

                        mGoogleMap.addPolyline(lineOptions);
                    }
                }

                @Override
                public void onFailure(Call<RouteData> call, Throwable t) {
                    t.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }


    public Location getLocation() {
        try {

            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            //getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            //getting network status
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled ) {
                // no network provider is enabled
                showSettingsAlert();
            } else {
                if (!checkPermission()) {
                    requestPermission();

                }
                mGoogleMap.setMyLocationEnabled(true);

                this.canGetLocation = true;
                // First get location from Network Provider
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d("Network", "Network");
                    if (locationManager != null) {
                        mLocation = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            if (mLocation != null) {
                                mLatitude = mLocation.getLatitude();
                                mLongitude = mLocation.getLongitude();
                            }
                    }


                    if (isGPSEnabled) {
                        if (mLocation == null) {
                            locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    MIN_TIME_BW_UPDATES,
                                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                            Log.d("GPS Enabled", "GPS Enabled");
                            if (locationManager != null) {
                                mLocation = locationManager
                                        .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                if (mLocation != null) {
                                    mLatitude = mLocation.getLatitude();
                                    mLongitude = mLocation.getLongitude();
                                }
                            }
                        }
                    }
                }

                if (mLocation != null) {
                    onLocationChanged(mLocation);
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return mLocation;
    }

    private void drawMarker(LatLng point){
        mPoints.add(point);
        MarkerOptions options = new MarkerOptions();
        options.position(point);
        if(mPoints.size()==1){
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        }else if(mPoints.size()==2){
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        }
        mGoogleMap.addMarker(options);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);

        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));
        // Zoom in, animating the camera.
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomIn());
        // Zoom out to zoom level 10, animating with a duration of 2 seconds.
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if(grantResults.length > 0){
                    Constant.locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (Constant.locationAccepted)
                        Toast.makeText(mContext, "All Permissions Granted",
                                Toast.LENGTH_SHORT).show();
                        //Snackbar.make(view, "Permission Granted, Now you can access location data and camera.", Snackbar.LENGTH_LONG).show();
                    else {

                        Toast.makeText(mContext, "Permission Denied, You cannot access location data",
                                Toast.LENGTH_SHORT).show();
                        //Snackbar.make(view, "Permission Denied, You cannot access location data and camera.", Snackbar.LENGTH_LONG).show();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
                                showMessageOKCancel("You need to allow access to the permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{ACCESS_FINE_LOCATION},
                                                            PERMISSION_REQUEST_CODE);
                                                }
                                            }
                                        });
                                return;
                            }
                        }

                    }

                    break;
                }
        }

    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(mContext)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private boolean checkPermission() {
        int locationPermissionResult = ContextCompat.checkSelfPermission(mContext, ACCESS_FINE_LOCATION);

        return locationPermissionResult == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(DetailActivity.this, new String[]{ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
    }

    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }
}
