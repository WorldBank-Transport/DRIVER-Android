package org.worldbank.transport.driver.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.azavea.androidvalidatedforms.tasks.ResizeImageTask;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.utilities.DriverUtilities;
import org.worldbank.transport.driver.utilities.ListItemLabels;

import java.util.ArrayList;

/**
 * Manages form list section item presentation.
 *
 * Created by kathrynkillebrew on 12/31/15.
 */
public class FormItemListAdapter extends RecyclerView.Adapter<FormItemListAdapter.ViewHolder>
    implements ResizeImageTask.ResizeImageCallback {

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

    public FormItemListAdapter(String defaultLabel, FormItemClickListener clickListener) {
        this.defaultLabel = defaultLabel;
        this.clickListener = clickListener;
    }

    public void buildLabelList(ArrayList items, Class itemClass) {
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
            holder.imageView.setVisibility(View.VISIBLE);

            // set image in background task
            ResizeImageTask resizeImageTask = new ResizeImageTask(holder.imageView, IMAGE_SIZE, IMAGE_SIZE, this);
            resizeImageTask.execute(listItemLabels.imagePaths.get(position));
        }
    }

    @Override
    public void imageNotSet() {
        Log.w(LOG_LABEL, "Could not set image on list view");
    }

    @Override
    public int getItemCount() {
        if (listItemLabels == null || listItemLabels.labels == null) {
            return 0;
        }

        return listItemLabels.labels.size();
    }
}
