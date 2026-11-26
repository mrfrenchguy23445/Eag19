package net.minecraft.world.entity.ai.gossip;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum GossipType implements StringRepresentable {
   MAJOR_NEGATIVE("major_negative", -5, 100, 10, 10),
   MINOR_NEGATIVE("minor_negative", -1, 200, 20, 20),
   MINOR_POSITIVE("minor_positive", 1, 200, 1, 5),
   MAJOR_POSITIVE("major_positive", 5, 100, 0, 100),
   TRADING("trading", 1, 25, 2, 20);

   public static final int REPUTATION_CHANGE_PER_EVENT = 25;
   public static final int REPUTATION_CHANGE_PER_EVERLASTING_MEMORY = 20;
   public static final int REPUTATION_CHANGE_PER_TRADE = 2;
   public final String id;
   public final int weight;
   public final int max;
   public final int decayPerDay;
   public final int decayPerTransfer;
   public static final Codec<GossipType> CODEC = StringRepresentable.fromEnum(GossipType::values);

   GossipType(String param2, int param3, int param4, int param5, int param6) {
      this.id = param2;
      this.weight = param3;
      this.max = param4;
      this.decayPerDay = param5;
      this.decayPerTransfer = param6;
   }

   @Override
   public String getSerializedName() {
      return this.id;
   }
}
