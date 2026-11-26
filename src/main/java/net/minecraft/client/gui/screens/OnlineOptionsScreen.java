package net.minecraft.client.gui.screens;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import org.apache.commons.compress.utils.Lists;

public class OnlineOptionsScreen extends SimpleOptionsSubScreen {
   @Nullable
   private final OptionInstance<Unit> difficultyDisplay;

   public static OnlineOptionsScreen createOnlineOptionsScreen(Minecraft param0, Screen param1, Options param2) {
      List<OptionInstance<?>> var3 = Lists.newArrayList();
      var3.add(param2.realmsNotifications());
      var3.add(param2.allowServerListing());
      OptionInstance<Unit> var4 = Optionull.map(
         param0.level,
         l_param0 -> {
            Difficulty l_var1 = l_param0.getDifficulty();
            return new OptionInstance<>(
               "options.difficulty.online",
               OptionInstance.noTooltip(),
               (l2_param1, l2_param2) -> l_var1.getDisplayName(),
               new OptionInstance.Enum<>(List.of(Unit.INSTANCE), Codec.EMPTY.codec()),
               Unit.INSTANCE,
               l2_param0 -> {}
            );
         }
      );
      if (var4 != null) {
         var3.add(var4);
      }

      return new OnlineOptionsScreen(param1, param2, var3.toArray(new OptionInstance[0]), var4);
   }

   private OnlineOptionsScreen(Screen param0, Options param1, OptionInstance<?>[] param2, @Nullable OptionInstance<Unit> param3) {
      super(param0, param1, Component.translatable("options.online.title"), param2);
      this.difficultyDisplay = param3;
   }

   @Override
   protected void init() {
      super.init();
      if (this.difficultyDisplay != null) {
         AbstractWidget var1 = this.list.findOption(this.difficultyDisplay);
         if (var1 != null) {
            var1.active = false;
         }
      }

      AbstractWidget var1 = this.list.findOption(this.options.telemetryOptInExtra());
      if (var1 != null) {
         var1.active = this.minecraft.extraTelemetryAvailable();
      }
   }
}
