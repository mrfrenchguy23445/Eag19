package net.lax1dude.eaglercraft.v1_8.socket;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.lax1dude.eaglercraft.v1_8.EagRuntime;

/**
 * Remembers which servers told this client to stop querying it. The browser
 * build cannot reconnect to a server that ratelimited it without making the
 * situation worse, so the server list skips those addresses instead.
 */
public class RateLimitTracker {

    private static long lastTickUpdate = 0L;

    private static final Map<String, Long> blocks = new HashMap<>();
    private static final Map<String, Long> lockout = new HashMap<>();

    public static boolean isLockedOut(String addr) {
        Long lockoutStatus = lockout.get(addr);
        return lockoutStatus != null && EagRuntime.steadyTimeMillis() - lockoutStatus.longValue() < 300000L;
    }

    public static boolean isProbablyLockedOut(String addr) {
        return blocks.containsKey(addr) || lockout.containsKey(addr);
    }

    public static void registerBlock(String addr) {
        blocks.put(addr, EagRuntime.steadyTimeMillis());
    }

    public static void registerLockOut(String addr) {
        long millis = EagRuntime.steadyTimeMillis();
        blocks.put(addr, millis);
        lockout.put(addr, millis);
    }

    public static void tick() {
        long millis = EagRuntime.steadyTimeMillis();
        if (millis - lastTickUpdate > 5000L) {
            lastTickUpdate = millis;
            Iterator<Long> blocksItr = blocks.values().iterator();
            while (blocksItr.hasNext()) {
                if (millis - blocksItr.next().longValue() > 900000L) {
                    blocksItr.remove();
                }
            }
            blocksItr = lockout.values().iterator();
            while (blocksItr.hasNext()) {
                if (millis - blocksItr.next().longValue() > 900000L) {
                    blocksItr.remove();
                }
            }
        }
    }

}
