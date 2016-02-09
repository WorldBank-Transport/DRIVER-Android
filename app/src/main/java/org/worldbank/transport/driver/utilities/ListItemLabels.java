package org.worldbank.transport.driver.utilities;

import java.util.ArrayList;

/**
 * Holder for record form list item labels. In addition to string label, can also hold
 * collection of paths to images to show in list view.
 *
 * Created by kathrynkillebrew on 2/8/16.
 */
public class ListItemLabels {
    public ArrayList<String> labels;
    public ArrayList<String> imagePaths;
    public boolean hasImages;

    public ListItemLabels(ArrayList<String> labels, ArrayList<String> imagePaths) {
        this.labels = labels;
        this.imagePaths = imagePaths;
        hasImages = imagePaths != null && imagePaths.size() > 0;

        if (labels == null) {
            throw new IllegalArgumentException("List item labels must not be null");
        }

        if (imagePaths != null && labels.size() != imagePaths.size()) {
            throw new IllegalArgumentException("List item labels and image paths differ in size");
        }
    }
}
