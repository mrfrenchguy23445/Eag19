/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.datafix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class DataFixers {
    public static final int BLENDING_VERSION = 3088;
    private static final DataFixer dataFixer = new UnavailableDataFixer();

    private DataFixers() {
    }

    public static DataFixer getDataFixer() {
        return dataFixer;
    }

    private static class UnavailableDataFixer implements DataFixer {
        @Override
        public <T> Dynamic<T> update(DSL.TypeReference typeReference, Dynamic<T> dynamic, int inputVersion, int outputVersion) {
            return dynamic;
        }

        @Override
        public Schema getSchema(int version) {
            throw new IllegalArgumentException("Data fixer schemas are not bundled; schema " + version + " is unavailable");
        }
    }
}
