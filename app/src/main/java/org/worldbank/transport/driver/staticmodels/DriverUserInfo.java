package org.worldbank.transport.driver.staticmodels;

import java.util.List;

/**
 * Created by kathrynkillebrew on 12/8/15.
 */
public class DriverUserInfo {

    public static final String ADMIN_GROUP = "admin";
    public static final String ANALYST_GROUP = "analyst";

    public int id;
    public String username;
    public String email;
    public List<String> groups;

    // Helper function to determine whether user has access to add new records or not.
    public boolean hasWritePermission() {
        return groups != null && (groups.contains(ADMIN_GROUP) || groups.contains(ANALYST_GROUP));
    }
}

