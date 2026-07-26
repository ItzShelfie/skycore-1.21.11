package shelf.paster.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import shelf.paster.Skycore;
import shelf.paster.client.animation.Animations;
import shelf.paster.client.hud.elements.CommandToast;
import shelf.paster.client.hud.elements.GpsHud;
import shelf.paster.client.hud.elements.Watermark;
import shelf.paster.render.Text;
import shelf.paster.render.font.TextUniforms;
import shelf.paster.render.Draw;
import shelf.paster.render.gpu.Pipelines;
import shelf.paster.render.Fonts;
import shelf.paster.render.font.Font;

public class SkycoreClient implements ClientModInitializer {

    private static final Identifier WATERMARK_LAYER = Identifier.parse("pasta:watermark");
    private static final Identifier TOAST_LAYER = Identifier.parse("pasta:command_toast");
    private static final Identifier GPS_LAYER = Identifier.parse("pasta:gps");
    private static final Identifier RELOAD_ID = Identifier.parse("pasta:render_reload");

    @Override
    public void onInitializeClient() {
        Pipelines.init();
        HudElementRegistry.addLast(WATERMARK_LAYER, (graphics, tickCounter) -> Watermark.render());
        HudElementRegistry.addLast(TOAST_LAYER, (graphics, tickCounter) -> CommandToast.render());
        HudElementRegistry.addLast(GPS_LAYER, (graphics, tickCounter) -> GpsHud.render());
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> Fonts.warmUp());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // ClickGUI drives Animations with real frame delta; avoid 20-tps double-step.
            if (!(client.screen instanceof shelf.paster.client.ui.clickgui.ClickGuiScreen)) {
                Animations.tick(1f / 20f);
            }
            if (Skycore.get() != null) {
                Skycore.get().modules().onClientTick();
                Skycore.get().macroHandler().onClientTick();
            }
        });
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return RELOAD_ID;
            }

            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                Font.refreshAll();
                Text.clearCaches();
                TextUniforms.clearResourceCache();
                Draw.clearResourceCache();
            }
        });
    }
}
