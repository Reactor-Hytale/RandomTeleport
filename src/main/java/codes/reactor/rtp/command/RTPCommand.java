package codes.reactor.rtp.command;

import codes.reactor.rtp.handler.RTPHandler;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.jetbrains.annotations.NotNull;

public final class RTPCommand extends CommandBase {

    private final RTPHandler rtpHandler;

    public RTPCommand(RTPHandler rtpHandler) {
        super("rtp", "Teleport a player to random location", false);
        this.rtpHandler = rtpHandler;
    }

    @Override
    protected void executeSync(@NotNull final CommandContext commandContext) {
        if (!(commandContext.sender() instanceof Player player)) {
            commandContext.sender().sendMessage(Message.raw("You need be a player to use this command"));
            return;
        }
        rtpHandler.teleport(player);
    }
}
