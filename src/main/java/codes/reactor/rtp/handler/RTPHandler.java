package codes.reactor.rtp.handler;

import codes.reactor.rtp.config.RTPConfig;
import codes.reactor.sdk.util.DecimalFormatter;
import codes.reactor.sdk.util.TimeFormatter;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.Opacity;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

@RequiredArgsConstructor
public final class RTPHandler {
    private static final Random RANDOM = new Random();

    private final Map<String, Long> playersInCooldown;
    private final RTPConfig rtpConfig;
    private final HytaleLogger logger;

    public void teleport(final Player player) {
        final World world = rtpConfig.getWorldTarget() != null
            ? Universe.get().getWorld(rtpConfig.getWorldTarget())
            : player.getWorld();

        if (world == null) {
            rtpConfig.getMultiLang().send("cant-found-valid-world", player);
            return;
        }

        if (rtpConfig.getBlacklistedWorlds().contains(world.getName())) {
            rtpConfig.getMultiLang().send("blacklist-world", player);
            return;
        }

        if (rtpConfig.getCooldown() > 0) {
            final long now = System.currentTimeMillis();
            final long lastTime = playersInCooldown.getOrDefault(player.getDisplayName(), 0L);
            final long elapsed = now - lastTime;

            if (elapsed < rtpConfig.getCooldown()) {
                rtpConfig.getMultiLang().send("cooldown-message", player,
                    "%time%", TimeFormatter.formatMillis(rtpConfig.getCooldown() - elapsed, false));
                return;
            }
            playersInCooldown.put(player.getDisplayName(), now);
        }

        teleportIgnoreRestrictions(player, world);
    }

    public void teleportIgnoreRestrictions(final Player player, final World world) {
        final AtomicInteger chunkAttempts = new AtomicInteger(0);
        final List<Location> locationsTested = Collections.synchronizedList(new ArrayList<>());

        searchSafeLocation(player, world, chunkAttempts, locationsTested);
    }

    private void searchSafeLocation(
        final Player player,
        final World world,
        final AtomicInteger chunkAttempts,
        final List<Location> locationsTested
    ) {
        if (chunkAttempts.incrementAndGet() > rtpConfig.getMaxChunkTries()) {
            handleFailure(player, world, locationsTested);
            return;
        }

        final int maxBlockTries = rtpConfig.getMaxBlockTries();

        final int x = (RANDOM.nextBoolean() ? 1 : -1) * RANDOM.nextInt((int) rtpConfig.getMaxRadius());
        final int z = (RANDOM.nextBoolean() ? 1 : -1) * RANDOM.nextInt((int) rtpConfig.getMaxRadius());

        world.getNonTickingChunkAsync(ChunkUtil.indexChunkFromBlock(x, z)).thenAccept(chunk -> {
            if (chunk == null) {
                searchSafeLocation(player, world, chunkAttempts, locationsTested);
                return;
            }

            for (int i = 0; i < maxBlockTries; i++) {
                final int localX = RANDOM.nextInt(32);
                final int localZ = RANDOM.nextInt(32);

                final short y = chunk.getHeight(localX, localZ);
                final BlockType block = chunk.getBlockType(localX, y, localZ);

                final int worldX = x + localX;
                final int worldZ = z + localZ;

                final Location loc = new Location(worldX, y + rtpConfig.getHeightOffset(), worldZ);
                locationsTested.add(loc);

                if (isSafe(block)) {
                    teleportPlayer(player, world, loc);
                    return;
                }
            }

            searchSafeLocation(player, world, chunkAttempts, locationsTested);
        }).exceptionally(ex -> {
            logger.atSevere().withCause(ex).log("Error on search valid chunks in: " + world.getName());
            return null;
        });
    }

    private boolean isSafe(BlockType block) {
        if (block == null) return false;

        return block.getOpacity() == Opacity.Solid;
    }

    private void handleFailure(Player player, World world, List<Location> locationsTested) {
        if (rtpConfig.isCancelIfCantFindSafeLocation() || locationsTested.isEmpty()) {
            rtpConfig.getMultiLang().send("cant-find-safe-location", player);
            playersInCooldown.remove(player.getDisplayName());
            return;
        }
        teleportPlayer(player, world, locationsTested.getFirst());
    }

    private void teleportPlayer(Player player, World world, Location location) {
        final Ref<EntityStore> reference = player.getReference();
        if (reference == null) return;

        reference.getStore().addComponent(reference, Teleport.getComponentType(), Teleport.createForPlayer(
            world,
            location.getPosition(),
            Vector3f.ZERO
        ));

        rtpConfig.getMultiLang().send("teleported", player,
            "%x%", DecimalFormatter.formatNumber(location.getPosition().x),
            "%y%", DecimalFormatter.formatNumber(location.getPosition().y),
            "%z%", DecimalFormatter.formatNumber(location.getPosition().z),
            "%world%", world.getName()
        );
    }
}