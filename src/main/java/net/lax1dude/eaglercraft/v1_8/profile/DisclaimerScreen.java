package net.lax1dude.eaglercraft.v1_8.profile;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.lax1dude.eaglercraft.v1_8.EagRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The one-time attribution warning shown after the first profile edit.
 *
 * <p>Unofficial ports of Eaglercraft are common enough that players assume the
 * badge means an official lax1dude / PeytonPlays build. This screen says
 * plainly that it is not, so nobody reports a bug upstream that they will not
 * accept. It is shown once per browser: {@link #isAcknowledged()} reads the
 * same local storage the profile itself lives in.</p>
 */
public class DisclaimerScreen extends Screen {

    private static final String STORAGE_KEY = "disclaimerAck";

    private static final Component TITLE = Component.literal("Unofficial port");
    private static final Component CONTINUE = Component.literal("I understand");

    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int BODY_COLOR = 0xA0A0A0;

    /** Widest the body may get; keeps the lines short enough to skim. */
    private static final int BODY_MAX_WIDTH = 310;

    /**
     * Kept as literals rather than translatable: the disclaimer must never fail
     * to say who did not make this build, and a missing translation key would
     * render the raw key instead. Minecraft's font does not treat {@code '\n'} as
     * a line break, so each paragraph is a separate string and is wrapped by
     * {@link Font#split}.
     */
    private static final String[] PARAGRAPHS = {
            "This build of Eaglercraft 1.19.4 was NOT made by lax1dude or PeytonPlayz.",
            "Lax1dude and PeytonPlays are the authors of the original Eaglercraft. This is "
                    + "a separate, unofficial port of their work, and it is not affiliated with, "
                    + "endorsed by, or supported by either of them.",
            "Problems with this build are not theirs to answer for."
    };

    private List<FormattedCharSequence> bodyLines = List.of();

    public DisclaimerScreen() {
        super(TITLE);
    }

    public static boolean isAcknowledged() {
        try {
            byte[] data = EagRuntime.getStorage(STORAGE_KEY);
            return data != null && data.length > 0 && data[0] == 1;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void acknowledge() {
        try {
            EagRuntime.setStorage(STORAGE_KEY, new byte[] { 1 });
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void init() {
        this.bodyLines = this.wrapBody(Math.min(BODY_MAX_WIDTH, this.width - 40));

        int y = Math.max(this.height - 40, 52 + this.bodyLines.size() * 9 + 14);

        this.addRenderableWidget(Button.builder(CONTINUE, button -> {
            acknowledge();
            this.minecraft.setScreen(BootFlow.afterDisclaimer());
        }).bounds(this.width / 2 - 75, y, 150, 20).build());
    }

    private List<FormattedCharSequence> wrapBody(int width) {
        List<FormattedCharSequence> lines = new ArrayList<>();

        for (int i = 0; i < PARAGRAPHS.length; i++) {
            if (i > 0) {
                lines.add(FormattedCharSequence.EMPTY);
            }
            lines.addAll(this.font.split(Component.literal(PARAGRAPHS[i]), width));
        }

        return lines;
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);

        drawCenteredString(pose, this.font, TITLE, this.width / 2, 24, TITLE_COLOR);

        Font font = this.font;
        int y = 52;
        for (FormattedCharSequence line : this.bodyLines) {
            font.draw(pose, line, (float) (this.width / 2 - font.width(line) / 2), (float) y, BODY_COLOR);
            y += 9;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
