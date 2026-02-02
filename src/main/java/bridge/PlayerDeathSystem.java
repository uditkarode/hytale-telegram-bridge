package bridge;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;

import javax.annotation.Nonnull;

public class PlayerDeathSystem extends DeathSystems.OnDeathSystem {
    private final BridgePlugin plugin;

    public PlayerDeathSystem(BridgePlugin plugin) {
        this.plugin = plugin;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent component, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Player victim = (Player) store.getComponent(ref, Player.getComponentType());
        if (victim == null) return;

        String victimName = victim.getDisplayName();
        String killerOrCause = "unknown causes";

        Damage deathInfo = component.getDeathInfo();
        if (deathInfo != null) {
            Damage.Source source = deathInfo.getSource();
            if (source instanceof Damage.EntitySource) {
                Ref<EntityStore> killerRef = ((Damage.EntitySource) source).getRef();
                if (killerRef != null && killerRef.isValid()) {
                    Player killerPlayer = store.getComponent(killerRef, Player.getComponentType());
                    if (killerPlayer != null) {
                        killerOrCause = killerPlayer.getDisplayName();
                    } else {
                        killerOrCause = "a creature";
                    }
                }
            } else {
                DamageCause cause = deathInfo.getCause();
                if (cause != null) {
                    killerOrCause = cause.getId().toLowerCase().replace("_", " ");
                }
            }
        }

        plugin.forwardToTelegram(victimName + " was killed by " + killerOrCause + ".");
    }
}
