package nightfall.analyzer;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import nightfall.core.Modules;
/** Bewertet einen geladenen Chunk anhand sichtbarer Daten (Block-Entities und Bloecke). */
public final class ChunkAnalyzer {
  static Set<Block> set(Block... b) { return new HashSet<>(Arrays.asList(b)); }
  static final Set<Block> LIGHT = set(Blocks.TORCH, Blocks.WALL_TORCH, Blocks.LANTERN, Blocks.SOUL_LANTERN, Blocks.END_ROD, Blocks.SEA_LANTERN);
  static final Set<Block> RS = set(Blocks.HOPPER, Blocks.DROPPER, Blocks.DISPENSER, Blocks.OBSERVER, Blocks.PISTON, Blocks.STICKY_PISTON,
    Blocks.REPEATER, Blocks.COMPARATOR, Blocks.REDSTONE_LAMP, Blocks.REDSTONE_BLOCK, Blocks.TNT, Blocks.LEVER);
  static final Set<Block> ART = set(Blocks.GLASS, Blocks.GLASS_PANE, Blocks.STONE_BRICKS, Blocks.BRICKS, Blocks.SMOOTH_STONE, Blocks.POLISHED_ANDESITE,
    Blocks.POLISHED_DIORITE, Blocks.POLISHED_GRANITE, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.IRON_BLOCK,
    Blocks.GOLD_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER,
    Blocks.IRON_DOOR, Blocks.WHITE_CONCRETE, Blocks.GRAY_CONCRETE, Blocks.BLACK_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.SCAFFOLDING);
  static final Set<Block> SPECIAL = set(Blocks.COBWEB, Blocks.MOSSY_COBBLESTONE, Blocks.BELL, Blocks.END_PORTAL_FRAME, Blocks.REINFORCED_DEEPSLATE,
    Blocks.SCULK_SHRIEKER, Blocks.NETHERITE_BLOCK, Blocks.BEACON);
  static boolean interesting(BlockState s) {
    Block b = s.getBlock();
    return LIGHT.contains(b) || RS.contains(b) || ART.contains(b) || SPECIAL.contains(b) || s.is(BlockTags.RAILS);
  }

  public static Result analyze(Level lv, LevelChunk ch, int tick) {
    var m = Modules.BASE; boolean nat = m.on("Natural Filter");
    int storage = 0, shulk = 0, spawn = 0, decoy = 0, trial = 0; double[] an = null;
    for (BlockEntity be : ch.getBlockEntities().values()) {
      var t = be.getType(); BlockPos p = be.getBlockPos();
      if (t == BlockEntityType.MOB_SPAWNER) {
        if (FakeSpawnerFilter.isKnownFakeSpawner(lv, p)) decoy++;
        else { spawn++; if (an == null) an = new double[] { p.getX() + .5, p.getY() + .5, p.getZ() + .5 }; }
      } else if (t == BlockEntityType.CHEST || t == BlockEntityType.TRAPPED_CHEST || t == BlockEntityType.BARREL || t == BlockEntityType.SHULKER_BOX || t == BlockEntityType.ENDER_CHEST) {
        storage++; if (t == BlockEntityType.SHULKER_BOX) shulk++;
        if (an == null) an = new double[] { p.getX() + .5, p.getY() + .5, p.getZ() + .5 };
      } else if (t == BlockEntityType.TRIAL_SPAWNER || t == BlockEntityType.VAULT) trial++;
    }
    int light = 0, rs = 0, art = 0, web = 0, rail = 0, moss = 0, bell = 0, frame = 0, reinf = 0, shriek = 0, valuable = 0;
    LevelChunkSection[] secs = ch.getSections();
    for (LevelChunkSection sec : secs) {
      if (sec.hasOnlyAir() || !sec.maybeHas(ChunkAnalyzer::interesting)) continue;
      for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
        BlockState s = sec.getBlockState(x, y, z); if (s.isAir()) continue;
        Block b = s.getBlock();
        if (LIGHT.contains(b)) light++; else if (RS.contains(b)) rs++; else if (ART.contains(b)) art++;
        else if (b == Blocks.COBWEB) web++; else if (b == Blocks.MOSSY_COBBLESTONE) moss++; else if (b == Blocks.BELL) bell++;
        else if (b == Blocks.END_PORTAL_FRAME) frame++; else if (b == Blocks.REINFORCED_DEEPSLATE) reinf++; else if (b == Blocks.SCULK_SHRIEKER) shriek++;
        else if (b == Blocks.NETHERITE_BLOCK || b == Blocks.BEACON) valuable++;
        else if (s.is(BlockTags.RAILS)) rail++;
      }
    }
    int score = 0, ev = 0, naturals = 0; List<String> why = new ArrayList<>();
    if (storage >= 1) { int p = storage >= 10 ? 40 : storage >= 4 ? 25 : 10; score += p; ev++; why.add("Lagerbloecke: " + storage + " (+" + p + ")"); }
    if (shulk >= 1) { score += 10; ev++; why.add("Shulker-Boxen: " + shulk + " (+10)"); }
    if (rs >= 3) { int p = rs >= 10 ? 20 : 10; score += p; ev++; why.add("Redstone/Hopper: " + rs + " (+" + p + ")"); }
    if (art + light >= 40) { int p = art + light >= 400 ? 30 : art + light >= 150 ? 20 : 10; score += p; ev++; why.add("Kuenstliche Bloecke: " + (art + light) + " (+" + p + ")"); }
    if (light >= 6) { score += 8; ev++; why.add("Beleuchtung: " + light + " (+8)"); }
    if (valuable >= 1) { score += 20; ev++; why.add("Netherite/Beacon: " + valuable + " (+20)"); }
    boolean dungeon = spawn > 0 && moss > 0;
    if (spawn > 0 && !(nat && (dungeon || trial > 0))) { score += 10; ev++; why.add("Spawner: " + spawn + " (+10)"); }
    if (nat) {
      if (web > 0 && rail > 0) { score -= 35; naturals++; why.add("Natuerlich: Mineshaft (-35)"); }
      if (dungeon) { score -= 30; naturals++; why.add("Natuerlich: Dungeon (-30)"); }
      if (bell > 0) { score -= 40; naturals++; why.add("Natuerlich: Dorf (-40)"); }
      if (trial > 0) { score -= 40; naturals++; why.add("Natuerlich: Trial Chamber (-40)"); }
      if (reinf > 0 || shriek > 0) { score -= 40; naturals++; why.add("Natuerlich: Ancient City (-40)"); }
      if (frame > 0) { score -= 40; naturals++; why.add("Natuerlich: Stronghold (-40)"); }
    }
    score = Math.max(0, Math.min(100, score));
    if (decoy > 0) why.add("Fake-Spawner an Bedrock-Chunk-Ecke ignoriert: " + decoy);
    boolean isDecoy = decoy > 0 && score < 25;
    int conf = Math.max(10, Math.min(95, (ev == 0 ? 30 : 35 + 12 * ev) - (naturals > 0 ? 15 : 0)));
    if (an == null) { int cx = ch.getPos().x * 16 + 8, cz = ch.getPos().z * 16 + 8; an = new double[] { cx, lv.getMinY() + 32, cz }; }
    return new Result(ch.getPos().x, ch.getPos().z, score, conf, ev, 100, isDecoy, why, an[0], an[1], an[2], tick);
  }
}
