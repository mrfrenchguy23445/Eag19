package net.lax1dude.eaglercraft.v1_8.profile;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * The local player's profile editor: a username field, a skin selector and a
 * live 3D preview of the player wearing the selected skin.
 *
 * <p>This is the part of Eaglercraft 1.8's {@code GuiScreenEditProfile} that
 * makes sense here. The import/export screens, the FNAW high-poly skin models,
 * the custom skin upload, the cape picker and the skin/cape network sync are
 * not part of this port; every cape is off and the only thing to choose is the
 * name and which of the {@link DefaultSkins} presets to wear.</p>
 *
 * <p><b>Commit semantics.</b> The username and the skin are staged locally and
 * applied on <em>Done</em> alone, because a half-typed name is not a name and
 * merely previewing a skin should not silently overwrite the stored one.
 * <em>Cancel</em> and <em>Escape</em> return without touching the profile.</p>
 *
 * <p><b>Preview rendering.</b> There is no {@code Level} behind the title
 * screen, so there is no player entity to hand to the render dispatcher. The
 * preview therefore bakes a {@link PlayerModel} directly and reproduces the
 * matrix chain {@code InventoryScreen.renderEntityInInventory} produces (model
 * view pushed to z=1000, model anchored at z=-950, negative z scale, a
 * 180-degree roll) followed by the transforms {@code LivingEntityRenderer}
 * applies before its model draws (a yaw, the x/y flip, the 0.9375 player scale
 * and the 1.501 shin offset).</p>
 */
public class EditProfileScreen extends Screen {

    private static final Component TITLE = Component.translatable("eagler.profile.title");
    private static final Component USERNAME_LABEL = Component.translatable("eagler.profile.username");

    /** The framed model preview, centred above the username field. */
    private static final int PANEL_W = 80;
    private static final int PANEL_H = 130;
    private static final int FIELD_W = 138;
    private static final int FIELD_H = 20;
    /** Skin selector buttons, sat either side of the preview frame. */
    private static final int SKIN_BUTTON_W = 16;

    /** GUI pixels per model block; tuned so the figure fills the frame. */
    private static final float PREVIEW_SCALE = 50.0F;

    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xA0A0A0;
    private static final int YELLOW = 0xFFFF55;
    private static final int PANEL_BORDER = 0xFFA0A0A0;
    private static final int PANEL_FILL = 0xFF000015;

    private final Screen parent;

    // The name in effect when the screen opened, restored on cancel.
    private final String originalName;

    // Layout, all recomputed in init() because a resize changes width/height.
    private int panelX;
    private int panelY;
    private int fieldX;
    private int fieldY;

    @Nullable
    private EditBox nameBox;

    /** The preview's rotation in degrees. Zero faces the camera. */
    private float previewYaw = 20.0F;

    /** The name currently typed in the box; survives a resize. */
    private String pendingName;

    @Nullable
    private PlayerModel<LivingEntity> model;

    /** The classic-armed twin of {@link #model}, baked for the slim skins. */
    @Nullable
    private PlayerModel<LivingEntity> slimModel;

    /**
     * The skin the &lt; / &gt; controls move through, committed on Done. Reading and
     * writing {@link EaglerProfile} directly would make opening this screen
     * overwrite a stored skin even when the player cancels.
     */
    private DefaultSkins pendingSkin;

    public EditProfileScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
        this.originalName = EaglerProfile.getName();
        this.pendingName = this.originalName;
        this.pendingSkin = EaglerProfile.getSkin();
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int cx = this.width / 2;
        int top = this.height / 6 + 8;

        int panelX = cx - PANEL_W / 2;
        int panelY = top;
        this.panelX = panelX;
        this.panelY = panelY;

        int skinY = panelY + PANEL_H / 2 - 10;
        this.addRenderableWidget(
                Button.builder(Component.literal("<"), button -> this.cycleSkin(-1))
                        .bounds(panelX - SKIN_BUTTON_W - 2, skinY, SKIN_BUTTON_W, 20)
                        .build()
        );
        this.addRenderableWidget(
                Button.builder(Component.literal(">"), button -> this.cycleSkin(1))
                        .bounds(panelX + PANEL_W + 2, skinY, SKIN_BUTTON_W, 20)
                        .build()
        );

        int fieldX = cx - FIELD_W / 2;
        // The gap is deliberately wider than the old +20: the skin-name line is drawn under
        // the panel and the username label above the field, and at +20 those two baselines sat
        // four pixels apart so the skin name rendered on top of the label.
        int fieldY = panelY + PANEL_H + 32;
        this.fieldX = fieldX;
        this.fieldY = fieldY;

        this.nameBox = new EditBox(this.font, fieldX, fieldY, FIELD_W, FIELD_H, USERNAME_LABEL);
        this.nameBox.setMaxLength(16);
        this.nameBox.setFilter(EditProfileScreen::isAllowedName);
        this.nameBox.setValue(this.pendingName);
        this.nameBox.setResponder(value -> this.pendingName = value);
        this.addRenderableWidget(this.nameBox);

