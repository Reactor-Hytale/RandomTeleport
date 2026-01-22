package codes.reactor.rtp.system;

import codes.reactor.rtp.config.RTPConfig;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public final class NoDamageEventSystem extends DamageEventSystem {

    private final long time;
    private final RTPConfig rtpConfig;
    private static final Query<EntityStore> QUERY = Player.getComponentType();

    @Override
    public void handle(final int i, @NotNull final ArchetypeChunk<EntityStore> archetypeChunk, @NotNull final Store<EntityStore> store, @NotNull final CommandBuffer<EntityStore> commandBuffer, @NotNull final Damage damage) {
        Player playerComponent = archetypeChunk.getComponent(i, Player.getComponentType());

        assert playerComponent != null;
        playerComponent.sendMessage(Message.raw("TE VOY A ROBARRRR " + (System.currentTimeMillis() - time) + (System.currentTimeMillis() - time < rtpConfig.getNoDamageTime())));
        if (System.currentTimeMillis() - time < rtpConfig.getNoDamageTime()) {
            damage.setCancelled(true);
            return;
        }

        store.getRegistry().unregisterSystem(getClass());
    }

    @Override
    public @NotNull Query<EntityStore> getQuery() {
        return QUERY;
    }
}
