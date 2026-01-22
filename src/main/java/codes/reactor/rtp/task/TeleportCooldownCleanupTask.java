package codes.reactor.rtp.task;

import codes.reactor.rtp.config.RTPConfig;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.Map;

@RequiredArgsConstructor
public final class TeleportCooldownCleanupTask implements Runnable {

    private final Map<String, Long> playersInCooldown;
    private final RTPConfig rtpConfig;

    @Override
    public void run() {
        if (playersInCooldown.isEmpty()) {
            return;
        }

        final Iterator<Long> iterator = playersInCooldown.values().iterator();
        final long time = System.currentTimeMillis();

        while (iterator.hasNext()) {
            if (time - iterator.next() >= rtpConfig.getCooldown()) {
                iterator.remove();
            }
        }
    }
}