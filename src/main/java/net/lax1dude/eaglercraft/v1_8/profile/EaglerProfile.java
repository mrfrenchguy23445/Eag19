package net.lax1dude.eaglercraft.v1_8.profile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import javax.annotation.Nullable;

import net.lax1dude.eaglercraft.v1_8.EagRuntime;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;

/**
 * The local player identity: a username, and the offline-mode UUID derived from
 * it.
 *
 * <p>Eaglercraft has no Mojang account, so a player is identified purely by a
 * name they type. The UUID is therefore the well-known offline UUID (v3, over
 * {@code "OfflinePlayer:" + name}) that vanilla already uses for singleplayer
 * worlds and offline servers. Using it means identity is <em>stable</em>: the
 * same name always yields the same UUID, so world data, inventory and position
 * carry across sessions.</p>
 *
 * <p>Persisted as uncompressed NBT under local-storage key
 * {@value #STORAGE_KEY}, the same key Eaglercraft 1.8 used so existing installs
 * keep their name. The uncompressed {@link NbtIo#write}/{@link NbtIo#read} pair
 * is deliberate: the compressed variants pull in {@code java.util.zip}, which is
 * more than this payload justifies on a web target.</p>
 *
 * <p><b>Scope.</b> This holds identity plus the local player's chosen preset
 * skin, preset cape and skin model (see {@link DefaultSkins},
 * {@link DefaultCapes}, {@link SkinModel}). The custom-skin upload, the
 * import/export screens, the skin/cape network sync packets and the direct GL
 * texture upload of the original 1.8 class are still unported: preset textures
 * are ordinary resource-pack PNGs, loaded on demand by the vanilla
 * {@code TextureManager}. The getters below are the hooks the client uses to
 * dress the local player.</p>
 */
public class EaglerProfile {

    /** Local-storage key, unchanged from Eaglercraft 1.8. */
    public static final String STORAGE_KEY = "p";

    private static final String NBT_USERNAME = "username";
    private static final String NBT_UUID = "uuid";
    private static final String NBT_SKIN_MODEL = "skinModel";
    private static final String NBT_SKIN = "skin";
    private static final String NBT_CAPE = "cape";

    /**
     * Name for a player who has never chosen one. Kept simple and human-readable
     * rather than the 1.8 {@code yeeish####} pattern, because a default-named
     * world should still be recognisable to its owner.
     */
    private static final String DEFAULT_USERNAME = "Player";

    private static String username = DEFAULT_USERNAME;
    private static UUID uuid = UUIDUtil.createOfflinePlayerUUID(DEFAULT_USERNAME);

    // Appearance. Independent of identity: a player keeps their look even if
    // they rename, and every value has a safe default so a profile written by
    // an older build (which stored none of these) loads fine.
    private static SkinModel skinModel = SkinModel.STEVE;
    private static DefaultSkins skin = DefaultSkins.DEFAULT_STEVE;
    private static DefaultCapes cape = DefaultCapes.NO_CAPE;
    private static boolean loaded = false;

    /**
     * The current player's name. Never null.
     *
     * <p>Safe to call before {@link #loadFromStorage()} — it returns the default
     * name until then. Callers needing persisted state must load first.</p>
     */
    public static String getName() {
        return username;
    }

    /**
     * The player's offline UUID, derived from {@link #getName()}. Never null.
     *
     * <p>Read-only. The UUID is a pure function of the name; it is persisted only
     * so a rename can be detected rather than silently continuing to serve a
     * stale identity.</p>
     */
    public static UUID getPlayerUUID() {
        return uuid;
    }

    /**
     * The UUID in Mojang's canonical dashed form, for {@code User}.
     */
    public static String getPlayerUUIDString() {
        return uuid.toString();
    }

    /**
     * Sets the player's name and recomputes the UUID to match.
     *
     * <p>Does not persist; call {@link #save()} for that. Names vanilla would
     * reject are refused rather than silently mangled, because the name is sent
     * to servers verbatim.</p>
     *
     * @param name the new name
     * @return true if the name was accepted
     */
    public static boolean setUsername(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        if (!isValidUsername(trimmed)) {
            return false;
        }
        username = trimmed;
        uuid = UUIDUtil.createOfflinePlayerUUID(trimmed);
        return true;
    }

