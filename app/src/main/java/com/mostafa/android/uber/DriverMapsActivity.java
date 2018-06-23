package com.mostafa.android.uber;

import android.Manifest;
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
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;

public class DriverMapsActivity extends FragmentActivity implements RoutingListener, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{

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
    @BindView(R.id.pickCustomer)
    Button pickCustomerButton;
    private int status = 0;
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};
    String userid;
    String id;
    int ForDestoryMap =0;
    FusedLocationProviderClient mFusedLocationClient;
    private String CustomerId="";
    private String destination="";
    private LatLng destinationLatLng;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);
        ForDestoryMap =0;
        polylines = new ArrayList<>();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        id= FirebaseAuth.getInstance().getCurrentUser().getUid();
        //log out from the map
        logout.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                disconnectDriver();
            }
        });
        //go to settings for edit information of the user
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DriverMapsActivity.this,DriverMapSettings.class));
            }
        });
        //to Make call  for the Driver to the Customer.
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

        pickCustomerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (status){
                    case 1:
                        pickCustomerButton.setText("Cancel request");
                        status = 2;
                        eraseline();
                        if(destinationLatLng !=null) {
                            MakeRoute(destinationLatLng);
                        }else{
                            Toast.makeText(DriverMapsActivity.this, "User is not Enter the destination", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 2:
                        recordRide();
                        endRide();
                        status =1;
                        break;
                }
            }
        });
        getAssignedRequest();
    }

    /*
    To Record the Ride History
     */
    private void recordRide() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("driver").child(userId).child("history");
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("customer").child(CustomerId).child("history");
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("history");
        String requestId = historyRef.push().getKey();
        driverRef.child(requestId).setValue(true);
        customerRef.child(requestId).setValue(true);
        HashMap map = new HashMap();
        map.put("driver", userId);
        map.put("customer", CustomerId);
        map.put("rating", 0);
        map.put("timeStamp",getCurrentTimestamp());
        map.put("destination", destination);
        map.put("location/from/lat", PickUpLocation.latitude);
        map.put("location/from/lng", PickUpLocation.longitude);
        map.put("location/to/lat", destinationLatLng.latitude);
        map.put("location/to/lng", destinationLatLng.longitude);
        historyRef.child(requestId).updateChildren(map);
    }
    private Long getCurrentTimestamp() {
        Long timestamp = System.currentTimeMillis()/1000;
        return timestamp;
    }
    private void getAssignedRequest() {
        String DriverID =FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child("driver").child(DriverID).child("Customer Requests").child("CustomerId");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
              try{
                  if(dataSnapshot.exists()){
                      status=1;
                      CustomerId = dataSnapshot.getValue().toString();
                      getCustomerLocation();
                      getAssignedCustomerDestination();
                      getCustomerInfo();
                  }else{

                  }
              }catch (Exception e){
                  Log.e("GetAssignedRequest",e.getMessage().toString());
              }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(DriverMapsActivity.this, ""+databaseError.getDetails().toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        logoutDialog();

    }
    Double destinationLat = 0.0;
    Double destinationLng = 0.0;
    private void getAssignedCustomerDestination() {
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("driver").child(id).child("Customer Requests");
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("destination")!=null){
                        destination = map.get("destination").toString();
                        driverDestintionTextView.setText("Destination: " + destination);
                    }
                    else{
                        driverDestintionTextView.setText("Destination: --");
                    }

                    if(map.get("destinationLat") != null){
                        destinationLat = Double.valueOf(map.get("destinationLat").toString());
                    }
                    if(map.get("destinationLng") != null){
                        destinationLng = Double.valueOf(map.get("destinationLng").toString());
                        destinationLatLng = new LatLng(destinationLat, destinationLng);
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void endRide() {

        eraseline();
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference CustomerRequestDRef = FirebaseDatabase.getInstance().getReference().child("Users").child("driver").child(userID).child("Customer Requests");
        CustomerRequestDRef.removeValue();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Customer Requests");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(CustomerId);
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

    private void getCustomerInfo() {
        DatabaseReference firebaseDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("customer").child(CustomerId);
        firebaseDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String,Object> map = (Map<String,Object>)dataSnapshot.getValue();
                if(dataSnapshot.exists()){
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
                    LatLng customerLatLng = new LatLng(locationlat,locationlng);
                    if(mCustomerMarker != null){
                        mCustomerMarker.remove();
                    }
                    mCustomerMarker = mMap.addMarker(new MarkerOptions().position(customerLatLng).title("your Customer").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_customer)));
                    MakeRoute(customerLatLng);
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
    private Routing routing;
    private LatLng PickUpLocation;
    private void  MakeRoute(LatLng mPickUpLocation){
        this.PickUpLocation = mPickUpLocation;
        routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(true)
                .waypoints(latLng,mPickUpLocation)
                .build();
        routing.execute();
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
        Log.e("stop ","Hi I am here");
        ForDestoryMap =1;
        DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("DriversAvailable");
        GeoFire geoFireAvailable = new GeoFire(refAvailable);
        geoFireAvailable.removeLocation(id);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.e("OnDestory ","Hi I am here");

        super.onDestroy();
    }

    private void disconnectDriver(){
      try{
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
        Log.e("On Disconnect driver","I am delete");
        startActivity(new Intent(DriverMapsActivity.this,MainActivity.class));
        finish();
      }catch(Exception e){
          Log.e("On Disconnect Driver",e.getMessage().toString());
      }
    }
    //remove line
    private void eraseline(){
        for(Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }
    //Routing Interface Methods .
    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            //Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {

    }

    private void logoutDialog(){new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Closing Activity")
            .setMessage("Are you sure you want to Sign out ?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    disconnectDriver();
                    finish();
                }

            })
            .setNegativeButton("No", null)
            .show();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        ForDestoryMap =0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ForDestoryMap =0;
    }
}