        int buttonY = this.height - 40;
        this.addRenderableWidget(
                Button.builder(Component.translatable("gui.done"), button -> this.onDone())
                        .bounds(fieldX - 1, buttonY, (FIELD_W + 2) / 2 - 1, 20)
                        .build()
        );
        this.addRenderableWidget(
                Button.builder(Component.translatable("gui.cancel"), button -> this.onCancel())
                        .bounds(fieldX + (FIELD_W + 2) / 2 + 1, buttonY, (FIELD_W + 2) / 2 - 1, 20)
                        .build()
        );
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);

        // The model render leaves the render state set up for 3D; draw it before
        // the flat UI so the 2D path is restored underneath the widgets.
        this.renderPanel(pose);
        this.renderPreview(pose);

        drawCenteredString(pose, this.font, TITLE, this.width / 2, 14, WHITE);
        drawCenteredString(pose, this.font, Component.literal(this.pendingSkin.name),
                this.width / 2, this.panelY + PANEL_H + 5, YELLOW);
        drawString(pose, this.font, USERNAME_LABEL, this.fieldX, this.fieldY - 12, GRAY);

        super.render(pose, mouseX, mouseY, partialTick);
    }

    /** The framed, dark preview box the model is drawn inside. */
    private void renderPanel(PoseStack pose) {
        fill(pose, this.panelX, this.panelY, this.panelX + PANEL_W, this.panelY + PANEL_H, PANEL_BORDER);
        fill(pose, this.panelX + 1, this.panelY + 1, this.panelX + PANEL_W - 1, this.panelY + PANEL_H - 1, PANEL_FILL);
    }

    /** Draws the local player, anchored at the feet so the figure sits centred. */
    private void renderPreview(PoseStack pose) {
        PlayerModel<LivingEntity> model = this.getPreviewModel();
        this.poseStanding(model);

        int x = this.panelX + PANEL_W / 2;
        int y = this.panelY + PANEL_H - 18;

        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.translate(0.0, 0.0, 1000.0);
        RenderSystem.applyModelViewMatrix();

        pose.pushPose();
        pose.translate((double) x, (double) y, -950.0);
        pose.mulPoseMatrix(new Matrix4f().scaling(PREVIEW_SCALE, PREVIEW_SCALE, -PREVIEW_SCALE));
        pose.mulPose(new Quaternionf().rotateZ((float) Math.PI));
        pose.mulPose(Axis.YP.rotationDegrees(this.previewYaw));
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.scale(0.9375F, 0.9375F, 0.9375F);
        pose.translate(0.0F, -1.501F, 0.0F);

        Lighting.setupForEntityInInventory();
        MultiBufferSource.BufferSource buffer = this.minecraft.renderBuffers().bufferSource();

        VertexConsumer body = buffer.getBuffer(RenderType.entityTranslucent(this.pendingSkin.location));
        model.renderToBuffer(pose, body, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        buffer.endBatch();
        Lighting.setupFor3DItems();
        pose.popPose();
        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Bakes (once) and returns the preview model matching the selected skin's arm
     * geometry. A slim skin drawn on the classic model widens each arm by a pixel,
     * which reads as a rendering bug rather than as a different skin.
     */
    private PlayerModel<LivingEntity> getPreviewModel() {
        if (this.pendingSkin.model.isSlim()) {
            if (this.slimModel == null) {
                this.slimModel = new PlayerModel<>(
                        this.minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), false);
            }
            return this.slimModel;
        }
        if (this.model == null) {
            // EntityModel defaults young=true, which halves the model; this is an
            // adult, so force it off.
            this.model = new PlayerModel<>(this.minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        }
        return this.model;
    }

    /** Steps through every preset skin, wrapping at either end. */
    private void cycleSkin(int direction) {
        DefaultSkins[] skins = DefaultSkins.defaultSkinsMap;
        int index = -1;
        for (int i = 0; i < skins.length; i++) {
            if (skins[i] == this.pendingSkin) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            index = 0;
            direction = 0;
        }
        int next = (index + direction + skins.length) % skins.length;
        this.pendingSkin = skins[next];
        if (this.pendingSkin == null) {
            // A hole in the id table should never blank the preview.
            this.pendingSkin = DefaultSkins.DEFAULT_STEVE;
        }
    }

    private void poseStanding(PlayerModel<LivingEntity> model) {
        model.young = false;
        model.rightArm.zRot = 0.08F;
        model.leftArm.zRot = -0.08F;
        model.jacket.copyFrom(model.body);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftPants.copyFrom(model.leftLeg);
        model.rightPants.copyFrom(model.rightLeg);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && isInside(this.panelX, this.panelY, PANEL_W, PANEL_H, mouseX, mouseY)) {
            this.previewYaw -= (float) dragX;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    // ------------------------------------------------------------------
    // Commit / revert
    // ------------------------------------------------------------------

    private void onDone() {
        String name = this.nameBox != null ? this.nameBox.getValue().trim() : this.originalName;
        EaglerProfile.setUsername(EaglerProfile.isValidUsername(name) ? name : this.originalName);
        EaglerProfile.setSkin(this.pendingSkin);
        EaglerProfile.setSkinModel(this.pendingSkin.model);
        EaglerProfile.save();
        // The handshake and the skin lookup both read Minecraft's session User,
        // which is built once at launch, so a name typed here would otherwise be
        // saved and then ignored until the next reload.
        this.minecraft.refreshUserFromProfile();
        this.minecraft.setScreen(this.parent);
    }

    private void onCancel() {
        if (EaglerProfile.isValidUsername(this.originalName)) {
            EaglerProfile.setUsername(this.originalName);
        }
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void onClose() {
        this.onCancel();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static boolean isInside(int x, int y, int w, int h, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    /** Usernames may only contain Minecraft-compatible name characters. */
    private static boolean isAllowedName(String value) {
        if (value.length() > 16) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean legal = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
            if (!legal) {
                return false;
            }
        }
        return true;
    }
}
