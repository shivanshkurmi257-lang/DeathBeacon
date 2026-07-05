package com.deathbeacon.data;

import java.util.UUID;

/**
 * A single waypoint, custom or death-generated.
 * Plain data holder - persisted directly with Gson.
 */
public class Waypoint {

    public UUID id;
    public String name;
    public String description;

    public double x;
    public double y;
    public double z;

    /** e.g. "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end" */
    public String dimension;

    /** World/save name this waypoint belongs to. Ignored for global waypoints. */
    public String worldName;

    /** ARGB packed color, e.g. 0xFFFF0000 for opaque red. */
    public int color = 0xFFFFFFFF;

    public boolean visible = true;
    public boolean favorite = false;
    public boolean beamEnabled = true;
    public boolean hudEnabled = true;

    /** True for auto-generated death waypoints. */
    public boolean isDeath = false;

    /** True for waypoints stored in the shared global category. */
    public boolean isGlobal = false;

    /** Only used when isDeath == true - "Death #N" numbering, per world. */
    public int deathNumber = 0;

    public long createdAt;
    public long modifiedAt;

    public Waypoint() {
        this.id = UUID.randomUUID();
        this.createdAt = System.currentTimeMillis();
        this.modifiedAt = this.createdAt;
    }

    public static Waypoint create(String name, double x, double y, double z, String dimension, String worldName, int color) {
        Waypoint w = new Waypoint();
        w.name = name;
        w.x = x;
        w.y = y;
        w.z = z;
        w.dimension = dimension;
        w.worldName = worldName;
        w.color = color;
        return w;
    }

    public void touch() {
        this.modifiedAt = System.currentTimeMillis();
    }

    public String displayName() {
        return isDeath ? ("Death #" + deathNumber) : name;
    }
}
