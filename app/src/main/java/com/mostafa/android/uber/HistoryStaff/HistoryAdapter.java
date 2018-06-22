package com.mostafa.android.uber.HistoryStaff;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mostafa.android.uber.R;

import java.util.List;
import java.util.Locale;

/**
 * Created by mostafa on 6/20/18.
 */

public class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolder> {

    private List<History> itemList;
    private Context context;

    public HistoryAdapter(List<History> itemList, Context context) {
        this.itemList = itemList;
        this.context = context;
    }
    @Override
    public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
       try {
           View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, null, false);
           RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
           layoutView.setLayoutParams(lp);
           HistoryViewHolder rcv = new HistoryViewHolder(layoutView);
           return rcv;
       }catch (Exception e){
           Log.e("onCreateViewHolder",e.getMessage().toString());
       }
       return null;
    }

    @Override
    public void onBindViewHolder(HistoryViewHolder holder, int position) {
      try {
          holder.TxRideId.setText(itemList.get(position).getRideId());
          holder.TxRideTime.setText(itemList.get(position).getRideTime());
      }catch (Exception e){
          Log.e("onBindViewHolder",e.getMessage().toString());
      }
    }



    @Override
    public int getItemCount() {
        return itemList.size();
    }
}
