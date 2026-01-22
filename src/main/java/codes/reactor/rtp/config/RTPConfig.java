package codes.reactor.rtp.config;

import codes.reactor.sdk.lang.MultiLang;
import lombok.Getter;

import java.util.Collection;

@Getter
public final class RTPConfig {
    MultiLang multiLang;
    Collection<String> blacklistedWorlds;

    String worldTarget;
    double maxRadius;

    long noDamageTime;
    long cooldown;

    int maxChunkTries;
    int maxBlockTries;
}
