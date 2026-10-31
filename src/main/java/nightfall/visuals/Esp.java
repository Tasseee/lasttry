package nightfall.visuals;
import java.util.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
import nightfall.core.*;
import nightfall.analyzer.FakeSpawnerFilter;
import nightfall.gui.Hud;
import nightfall.gui.NoticeManager;
import nightfall.core.Module;
/** 2D-Projektions-ESP: rechnet Weltpunkte selbst auf den Bildschirm um (kein Eingriff in die 3D-Render-Pipeline). */
public final class Esp {
  record Hit(AABB box, int type) {}
  static volatile List<Hit> storage = List.of();
  static volatile List<Hit> spawners = List.of();
  static final EntityType<?>[] ET = { EntityType.VILLAGER, EntityType.WANDERING_TRADER, EntityType.ITEM_FRAME, EntityType.GLOW_ITEM_FRAME, EntityType.ARMOR_STAND, EntityType.MINECART, EntityType.ALLAY };
  static final String[] EN = { "Villager", "Wandering Trader", "Item Frame", "Glow Item Frame", "Armor Stand", "Minecart", "Allay" };
  static final String[] SN = { "Chest", "Trapped Chest", "Barrel", "Shulker Box", "Ender Chest" };
  static final int[] SC = { 0xFFE8A33D, 0xFFE5484D, 0xFF9C6B3C, 0xFFE879F9, 0xFF22D3EE };
  static double cx, cy, cz, fx, fy, fz, rx, rz, ux, uy, uz, th, asp; static int W, H, tracers;
  static final Set<java.util.UUID> notifiedItems = new HashSet<>();
  static final Set<String> notifiedBlocks = new HashSet<>();
  static double[] smoothBlock;
  static final Set<java.util.UUID> notifiedEntities = new HashSet<>();

  public static void tick(Minecraft mc) {
    boolean scanStorage = Modules.STORAGE.enabled;
    boolean scanSpawners = Modules.SPAWNER.enabled;
    boolean scanItems = Modules.ITEM != null && Modules.ITEM.enabled && Modules.ITEM.on("Notify");
    if (mc.level == null || mc.player == null) { storage = List.of(); spawners = List.of(); notifiedItems.clear(); notifiedBlocks.clear(); notifiedEntities.clear(); return; }
    if (mc.player.tickCount % 20 != 0) return;
    for (Entity e : mc.level.entitiesForRendering()) {
      double d2 = e.distanceToSqr(mc.player);
      if (scanItems && e instanceof ItemEntity item && d2 <= Modules.ITEM.s("Range").d() * Modules.ITEM.s("Range").d() && notifiedItems.add(item.getUUID())) {
        String itemName = item.getItem().getHoverName().getString();
        NoticeManager.push("Item entdeckt", itemName, Modules.ITEM.s("Color").rgb());
      }
      if (Modules.PESP.enabled && Modules.PESP.on("Notify") && e.getType() == EntityType.PLAYER && d2 <= Modules.PESP.s("Range").d() * Modules.PESP.s("Range").d() && notifiedEntities.add(e.getUUID()))
        NoticeManager.push("Spieler entdeckt", e.getName().getString(), Modules.PESP.s("Color").rgb());
      if (Modules.EESP.enabled && Modules.EESP.on("Notify") && d2 <= Modules.EESP.s("Range").d() * Modules.EESP.s("Range").d() && notifiedEntities.add(e.getUUID())) {
        for (int i = 0; i < ET.length; i++) if (e.getType() == ET[i] && Modules.EESP.on(EN[i])) { NoticeManager.push("Entity entdeckt", EN[i], Modules.EESP.s("Color").rgb()); break; }
      }
    }
    if (!scanStorage && !scanSpawners) return;
    int range = Math.max(scanStorage ? Modules.STORAGE.s("Range").i() : 0, scanSpawners ? Modules.SPAWNER.s("Range").i() : 0);
    int r = (int) Math.ceil(range / 16.0), pcx = mc.player.getBlockX() >> 4, pcz = mc.player.getBlockZ() >> 4;
    List<Hit> out = new ArrayList<>(), spawnerOut = new ArrayList<>();
    for (int a = -r; a <= r; a++) for (int b = -r; b <= r; b++) {
      var ch = mc.level.getChunkSource().getChunkNow(pcx + a, pcz + b); if (ch == null) continue;
      for (var be : ch.getBlockEntities().values()) {
        var t = be.getType();
        if (scanStorage) {
          int k = t == BlockEntityType.CHEST ? 0 : t == BlockEntityType.TRAPPED_CHEST ? 1 : t == BlockEntityType.BARREL ? 2 : t == BlockEntityType.SHULKER_BOX ? 3 : t == BlockEntityType.ENDER_CHEST ? 4 : -1;
          if (k >= 0 && Modules.STORAGE.on(SN[k])) out.add(new Hit(new AABB(be.getBlockPos()), k));
        }
        if (scanSpawners && t == BlockEntityType.MOB_SPAWNER && !FakeSpawnerFilter.isKnownFakeSpawner(mc.level, be.getBlockPos()))
          spawnerOut.add(new Hit(new AABB(be.getBlockPos()), 0));
      }
    }
    storage = out;
    spawners = spawnerOut;
    if (scanStorage && Modules.STORAGE.on("Notify")) for (Hit h : out) {
      String id = "s:" + h.box.getCenter().x + ":" + h.box.getCenter().y + ":" + h.box.getCenter().z;
      if (notifiedBlocks.add(id)) NoticeManager.push("Block entdeckt", SN[h.type], SC[h.type]);
    }
    if (scanSpawners && Modules.SPAWNER.on("Notify")) for (Hit h : spawnerOut) {
      String id = "p:" + h.box.getCenter().x + ":" + h.box.getCenter().y + ":" + h.box.getCenter().z;
      if (notifiedBlocks.add(id)) NoticeManager.push("Spawner entdeckt", "Geladener Chunk", Modules.SPAWNER.s("Color").rgb());
    }
    if (notifiedBlocks.size() > 4096) notifiedBlocks.clear();
    if (notifiedItems.size() > 2048) notifiedItems.clear();
  }

