package com.mostafa.android.uber;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mostafa.android.uber.HistoryStaff.History;
import com.mostafa.android.uber.HistoryStaff.HistoryAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HistoryActivity extends AppCompatActivity {
    @BindView(R.id.historyRecyclerView)
    RecyclerView mHistoryRecyclerView;
    String customerOrDriver,userId;
    private RecyclerView.Adapter mHistoryAdapter;
    private RecyclerView.LayoutManager mHistoryLayoutManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      try {
          setContentView(R.layout.activity_history);
          ButterKnife.bind(this);
          mHistoryRecyclerView.setNestedScrollingEnabled(true);
          mHistoryRecyclerView.setHasFixedSize(true);
          mHistoryLayoutManager = new LinearLayoutManager(HistoryActivity.this);
          mHistoryRecyclerView.setLayoutManager(mHistoryLayoutManager);
          mHistoryAdapter = new HistoryAdapter(getDataSetHistory(), HistoryActivity.this);
          mHistoryRecyclerView.setAdapter(mHistoryAdapter);



          mHistoryAdapter.notifyDataSetChanged();
          customerOrDriver = "customer";
          userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
          getUserHistoryIds();
      }catch (Exception e){
          Log.e("History Activity",e.getMessage().toString());
      }

    }
    private void getUserHistoryIds() {
        DatabaseReference userHistoryDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(customerOrDriver).child(userId).child("history");
        userHistoryDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot history : dataSnapshot.getChildren()){
                        FetchRideInformation(history.getKey());
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    private String getData(Long time) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(time*1000);
        String date = DateFormat.format("MM-dd-yyyy hh:mm", cal).toString();
        return date;
    }
    private void FetchRideInformation(String rideKey) {
        DatabaseReference historyDatabase = FirebaseDatabase.getInstance().getReference().child("history").child(rideKey);
        historyDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String rideId = dataSnapshot.getKey();
                    Long Time =0L;
                    for(DataSnapshot child:dataSnapshot.getChildren()){
                        if (child.getKey().equals("timeStamp")){
                            Time = Long.valueOf(child.getValue().toString());
                        }
                    }
                    History obj = new History(rideId,getData(Time));
                    resultsHistory.add(obj);
                    mHistoryAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    private ArrayList resultsHistory = new ArrayList<History>();
    private ArrayList<History> getDataSetHistory() {
        return resultsHistory;
    }
}
