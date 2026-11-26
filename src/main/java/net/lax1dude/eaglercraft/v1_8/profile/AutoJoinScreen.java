package net.lax1dude.eaglercraft.v1_8.profile;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.language.I18n;

public class AutoJoinScreen extends Screen {

    private final String address;
    private boolean started;

    public AutoJoinScreen(String address) {
        super(Component.translatable("connect.connecting"));
        this.address = address;
    }

    public String getAddress() {
        return this.address;
    }

    @Override
    public void tick() {
        if (this.started) {
            return;
        }

        this.started = true;
        try {
            System.out.println("[autojoin] connecting to " + this.address);
        } catch (Throwable ignored) {
        }

        ConnectScreen.startConnecting(
                new TitleScreen(),
                this.minecraft,
                ServerAddress.parseString(this.address),
                new ServerData(I18n.get("selectServer.defaultName"), this.address, false)
        );
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        drawCenteredString(pose, this.font, this.title, this.width / 2, this.height / 2 - 20, 16777215);
        drawCenteredString(pose, this.font, this.address, this.width / 2, this.height / 2 + 4, 8421504);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
