package ablivity.dev.not_an_old_pulse.client.gui.screen;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.gui.screen.TitleScreen;
import java.io.FileWriter;
public class TestReflection {
    public static void run() {
        try (FileWriter fw = new FileWriter("titlescreen_methods.txt")) {
            for (Method m : TitleScreen.class.getDeclaredMethods()) {
                fw.write(m.getName() + " " + m.getReturnType().getName() + " (");
                for (Class<?> p : m.getParameterTypes()) fw.write(p.getName() + ",");
                fw.write(")\n");
            }
            fw.write("\nFields:\n");
            for (Field f : TitleScreen.class.getDeclaredFields()) {
                fw.write(f.getName() + " " + f.getType().getName() + "\n");
            }
        } catch (Exception e) {}
    }
}

