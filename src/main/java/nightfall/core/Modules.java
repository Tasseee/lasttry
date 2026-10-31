package nightfall.core;
import java.util.*;
import net.minecraft.client.Minecraft;
import nightfall.gui.ClickGui;
import nightfall.gui.ChunkMap;
import static nightfall.core.Setting.*;
public final class Modules {
  public static final List<Module> ALL = new ArrayList<>();
  public static Module FREELOOK, FREECAM, HUD, GUI, PESP, EESP, STORAGE, SPAWNER, VIEW, BASE, MAP, SOUND, OPSEC, ITEM, BLOCK_HIGHLIGHT, SLOW_SWING, NOTIFIER, STAFF, ANTIVANISH;
  private static Module add(Module m, Setting... s) { m.settings.addAll(List.of(s)); ALL.add(m); return m; }
  public static void init() {
    FREELOOK = add(new Module("Freelook", "Kamera frei drehen, Laufrichtung bleibt", Category.MOVEMENT, 86) {
      public void onEnable() { if (!Cam.active()) Cam.start(); Cam.freelook = true; }
      public void onDisable() { Cam.freelook = false; } });
    FREELOOK.hold = true;
    FREECAM = add(new Module("Freecam", "Freie Kamera (WASD, Space, Shift)", Category.MOVEMENT, 67) {
      public void onEnable() { Cam.start(); Cam.freecam = true; Cam.speed = s("Speed").d(); }
      public void onDisable() { Cam.freecam = false; } }, num("Speed", 12, 2, 60, 1));
    PESP = add(new Module("Player ESP", "Box, Name, Distanz, Leben und Tracer fuer Spieler", Category.VISUALS, -1) {},
      bool("Box", true), bool("Name", true), bool("Distance", true), bool("Health", true), bool("Tracer", false),
      num("Range", 128, 16, 256, 8), color("Color", 0.75), bool("Notify", false));
    EESP = add(new Module("Entity Intelligence", "Villager, Trader, Rahmen, Armorstand, Minecart, Allay", Category.VISUALS, -1) {},
      bool("Villager", true), bool("Wandering Trader", true), bool("Item Frame", true), bool("Glow Item Frame", true),
      bool("Armor Stand", true), bool("Minecart", true), bool("Allay", true),
      bool("Box", true), bool("Name", true), bool("Tracer", false), num("Range", 96, 16, 256, 8), color("Color", 0.55), bool("Notify", false));
    STORAGE = add(new Module("Storage ESP", "Truhen, Barrels, Shulker in geladenen Chunks", Category.VISUALS, -1) {},
      bool("Chest", true), bool("Trapped Chest", true), bool("Barrel", true), bool("Shulker Box", true), bool("Ender Chest", false),
      bool("Box", true), bool("Tracer", false), num("Range", 96, 16, 256, 8), bool("Notify", false));
    SPAWNER = add(new Module("Spawner ESP", "Zeigt geladene Spawner; bekannte Fake-Spawner werden gefiltert", Category.VISUALS, -1) {},
      bool("Box", true), bool("Tracer", false), num("Range", 96, 16, 256, 8), color("Color", 0.12), bool("Notify", false));
    VIEW = add(new Module("Viewmodel", "Position, Groesse und Neigung von Hand und Item", Category.VISUALS, -1) {},
      mode("Preset", 0, "Custom", "Knife", "Low"), num("X", 0, -1, 1, 0.01), num("Y", 0, -1, 1, 0.01), num("Z", 0, -1, 1, 0.01),
      num("Scale", 1, 0.3, 1.6, 0.01), num("Rot X", 0, -90, 90, 1), num("Rot Y", 0, -90, 90, 1), num("Rot Z", 0, -90, 90, 1));
    BASE = add(new Module("OG Base Finder", "Bewertet geladene Chunks (Lager, Redstone, Bauten) mit Score und Konfidenz", Category.BASE_FINDING, -1) {},
      num("Min Score", 40, 10, 90, 5), num("Min Evidence", 2, 1, 5, 1), num("Scan Speed", 2, 1, 8, 1), num("Range", 12, 4, 24, 1),
      bool("Natural Filter", true), bool("Show Decoys", false), bool("Ignore Bedrock Corner Spawners", true),
      num("Decoy Y Range", 4, 0, 16, 1), bool("Overlay", true), bool("Notify", true), bool("Auto Clear RTP", true), num("RTP Threshold", 128, 64, 512, 16), bool("Show Normal Chunks", true), num("Overlay Range", 6, 2, 16, 1));
    MAP = add(new Module("Chunk Explorer", "Oeffnet die Chunk-Karte mit Scores und Details", Category.BASE_FINDING, -1) {
      public void onEnable() { enabled = false; Minecraft.getInstance().setScreen(new ChunkMap()); } });
    HUD = add(new Module("HUD", "Watermark, Modulliste, Koordinaten", Category.VISUALS, -1) {});
    HUD.enabled = true;
    GUI = add(new Module("ClickGUI", "Oeffnet das Menue", Category.MISC, 344) {
      public void onEnable() { enabled = false; Minecraft.getInstance().setScreen(new ClickGui()); } });
    SOUND = add(new Module("GUI Sounds", "Klick-Sound, Stil und Lautstaerke", Category.MISC, -1) {},
      bool("Enabled", true), mode("Style", 0, "Soft", "Mechanical", "Digital", "Minimal"), num("Volume", 0.65, 0, 1, 0.05));
    OPSEC = add(new Module("Streamer Privacy", "Lokaler Datenschutz fuer HUD und Stream-Aufnahmen", Category.OPSEC, -1) {},
      bool("Hide Coordinates", true), bool("Hide Module List", true), bool("Hide FPS", true));
    ITEM = add(new Module("Item ESP", "Markiert fallengelassene Items", Category.VISUALS, -1) {},
      bool("Box", true), bool("Name", true), bool("Tracer", false), bool("Notify", false),
      num("Range", 96, 8, 256, 8), color("Color", 0.78));
    BLOCK_HIGHLIGHT = add(new Module("Block Highlight", "Weisse, weich gleitende 3D-Outline fuer den anvisierten Block", Category.VISUALS, -1) {},
      bool("Enabled", true), num("Smoothness", 0.35, 0.05, 1.0, 0.05));
    BLOCK_HIGHLIGHT.enabled = true;
    SLOW_SWING = add(new Module("Slow Swing", "Stellt die lokale Hand-Swing-Animation langsamer dar", Category.VISUALS, -1) {},
      num("Speed", 0.65, 0.15, 1.0, 0.05));
    NOTIFIER = add(new Module("Notifier", "Zentrale, einstellbare Nightfall-Benachrichtigungen", Category.MISC, -1) {},
      bool("Enabled", true), bool("Sound", true), num("Duration", 4, 1, 10, 1), num("Cooldown", 5, 1, 30, 1));
    NOTIFIER.enabled = true;
    STAFF = add(new Module("Staff List", "Tab-Informationen und manuell gepflegte Staff-Hinweise", Category.MISC, -1) {},
      bool("Show Known Staff", true), bool("Notify", false));
    ANTIVANISH = add(new Module("AntiVanish", "Zeigt nur serverseitig sichtbare Hinweise; erkennt keine unsichtbaren Daten", Category.MISC, -1) {},
      bool("Notify", false));
  }
}
