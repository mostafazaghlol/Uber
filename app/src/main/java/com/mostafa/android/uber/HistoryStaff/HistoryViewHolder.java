package com.mostafa.android.uber.HistoryStaff;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mostafa.android.uber.HistorySingleActivity;
import com.mostafa.android.uber.R;


/**
 * Created by mostafa on 6/20/18.
 */

public class HistoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    TextView TxRideId,TxRideTime;
    public HistoryViewHolder(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
        TxRideId = (TextView)itemView.findViewById(R.id.rideId);
        TxRideTime = (TextView)itemView.findViewById(R.id.rideTime);

    }

    @Override
    public void onClick(View view) {
        Intent openSinglHistory = new Intent(view.getContext(), HistorySingleActivity.class);
        openSinglHistory.putExtra("Ride ID",TxRideId.getText().toString());
        view.getContext().startActivity(openSinglHistory);
    }
}
