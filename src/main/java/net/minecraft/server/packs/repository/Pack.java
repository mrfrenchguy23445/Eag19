package net.minecraft.server.packs.repository;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.packs.FeatureFlagsMetadataSection;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.world.flag.FeatureFlagSet;
import org.slf4j.Logger;

public class Pack {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final String id;
   private final Pack.ResourcesSupplier resources;
   private final Component title;
   private final Component description;
   private final PackCompatibility compatibility;
   private final FeatureFlagSet requestedFeatures;
   private final Pack.Position defaultPosition;
   private final boolean required;
   private final boolean fixedPosition;
   private final PackSource packSource;

   @Nullable
   public static Pack readMetaAndCreate(
      String param0, Component param1, boolean param2, Pack.ResourcesSupplier param3, PackType param4, Pack.Position param5, PackSource param6
   ) {
      Pack.Info var7 = readPackInfo(param0, param3);
      return var7 != null ? create(param0, param1, param2, param3, var7, param4, param5, false, param6) : null;
   }

   public static Pack create(
      String param0,
      Component param1,
      boolean param2,
      Pack.ResourcesSupplier param3,
      Pack.Info param4,
      PackType param5,
      Pack.Position param6,
      boolean param7,
      PackSource param8
   ) {
      return new Pack(param0, param2, param3, param1, param4, param4.compatibility(param5), param6, param7, param8);
   }

   private Pack(
      String param0,
      boolean param1,
      Pack.ResourcesSupplier param2,
      Component param3,
      Pack.Info param4,
      PackCompatibility param5,
      Pack.Position param6,
      boolean param7,
      PackSource param8
   ) {
      this.id = param0;
      this.resources = param2;
      this.title = param3;
      this.description = param4.description();
      this.compatibility = param5;
      this.requestedFeatures = param4.requestedFeatures();
      this.required = param1;
      this.defaultPosition = param6;
      this.fixedPosition = param7;
      this.packSource = param8;
   }

   @Nullable
   public static Pack.Info readPackInfo(String param0, Pack.ResourcesSupplier param1) {
      try (PackResources var2 = param1.open(param0)) {
         PackMetadataSection var3 = var2.getMetadataSection(PackMetadataSection.TYPE);
         if (var3 == null) {
            LOGGER.warn("Missing metadata in pack {}", param0);
            return null;
         } else {
            FeatureFlagsMetadataSection var4 = var2.getMetadataSection(FeatureFlagsMetadataSection.TYPE);
            FeatureFlagSet var5 = var4 != null ? var4.flags() : FeatureFlagSet.of();
            return new Pack.Info(var3.getDescription(), var3.getPackFormat(), var5);
         }
      } catch (Exception var2) {
         LOGGER.warn("Failed to read pack metadata", var2);
         return null;
      }
   }

   public Component getTitle() {
      return this.title;
   }

   public Component getDescription() {
      return this.description;
   }

   public Component getChatLink(boolean param0) {
      return ComponentUtils.wrapInSquareBrackets(this.packSource.decorate(Component.literal(this.id)))
         .withStyle(
            l_param1 -> l_param1.withColor(param0 ? ChatFormatting.GREEN : ChatFormatting.RED)
               .withInsertion(StringArgumentType.escapeIfRequired(this.id))
               .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.empty().append(this.title).append("\n").append(this.description)))
         );
   }

   public PackCompatibility getCompatibility() {
      return this.compatibility;
   }

   public FeatureFlagSet getRequestedFeatures() {
      return this.requestedFeatures;
   }

   public PackResources open() {
      return this.resources.open(this.id);
   }

   public String getId() {
      return this.id;
   }

   public boolean isRequired() {
      return this.required;
   }

   public boolean isFixedPosition() {
      return this.fixedPosition;
   }

   public Pack.Position getDefaultPosition() {
      return this.defaultPosition;
   }

   public PackSource getPackSource() {
      return this.packSource;
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else {
         return !(param0 instanceof Pack var2) ? false : this.id.equals(var2.id);
      }
   }

   @Override
   public int hashCode() {
      return this.id.hashCode();
   }

   public record Info(Component description, int format, FeatureFlagSet requestedFeatures) {
      public PackCompatibility compatibility(PackType param0) {
         return PackCompatibility.forFormat(this.format, param0);
      }
   }

   public enum Position {
      TOP,
      BOTTOM;

      public <T> int insert(List<T> param0, T param1, Function<T, Pack> param2, boolean param3) {
         Pack.Position var5 = param3 ? this.opposite() : this;
         if (var5 == BOTTOM) {
            int var6;
            for (var6 = 0; var6 < param0.size(); var6++) {
               Pack var7 = param2.apply(param0.get(var6));
               if (!var7.isFixedPosition() || var7.getDefaultPosition() != this) {
                  break;
               }
            }

            param0.add(var6, param1);
            return var6;
         } else {
            int var6;
            for (var6 = param0.size() - 1; var6 >= 0; var6--) {
               Pack var7 = param2.apply(param0.get(var6));
               if (!var7.isFixedPosition() || var7.getDefaultPosition() != this) {
                  break;
               }
            }

            param0.add(var6 + 1, param1);
            return var6 + 1;
         }
      }

      public Pack.Position opposite() {
         return this == TOP ? BOTTOM : TOP;
      }
   }

   @FunctionalInterface
   public interface ResourcesSupplier {
      PackResources open(String var1);
   }
}
