package net.lax1dude.eaglercraft.v1_8.profile;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The geometry a profile skin is drawn with.
 *
 * <p>This is the low-poly half of Eaglercraft 1.8's {@code SkinModel}. The
 * high-poly mesh models (Long Arms, Baby Winston, ...) are not part of this
 * port: they need the FNAW {@code .mdl} meshes and a bespoke renderer, and are
 * deliberately omitted. The three entries here cover every skin in
 * {@link DefaultSkins}.</p>
 *
 * <p>{@link #ZOMBIE} exists because 1.8's preset list marks Herobrine and
 * Zombie with it (the zombie renderer hides the hat/overlay layer). 1.19.4
 * registers only {@code "default"} and {@code "slim"} player renderers, so
 * {@link #getRenderModelName()} folds ZOMBIE onto "default"; the two zombie
 * skins happen to carry transparent overlay pixels, so nothing is lost.</p>
 */
public enum SkinModel {

    STEVE(0, "default"),
    ALEX(1, "slim"),
    ZOMBIE(2, "zombie");

    public final int id;

    /** The name stored in NBT / sent on the wire. */
    public final String profileSkinType;

    public static final SkinModel[] skinModels = new SkinModel[3];

    private static final Map<String, SkinModel> BY_NAME = new HashMap<>();

    SkinModel(int id, String profileSkinType) {
        this.id = id;
        this.profileSkinType = profileSkinType;
    }

    /** True when this model uses the narrower "Alex" arm geometry. */
    public boolean isSlim() {
        return this == ALEX;
    }

    /**
     * The key 1.19.4's {@code EntityRenderDispatcher} uses to select a
     * {@code PlayerRenderer}. Only {@code "default"} and {@code "slim"} are
     * registered in this port; anything else silently falls back to
     * {@code "default"} anyway, but returning it explicitly keeps the intent
     * clear.
     */
    public String getRenderModelName() {
        return this == ALEX ? "slim" : "default";
    }

    /**
     * Looks a model up by its stored name. Unknown or null names yield
     * {@link #STEVE}, so a corrupt profile can never select a missing model.
     */
    public static SkinModel getModelFromId(String name) {
        if (name == null) {
            return STEVE;
        }
        SkinModel model = BY_NAME.get(name.toLowerCase(Locale.ROOT));
        return model != null ? model : STEVE;
    }

    public static SkinModel getModelFromId(int id) {
        SkinModel model = id >= 0 && id < skinModels.length ? skinModels[id] : null;
        return model != null ? model : STEVE;
    }

    static {
        for (SkinModel model : values()) {
            skinModels[model.id] = model;
            BY_NAME.put(model.profileSkinType, model);
        }
    }
}
