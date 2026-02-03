package bridge;

import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;

public class TelegramReplyPage extends BasicCustomUIPage {
    private final ReplyContext reply;

    public TelegramReplyPage(PlayerRef playerRef, ReplyContext reply) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.reply = reply;
    }

    @Override
    public void build(UICommandBuilder builder) {
        builder.append("Pages/TelegramReplyPage.ui");

        String originalSender = safe(reply.originalSender());
        String originalText = safe(reply.originalText());
        String replySender = safe(reply.replySender());
        String replyText = safe(reply.replyText());

        builder.set("#Title.Text", "Reply");
        builder.set("#ReplyTo.Text", "(Reply to: " + originalSender + ") " + originalText);
        builder.set("#ReplyFrom.Text", "(Reply from: " + replySender + ") " + replyText);
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value;
    }
}
