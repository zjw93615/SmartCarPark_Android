package au.edu.unimelb.david.myapplication;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.location.places.ui.*;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import android.content.DialogInterface;
import java.lang.Thread;
import android.support.v7.app.AlertDialog;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class MapsActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        OnMyLocationButtonClickListener,
        OnMyLocationClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback{

    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final long MIN_TIME = 1000;
    private static final float MIN_DISTANCE = 50;


    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean mPermissionDenied = false;

    private GoogleMap mMap;
    private Location mLocation;
    private LocationManager locationManager;
    private Criteria criteria;
    private String provider;

    public CopyOnWriteArrayList<Marker> markerList = new CopyOnWriteArrayList<Marker>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        enableMyLocation();



    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);

            // Getting LocationManager object from System Service LOCATION_SERVICE
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            // Creating a criteria object to retrieve provider
            criteria = new Criteria();

            // Getting the name of the best provider
            provider = locationManager.getBestProvider(criteria, true);

            // Getting Current Location
            mLocation = locationManager.getLastKnownLocation(provider);


            if(mLocation != null) {
                cameraMove(mLocation.getLatitude(),mLocation.getLongitude());
            }
        }
    }

    private void cameraMove(double lat, double lng) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(lat,lng),15);
        mMap.animateCamera(cameraUpdate);
    }

    private void cameraMove(LatLng latLng) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng,15);
        mMap.animateCamera(cameraUpdate);
    }

    private void removeMarkers() {
        Iterator<Marker> iterator = markerList.iterator();
        while (iterator.hasNext()) {
            iterator.next().remove();
        }
        markerList = new CopyOnWriteArrayList<Marker>();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);
        MenuItem item = menu.findItem(R.id.menuSearch);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.menuSearch) {
                    findPlace();
                }
                return false;
            }
        });
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                return false;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                return false;
//            }
//        });
        return super.onCreateOptionsMenu(menu);
    }

    public void findPlace() {

        try {
            VisibleRegion visibleRegion = mMap.getProjection().getVisibleRegion();
            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                            .setBoundsBias(new LatLngBounds(visibleRegion.nearLeft, visibleRegion.farRight))
                            .build(this);
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException e) {
            // TODO: Handle the error.
        } catch (GooglePlayServicesNotAvailableException e) {
            // TODO: Handle the error.
        }
    }

    // A place has been received; use requestCode to track the request.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                removeMarkers();
                Place place = PlaceAutocomplete.getPlace(this, data);
                requestCarPark(place.getLatLng());
                MarkerOptions markerOptions = new MarkerOptions().position(place.getLatLng())
                        .title(""+place.getName());

                Marker maker = mMap.addMarker(markerOptions);
                markerList.add(maker);
                cameraMove(place.getLatLng());
                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        final Marker m = marker;
                        String str = "Car Park";
                        if(marker.getTitle().toLowerCase().contains(str.toLowerCase())) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                            builder.setTitle("Start Navigation")
                                    .setMessage("Are you sure you want to be navigated to this car park?")
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // continue with delete
                                            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + m.getPosition().latitude + "," + m.getPosition().longitude);
                                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                                            mapIntent.setPackage("com.google.android.apps.maps");
                                            startActivity(mapIntent);
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // do nothing
                                        }
                                    })
                                    .setIcon(android.R.drawable.ic_dialog_map)
                                    .show();
                        }
                        Log.i("message", "Place: " + marker.getTitle());
                        return false;
                    }
                });
                Log.i("message", "Place: " + place.getName());
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.i("message", status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    private void requestCarPark(LatLng latLng) {
        HttpRequest httpRequest = new HttpRequest(this, mMap, latLng);
        httpRequest.start();

    }

    class HttpRequest extends Thread {
        private GoogleMap mMap;
        private LatLng latLng;
        private double lat;
        private double lng;
        private int id;
        private MapsActivity mapsActivity;
        private ArrayList<Data> list = new ArrayList<>();

        class Data {
            public double lat;
            public double lng;
            public int id;
            public Data (double a, double n, int i) {
                lat = a;
                lng = n;
                id = i;
            }
        }

        public HttpRequest(MapsActivity ma, GoogleMap m, LatLng l) {
            mMap = m;
            latLng = l;
            mapsActivity = ma;
        }

        @Override
        public void run() {
            String urlString = "http://18.220.135.246/SmartCarPark/assets/php/getCarPark.php";
            HttpURLConnection client = null;
            try {
                URL url = new URL(urlString);
                client = (HttpURLConnection) url.openConnection();
                client.setRequestMethod("POST");
//                client.setRequestProperty("lat",""+latLng.latitude);
//                client.setRequestProperty("lng",""+latLng.longitude);
                client.setDoOutput(true);
                BufferedOutputStream outputPost = new BufferedOutputStream(client.getOutputStream());
                String request = "lat="+latLng.latitude+"&lng="+latLng.longitude+"&num=100";
                outputPost.write(request.getBytes());
                outputPost.flush();
                outputPost.close();
                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String results = "";
                String str;
                while ((str = reader.readLine()) !=null){
                    results += str;
                }
                reader.close();
                JSONObject obj = new JSONObject(results);
                JSONArray jArray = obj.getJSONArray("results");
                for(int i = 0; i < jArray.length(); i++) {
                    JSONObject result = jArray.getJSONObject(i);
                    JSONObject data = result.getJSONObject("data");
                    lat = data.getDouble("lat");
                    lng = data.getDouble("lng");
                    id = data.getInt("id");
                    Log.i("info", ""+id);
                    Data d = new Data(lat,lng,id);
                    list.add(d);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Iterator<Data> iterator = list.iterator();
                        while (iterator.hasNext()) {
                            Data data = iterator.next();
                            Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(data.lat,data.lng))
                                    .title("Car Park " + data.id)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                            mapsActivity.markerList.add(marker);
                            Log.i("info", ""+data.id);
                        }

                    }
                });
                Log.i("info", results);
            }catch (Exception e) {
                Log.e("error", e.toString());
            }finally {
                if(client != null)
                    client.disconnect();
            }
        }
    }

}

