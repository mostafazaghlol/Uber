package com.mostafa.android.uber;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
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

import java.security.Permission;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DriverMapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;
    GoogleApiClient mgooGoogleApiClient;
    Location mlocation;
    LocationRequest mLocationRequest;
    @BindView(R.id.logout)
    Button logout;
    @BindView(R.id.settingsDriver)
    Button settingsButton;
    @BindView(R.id.DriverInfo)
    RelativeLayout driverInfo;
    @BindView(R.id.customerDetailsLinearlayout)
    LinearLayout customerDetals;
    @BindView(R.id.driverImage)
    ImageView driverImageView;
    @BindView(R.id.driverName)
    TextView driverNameTextView;
    @BindView(R.id.driverPhone)
    TextView driverPhoneTextView;
    @BindView(R.id.driverDestion)
    TextView driverDestintionTextView;
    @BindView(R.id.phoneCall)
    Button phonecallImageView;
    @BindView(R.id.noInfoDriver)
    TextView noInfoDriver;
    String userid;
    String id;
    FusedLocationProviderClient mFusedLocationClient;
    private String CustomerId="";
    private String destination="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        id= FirebaseAuth.getInstance().getCurrentUser().getUid();

        logout.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                userid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                disconnectDriver();
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(DriverMapsActivity.this,MainActivity.class));
                finish();
                return;
            }
        });
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DriverMapsActivity.this,DriverMapSettings.class));
            }
        });

            phonecallImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.e("DriverMapActivity","clicked");
                    if (ActivityCompat.checkSelfPermission(DriverMapsActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(DriverMapsActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)  {

                    }
                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + driverPhoneTextView.getText().toString().trim()));
                    startActivity(intent);
                }
            });

        getAssignedRequest();
    }

    private void getAssignedRequest() {
        String driverID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child("driver").child(driverID);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String,Object> map = (Map<String, Object>)dataSnapshot.getValue();

                if(dataSnapshot.exists() && map.get("CustomerId") !=null && map.get("destination") != null ){
                    CustomerId = map.get("CustomerId").toString();
                    destination = map.get("destination").toString();
                    getCustomerLocation();
                    getCustomerInfo();
                }else{
                    CustomerId="";
                    if(mCustomerMarker != null){
                        mCustomerMarker.remove();
                    }
                    if (valueEventListenerDataRef != null){
                        dataRef.removeEventListener(valueEventListenerDataRef);
                    }
                    driverInfo.setVisibility(View.GONE);
                    driverNameTextView.setText("");
                    driverPhoneTextView.setText("");
                    driverImageView.setImageResource(R.mipmap.ic_proflie);
                    driverDestintionTextView.setText("");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getCustomerInfo() {
        DatabaseReference firebaseDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("customer").child(CustomerId);

        firebaseDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String,Object> map = (Map<String,Object>)dataSnapshot.getValue();
                if(dataSnapshot.exists()&& (map.get("name")!= null||map.get("phone")!= null||map.get("profileImageUrl") !=null )){
                    noInfoDriver.setVisibility(View.GONE);
                    customerDetals.setVisibility(View.VISIBLE);
                    driverImageView.setVisibility(View.VISIBLE);
                    if(map.get("name") != null){
                        driverNameTextView.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!=null){
                        driverPhoneTextView.setText(map.get("phone").toString());
                    }
                    if(!destination.isEmpty()){
                        driverDestintionTextView.setText("Destination is : " +destination);
                    }else{
                        driverDestintionTextView.setText("Destination is not specified ");

                    }
                    if(map.get("profileImageUrl")!=null){
                        String mProfileImageUrl = map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(mProfileImageUrl).into(driverImageView);
                    }
                }else{
                    noInfoDriver.setVisibility(View.VISIBLE);
                    customerDetals.setVisibility(View.GONE);
                    driverImageView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private Marker mCustomerMarker;
    private DatabaseReference dataRef;
    private ValueEventListener valueEventListenerDataRef;
    private void getCustomerLocation() {
        dataRef= FirebaseDatabase.getInstance().getReference().child("Customer Requests").child(CustomerId).child("l");
        valueEventListenerDataRef =  dataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    driverInfo.setVisibility(View.VISIBLE);
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
                    if(mCustomerMarker != null){
                        mCustomerMarker.remove();
                    }
                    mCustomerMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("your Customer").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_customer)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    LocationCallback locationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            onLocationChanged(locationResult.getLastLocation());
        }
    };
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
            return;
        }
        bulidGoogleApiClient();
        mMap.setMyLocationEnabled(true);
        mMap.animateCamera(CameraUpdateFactory.zoomBy(15));


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
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback,Looper.myLooper());

    }
    LatLng latLng;
    public void onLocationChanged(Location location) {
        // You can now create a LatLng Object for use with maps
        latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null) {
            DatabaseReference AvaliableDriver = FirebaseDatabase.getInstance().getReference("DriversAvailable");
            GeoFire geoFireAv = new GeoFire(AvaliableDriver);
            DatabaseReference WorkingDriver= FirebaseDatabase.getInstance().getReference("DriversWorking");
            GeoFire geoFireWo = new GeoFire(WorkingDriver);
            switch (CustomerId) {
                case "":
                    geoFireAv.setLocation(id, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    geoFireWo.removeLocation(id);
                    break;
                default:
                    geoFireWo.setLocation(id, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    geoFireAv.removeLocation(id);
                    break;
            }
            Log.e("On Location Changed ", "I am updataing ");
        }


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
    private void disconnectDriver(){
        if(mFusedLocationClient != null){
            mFusedLocationClient.removeLocationUpdates(locationCallback);
        }
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(new LocationCallback(){});
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("DriversAvailable");
        DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("DriversWorking");
        GeoFire geoFireAvailable = new GeoFire(refAvailable);
        geoFireAvailable.removeLocation(userId);
        GeoFire geoFireWorking = new GeoFire(refWorking);
        geoFireWorking.removeLocation(userId);
        FirebaseAuth.getInstance().signOut();
        Log.e("On Disconnect driver","I am delet");
        onStop();
    }


}

