package com.mostafa.android.uber;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HistorySingleActivity extends AppCompatActivity  implements OnMapReadyCallback,RoutingListener {
    GoogleMap mMap;
    DatabaseReference historyDatabaseRefereance ;
    String customerId , driverID, Rating,userDriverOrCustomer,currentUserId;
    @BindView(R.id.TextviewnameUser)
    TextView userName;
    @BindView(R.id.TextviewphoneUser)
    TextView userPhone;
    @BindView(R.id.ImageSingleHistory)
    ImageView userImage;
    @BindView(R.id.Textviewdistance)
    TextView txDistanec;
    @BindView(R.id.TextviewFromLat)
    TextView txFromLat;
    @BindView(R.id.TextviewFromLng)
    TextView txFromLng;
    @BindView(R.id.TextviewToLat)
    TextView txToLat;
    @BindView(R.id.TextviewToLng)
    TextView txToLng;
    @BindView(R.id.TextviewdataSignle)
    TextView txData;

    LatLng pickupLatLng,destinationLatLng;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
        setContentView(R.layout.activity_history_single);
        ButterKnife.bind(this);
        String rideId = getIntent().getStringExtra("Ride ID");
            Log.e("Error",rideId.toString());
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        historyDatabaseRefereance = FirebaseDatabase.getInstance().getReference().child("history").child(rideId);
        userDriverOrCustomer = "customer";
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        gethistoryInfo();
        }catch (Exception e){
            Log.e("Error",e.getMessage().toString());
        }
    }

    private void gethistoryInfo() {
        historyDatabaseRefereance.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for (DataSnapshot child :dataSnapshot.getChildren()){
                        if (child.getKey().equals("customer")){
                            customerId = child.getValue().toString();
                            if(!customerId.equals(currentUserId)){
                                userDriverOrCustomer = "Drivers";
                                getUserInformation("customer", customerId);
                            }
                        }
                        if(child.getKey().equals("driver")){
                            driverID = child.getValue().toString();
                            if(!driverID.equals(currentUserId)){
                                userDriverOrCustomer = "customer";
                                getUserInformation("driver",driverID);
                            }
                        }
                        if (child.getKey().equals("timeStamp")){
                            txData.setText(getDate(Long.valueOf(child.getValue().toString())));
                        }

                        if (child.getKey().equals("rating")) {
                            Rating = child.getValue().toString();
                        }

                        if (child.getKey().equals("destination")){
                            txDistanec.setText(child.getValue().toString());
                        }
                        if (child.getKey().equals("location")){
                            pickupLatLng = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()), Double.valueOf(child.child("from").child("lng").getValue().toString()));
                            destinationLatLng = new LatLng(Double.valueOf(child.child("to").child("lat").getValue().toString()), Double.valueOf(child.child("to").child("lng").getValue().toString()));
                            if(destinationLatLng != new LatLng(0,0)){
                                txFromLat.setText(String.valueOf(pickupLatLng.latitude));
                                txFromLng.setText(String.valueOf(pickupLatLng.longitude));
                                txToLat.setText(String.valueOf(destinationLatLng.latitude));
                                txToLat.setText(String.valueOf(destinationLatLng.longitude));
                                getRouteToMarker();
                            }
                        }
                    }

                    }
                }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getRouteToMarker() {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(pickupLatLng, destinationLatLng)
                .build();
        routing.execute();
    }
    private String getDate(Long time) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(time*1000);
        String date = DateFormat.format("MM-dd-yyyy hh:mm", cal).toString();
        return date;
    }
    private void getUserInformation(String customers, String customerId) {
        DatabaseReference getUserInfoRef = FirebaseDatabase.getInstance().getReference().child("Users").child(customers).child(customerId);
        getUserInfoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name") != null){
                        userName.setText(map.get("name").toString());
                    }
                    if(map.get("phone") != null){
                        userPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("profileImageUrl") != null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(userImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

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
    private void erasePolylines(){
        for(Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }
    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int i) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(pickupLatLng);
        builder.include(destinationLatLng);
        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (width * 0.2);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

        mMap.animateCamera(cameraUpdate);

        mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("pickup location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_customer)));
        mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("destination"));

        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i1  = 0; i1 < route.size(); i1++) {

            //In case of more than 5 alternative routes
            int colorIndex = i1 % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i1 * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(), "Route " + (i1 + 1) + ": distance - " + route.get(i1).getDistanceValue() + ": duration - " + route.get(i1).getDurationValue(), Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onRoutingCancelled() {

    }

}
