package com.mostafa.android.uber;

import android.*;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;
    GoogleApiClient mgooGoogleApiClient;
    Location mlocation;
    LocationRequest mLocationRequest;
    @BindView(R.id.customerlogout)
    Button logout;
    @BindView(R.id.Pickup)
    Button pickup;
    @BindView(R.id.distance)
    Button distance;
    @BindView(R.id.settings)
    Button settings;
    private short stopcount = 0;
    String userid;
    String id;
    LatLng pickupmarkder;
    private boolean isRequsted =false;
    private Marker Pickumarker;
    String destination = "";
    @BindView(R.id.CustomerInfo)
    RelativeLayout DriverlinearLayout;
    @BindView(R.id.CustomerDetails)
    LinearLayout DriverDetalislinearLayout;
    @BindView(R.id.customerImage)
    ImageView DriverImageView;
    @BindView(R.id.customerName)
    TextView DriverNameTextView;
    @BindView(R.id.customerPhone)
    TextView DriverPhoneTextView;
    @BindView(R.id.customercar)
    TextView DriverCarTextView;
    @BindView(R.id.phoneCallCustomer)
    Button phoneCallButton;
    @BindView(R.id.noInfo)
    TextView noInfo;
    @BindView(R.id.Histroy)
    TextView HistoryButton;
    private LatLng destinationLatLng;
    //To stop search about Driver Avaialbe.
    private boolean stopSearch = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        ButterKnife.bind(this);
        destinationLatLng = new LatLng(0.0,0.0);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapq);
        mapFragment.getMapAsync(this);
        id= FirebaseAuth.getInstance().getCurrentUser().getUid();
        logout.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                userid = FirebaseAuth.getInstance().getCurrentUser().getUid();
               // DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Customer Requests");
                if(isRequsted) {
                    CancelRequest();
                } //}
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(CustomerMapActivity.this,MainActivity.class));
                finish();
                return;
            }
        });
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(CustomerMapActivity.this,CustomerSettingsActivity.class));
            }
        });
        HistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openHistoryActivity = new Intent(CustomerMapActivity.this,HistoryActivity.class);
                startActivity(openHistoryActivity);
            }
        });
        pickup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopcount= 0;
                if(isRequsted){
                    DriverlinearLayout.setVisibility(View.GONE);
                    noInfo.setVisibility(View.GONE);
                    CancelRequest();
                    stopSearch = false;
                }else {
                    stopSearch =true;
                    isRequsted = true;
                    String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Customer Requests");
                    GeoFire geoFire = new GeoFire(reference);
                    geoFire.setLocation(userID, new GeoLocation(mlocation.getLatitude(), mlocation.getLongitude()));
                    pickupmarkder = new LatLng(mlocation.getLatitude(), mlocation.getLongitude());
                    Pickumarker =  mMap.addMarker(new MarkerOptions().position(pickupmarkder).title("Pick me").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_customer)));
                    distance.setText("Getting the driver .... ");
                    pickup.setText("Cancel Request ");
                    getTheDriver();
                }
            }
        });
        phoneCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("DriverMapActivity","clicked");
                if (ActivityCompat.checkSelfPermission(CustomerMapActivity.this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(CustomerMapActivity.this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)  {

                }
                try{Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + DriverPhoneTextView.getText().toString().trim()));
                    startActivity(intent);}catch (Exception e){Log.e("CustomerMApp",e.getMessage().toString());}

            }
        });
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected pla
                // ce.
                destination = place.getName().toString();

                destinationLatLng = place.getLatLng();
                Log.e("Distination",destination);
                Log.e("DistinationLatLng",String.valueOf(destinationLatLng));
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
            }
        });


    }
    private int radius = 1;
    private boolean driverFound = false;
    private String driverkey;
    private GeoQuery geoQuery;
    private DatabaseReference dataRefDriverKey,dataRefDriverKeyData;
    private void getTheDriver() {
        DatabaseReference  ref = FirebaseDatabase.getInstance().getReference().child("DriversAvailable");
        GeoFire geoFire = new GeoFire(ref);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupmarkder.latitude,pickupmarkder.longitude),radius);
        geoQuery.removeAllListeners();
        if(stopSearch){
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    if(!driverFound && isRequsted) {
                        driverFound = true;
                        driverkey = key;
                        distance.setText("Found the driver in Range of " + String.valueOf(radius) + " KM");
                        dataRefDriverKey = FirebaseDatabase.getInstance().getReference().child("Users").child("driver").child(driverkey).child("Customer Requests");
                        String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        /*Update the Driver database with the Customer data
                        from ID,destination.
                         */
                        Map updatePost = new HashMap();
                        updatePost.put("CustomerId", customerId);
                        if (!destination.isEmpty()) {
                            updatePost.put("destination", destination);
                        } else {
                            updatePost.put("destination", "");
                        }
                        if(destinationLatLng.latitude == 0 &&destinationLatLng.longitude == 0){
                            updatePost.put("destinationLat",String.valueOf(0));
                            updatePost.put("destinationLng",String.valueOf(0));
                        }else{
                            updatePost.put("destinationLat",String.valueOf(destinationLatLng.latitude));
                            updatePost.put("destinationLng",String.valueOf(destinationLatLng.longitude));
                        }
                        dataRefDriverKey.updateChildren(updatePost);


                        getDriverLocation();
                        getDriverInfo();
                        getHasRideEnded();
                    }
                }

                @Override
                public void onKeyExited(String key) {

                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {

                }

                @Override
                public void onGeoQueryReady() {
                    if(!driverFound){
                        if(stopcount == 1){
                            Log.e("Stop counting ","I stopped Here");
                            radius = 1;
                        }else{
                            radius+=1;
                            distance.setText("Getting the driver in Range of "+String.valueOf(radius)+" KM");
                            getTheDriver();}

                    }

                }

                @Override
                public void onGeoQueryError(DatabaseError error) {
                }
            });
        }


    }
    public DatabaseReference driveHasEndedRef;
    public ValueEventListener driveHasEndedRefListener;
    private void getHasRideEnded(){
        driveHasEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("drivers").child(driverkey).child("Customer Requests").child("CustomerId");
        driveHasEndedRefListener = driveHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try{
                if(dataSnapshot.exists()){

                }else{
                    CancelRequest();
                }
                }catch (Exception e){
                    Log.e("getHasRideEnded",e.getMessage().toString());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    private void getDriverInfo() {
        DriverlinearLayout.setVisibility(View.VISIBLE);
        dataRefDriverKeyData = FirebaseDatabase.getInstance().getReference().child("Users").child("driver").child(driverkey);
        dataRefDriverKeyData.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                if (dataSnapshot.exists()) {
                    DriverImageView.setVisibility(View.VISIBLE);
                    DriverDetalislinearLayout.setVisibility(View.VISIBLE);
                    noInfo.setVisibility(View.GONE);
                    if (map.get("name") != null) {
                        DriverNameTextView.setText(map.get("name").toString().trim());
                    }
                    if (map.get("phone") != null) {
                        DriverPhoneTextView.setText(map.get("phone").toString());
                    }
                    if (map.get("car") != null) {
                        DriverCarTextView.setText(map.get("car").toString());
                    }
                    if (map.get("profileImageUrl") != null) {
                        String mProfileImageUrl = map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(mProfileImageUrl).into(DriverImageView);
                    }
                } else {
                    noInfo.setVisibility(View.VISIBLE);
                    DriverDetalislinearLayout.setVisibility(View.GONE);
                    DriverImageView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void CancelRequest(){
        try{
            if(driverFound) {
                stopcount = 2;
                isRequsted = false;
                destination = "";
                geoQuery.removeAllListeners();
                dataRef.removeEventListener(dataRefValueListener);
                driveHasEndedRef.removeEventListener(driveHasEndedRefListener);
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                if (driverkey != null) {
                    DatabaseReference dataRefcustomerId = FirebaseDatabase.getInstance().getReference().child("Users").child("driver").child(driverkey).child("Customer Requests");
                    dataRefcustomerId.removeValue();
                    driverkey = null;
                }
                driverFound = false;
                radius = 1;
//                DatabaseReference databaseReferenceCustomerRequest = FirebaseDatabase.getInstance().getReference("Customer Requests");
//                GeoFire geoFire = new GeoFire(databaseReferenceCustomerRequest);
//                geoFire.removeLocation(userId);
                if (Pickumarker != null) {
                    Pickumarker.remove();
                }
                if (mDriverMarker != null) {
                    mDriverMarker.remove();
                }
                pickup.setText("call uber");
                distance.setText("Distance is ....");
                DriverlinearLayout.setVisibility(View.GONE);
                DriverNameTextView.setText("");
                DriverPhoneTextView.setText("");
                DriverCarTextView.setText("Destination: --");
                DriverImageView.setImageResource(R.mipmap.ic_proflie);
            }else{
                stopcount = 1;
                isRequsted = false;
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                geoQuery.removeAllListeners();

                radius = 1;
//                DatabaseReference databaseReferenceCustomerRequest = FirebaseDatabase.getInstance().getReference("Customer Requests");
//                GeoFire geoFire = new GeoFire(databaseReferenceCustomerRequest);
//                geoFire.removeLocation(userId);
                if (Pickumarker != null) {
                    Pickumarker.remove();
                }
                pickup.setText("call uber");
                distance.setText("Distance is ....");
            }
        }catch (Exception e){
                Log.e("Problem " , e.getMessage().toString().trim());
                }

    }
    private Marker mDriverMarker;
    private DatabaseReference dataRef;
    private ValueEventListener dataRefValueListener;
    private void getDriverLocation() {
         dataRef = FirebaseDatabase.getInstance().getReference().child("DriversWorking").child(driverkey).child("l");
         dataRefValueListener = dataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    List<Object> map = (List<Object>)dataSnapshot.getValue();
                    double locationlat = 0;
                    double locationlng = 0;
                    if(map.get(0)!= null){
                        locationlat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1)!= null){
                        locationlng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationlat,locationlng);
                    if(mDriverMarker != null){
                        mDriverMarker.remove();
                    }
                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("your driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_taxi)));
                    Location locPic = new Location("");
                    locPic.setLatitude(pickupmarkder.latitude);
                    locPic.setLongitude(pickupmarkder.longitude);
                    Location locDriver= new Location("");
                    locDriver.setLatitude(locationlat);
                    locDriver.setLongitude(locationlng);
                    float x = locPic.distanceTo(locDriver);
                    distance.setText("The Driver is "+ String.valueOf(x)+" away from you");
                    if(x <100){
                        distance.setText("The driver arrived");
                    }
                }else{
                    Toast.makeText(CustomerMapActivity.this, "The Driver is not found !", Toast.LENGTH_SHORT).show();
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }
    private void checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            }
            else{
                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }else{
            checkLocationPermission();
        }
        bulidGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }



    protected synchronized void bulidGoogleApiClient(){
        mgooGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        mgooGoogleApiClient.connect();
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    onLocationChanged(locationResult.getLastLocation());
                }
            }, Looper.myLooper());

    }

    public void onLocationChanged(Location location) {
        mlocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        try {
            logoutDialog();
        }catch (Exception e){
            Log.e("On Back Pressed",e.getMessage().toString());
        }
    }

    private void logoutDialog(){new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Closing Activity")
            .setMessage("Are you sure you want to Sign out ?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(isRequsted) {
                        CancelRequest();
                    }
                    FirebaseAuth.getInstance().signOut();
                    finish();
                }

            })
            .setNegativeButton("No", null)
            .show();
    }
}
