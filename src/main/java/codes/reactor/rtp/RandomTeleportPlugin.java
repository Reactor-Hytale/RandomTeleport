package codes.reactor.rtp;

import codes.reactor.rtp.command.RTPAdminCommand;
import codes.reactor.rtp.command.RTPCommand;
import codes.reactor.rtp.config.RTPConfig;
import codes.reactor.rtp.config.RTPConfigLoader;
import codes.reactor.rtp.system.RTPSystem;
import codes.reactor.rtp.task.TeleportCooldownCleanupTask;
import codes.reactor.sdk.config.ConfigServiceByContext;
import codes.reactor.sdk.plugin.ReactorPlugin;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.*;

public final class RandomTeleportPlugin extends ReactorPlugin {

    public RandomTeleportPlugin(@NotNull final JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        final RTPConfig rtpConfig = new RTPConfig();
        final RTPConfigLoader rtpConfigLoader = new RTPConfigLoader(
            ConfigServiceByContext.from("yaml", context),
            getLogger(),
            rtpConfig
        );

        rtpConfigLoader.load();

        final Map<String, Long> playersInCooldown = new ConcurrentHashMap<>();
        final RTPSystem rtpSystem = new RTPSystem(playersInCooldown, rtpConfig, getLogger());
        getCommandRegistry().registerCommand(new RTPCommand(rtpSystem, rtpConfig));
        getCommandRegistry().registerCommand(new RTPAdminCommand(rtpConfigLoader, rtpConfig));

        HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
            new TeleportCooldownCleanupTask(playersInCooldown, rtpConfig),
            10,
            10,
            TimeUnit.SECONDS
        );
    }
}