  static void setup(Minecraft mc, float pt) {
    var p = mc.player;
    if (Cam.freecam) { cx = Cam.x; cy = Cam.y; cz = Cam.z; } else { var v = p.getEyePosition(pt); cx = v.x; cy = v.y; cz = v.z; }
    float yaw = Cam.active() ? Cam.yaw : p.getViewYRot(pt), pitch = Cam.active() ? Cam.pitch : p.getViewXRot(pt);
    double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
    fx = -Math.sin(yr) * Math.cos(pr); fy = -Math.sin(pr); fz = Math.cos(yr) * Math.cos(pr);
    rx = -Math.cos(yr); rz = -Math.sin(yr);
    ux = -rz * fy; uy = rz * fx - rx * fz; uz = rx * fy;
    th = Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
    W = mc.getWindow().getGuiScaledWidth(); H = mc.getWindow().getGuiScaledHeight(); asp = (double) W / H; tracers = 0;
  }
  static double[] proj(double x, double y, double z) {
    double dx = x - cx, dy = y - cy, dz = z - cz, zc = dx * fx + dy * fy + dz * fz;
    if (zc < 0.05) return null;
    double xc = dx * rx + dz * rz, yc = dx * ux + dy * uy + dz * uz;
    return new double[] { W / 2.0 + xc / (zc * th * asp) * W / 2.0, H / 2.0 - yc / (zc * th) * H / 2.0 };
  }
  static double[] box2d(AABB b) {
    double x1 = 1e9, y1 = 1e9, x2 = -1e9, y2 = -1e9;
    for (int i = 0; i < 8; i++) {
      double[] s = proj((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY, (i & 4) == 0 ? b.minZ : b.maxZ);
      if (s == null) return null;
      x1 = Math.min(x1, s[0]); y1 = Math.min(y1, s[1]); x2 = Math.max(x2, s[0]); y2 = Math.max(y2, s[1]);
    }
    return new double[] { x1, y1, x2, y2 };
  }
  static void line(GuiGraphics g, double x1, double y1, double x2, double y2, int c) {
    double dx = x2 - x1, dy = y2 - y1; int n = (int) Math.min(2500, Math.max(Math.abs(dx), Math.abs(dy)));
    for (int i = 0; i <= n; i++) { int x = (int) (x1 + dx * i / Math.max(n, 1)), y = (int) (y1 + dy * i / Math.max(n, 1)); g.fill(x, y, x + 1, y + 1, c); }
  }
  static void draw(GuiGraphics g, Font f, AABB b, int col, boolean box, String label, boolean tracer, double hp) {
    double[] r = box2d(b); if (r == null) { if (tracer) offscreen(g, b, col, true); return; }
    int x1 = (int) r[0], y1 = (int) r[1], x2 = (int) r[2], y2 = (int) r[3];
    if (x2 < 0 || x1 > W || y2 < 0 || y1 > H) { if (tracer) offscreen(g, b, col, true); return; }
    if (box) { g.fill(x1, y1, x2, y1 + 1, col); g.fill(x1, y2, x2, y2 + 1, col); g.fill(x1, y1, x1 + 1, y2, col); g.fill(x2, y1, x2 + 1, y2 + 1, col); }
    if (hp >= 0) { int h = (int) ((y2 - y1) * hp); g.fill(x1 - 4, y1, x1 - 2, y2, 0xAA000000); g.fill(x1 - 4, y2 - h, x1 - 2, y2, Hud.lerp(0xFFEF4444, 0xFF22C55E, (float) hp)); }
    if (label != null && !label.isEmpty()) {
      int w = f.width(label), lx = (x1 + x2) / 2 - w / 2; g.fill(lx - 2, y1 - 12, lx + w + 2, y1 - 1, 0xA0000000); g.drawString(f, label, lx, y1 - 10, 0xFFFFFFFF, false);
    }
    if (tracer && tracers++ < 40) line(g, W / 2.0, H / 2.0, (x1 + x2) / 2.0, (y1 + y2) / 2.0, col & 0xB0FFFFFF | 0x70000000);
  }

  static void offscreen(GuiGraphics g, AABB b, int col, boolean tracer) {
    if (!tracer) return;
    double dx = b.getCenter().x - cx, dy = b.getCenter().y - cy, dz = b.getCenter().z - cz;
    double zc = dx * fx + dy * fy + dz * fz;
    double xc = dx * rx + dz * rz, yc = dx * ux + dy * uy + dz * uz;
    if (zc < 0) { xc = -xc; yc = -yc; }
    double sx = xc / (Math.max(1.0, Math.abs(zc)) * th * asp) * W / 2.0;
    double sy = -yc / (Math.max(1.0, Math.abs(zc)) * th) * H / 2.0;
    double len = Math.max(1.0, Math.hypot(sx, sy));
    double vx = sx / len, vy = sy / len;
    double edge = Math.min((W / 2.0 - 18) / Math.max(0.001, Math.abs(vx)), (H / 2.0 - 18) / Math.max(0.001, Math.abs(vy)));
    double ex = W / 2.0 + vx * edge, ey = H / 2.0 + vy * edge;
    line(g, W / 2.0, H / 2.0, ex, ey, (col & 0x00FFFFFF) | 0x65000000);
    double px = -vy, py = vx;
    line(g, ex, ey, ex - vx * 8 + px * 5, ey - vy * 8 + py * 5, col);
    line(g, ex, ey, ex - vx * 8 - px * 5, ey - vy * 8 - py * 5, col);
    line(g, ex - vx * 8 + px * 5, ey - vy * 8 + py * 5, ex - vx * 8 - px * 5, ey - vy * 8 - py * 5, col);
  }

  static void drawBlockHighlight(GuiGraphics g, Minecraft mc) {
    if (Modules.BLOCK_HIGHLIGHT == null || !Modules.BLOCK_HIGHLIGHT.enabled || !Modules.BLOCK_HIGHLIGHT.on("Enabled")) return;
    if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) { smoothBlock = null; return; }
    AABB b = new AABB(hit.getBlockPos()); double[] target = new double[16];
    for (int i = 0; i < 8; i++) {
      double[] q = proj((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY, (i & 4) == 0 ? b.minZ : b.maxZ);
      if (q == null) { smoothBlock = null; return; }
      target[i * 2] = q[0]; target[i * 2 + 1] = q[1];
    }
    double k = Modules.BLOCK_HIGHLIGHT.s("Smoothness").d();
    if (smoothBlock == null) smoothBlock = target.clone();
    for (int i = 0; i < target.length; i++) smoothBlock[i] += (target[i] - smoothBlock[i]) * k;
    int white = 0xF2FFFFFF;
    for (int i = 0; i < 8; i++) for (int bit = 0; bit < 3; bit++) if ((i & (1 << bit)) == 0) {
      int j = i | (1 << bit);
      line(g, smoothBlock[i * 2], smoothBlock[i * 2 + 1], smoothBlock[j * 2], smoothBlock[j * 2 + 1], white);
    }
  }

  public static void render(GuiGraphics g, DeltaTracker dt) {
    var mc = Minecraft.getInstance();
    if (mc.player == null || mc.level == null || mc.options.hideGui) return;
    nightfall.core.Module P = Modules.PESP, E = Modules.EESP, S = Modules.STORAGE;
    if (!P.enabled && !E.enabled && !S.enabled && !Modules.SPAWNER.enabled && !Modules.BASE.enabled && !Modules.ITEM.enabled && !Modules.BLOCK_HIGHLIGHT.enabled) return;
    float pt = dt.getGameTimeDeltaPartialTick(false); setup(mc, pt); Font f = mc.font;
    for (Entity e : mc.level.entitiesForRendering()) {
      if (e == mc.player) continue;
      var off = e.getPosition(pt).subtract(e.position()); AABB bb = e.getBoundingBox().move(off);
      double dist = Math.sqrt(bb.getCenter().distanceToSqr(cx, cy, cz));
      if (Modules.ITEM.enabled && e instanceof ItemEntity item && dist <= Modules.ITEM.s("Range").d()) {
        String label = Modules.ITEM.on("Name") ? item.getItem().getHoverName().getString() + " x" + item.getItem().getCount() : "";
        draw(g, f, bb, Modules.ITEM.s("Color").rgb(), Modules.ITEM.on("Box"), label, Modules.ITEM.on("Tracer"), -1);
      } else if (P.enabled && e.getType() == EntityType.PLAYER && dist <= P.s("Range").d()) {
        String l = (P.on("Name") ? e.getName().getString() : "") + (P.on("Distance") ? String.format(" %.0fm", dist) : "");
        double hp = P.on("Health") && e instanceof LivingEntity le ? Math.max(0, Math.min(1, le.getHealth() / le.getMaxHealth())) : -1;
        draw(g, f, bb, P.s("Color").rgb(), P.on("Box"), l.trim(), P.on("Tracer"), hp);
      } else if (E.enabled && dist <= E.s("Range").d()) {
        for (int i = 0; i < ET.length; i++) if (e.getType() == ET[i] && E.on(EN[i]))
          draw(g, f, bb, E.s("Color").rgb(), E.on("Box"), E.on("Name") ? EN[i] : "", E.on("Tracer"), -1);
      }
    }
    if (S.enabled) for (Hit h : storage) {
      if (Math.sqrt(h.box.getCenter().distanceToSqr(cx, cy, cz)) > S.s("Range").d()) continue;
      draw(g, f, h.box, SC[h.type], S.on("Box"), "", S.on("Tracer"), -1);
    }
    nightfall.core.Module SP = Modules.SPAWNER;
    if (SP.enabled) for (Hit h : spawners) {
      if (Math.sqrt(h.box.getCenter().distanceToSqr(cx, cy, cz)) > SP.s("Range").d()) continue;
      draw(g, f, h.box, SP.s("Color").rgb(), SP.on("Box"), "Spawner", SP.on("Tracer"), -1);
    }
    SusOverlay.draw(g, f, mc);
    drawBlockHighlight(g, mc);
  }
}
