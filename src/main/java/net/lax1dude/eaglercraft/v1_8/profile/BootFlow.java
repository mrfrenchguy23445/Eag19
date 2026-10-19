package net.lax1dude.eaglercraft.v1_8.profile;

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
        return DisclaimerScreen.isAcknowledged() ? new TitleScreen(true) : new DisclaimerScreen();
    }
}
