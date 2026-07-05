package com.deathbeacon.data;

public enum WaypointCategory {
    WAYPOINTS("Waypoints"),
    DEATHS("Deaths"),
    FAVORITES("Favorites"),
    GLOBAL("Global");

    public final String label;

    WaypointCategory(String label) {
        this.label = label;
    }
}
