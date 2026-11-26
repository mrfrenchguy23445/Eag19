package net.lax1dude.eaglercraft.v1_8.profile;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


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

    public boolean isSlim() {
        return this == ALEX;
    }


    public String getRenderModelName() {
        return this == ALEX ? "slim" : "default";
    }


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
