package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public class CreateBuffetWorldScreen extends Screen {
   private static final Component BIOME_SELECT_INFO = Component.translatable("createWorld.customize.buffet.biome");
   private final Screen parent;
   private final Consumer<Holder<Biome>> applySettings;
   final Registry<Biome> biomes;
   private CreateBuffetWorldScreen.BiomeList list;
   Holder<Biome> biome;
   private Button doneButton;

   public CreateBuffetWorldScreen(Screen param0, WorldCreationContext param1, Consumer<Holder<Biome>> param2) {
      super(Component.translatable("createWorld.customize.buffet.title"));
      this.parent = param0;
      this.applySettings = param2;
      this.biomes = param1.worldgenLoadContext().registryOrThrow(Registries.BIOME);
      Holder<Biome> var4 = this.biomes.getHolder(Biomes.PLAINS).or(() -> this.biomes.holders().findAny()).orElseThrow();
      this.biome = param1.selectedDimensions().overworld().getBiomeSource().possibleBiomes().stream().findFirst().orElse(var4);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   @Override
   protected void init() {
      this.list = new CreateBuffetWorldScreen.BiomeList();
      this.addWidget(this.list);
      this.doneButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, l_param0 -> {
         this.applySettings.accept(this.biome);
         this.minecraft.setScreen(this.parent);
      }).bounds(this.width / 2 - 155, this.height - 28, 150, 20).build());
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, l_param0 -> this.minecraft.setScreen(this.parent))
            .bounds(this.width / 2 + 5, this.height - 28, 150, 20)
            .build()
      );
      this.list.setSelected(this.list.children().stream().filter(l_param0 -> Objects.equals(l_param0.biome, this.biome)).findFirst().orElse(null));
   }

   void updateButtonValidity() {
      this.doneButton.active = this.list.getSelected() != null;
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderDirtBackground(param0);
      this.list.render(param0, param1, param2, param3);
      drawCenteredString(param0, this.font, this.title, this.width / 2, 8, 16777215);
      drawCenteredString(param0, this.font, BIOME_SELECT_INFO, this.width / 2, 28, 10526880);
      super.render(param0, param1, param2, param3);
   }

   class BiomeList extends ObjectSelectionList<CreateBuffetWorldScreen.BiomeList.Entry> {
      BiomeList() {
         super(
            CreateBuffetWorldScreen.this.minecraft,
            CreateBuffetWorldScreen.this.width,
            CreateBuffetWorldScreen.this.height,
            40,
            CreateBuffetWorldScreen.this.height - 37,
            16
         );
         // Was Collator.getInstance(Locale.getDefault()), to sort the biome list the way the
         // player's locale would order those names. String.CASE_INSENSITIVE_ORDER is the
         // replacement, and Collator is not something the classlib models at all: the link
         // reported five errors out of Collator.getInstance, all of them inside icu4j's
         // Locale -> ULocale bridge, which asks java.util.Locale for five methods the
         // classlib does not have (getScript, getExtension and the three getUnicodeLocale*
         // pair).
         //
         // The two agree on what is actually being sorted. Every biome name is an ASCII
         // ResourceLocation path -- badlands, bamboo_jungle, basalt_deltas -- so there is no
         // accented character for locale collation to weight differently, and
         // CASE_INSENSITIVE_ORDER compares the same characters in the same order that
         // Collator would. The visible difference is only that the result no longer varies
         // with the browser's locale, which for an identifier list is the better behaviour
         // anyway: the order is now the same for every player looking at the same list.
         java.util.Comparator<String> var2 = String.CASE_INSENSITIVE_ORDER;
         CreateBuffetWorldScreen.this.biomes
            .holders()
            .map(l_param0 -> new CreateBuffetWorldScreen.BiomeList.Entry((Holder.Reference<Biome>)l_param0))
            .sorted(Comparator.comparing(l_param0 -> l_param0.name.getString(), var2))
            .forEach(l_param1 -> this.addEntry(l_param1));
      }

      public void setSelected(@Nullable CreateBuffetWorldScreen.BiomeList.Entry param0) {
         super.setSelected(param0);
         if (param0 != null) {
            CreateBuffetWorldScreen.this.biome = param0.biome;
         }

         CreateBuffetWorldScreen.this.updateButtonValidity();
      }

      class Entry extends ObjectSelectionList.Entry<CreateBuffetWorldScreen.BiomeList.Entry> {
         final Holder.Reference<Biome> biome;
         final Component name;

         public Entry(Holder.Reference<Biome> param1) {
            this.biome = param1;
            ResourceLocation var3 = param1.key().location();
            String var4 = var3.toLanguageKey("biome");
            if (Language.getInstance().has(var4)) {
               this.name = Component.translatable(var4);
            } else {
               this.name = Component.literal(var3.toString());
            }
         }

         @Override
         public Component getNarration() {
            return Component.translatable("narrator.select", this.name);
         }

         @Override
         public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
            GuiComponent.drawString(param0, CreateBuffetWorldScreen.this.font, this.name, param3 + 5, param2 + 2, 16777215);
         }

         @Override
         public boolean mouseClicked(double param0, double param2, int param4) {
            if (param4 == 0) {
               BiomeList.this.setSelected(this);
               return true;
            } else {
               return false;
            }
         }
      }
   }
}
