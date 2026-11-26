package net.lax1dude.eaglercraft.v1_8.profile;

import net.lax1dude.eaglercraft.v1_8.EagRuntime;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Decides which screen the client opens with, so the sequence at launch is:
 * profile editor, then (first time only) the attribution disclaimer, then the
 * title screen.
 *
 * <p>The profile editor runs first on every launch so the name and skin in use
 * are visible before anything can connect. The disclaimer is once-only, because
 * re-reading it every session trains people to click past it.</p>
 *
 * <p>If the host page asked for a server ({@code ?server=...}), the last step
 * of the flow is an {@link AutoJoinScreen} instead of the title screen, so the
 * player lands in the world they were linked to.</p>
 */
public final class BootFlow {

    private BootFlow() {
    }

    /** The screen handed to {@code setScreen} once resources have loaded. */
    public static Screen firstScreen() {
        return new EditProfileScreen(afterProfile());
    }

    /** What the profile editor returns to: the disclaimer, else the title. */
    public static Screen afterProfile() {
        return DisclaimerScreen.isAcknowledged() ? afterDisclaimer() : new DisclaimerScreen();
    }

    /** What the disclaimer returns to once it has been acknowledged. */
    public static Screen afterDisclaimer() {
        String join = joinServer();
        return join != null ? new AutoJoinScreen(join) : new TitleScreen(true);
    }

    /**
     * The server the host page asked to join, or {@code null} when the client
     * should open on the title screen.
     */
    public static String joinServer() {
        try {
            String server = EagRuntime.getConfiguration().getServerToJoin();
            if (server != null && !server.trim().isEmpty()) {
                return server.trim();
            }
        } catch (Throwable ignored) {
            // A missing/failed configuration must not stop the game booting.
        }

        return null;
    }
}
