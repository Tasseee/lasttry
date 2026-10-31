package nightfall.core;
import com.google.gson.*;
import java.nio.file.*; import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import nightfall.NightfallClient;
public final class Config {
  static class E { int key; boolean hold, enabled; Map<String, Double> s = new LinkedHashMap<>(); }
  static class F { boolean sound = true; Map<String, E> mods = new LinkedHashMap<>(); }
  private static final Gson G = new GsonBuilder().setPrettyPrinting().create();
  private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("nightfall.json"); }
  public static void save() {
    try {
      F f = new F(); f.sound = NightfallClient.sound;
      for (Module m : Modules.ALL) { E e = new E(); e.key = m.key; e.hold = m.hold; e.enabled = m.enabled;
        for (Setting s : m.settings) e.s.put(s.name, s.val); f.mods.put(m.name, e); }
      Files.writeString(file(), G.toJson(f));
    } catch (Exception ex) { System.err.println("[Nightfall] save failed: " + ex); }
  }
  public static void load() {
    try {
      if (!Files.exists(file())) return;
      F f = G.fromJson(Files.readString(file()), F.class); if (f == null) return;
      NightfallClient.sound = f.sound;
      for (Module m : Modules.ALL) { E e = f.mods.get(m.name); if (e == null) continue;
        m.key = e.key; m.hold = e.hold; if (m == Modules.HUD) m.enabled = e.enabled;
        for (Setting s : m.settings) { Double v = e.s == null ? null : e.s.get(s.name); if (v != null) s.val = v; } }
    } catch (Exception ex) { System.err.println("[Nightfall] load failed: " + ex); }
  }
}
