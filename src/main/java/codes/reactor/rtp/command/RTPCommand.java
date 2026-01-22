package codes.reactor.rtp.command;

import codes.reactor.rtp.config.RTPConfig;
import codes.reactor.rtp.system.RTPSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.jetbrains.annotations.NotNull;

public final class RTPCommand extends CommandBase {

    private final RTPSystem rtpSystem;
    private final RTPConfig rtpConfig;

    public RTPCommand(RTPSystem rtpSystem, RTPConfig rtpConfig) {
        super("rtp", "Teleport a player to random location", false);
        this.rtpSystem = rtpSystem;
        this.rtpConfig = rtpConfig;
    }

    @Override
    protected void executeSync(@NotNull final CommandContext commandContext) {
        if (!(commandContext.sender() instanceof Player player)) {
            commandContext.sender().sendMessage(Message.raw("You need be a player to use this command"));
            return;
        }

        if (!player.hasPermission("rtp.use")) {
            rtpConfig.getMultiLang().send("no-permission.use", player);
            return;
        }

        rtpSystem.teleport(player);
    }
}
