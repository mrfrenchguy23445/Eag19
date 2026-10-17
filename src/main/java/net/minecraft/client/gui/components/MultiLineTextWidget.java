package net.minecraft.client.gui.components;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.OptionalInt;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.SingleKeyCache;

public class MultiLineTextWidget extends AbstractStringWidget {
   private OptionalInt maxWidth = OptionalInt.empty();
   private OptionalInt maxRows = OptionalInt.empty();
   private final SingleKeyCache<MultiLineTextWidget.CacheKey, MultiLineLabel> cache;
   private boolean centered = false;

   public MultiLineTextWidget(Component param0, Font param1) {
      this(0, 0, param0, param1);
   }

   public MultiLineTextWidget(int param0, int param1, Component param2, Font param3) {
      super(param0, param1, 0, 0, param2, param3);
      this.cache = Util.singleKeyCache(
         l_param1 -> l_param1.maxRows.isPresent()
            ? MultiLineLabel.create(param3, l_param1.message, l_param1.maxWidth, l_param1.maxRows.getAsInt())
            : MultiLineLabel.create(param3, l_param1.message, l_param1.maxWidth)
      );
      this.active = false;
   }

   public MultiLineTextWidget setColor(int param0) {
      super.setColor(param0);
      return this;
   }

   public MultiLineTextWidget setMaxWidth(int param0) {
      this.maxWidth = OptionalInt.of(param0);
      return this;
   }

   public MultiLineTextWidget setMaxRows(int param0) {
      this.maxRows = OptionalInt.of(param0);
      return this;
   }

   public MultiLineTextWidget setCentered(boolean param0) {
      this.centered = param0;
      return this;
   }

   @Override
   public int getWidth() {
      return this.cache.getValue(this.getFreshCacheKey()).getWidth();
   }

   @Override
   public int getHeight() {
      return this.cache.getValue(this.getFreshCacheKey()).getLineCount() * 9;
   }

   @Override
   public void renderWidget(PoseStack param0, int param1, int param2, float param3) {
      MultiLineLabel var5 = this.cache.getValue(this.getFreshCacheKey());
      int var6 = this.getX();
      int var7 = this.getY();
      int var8 = 9;
      int var9 = this.getColor();
      if (this.centered) {
         var5.renderCentered(param0, var6 + this.getWidth() / 2, var7, var8, var9);
      } else {
         var5.renderLeftAligned(param0, var6, var7, var8, var9);
      }
   }

   private MultiLineTextWidget.CacheKey getFreshCacheKey() {
      return new MultiLineTextWidget.CacheKey(this.getMessage(), this.maxWidth.orElse(Integer.MAX_VALUE), this.maxRows);
   }

   record CacheKey(Component message, int maxWidth, OptionalInt maxRows) {
   }
}
