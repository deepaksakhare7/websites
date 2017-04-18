package job.com.searchnearbyplaces;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.IdRes;
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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import job.com.searchnearbyplaces.model.Geometry;
import job.com.searchnearbyplaces.model.NearByPlaces;
import job.com.searchnearbyplaces.model.Result;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class SearchActivity extends FragmentActivity implements LocationListener, View.OnClickListener {

    private String mGoogleKey;
    GoogleMap mGoogleMap;
    double latitude = 0;
    double longitude = 0;
    private int PROXIMITY_RADIUS = 5000;
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
    private Context mContext;
    private RadioGroup mRadioLocation;
    private RadioButton mRadioBtnFav, mradioBtnCurrent;
    private EditText mLocationEdit;
    private Button mSearchButton, mListButton;
    private MyApplication myApplication;
    private Spinner mSpinnerPlace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mGoogleKey = getResources().getString(R.string.places_key);

        myApplication = MyApplication.getApp();

        mContext = SearchActivity.this;

        mRadioLocation = (RadioGroup) findViewById(R.id.radioLocation);
        mRadioLocation.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                if(i == R.id.radioCurrent){
                    mLocationEdit.setText("");
                }
            }
        });

        mLocationEdit = (EditText) findViewById(R.id.location_text);

        mSearchButton = (Button) findViewById(R.id.search_btn);
        mSearchButton.setOnClickListener(this);

        mListButton = (Button) findViewById(R.id.list_btn);
        mListButton.setOnClickListener(this);

        mSpinnerPlace = (Spinner)findViewById(R.id.spinner_place);
        List<String> categories = new ArrayList<String>();
        categories.add("Restaurants");
        categories.add("Entertainment");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        mSpinnerPlace.setAdapter(dataAdapter);


        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());
        if(status!= ConnectionResult.SUCCESS){ // Google Play Services are not available

            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();
        } else { //Play services are available
            SupportMapFragment fragment = ( SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

            fragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    mGoogleMap = googleMap;

                    getLocation();

                }
            });
        }

    }



    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id){
            case R.id.search_btn:
                attemptSearch();
                break;
            case R.id.list_btn:
                Intent intent = new Intent(mContext, ListActivity.class);
                startActivity(intent);
                break;
        }

    }

    private void attemptSearch() {
        try {

            int selectedId = mRadioLocation.getCheckedRadioButtonId();
            if(selectedId == R.id.radiofavourite){
                mLocationEdit.setError(null);
                String location_value = mLocationEdit.getText().toString().trim();

                boolean cancel = false;
                View focusView = null;

                if (TextUtils.isEmpty(location_value)) {
                    mLocationEdit.setError(getString(R.string.error_field_required));
                    focusView = mLocationEdit;
                    cancel = true;
                }

                if (cancel) {
                    // There was an error; don't attempt login and focus the first
                    // form field with an error.
                    focusView.requestFocus();
                } else {
                    // Show a progress spinner, and kick off a background task to
                    // perform the user login attempt.
                    getNearByPlaces(location_value);
                }
            } else {
                getNearByPlaces("");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getNearByPlaces(String search_txt) {
        try {

            final ProgressDialog progressDialog;
            progressDialog = new ProgressDialog(mContext);
            progressDialog.setMessage("loading...");
            progressDialog.show();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(Constant.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            RequestInterface requestInterface = retrofit.create(RequestInterface.class);

            LinkedHashMap<String,String> data=new LinkedHashMap<>();

            String place = mSpinnerPlace.getSelectedItem().toString().toLowerCase().trim();
            if(TextUtils.isEmpty(search_txt)){
                data.put("query", place);
                data.put("location",mLatitude+","+mLongitude);
            } else {
                data.put("query",place+"in"+search_txt);
            }

            data.put("key", mGoogleKey);


            Call<NearByPlaces> response = requestInterface.getNearByPlaces(data);
            response.enqueue(new Callback<NearByPlaces>() {
                @Override
                public void onResponse(Call<NearByPlaces> call, Response<NearByPlaces> response) {
                    progressDialog.dismiss();
                    NearByPlaces nearByPlaces = response.body();
                    if(nearByPlaces.getStatus().equalsIgnoreCase(Constant.STATUS_OK)){
                        myApplication.resultList = nearByPlaces.getResults();
                        mGoogleMap.clear();
                        for(int i = 0 ; i < myApplication.resultList.size(); i++){
                            MarkerOptions markerOptions = new MarkerOptions();
                            Result result = myApplication.resultList.get(i);
                            Geometry geometry = result.getGeometry();
                            job.com.searchnearbyplaces.model.Location location = geometry.getLocation();
                            double latitude = location.getLat();
                            double longitude = location.getLng();
                            String place_name = result.getName();
                            String vicinity = result.getFormattedAddress();
                            LatLng latLng = new LatLng(latitude, longitude);
                            markerOptions.position(latLng);
                            markerOptions.title(place_name + " : " + vicinity);
                            mGoogleMap.addMarker(markerOptions);

                        }

                    }

                }

                @Override
                public void onFailure(Call<NearByPlaces> call, Throwable t) {
                    progressDialog.dismiss();
                    t.printStackTrace();

                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
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
                            /*if (mLocation != null) {
                                mLatitude = mLocation.getLatitude();
                                mLongitude = mLocation.getLongitude();
                            }*/
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
                                /*if (mLocation != null) {
                                    mLatitude = mLocation.getLatitude();
                                    mLongitude = mLocation.getLongitude();
                                }*/
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
        ActivityCompat.requestPermissions(SearchActivity.this, new String[]{ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
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
