package codes.reactor.rtp.config;

import codes.reactor.sdk.config.ConfigServiceByContext;
import codes.reactor.sdk.config.section.ConfigSection;
import codes.reactor.sdk.lang.LangLoaderBuilder;
import codes.reactor.sdk.util.TimeFormatter;
import com.hypixel.hytale.logger.HytaleLogger;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@RequiredArgsConstructor
public final class RTPConfigLoader {

    private final ConfigServiceByContext configService;
    private final HytaleLogger logger;
    private final RTPConfig rtpConfig;

    public void load() {
        loadLang();

        try {
            loadConfig(configService.createIfAbsentAndLoad("config.yml"));
        } catch (IOException e) {
            logger.at(Level.SEVERE).withCause(e).log("Error on load config.yml");
        }
    }

    private void loadLang() {
        rtpConfig.multiLang = new LangLoaderBuilder().build(configService).load();
    }

    private void loadConfig(final ConfigSection config) {
        rtpConfig.maxRadius = config.getDouble("max-radius");

        final ConfigSection blackListSection = config.getOrCreateSection("blacklist-worlds");
        rtpConfig.blacklistedWorlds = blackListSection.getBoolean("enable")
            ? Set.copyOf(blackListSection.getStringList("list"))
            : List.of();

        final ConfigSection targetWorldSection = config.getOrCreateSection("target-world");
        rtpConfig.worldTarget = targetWorldSection.getBoolean("enable")
            ? targetWorldSection.getString("world")
            : null;

        final ConfigSection onTeleportSection = config.getOrCreateSection("on-teleport");
        rtpConfig.noDamageTime = TimeUnit.SECONDS.toMillis(TimeFormatter.convertToSeconds(onTeleportSection.getOrDefault("no-damage-time", "3s")));
        rtpConfig.cooldown = TimeUnit.SECONDS.toMillis(TimeFormatter.convertToSeconds(onTeleportSection.getOrDefault("cooldown", "10s")));

        final ConfigSection algorithmSection = config.getOrCreateSection("algorithm");
        rtpConfig.maxBlockTries = algorithmSection.getInt("max-blocks-tries", 5);
        rtpConfig.maxChunkTries = algorithmSection.getInt("max-chunk-tries", 3);
        logger.at(Level.INFO).log("RTP Config loaded! " + rtpConfig);
    }
}