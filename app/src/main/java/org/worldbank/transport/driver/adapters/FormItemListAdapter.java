package org.worldbank.transport.driver.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.azavea.androidvalidatedforms.controllers.ImageController;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.utilities.DriverUtilities;
import org.worldbank.transport.driver.utilities.ListItemLabels;

import java.util.ArrayList;

/**
 * Manages form list section item presentation.
 *
 * Created by kathrynkillebrew on 12/31/15.
 */
public class FormItemListAdapter extends RecyclerView.Adapter<FormItemListAdapter.ViewHolder> {

    private static final String LOG_LABEL = "FormItemListAdapter";

    // size for downscaled images in list view; should match size in record_form_item.xml
    private static final int IMAGE_SIZE = 80;

    private String defaultLabel;
    private ListItemLabels listItemLabels;
    private FormItemClickListener clickListener;

    public interface FormItemClickListener {
        void clickedItem(View view, int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView textView;
        public ImageView imageView;
        public FormItemClickListener listener;

        public ViewHolder(RelativeLayout v, FormItemClickListener listener) {
            super(v);
            this.textView = (TextView)v.findViewById(R.id.record_form_list_item_text);
            this.imageView = (ImageView)v.findViewById(R.id.record_form_list_item_image);
            this.listener = listener;
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            listener.clickedItem(v, this.getAdapterPosition());
        }
    }

    public FormItemListAdapter(ArrayList items, Class itemClass, String defaultLabel, FormItemClickListener clickListener) {
        this.defaultLabel = defaultLabel;
        this.clickListener = clickListener;
        this.listItemLabels = DriverUtilities.getListItemLabels(items, itemClass, defaultLabel);
    }

    public void rebuildLabelList(ArrayList items, Class itemClass) {
        this.listItemLabels = DriverUtilities.getListItemLabels(items, itemClass, defaultLabel);
        notifyDataSetChanged();
    }

    @Override
    public FormItemListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RelativeLayout view = (RelativeLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.record_form_item, parent, false);
        return new ViewHolder(view, clickListener);
    }

    @Override
    public void onBindViewHolder(FormItemListAdapter.ViewHolder holder, int position) {
        holder.textView.setText(listItemLabels.labels.get(position));

        if (listItemLabels.hasImages) {
            Log.d(LOG_LABEL, "setting image label");
            holder.imageView.setVisibility(View.VISIBLE);
            ImageController.setDownscaledImageFromFilePath(holder.imageView,
                    listItemLabels.imagePaths.get(position), IMAGE_SIZE, IMAGE_SIZE);
        }
    }

    @Override
    public int getItemCount() {
        return listItemLabels.labels.size();
    }
}
