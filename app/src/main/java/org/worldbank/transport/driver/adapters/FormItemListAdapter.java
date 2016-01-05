package org.worldbank.transport.driver.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.utilities.DriverUtilities;

import java.util.ArrayList;

/**
 * Manages form list section item presentation.
 *
 * Created by kathrynkillebrew on 12/31/15.
 */
public class FormItemListAdapter extends RecyclerView.Adapter<FormItemListAdapter.ViewHolder> {

    private static final String LOG_LABEL = "FormItemListAdapter";

    private String defaultLabel;
    private ArrayList<String> labelList;
    private FormItemClickListener clickListener;

    public interface FormItemClickListener {
        void clickedItem(View view, int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView textView;
        public FormItemClickListener listener;

        public ViewHolder(TextView v, FormItemClickListener listener) {
            super(v);
            this.textView = v;
            this.listener = listener;
            textView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            listener.clickedItem(v, this.getAdapterPosition());
        }
    }

    public FormItemListAdapter(ArrayList items, Class itemClass, String defaultLabel, FormItemClickListener clickListener) {
        this.defaultLabel = defaultLabel;
        this.clickListener = clickListener;
        this.labelList = DriverUtilities.getListItemLabels(items, itemClass, defaultLabel);
    }

    public void rebuildLabelList(ArrayList items, Class itemClass) {
        this.labelList = DriverUtilities.getListItemLabels(items, itemClass, defaultLabel);
        notifyDataSetChanged();
    }

    @Override
    public FormItemListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        TextView view = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.record_form_item, parent, false);
        return new ViewHolder(view, clickListener);
    }

    @Override
    public void onBindViewHolder(FormItemListAdapter.ViewHolder holder, int position) {
        holder.textView.setText(labelList.get(position));
    }

    @Override
    public int getItemCount() {
        return labelList.size();
    }
}
