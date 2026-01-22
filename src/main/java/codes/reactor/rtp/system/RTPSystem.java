package codes.reactor.rtp.system;

import codes.reactor.rtp.config.RTPConfig;
import codes.reactor.sdk.util.DecimalFormatter;
import codes.reactor.sdk.util.TimeFormatter;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public final class RTPSystem {
    private static final Random RANDOM = new Random();

    private final Map<String, Long> playersInCooldown;
    private final Set<String> playersInQueue = Collections.synchronizedSet(new HashSet<>());

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

        if (playersInQueue.contains(player.getDisplayName())) {
            rtpConfig.getMultiLang().send("in-queue", player);
            return;
        }

        if (rtpConfig.getCooldown() > 0 && !player.hasPermission("rtp.cooldownbypass")) {
            final long now = System.currentTimeMillis();
            final long lastTime = playersInCooldown.getOrDefault(player.getDisplayName(), 0L);
            final long elapsed = now - lastTime;

            player.sendMessage(Message.raw("COODLWON : " + (elapsed < rtpConfig.getCooldown())));
            if (elapsed < rtpConfig.getCooldown()) {
                rtpConfig.getMultiLang().send("cooldown-message", player,
                    "%time%", TimeFormatter.formatMillis(rtpConfig.getCooldown() - elapsed, false));
                return;
            }
            playersInCooldown.put(player.getDisplayName(), now);
        }

        playersInQueue.add(player.getDisplayName());
        teleportIgnoreRestrictions(player, world);
    }

    public void teleportIgnoreRestrictions(final Player player, final World world) {
        final AtomicInteger chunkAttempts = new AtomicInteger(0);
        searchSafeLocation(player, world, chunkAttempts);
    }

    private void searchSafeLocation(
        final Player player,
        final World world,
        final AtomicInteger chunkAttempts
    ) {
        if (chunkAttempts.incrementAndGet() > rtpConfig.getMaxChunkTries()) {
            handleFailure(player);
            return;
        }

        final int maxBlockTries = rtpConfig.getMaxBlockTries();

        final int x = (RANDOM.nextBoolean() ? 1 : -1) * RANDOM.nextInt((int) rtpConfig.getMaxRadius());
        final int z = (RANDOM.nextBoolean() ? 1 : -1) * RANDOM.nextInt((int) rtpConfig.getMaxRadius());

        world.getChunkAsync(ChunkUtil.indexChunkFromBlock(x, z)).thenAccept(chunk -> {
            if (chunk == null) {
                searchSafeLocation(player, world, chunkAttempts);
                return;
            }

            for (int i = 0; i < maxBlockTries; i++) {
                final int localX = RANDOM.nextInt(29) + 1;
                final int localZ = RANDOM.nextInt(29) + 1;

                final int y = findSafeSurfaceY(chunk, localX, localZ, 0, 319);
                if (y == -1) {
                    continue;
                }

                teleportPlayer(player, world, new Location(x + localX, y, z + localZ));
                return;
            }

            searchSafeLocation(player, world, chunkAttempts);
        }).exceptionally(ex -> {
            playersInQueue.remove(player.getDisplayName());
            logger.atSevere().withCause(ex).log("Error on search valid chunks in: " + world.getName());
            return null;
        });
    }

    private int findSafeSurfaceY(WorldChunk chunk, int x, int z, int minHeight, int maxHeight) {
        int low = minHeight;
        int high = maxHeight;
        int surfaceY = -1;

        while (low <= high) {
            int mid = (low + high) / 2;
            if (chunk.getBlock(x, mid, z) != 0) {
                surfaceY = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        if (surfaceY == -1) return -1;

        if (surfaceY + 2 <= maxHeight) {
            if (chunk.getBlock(x, surfaceY + 1, z) == 0 &&
                chunk.getBlock(x, surfaceY + 2, z) == 0 &&
                chunk.getFluidId(x, surfaceY + 1, z) == 0 &&
                chunk.getFluidId(x, surfaceY + 2, z) == 0 &&
                hasSolidAround(chunk, x, surfaceY, z)) {
                return surfaceY;
            }
        }

        return -1;
    }

    private boolean hasSolidAround(WorldChunk world, int x, int y, int z) {
        return (world.getBlock(x + 1, y, z) != 0 && world
            .getBlock(x - 1, y, z) != 0 && world
            .getBlock(x, y, z + 1) != 0 && world
            .getBlock(x, y, z - 1) != 0);
    }

    private void handleFailure(Player player) {
        rtpConfig.getMultiLang().send("cant-find-safe-location", player);
        playersInCooldown.remove(player.getDisplayName());
        playersInQueue.remove(player.getDisplayName());
    }

    private void teleportPlayer(Player player, World world, Location location) {
        playersInQueue.remove(player.getDisplayName());

        final Ref<EntityStore> reference = player.getReference();
        if (reference == null) {
            return;
        }

        reference.getStore().addComponent(reference, Teleport.getComponentType(), Teleport.createForPlayer(
            world,
            location.getPosition(),
            Vector3f.ZERO
        ));

        reference.getStore().getRegistry().unregisterSystem(NoDamageEventSystem.class); // Remove old rtp event system
        reference.getStore().getRegistry().registerSystem(new NoDamageEventSystem(System.currentTimeMillis(), rtpConfig));

        rtpConfig.getMultiLang().send("teleported", player,
            "%x%", DecimalFormatter.formatNumber(location.getPosition().x),
            "%y%", DecimalFormatter.formatNumber(location.getPosition().y),
            "%z%", DecimalFormatter.formatNumber(location.getPosition().z),
            "%world%", world.getName()
        );
    }
}