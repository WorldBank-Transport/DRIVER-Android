package org.worldbank.transport.driver.ActivityTests;

import android.app.Activity;
import android.app.Instrumentation;
import android.view.View;
import android.view.ViewGroup;

import org.worldbank.transport.driver.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Static methods to help with testing form activities.
 *
 * Created by kathrynkillebrew on 1/12/16.
 */
public class FormActivityTestHelpers {

    // helper to recursively find all child views in a view hierarchy
    // http://stackoverflow.com/questions/18668897/android-get-all-children-elements-of-a-viewgroup
    public static List<View> getAllChildViews(View v) {
        List<View> visited = new ArrayList<>();
        List<View> unvisited = new ArrayList<>();
        unvisited.add(v);

        while (!unvisited.isEmpty()) {
            View child = unvisited.remove(0);
            visited.add(child);
            if (!(child instanceof ViewGroup)) continue;
            ViewGroup group = (ViewGroup) child;
            final int childCount = group.getChildCount();
            for (int i=0; i<childCount; i++) unvisited.add(group.getChildAt(i));
        }

        return visited;
    }

    public static void waitForLoaderToDisappear(Instrumentation instrumentation, Activity activity) {
        instrumentation.waitForIdleSync();
        final View loaderView = activity.findViewById(R.id.form_progress);
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                int counter = 0;
                while ((loaderView.getVisibility() == View.VISIBLE) && counter < 10) {
                    try {
                        Thread.sleep(1000);
                        counter += 1;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        instrumentation.waitForIdleSync();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
