package org.worldbank.transport.driver.adapters;

import android.content.Context;
import android.database.Cursor;
import android.widget.SimpleCursorAdapter;

/**
 * Created by kathrynkillebrew on 1/6/16.
 */
public class RecordListAdapter extends SimpleCursorAdapter {
    public RecordListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }
}
