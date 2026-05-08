package net.z2six.toomanychests.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.z2six.toomanychests.Constants;
import net.z2six.toomanychests.client.config.TrackerConfigManager;
import net.z2six.toomanychests.client.data.ContainerStore;
import net.z2six.toomanychests.client.data.ContainerStorePersistence;
import net.z2six.toomanychests.client.data.InteractionTracker;
import net.z2six.toomanychests.client.gui.TrackerScreen;
import net.z2six.toomanychests.client.render.HighlightManager;
import net.z2six.toomanychests.client.render.MarkerHudRenderer;
import net.z2six.toomanychests.client.render.MarkerRenderer;

public final class TooManyChestsClient {
    private static final ContainerStore CONTAINER_STORE = new ContainerStore();
    private static final ContainerStorePersistence CONTAINER_STORE_PERSISTENCE = new ContainerStorePersistence();
    private static final InteractionTracker INTERACTION_TRACKER = new InteractionTracker();
    private static final HighlightManager HIGHLIGHT_MANAGER = new HighlightManager();
    private static final ContainerCaptureController CAPTURE_CONTROLLER = new ContainerCaptureController(CONTAINER_STORE, INTERACTION_TRACKER);

    private static KeyMapping openTrackerKey;

    private TooManyChestsClient() {
    }

    public static void init(IEventBus modEventBus) {
        TrackerConfigManager.init();

        modEventBus.addListener(TooManyChestsClient::registerKeyMapping);

        NeoForge.EVENT_BUS.addListener(TooManyChestsClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(TooManyChestsClient::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(TooManyChestsClient::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(TooManyChestsClient::onRenderGuiPost);
        NeoForge.EVENT_BUS.addListener(TooManyChestsClient::onClientLogout);

        Constants.LOG.info("Initialized TooManyChests client systems");
    }

    private static void registerKeyMapping(RegisterKeyMappingsEvent event) {
        openTrackerKey = new KeyMapping("key.toomanychests.open_tracker", InputConstants.UNKNOWN.getValue(), "key.categories.toomanychests");
        event.register(openTrackerKey);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        TrackerConfigManager.hotReloadIfNeeded();

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        CAPTURE_CONTROLLER.tick(minecraft);
        CONTAINER_STORE_PERSISTENCE.tick(minecraft, CONTAINER_STORE);
        HIGHLIGHT_MANAGER.tick(minecraft.level.getGameTime());

        if (openTrackerKey != null) {
            while (openTrackerKey.consumeClick()) {
                minecraft.setScreen(new TrackerScreen(CONTAINER_STORE, HIGHLIGHT_MANAGER));
            }
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() || !(event.getEntity() instanceof LocalPlayer)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos pos = event.getPos();
        if (minecraft.level != null && pos != null) {
            CAPTURE_CONTROLLER.recordRightClickBlock(minecraft, pos);
        }
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        MarkerRenderer.render(event, HIGHLIGHT_MANAGER);
    }

    private static void onRenderGuiPost(RenderGuiEvent.Post event) {
        MarkerHudRenderer.render(event);
    }

    private static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft minecraft = Minecraft.getInstance();
        CONTAINER_STORE_PERSISTENCE.flush(minecraft, CONTAINER_STORE);
        CONTAINER_STORE_PERSISTENCE.resetSession();
    }
}
