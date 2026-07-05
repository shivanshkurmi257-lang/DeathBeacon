package com.deathbeacon.storage;

import com.deathbeacon.data.Waypoint;
import com.deathbeacon.data.WaypointCategory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Owns all persisted waypoint/death/global data and reads/writes it to
 * .minecraft/config/deathbeacon/*.json
 *
 * Waypoints and deaths are scoped to the current world (matched by worldName).
 * Global waypoints are shared across all worlds and live in their own file.
 */
public final class WaypointManager {

    private static final WaypointManager INSTANCE = new WaypointManager();

    public static WaypointManager get() {
        return INSTANCE;
    }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Type listType = new TypeToken<List<Waypoint>>() {}.getType();

    private final Path configDir;
    private final Path waypointsFile;
    private final Path deathsFile;
    private final Path globalFile;

    private List<Waypoint> waypoints = new ArrayList<>();
    private List<Waypoint> deaths = new ArrayList<>();
    private List<Waypoint> globals = new ArrayList<>();

    private String currentWorldKey = "unknown";

    private WaypointManager() {
        this.configDir = FabricLoader.getInstance().getConfigDir().resolve("deathbeacon");
        this.waypointsFile = configDir.resolve("waypoints.json");
        this.deathsFile = configDir.resolve("deaths.json");
        this.globalFile = configDir.resolve("global.json");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create DeathBeacon config directory", e);
        }
    }

    // ---------------------------------------------------------------
    // World context
    // ---------------------------------------------------------------

    /** Call this whenever the player joins a world/server so data is scoped correctly. */
    public void onWorldChange(String worldKey) {
        if (worldKey == null || worldKey.isEmpty()) worldKey = "unknown";
        if (worldKey.equals(this.currentWorldKey) && !waypoints.isEmpty()) {
            return; // already loaded for this world
        }
        this.currentWorldKey = worldKey;
        loadAll();
    }

    public String currentWorldKey() {
        return currentWorldKey;
    }

    // ---------------------------------------------------------------
    // Load / Save
    // ---------------------------------------------------------------

    public void loadAll() {
        waypoints = loadList(waypointsFile).stream()
                .filter(w -> currentWorldKey.equals(w.worldName))
                .collect(Collectors.toCollection(ArrayList::new));
        deaths = loadList(deathsFile).stream()
                .filter(w -> currentWorldKey.equals(w.worldName))
                .collect(Collectors.toCollection(ArrayList::new));
        globals = loadList(globalFile);
    }

    private List<Waypoint> loadList(Path file) {
        if (!Files.exists(file)) return new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<Waypoint> loaded = gson.fromJson(reader, listType);
            return loaded != null ? loaded : new ArrayList<>();
        } catch (IOException e) {
            // Attempt to fall back to a backup copy if the main file is corrupt.
            Path backup = Paths.get(file.toString() + ".bak");
            if (Files.exists(backup)) {
                try (Reader reader = Files.newBufferedReader(backup, StandardCharsets.UTF_8)) {
                    List<Waypoint> loaded = gson.fromJson(reader, listType);
                    return loaded != null ? loaded : new ArrayList<>();
                } catch (IOException ignored) {
                }
            }
            return new ArrayList<>();
        }
    }

    private void saveList(Path file, List<Waypoint> all) {
        try {
            Files.createDirectories(configDir);
            if (Files.exists(file)) {
                Files.copy(file, Paths.get(file.toString() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            Path tmp = Paths.get(file.toString() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                gson.toJson(all, listType, writer);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("[DeathBeacon] Failed to save " + file + ": " + e.getMessage());
        }
    }

    /**
     * Persists a per-category in-memory list back to disk, merging with any
     * waypoints belonging to other worlds already on disk (since the file on
     * disk holds every world's data, and we only keep the current world's in RAM).
     */
    private void persist(Path file, List<Waypoint> currentWorldList) {
        List<Waypoint> onDisk = loadList(file);
        List<Waypoint> others = onDisk.stream()
                .filter(w -> !currentWorldKey.equals(w.worldName))
                .collect(Collectors.toCollection(ArrayList::new));
        others.addAll(currentWorldList);
        saveList(file, others);
    }

    public void saveWaypoints() {
        persist(waypointsFile, waypoints);
    }

    public void saveDeaths() {
        persist(deathsFile, deaths);
    }

    public void saveGlobals() {
        saveList(globalFile, globals);
    }

    // ---------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public List<Waypoint> getDeaths() {
        return deaths;
    }

    public List<Waypoint> getGlobals() {
        return globals;
    }

    public List<Waypoint> getFavorites() {
        List<Waypoint> all = new ArrayList<>();
        all.addAll(waypoints.stream().filter(w -> w.favorite).toList());
        all.addAll(deaths.stream().filter(w -> w.favorite).toList());
        all.addAll(globals.stream().filter(w -> w.favorite).toList());
        return all;
    }

    public List<Waypoint> byCategory(WaypointCategory category) {
        return switch (category) {
            case WAYPOINTS -> getWaypoints();
            case DEATHS -> getDeaths();
            case FAVORITES -> getFavorites();
            case GLOBAL -> getGlobals();
        };
    }

    /** All waypoints that should be rendered in the world right now (beams/HUD). */
    public List<Waypoint> getAllRenderable() {
        List<Waypoint> all = new ArrayList<>();
        all.addAll(waypoints);
        all.addAll(deaths);
        all.addAll(globals);
        return all.stream().filter(w -> w.visible).collect(Collectors.toList());
    }

    public Waypoint findById(UUID id) {
        for (Waypoint w : getAllRenderable()) {
            if (w.id.equals(id)) return w;
        }
        for (Waypoint w : waypoints) if (w.id.equals(id)) return w;
        for (Waypoint w : deaths) if (w.id.equals(id)) return w;
        for (Waypoint w : globals) if (w.id.equals(id)) return w;
        return null;
    }

    // ---------------------------------------------------------------
    // Mutations
    // ---------------------------------------------------------------

    public void addWaypoint(Waypoint w) {
        w.worldName = currentWorldKey;
        w.isDeath = false;
        w.isGlobal = false;
        waypoints.add(w);
        saveWaypoints();
    }

    public void addGlobal(Waypoint w) {
        w.isGlobal = true;
        w.isDeath = false;
        globals.add(w);
        saveGlobals();
    }

    /** Records an automatic death waypoint. Called from the death-detection hook. */
    public Waypoint recordDeath(String playerName, double x, double y, double z, String dimension, int color, int maxDeaths) {
        int nextNumber = deaths.stream().mapToInt(d -> d.deathNumber).max().orElse(0) + 1;
        Waypoint w = Waypoint.create("Death #" + nextNumber, x, y, z, dimension, currentWorldKey, color);
        w.isDeath = true;
        w.deathNumber = nextNumber;
        w.description = playerName + " died";
        deaths.add(w);

        if (maxDeaths > 0) {
            while (deaths.size() > maxDeaths) {
                // Oldest non-favorite first; per spec "no automatic deletion" is the
                // default (maxDeaths <= 0 disables this entirely).
                Waypoint oldest = deaths.stream()
                        .filter(d -> !d.favorite)
                        .min((a, b) -> Long.compare(a.createdAt, b.createdAt))
                        .orElse(null);
                if (oldest == null) break;
                deaths.remove(oldest);
            }
        }

        saveDeaths();
        return w;
    }

    public boolean delete(UUID id) {
        boolean removed = waypoints.removeIf(w -> w.id.equals(id));
        if (removed) { saveWaypoints(); return true; }
        removed = deaths.removeIf(w -> w.id.equals(id));
        if (removed) { saveDeaths(); return true; }
        removed = globals.removeIf(w -> w.id.equals(id));
        if (removed) { saveGlobals(); return true; }
        return false;
    }

    public void update(Waypoint updated) {
        updated.touch();
        saveOwningList(updated);
    }

    public void toggleFavorite(UUID id) {
        Waypoint w = findById(id);
        if (w == null) return;
        w.favorite = !w.favorite;
        w.touch();
        saveOwningList(w);
    }

    public void toggleVisible(UUID id) {
        Waypoint w = findById(id);
        if (w == null) return;
        w.visible = !w.visible;
        w.touch();
        saveOwningList(w);
    }

    private void saveOwningList(Waypoint w) {
        if (w.isDeath) {
            saveDeaths();
        } else if (w.isGlobal) {
            saveGlobals();
        } else {
            saveWaypoints();
        }
    }
          }