    /**
     * True if {@code name} is one vanilla would accept: 3-16 characters of
     * {@code [A-Za-z0-9_]}. Deliberately identical to vanilla's rule, so a name
     * accepted here is not rejected by a server later.
     */
    public static boolean isValidUsername(String name) {
        if (name == null) {
            return false;
        }
        int len = name.length();
        if (len < 3 || len > 16) {
            return false;
        }
        for (int i = 0; i < len; ++i) {
            char c = name.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '_';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /**
     * True for the auto-assigned names Eaglercraft hands out. Callers use this to
     * nudge the player towards picking a real name.
     */
    public static boolean isDefaultUsername(String name) {
        return name != null && name.startsWith(DEFAULT_USERNAME);
    }

    // ------------------------------------------------------------------
    // Appearance
    // ------------------------------------------------------------------

    /** The skin geometry the local player is drawn with. Never null. */
    public static SkinModel getSkinModel() {
        return skinModel;
    }

    /**
     * Sets the skin geometry. Does not persist; call {@link #save()}.
     * A null argument is ignored rather than clearing the model.
     */
    public static void setSkinModel(SkinModel model) {
        if (model != null) {
            skinModel = model;
        }
    }

    /** The chosen preset skin. Never null. */
    public static DefaultSkins getSkin() {
        return skin;
    }

    /**
     * The texture the local player is drawn with. Never null, so the player is
     * never untextured even if a preset were somehow missing.
     */
    public static ResourceLocation getSkinLocation() {
        return skin.location;
    }

    /**
     * Selects a preset skin. Does not persist; call {@link #save()}. A null
     * argument is ignored.
     */
    public static void setSkin(DefaultSkins newSkin) {
        if (newSkin != null) {
            skin = newSkin;
        }
    }

    /** The chosen preset cape, or {@link DefaultCapes#NO_CAPE}. Never null. */
    public static DefaultCapes getCape() {
        return cape;
    }

    /**
     * The cape texture for the local player, or null for no cape. Null is the
     * signal {@code CapeLayer} uses to skip the layer entirely.
     */
    @Nullable
    public static ResourceLocation getCapeLocation() {
        return cape.location;
    }

    /**
     * Selects a preset cape. Does not persist; call {@link #save()}. A null
     * argument is ignored.
     */
    public static void setCape(DefaultCapes newCape) {
        if (newCape != null) {
            cape = newCape;
        }
    }

    /**
     * The stored appearance name, e.g. {@code "default"} or {@code "slim"}.
     */
    public static String getProfileSkinType() {
        return skinModel.profileSkinType;
    }

    /**
     * The 1.19.4 renderer key for the chosen model, {@code "default"} or
     * {@code "slim"}.
     */
    public static String getRenderModelName() {
        return skinModel.getRenderModelName();
    }

    /** Reads the appearance keys from {@code tag}, defaulting any that are absent. */
    private static void readAppearance(CompoundTag tag) {
        skinModel = SkinModel.getModelFromId(tag.getString(NBT_SKIN_MODEL));
        skin = DefaultSkins.getSkinFromId(tag.getInt(NBT_SKIN));
        cape = DefaultCapes.getCapeFromId(tag.getInt(NBT_CAPE));
    }

    /**
     * Reads the profile from local storage.
     *
     * <p>Idempotent; only the first call touches storage. Any failure — absent
     * key, corrupt bytes, absent or illegal username — leaves defaults in place
     * rather than throwing, because a damaged profile must not stop the client
     * starting.</p>
     */
    public static void loadFromStorage() {
        if (loaded) {
            return;
        }
        loaded = true;

        byte[] data;
        try {
            data = EagRuntime.getStorage(STORAGE_KEY);
        } catch (Throwable t) {
            // Storage can be unavailable (private browsing, cookies disabled).
            // Running with a default name beats failing to launch.
            applyDefault();
            return;
        }
        if (data == null || data.length == 0) {
            applyDefault();
            return;
        }

        CompoundTag tag;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            tag = NbtIo.read(in);
        } catch (Throwable t) {
            // Truncated or corrupt storage written by an older build.
            applyDefault();
            return;
        }
        if (tag == null || !tag.contains(NBT_USERNAME)) {
            applyDefault();
            return;
        }

        String stored;
        try {
            stored = tag.getString(NBT_USERNAME);
        } catch (Throwable t) {
            applyDefault();
            return;
        }
        if (isValidUsername(stored)) {
            username = stored;
            uuid = readStoredUUID(tag, stored);
            readAppearance(tag);
        } else {
            applyDefault();
        }
    }

    /**
     * The UUID is derived from the name, but an explicit stored value wins so a
     * rename does not orphan the player's world. If it disagrees with the
     * name-derived value — hand-edited or corrupt — the derived value is used and
     * corrected on the next {@link #save()}.
     */
    private static UUID readStoredUUID(CompoundTag tag, String forName) {
        UUID derived = UUIDUtil.createOfflinePlayerUUID(forName);
        if (!tag.contains(NBT_UUID)) {
            return derived;
        }
        try {
            int[] ints = tag.getIntArray(NBT_UUID);
            if (ints.length != 4) {
                return derived;
            }
            return UUIDUtil.uuidFromIntArray(ints);
        } catch (Throwable t) {
            return derived;
        }
    }

    /**
     * Writes the profile to local storage. Failures are swallowed: losing a
     * profile write should never take down a running client.
     */
    public static void save() {
        if (!loaded) {
            loadFromStorage();
        }
        try {
            EagRuntime.setStorage(STORAGE_KEY, write());
        } catch (Throwable t) {
            // Non-fatal.
        }
    }

    /**
     * Serialises the profile to uncompressed NBT bytes.
     */
    public static byte[] write() {
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_USERNAME, username);
        tag.putIntArray(NBT_UUID, UUIDUtil.uuidToIntArray(uuid));
        tag.putString(NBT_SKIN_MODEL, skinModel.profileSkinType);
        tag.putInt(NBT_SKIN, skin.id);
        tag.putInt(NBT_CAPE, cape.id);

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                NbtIo.write(tag, out);
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    /**
     * Restores profile state from NBT bytes, bypassing local storage. For callers
     * already holding a payload, and for tests.
     *
     * @return true if the payload held a usable profile
     */
    public static boolean read(byte[] profileStorage) {
        if (profileStorage == null || profileStorage.length == 0) {
            return false;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(profileStorage))) {
            CompoundTag tag = NbtIo.read(in);
            if (tag == null || !tag.contains(NBT_USERNAME)) {
                return false;
            }
            String stored = tag.getString(NBT_USERNAME);
            if (!isValidUsername(stored)) {
                return false;
            }
            username = stored;
            uuid = readStoredUUID(tag, stored);
            readAppearance(tag);
            loaded = true;
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void applyDefault() {
        username = DEFAULT_USERNAME;
        uuid = UUIDUtil.createOfflinePlayerUUID(DEFAULT_USERNAME);
        skinModel = SkinModel.STEVE;
        skin = DefaultSkins.DEFAULT_STEVE;
        cape = DefaultCapes.NO_CAPE;
    }

    /**
     * Resets in-memory state to defaults. Does not touch storage.
     */
    static void resetForTesting() {
        loaded = false;
        applyDefault();
    }
}