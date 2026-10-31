package nightfall;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import nightfall.core.*;
import nightfall.core.Module;
import nightfall.gui.Hud;
import nightfall.gui.NoticeManager;
import nightfall.visuals.Esp;
import nightfall.analyzer.OGBaseFinder;
import org.lwjgl.glfw.GLFW;
public class NightfallClient implements ClientModInitializer {
  public static boolean sound = true;
  private static final Map<Module, Boolean> down = new HashMap<>();
  @Override public void onInitializeClient() {
    Modules.init(); Config.load();
    FabricLoader.getInstance().getModContainer("nightfall").ifPresent(container ->
      ResourceManagerHelper.registerBuiltinResourcePack(Identifier.fromNamespaceAndPath("nightfall", "nightfall_cs_knife"), container,
        Component.literal("Nightfall CS Knife"), ResourcePackActivationType.NORMAL));
    HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, Identifier.fromNamespaceAndPath("nightfall", "hud"), (g, dt) -> { Esp.render(g, dt); Hud.render(g, dt); });
    ClientTickEvents.END_CLIENT_TICK.register(NightfallClient::tick);
    Runtime.getRuntime().addShutdownHook(new Thread(Config::save));
  }
  public static void click(boolean on) {
    if (!sound || Modules.SOUND == null || !Modules.SOUND.on("Enabled")) return;
    int style = Modules.SOUND.s("Style").i();
    float volume = (float) Modules.SOUND.s("Volume").d();
    SoundEvent event = switch (style) {
      case 1 -> SoundEvents.LEVER_CLICK;
      case 2 -> SoundEvents.NOTE_BLOCK_PLING.value();
      default -> SoundEvents.UI_BUTTON_CLICK.value();
    };
    float pitch = switch (style) {
      case 0 -> on ? 0.95f : 0.82f; // Soft
      case 1 -> on ? 1.10f : 0.92f; // Mechanical
      case 2 -> on ? 1.55f : 1.25f; // Digital
      default -> on ? 1.35f : 1.15f; // Minimal
    };
    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
  }
  private static void tick(Minecraft mc) {
    Esp.tick(mc); OGBaseFinder.tick(mc); PlayerWatch.tick(mc);
    if (mc.player == null) { Cam.freelook = Cam.freecam = false; Modules.FREELOOK.enabled = Modules.FREECAM.enabled = false; return; }
    long w = mc.getWindow().handle();
    for (Module m : Modules.ALL) {
      if (m.key < 0) continue;
      boolean d = GLFW.glfwGetKey(w, m.key) == GLFW.GLFW_PRESS, was = down.getOrDefault(m, false);
      down.put(m, d);
      if (mc.screen != null) { if (m.hold && m.enabled && !d) m.setEnabled(false); continue; }
      if (m.hold) { if (d != was) m.setEnabled(d); } else if (d && !was) m.toggle();
    }
  }
}
