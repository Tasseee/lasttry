package nightfall.visuals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.levelgen.Heightmap;
import nightfall.analyzer.OGBaseFinder;
import nightfall.analyzer.Result;
import nightfall.core.Modules;

/** Ground-plane chunk overlays projected into the current world view. */
public final class SusOverlay {
  private SusOverlay() {}

  private static void polygon(GuiGraphics g, double[][] p, int fill, int edge) {
    double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
    for (double[] v : p) { minY = Math.min(minY, v[1]); maxY = Math.max(maxY, v[1]); }
    for (int y = (int) minY; y <= (int) maxY; y++) {
      List<Double> xs = new ArrayList<>();
      for (int i = 0; i < p.length; i++) {
        double[] a = p[i], b = p[(i + 1) % p.length];
        if ((a[1] <= y && b[1] > y) || (b[1] <= y && a[1] > y)) xs.add(a[0] + (y - a[1]) * (b[0] - a[0]) / (b[1] - a[1]));
      }
      Collections.sort(xs);
      for (int i = 0; i + 1 < xs.size(); i += 2) g.fill((int) xs.get(i), y, (int) xs.get(i + 1) + 1, y + 1, fill);
    }
    for (int i = 0; i < p.length; i++) Esp.line(g, p[i][0], p[i][1], p[(i + 1) % p.length][0], p[(i + 1) % p.length][1], edge);
  }

  public static void draw(GuiGraphics g, Font f, Minecraft mc) {
    var m = Modules.BASE;
    if (!m.enabled || !m.on("Overlay") || mc.level == null) return;
    for (Result r : OGBaseFinder.results.values()) {
      int pcx = mc.player.getBlockX() >> 4, pcz = mc.player.getBlockZ() >> 4;
      int chunkDistance = Math.max(Math.abs(r.cx() - pcx), Math.abs(r.cz() - pcz));
      if (!OGBaseFinder.shown(r) && (!m.on("Show Normal Chunks") || chunkDistance > m.s("Overlay Range").i())) continue;
      int bx = r.cx() * 16, bz = r.cz() * 16;
      int[][] corners = {{bx, bz}, {bx + 16, bz}, {bx + 16, bz + 16}, {bx, bz + 16}};
      double[][] pts = new double[4][2]; boolean visible = true;
      for (int i = 0; i < 4; i++) {
        int x = corners[i][0], z = corners[i][1];
        int y = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        double[] q = Esp.proj(x, y + 0.08, z);
        if (q == null) { visible = false; break; }
        pts[i] = q;
      }
      if (!visible) continue;
      int c = OGBaseFinder.color(r);
      polygon(g, pts, (c & 0x00FFFFFF) | 0x36000000, (c & 0x00FFFFFF) | 0xE0000000);
      double ax = bx + 8, az = bz + 8;
      int ay = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) ax, (int) az);
      double[] s = Esp.proj(ax, ay + 0.25, az); if (s == null) continue;
      double d = Math.sqrt((ax - mc.player.getX()) * (ax - mc.player.getX()) + (az - mc.player.getZ()) * (az - mc.player.getZ()));
      String l = "SUS " + r.score() + "  " + String.format("%.0fm", d); int w = f.width(l);
      int x = (int) s[0], y = (int) s[1];
      g.fill(x - w / 2 - 3, y - 13, x + w / 2 + 3, y - 2, 0xD9000000);
      g.drawString(f, l, x - w / 2, y - 11, 0xFFF4F0FA, false);
    }
  }
}
