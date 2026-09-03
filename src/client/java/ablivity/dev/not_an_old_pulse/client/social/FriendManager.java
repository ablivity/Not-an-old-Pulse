package ablivity.dev.not_an_old_pulse.client.social;

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

public class FriendManager {
    private static final Gson GSON = new Gson();
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "not_an_old_pulse_friends.json");
    private static final List<String> FRIENDS = new ArrayList<>();

    static {
        load();
    }

    public static synchronized void load() {
        if (!FILE.exists()) return;
        try (FileReader reader = new FileReader(FILE)) {
            Type type = new TypeToken<List<String>>() {}.getType();
            List<String> list = GSON.fromJson(reader, type);
            if (list != null) {
                FRIENDS.clear();
                FRIENDS.addAll(list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(FRIENDS, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized boolean isFriend(String name) {
        if (name == null) return false;
        for (String f : FRIENDS) {
            if (f.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public static synchronized void addFriend(String name) {
        if (name == null || name.isBlank()) return;
        if (!isFriend(name)) {
            FRIENDS.add(name.trim());
            save();
        }
    }

    public static synchronized void removeFriend(String name) {
        if (name == null) return;
        FRIENDS.removeIf(f -> f.equalsIgnoreCase(name.trim()));
        save();
    }

    public static synchronized List<String> getFriends() {
        return Collections.unmodifiableList(new ArrayList<>(FRIENDS));
    }
}
