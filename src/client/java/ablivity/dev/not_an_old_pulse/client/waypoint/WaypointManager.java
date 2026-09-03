package ablivity.dev.not_an_old_pulse.client.waypoint;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WaypointManager {
    public static class Waypoint {
        public String name;
        public double x, y, z;
        public String dimension;
        public String color;
        public boolean visible = true;

        public Waypoint(String name, double x, double y, double z, String dimension, String color) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.color = color;
        }
    }

    private static final Gson GSON = new Gson();
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "not_an_old_pulse_waypoints.json");
    private static final List<Waypoint> WAYPOINTS = new ArrayList<>();

    static {
        load();
    }

    public static synchronized void load() {
        if (!FILE.exists()) return;
        try (FileReader reader = new FileReader(FILE)) {
            Type type = new TypeToken<List<Waypoint>>() {}.getType();
            List<Waypoint> list = GSON.fromJson(reader, type);
            if (list != null) {
                WAYPOINTS.clear();
                WAYPOINTS.addAll(list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(WAYPOINTS, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void addWaypoint(String name, double x, double y, double z, String dimension, String color) {
        WAYPOINTS.add(new Waypoint(name, x, y, z, dimension, color));
        save();
    }

    public static synchronized void deleteWaypoint(String name) {
        WAYPOINTS.removeIf(w -> w.name.equalsIgnoreCase(name));
        save();
    }

    public static synchronized void setVisible(String name, boolean visible) {
        for (Waypoint w : WAYPOINTS) {
            if (w.name.equalsIgnoreCase(name)) {
                w.visible = visible;
                break;
            }
        }
        save();
    }

    public static synchronized List<Waypoint> getWaypoints() {
        return Collections.unmodifiableList(new ArrayList<>(WAYPOINTS));
    }
}
