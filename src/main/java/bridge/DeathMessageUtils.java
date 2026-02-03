package bridge;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class DeathMessageUtils {

    public static String determineKillerOrCause(DeathComponent component, Store<EntityStore> store) {
        Damage deathInfo = component.getDeathInfo();
        if (deathInfo == null) return "unknown causes";

        Damage.Source source = deathInfo.getSource();
        if (source instanceof Damage.EntitySource entitySource) {
            return resolveEntityName(entitySource, store);
        }

        return formatDamageCause(deathInfo.getCause());
    }

    private static String resolveEntityName(Damage.EntitySource entitySource, Store<EntityStore> store) {
        Ref<EntityStore> killerRef = entitySource.getRef();
        if (killerRef == null || !killerRef.isValid()) return "unknown causes";

        Player killerPlayer = store.getComponent(killerRef, Player.getComponentType());
        return killerPlayer != null ? killerPlayer.getDisplayName() : "a creature";
    }

    private static String formatDamageCause(DamageCause cause) {
        if (cause == null) return "unknown causes";
        return cause.getId().toLowerCase().replace("_", " ");
    }
}
