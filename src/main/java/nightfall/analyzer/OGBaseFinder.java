package nightfall.analyzer;
import java.util.*; import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import nightfall.NightfallClient;
import nightfall.core.Modules;
import nightfall.gui.NoticeManager;
/** Zentrale Steuerung: scannt geladene Chunks inkrementell (Zeitbudget pro Tick) und sammelt Ergebnisse. */
public final class OGBaseFinder {
  public static final Map<Long, Result> results = new ConcurrentHashMap<>();
  static final Map<Long, Integer> seen = new HashMap<>(); static final Set<Long> notified = new HashSet<>();
  static String dim = ""; static int offR = -1; static List<int[]> offs = new ArrayList<>();
  static double lastX = Double.NaN, lastZ = Double.NaN;
  public static boolean shown(Result r) {
    var m = Modules.BASE;
    if (r.decoy()) return m.on("Show Decoys");
    return r.score() >= m.s("Min Score").i() && r.evidence() >= m.s("Min Evidence").i();
  }
  public static int color(Result r) {
    if (r.decoy()) return 0xFF6B6B7B;
    return r.score() < 25 ? 0xFF22C55E : r.score() < 50 ? 0xFFEAB308 : r.score() < 75 ? 0xFFF97316 : 0xFFEF4444;
  }
  public static void clearForTeleport() {
    results.clear(); seen.clear(); notified.clear();
  }
  public static void tick(Minecraft mc) {
    var m = Modules.BASE;
    if (mc.level == null || mc.player == null) { lastX = lastZ = Double.NaN; return; }
    String d = mc.level.dimension().toString();
    if (!d.equals(dim)) { dim = d; clearForTeleport(); lastX = lastZ = Double.NaN; }
    double px = mc.player.getX(), pz = mc.player.getZ();
    if (!Double.isNaN(lastX) && m.on("Auto Clear RTP")) {
      double threshold = m.s("RTP Threshold").d();
      if ((px - lastX) * (px - lastX) + (pz - lastZ) * (pz - lastZ) > threshold * threshold) {
        clearForTeleport();
        NoticeManager.push("Gebiet gewechselt", "Alte Chunk-Markierungen entfernt", 0xFFA855F7);
      }
    }
    lastX = px; lastZ = pz;
    if (!m.enabled) return;
    int R = m.s("Range").i(), budget = m.s("Scan Speed").i(), t = mc.player.tickCount;
    if (R != offR) { offR = R; offs = new ArrayList<>();
      for (int a = -R; a <= R; a++) for (int b = -R; b <= R; b++) offs.add(new int[] { a, b });
      offs.sort(Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1])); }
    int pcx = mc.player.getBlockX() >> 4, pcz = mc.player.getBlockZ() >> 4;
    for (int[] o : offs) {
      if (budget <= 0) break;
      int cx = pcx + o[0], cz = pcz + o[1]; long k = ChunkPos.asLong(cx, cz);
      Result old = results.get(k); if (old != null && t - old.tick() < 6000) continue;
      var ch = mc.level.getChunkSource().getChunkNow(cx, cz); if (ch == null) continue;
      int fs = seen.computeIfAbsent(k, x -> t); if (t - fs < 40) continue; // Block-Entities treffen leicht verzoegert ein
      Result res = ChunkAnalyzer.analyze(mc.level, ch, t); results.put(k, res); budget--;
      if (m.on("Notify") && !res.decoy() && shown(res)) {
        boolean near = false;
        for (long n : notified) if (Math.max(Math.abs(ChunkPos.getX(n) - cx), Math.abs(ChunkPos.getZ(n) - cz)) <= 3) { near = true; break; }
        if (!near) { notified.add(k);
          mc.player.displayClientMessage(Component.literal("[Nightfall] Auffaelliger Chunk bei " + (cx * 16) + " " + (cz * 16) + " | Score " + res.score() + " | Konfidenz " + res.conf() + "%"), false);
          NoticeManager.push("Sus-Chunk gefunden", (cx * 16) + ", " + (cz * 16) + " | Score " + res.score(), color(res)); }
      }
    }
  }
}
