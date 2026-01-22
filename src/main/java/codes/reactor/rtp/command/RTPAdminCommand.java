package codes.reactor.rtp.command;

import codes.reactor.rtp.config.RTPConfig;
import codes.reactor.rtp.config.RTPConfigLoader;
import codes.reactor.sdk.message.minimessage.MiniMessage;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public final class RTPAdminCommand extends CommandBase {

    private final RTPConfigLoader rtpConfigLoader;
    private final RTPConfig rtpConfig;

    private final RequiredArg<String> messageArg;

    public RTPAdminCommand(RTPConfigLoader rtpConfigLoader, RTPConfig rtpConfig) {
        super("rtpadmin", "Teleport a player to random location", false);
        this.rtpConfigLoader = rtpConfigLoader;
        this.rtpConfig = rtpConfig;

        this.messageArg = this.withRequiredArg("message", "Get message from config",
            ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@NotNull final CommandContext commandContext) {
        final String message = commandContext.get(this.messageArg);
        final CommandSender sender = commandContext.sender();
        if (message.equalsIgnoreCase("reload")) {
            rtpConfigLoader.load();
            sender.sendMessage(MiniMessage.format("<green>Config reloaded!"));
            return;
        }

        if (message.equalsIgnoreCase("available")) {
            sender.sendMessage(MiniMessage.format("<green>Available messages: " + rtpConfig.getMultiLang().getDefaultLang().toString()));
            return;
        }

        final String langMessage = rtpConfig.getMultiLang().get(message, sender);
        sender.sendMessage(MiniMessage.format("<green>Message with the key \"" + message + "\""));
        sender.sendMessage(MiniMessage.format((langMessage == null) ? "<red>Can't found in default lang file" : langMessage));
    }
}
