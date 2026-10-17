package net.lax1dude.eaglercraft.v1_8.profile;

import net.minecraft.resources.ResourceLocation;

/**
 * The preset capes shipped with Eaglercraft 1.8, exported 1:1 from the 1.8
 * enum so an existing profile keeps its choice.
 *
 * <p>The source PNGs are 32x32 in Eagler's own layout, where the cape art
 * occupies the same pixel rectangle, {@code (0,0)-(22,17)}, that a vanilla
 * cape box unwraps. The vanilla model samples a 64x64 texture, so the exported
 * files here have each 32x32 image padded to 64x64 at the origin; that keeps
 * the pixels 1:1 and makes {@code CapeLayer} draw them correctly with no
 * custom renderer.</p>
 *
 * <p>{@link #NO_CAPE} is the "off" state and stores a null location, which is
 * exactly what {@code AbstractClientPlayer.getCloakTextureLocation()} returns
 * to suppress the cape layer.</p>
 */
public enum DefaultCapes {

    NO_CAPE(0, "No Cape", null),
    MINECON_2011(1, "Minecon 2011", new ResourceLocation("eagler:capes/01.minecon_2011.png")),
    MINECON_2012(2, "Minecon 2012", new ResourceLocation("eagler:capes/02.minecon_2012.png")),
    MINECON_2013(3, "Minecon 2013", new ResourceLocation("eagler:capes/03.minecon_2013.png")),
    MINECON_2015(4, "Minecon 2015", new ResourceLocation("eagler:capes/04.minecon_2015.png")),
    MINECON_2016(5, "Minecon 2016", new ResourceLocation("eagler:capes/05.minecon_2016.png")),
    MICROSOFT_ACCOUNT(6, "Microsoft Account", new ResourceLocation("eagler:capes/06.microsoft_account.png")),
    MAPMAKER(7, "Realms Mapmaker", new ResourceLocation("eagler:capes/07.mapmaker.png")),
    MOJANG_OLD(8, "Mojang Old", new ResourceLocation("eagler:capes/08.mojang_old.png")),
    MOJANG_NEW(9, "Mojang New", new ResourceLocation("eagler:capes/09.mojang_new.png")),
    JIRA_MOD(10, "Jira Moderator", new ResourceLocation("eagler:capes/10.jira_mod.png")),
    MOJANG_VERY_OLD(11, "Mojang Very Old", new ResourceLocation("eagler:capes/11.mojang_very_old.png")),
    SCROLLS(12, "Scrolls", new ResourceLocation("eagler:capes/12.scrolls.png")),
    COBALT(13, "Cobalt", new ResourceLocation("eagler:capes/13.cobalt.png")),
    TRANSLATOR(14, "Lang Translator", new ResourceLocation("eagler:capes/14.translator.png")),
    MILLIONTH_ACCOUNT(15, "Millionth Player", new ResourceLocation("eagler:capes/15.millionth_account.png")),
    PRISMARINE(16, "Prismarine", new ResourceLocation("eagler:capes/16.prismarine.png")),
    SNOWMAN(17, "Snowman", new ResourceLocation("eagler:capes/17.snowman.png")),
    SPADE(18, "Spade", new ResourceLocation("eagler:capes/18.spade.png")),
    BIRTHDAY(19, "Birthday", new ResourceLocation("eagler:capes/19.birthday.png")),
    DB(20, "dB", new ResourceLocation("eagler:capes/20.db.png"));

    /** Number of shipped capes (including {@link #NO_CAPE}). */
    public static final int COUNT = 21;

    public static final DefaultCapes[] defaultCapesMap = new DefaultCapes[COUNT];

    public final int id;
    public final String name;

    /** Null for {@link #NO_CAPE}. */
    public final ResourceLocation location;

    DefaultCapes(int id, String name, ResourceLocation location) {
        this.id = id;
        this.name = name;
        this.location = location;
    }

    /** Ids outside the range, or a hole, fall back to {@link #NO_CAPE}. */
    public static DefaultCapes getCapeFromId(int id) {
        DefaultCapes cape = id >= 0 && id < defaultCapesMap.length ? defaultCapesMap[id] : null;
        return cape != null ? cape : NO_CAPE;
    }

    static {
        for (DefaultCapes cape : values()) {
            defaultCapesMap[cape.id] = cape;
        }
    }
}
