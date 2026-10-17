package net.minecraft.client.resources;

import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;

public class SplashManager extends SimplePreparableReloadListener<List<String>> {
   private static final ResourceLocation SPLASHES_LOCATION = new ResourceLocation("texts/splashes.txt");
   private static final RandomSource RANDOM = RandomSource.create();
   private final List<String> splashes = Lists.newArrayList();
   private final User user;

   public SplashManager(User param0) {
      this.user = param0;
   }

   protected List<String> prepare(ResourceManager param0, ProfilerFiller param1) {
      try (BufferedReader var3 = Minecraft.getInstance().getResourceManager().openAsReader(SPLASHES_LOCATION)) {
         return var3.lines().map(String::trim).filter(l_param0 -> l_param0.hashCode() != 125780783).collect(Collectors.toList());
      } catch (IOException var3) {
         return Collections.emptyList();
      }
   }

   protected void apply(List<String> param0, ResourceManager param1, ProfilerFiller param2) {
      this.splashes.clear();
      this.splashes.addAll(param0);
   }

   @Nullable
   public String getSplash() {
      Calendar var1 = Calendar.getInstance();
      var1.setTime(new Date());
      if (var1.get(2) + 1 == 12 && var1.get(5) == 24) {
         return "Merry X-mas!";
      } else if (var1.get(2) + 1 == 1 && var1.get(5) == 1) {
         return "Happy new year!";
      } else if (var1.get(2) + 1 == 10 && var1.get(5) == 31) {
         return "OOoooOOOoooo! Spooky!";
      } else if (this.splashes.isEmpty()) {
         return null;
      } else {
         return this.user != null && RANDOM.nextInt(this.splashes.size()) == 42
            ? this.user.getName().toUpperCase(Locale.ROOT) + " IS YOU"
            : this.splashes.get(RANDOM.nextInt(this.splashes.size()));
      }
   }
}
