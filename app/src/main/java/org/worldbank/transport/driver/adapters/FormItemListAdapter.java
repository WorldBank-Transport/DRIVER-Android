package org.worldbank.transport.driver.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.worldbank.transport.driver.R;

import java.util.ArrayList;

/**
 * Created by kathrynkillebrew on 12/31/15.
 */
public class FormItemListAdapter extends RecyclerView.Adapter<FormItemListAdapter.ViewHolder> {

    private ArrayList<String> itemList;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public ViewHolder(TextView v) {
            super(v);
            textView = v;
        }
    }

    public FormItemListAdapter(ArrayList<String> itemList) {
        this.itemList = itemList;
    }

    @Override
    public FormItemListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView view = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.record_form_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FormItemListAdapter.ViewHolder holder, int position) {
        holder.textView.setText(itemList.get(position));
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}
