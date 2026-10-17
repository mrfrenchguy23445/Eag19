package net.lax1dude.eaglercraft.v1_8.profile;

import java.util.UUID;

/** Standalone verification of EaglerProfile's NBT round-trip and UUID rules. */
public class EaglerProfileVerify {

    private static int failures = 0;

    private static void check(String what, boolean ok) {
        if (!ok) {
            System.out.println("  FAIL  " + what);
            failures++;
        } else {
            System.out.println("  ok    " + what);
        }
    }

    public static void main(String[] args) {
        System.out.println("EaglerProfile verification");

        // --- write/read round trip, no platform involved ---
        check("setUsername accepts a legal name",
                EaglerProfile.setUsername("Notch"));
        UUID expected = EaglerProfile.getPlayerUUID();
        check("getPlayerUUID equals the canonical offline UUID",
                net.minecraft.core.UUIDUtil.createOfflinePlayerUUID("Notch").equals(expected));
        check("offline UUID is version 3 (name-based)",
                expected.version() == 3);

        byte[] blob = EaglerProfile.write();
        check("write() produced non-empty bytes", blob.length > 0);

        EaglerProfile.resetForTesting();
        check("reset restores default name", "Player".equals(EaglerProfile.getName()));

        check("read() accepts the blob", EaglerProfile.read(blob));
        check("read() restored the name", "Notch".equals(EaglerProfile.getName()));
        check("read() restored the UUID", expected.equals(EaglerProfile.getPlayerUUID()));

        // --- stability: same name always gives the same UUID ---
        EaglerProfile.resetForTesting();
        EaglerProfile.setUsername("Notch");
        check("UUID is stable across a rename cycle",
                expected.equals(EaglerProfile.getPlayerUUID()));

        // --- rename recomputes the UUID ---
        check("setUsername accepts a second name", EaglerProfile.setUsername("jeb_"));
        check("UUID changes when the name changes",
                !expected.equals(EaglerProfile.getPlayerUUID()));
        check("rename persisted a matching UUID",
                net.minecraft.core.UUIDUtil.createOfflinePlayerUUID("jeb_")
                        .equals(EaglerProfile.getPlayerUUID()));

        // --- rejection rules ---
        check("rejects too-short name", !EaglerProfile.setUsername("ab"));
        check("rejects too-long name", !EaglerProfile.setUsername("abcdefghijklmnopq"));
        check("rejects illegal char", !EaglerProfile.setUsername("no spaces"));
        check("rejects null", !EaglerProfile.setUsername(null));
        check("name unchanged after rejection", "jeb_".equals(EaglerProfile.getName()));

        // --- corrupt input must not throw ---
        check("read() rejects garbage", !EaglerProfile.read(new byte[] { 1, 2, 3 }));
        check("read() rejects empty", !EaglerProfile.read(new byte[0]));
        check("read() rejects null", !EaglerProfile.read(null));
        check("name survives corrupt read", "jeb_".equals(EaglerProfile.getName()));

        System.out.println(failures == 0 ? "\nALL PASS" : "\n" + failures + " FAILURE(S)");
        System.exit(failures == 0 ? 0 : 1);
    }
}