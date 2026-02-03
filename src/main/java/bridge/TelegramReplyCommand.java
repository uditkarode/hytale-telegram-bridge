package bridge;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.concurrent.CompletableFuture;

public class TelegramReplyCommand extends AbstractCommand {
    private final BridgePlugin plugin;
    private final OptionalArg<String> replyIdArg;

    public TelegramReplyCommand(BridgePlugin plugin) {
        super("tgreply", "Open the Telegram reply context");
        this.plugin = plugin;
        setPermissionGroup(GameMode.Adventure);
        this.replyIdArg = withOptionalArg("id", "Reply id", ArgTypes.STRING);
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext context) {
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("This command can only be used by a player."));
            return CompletableFuture.completedFuture(null);
        }

        String id = null;
        if (context.provided(replyIdArg)) {
            id = context.get(replyIdArg);
        } else {
            id = plugin.getLastReplyId();
        }
        ReplyContext reply = plugin.getReplyContext(id);
        if (reply == null) {
            context.sendMessage(Message.raw("Reply context not found or expired."));
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> ref = context.senderAsPlayerRef();
        if (ref == null || !ref.isValid()) {
            context.sendMessage(Message.raw("You are not in a world."));
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> store = ref.getStore();
        EntityStore entityStore = (EntityStore) store.getExternalData();
        World world = entityStore == null ? null : entityStore.getWorld();
        if (world == null) {
            context.sendMessage(Message.raw("You are not in a world."));
            return CompletableFuture.completedFuture(null);
        }

        world.execute(() -> {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                context.sendMessage(Message.raw("Unable to open reply context."));
                return;
            }
            player.getPageManager().openCustomPage(ref, store, new TelegramReplyPage(player.getPlayerRef(), reply));
        });
        return CompletableFuture.completedFuture(null);
    }
}
