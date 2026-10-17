package net.minecraft.advancements;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.apache.commons.lang3.ArrayUtils;

public class Advancement {
   @Nullable
   private final Advancement parent;
   @Nullable
   private final DisplayInfo display;
   private final AdvancementRewards rewards;
   private final ResourceLocation id;
   private final Map<String, Criterion> criteria;
   private final String[][] requirements;
   private final Set<Advancement> children = Sets.newLinkedHashSet();
   private final Component chatComponent;

   public Advancement(
      ResourceLocation param0,
      @Nullable Advancement param1,
      @Nullable DisplayInfo param2,
      AdvancementRewards param3,
      Map<String, Criterion> param4,
      String[][] param5
   ) {
      this.id = param0;
      this.display = param2;
      this.criteria = ImmutableMap.copyOf(param4);
      this.parent = param1;
      this.rewards = param3;
      this.requirements = param5;
      if (param1 != null) {
         param1.addChild(this);
      }

      if (param2 == null) {
         this.chatComponent = Component.literal(param0.toString());
      } else {
         Component var7 = param2.getTitle();
         ChatFormatting var8 = param2.getFrame().getChatColor();
         Component var9 = ComponentUtils.mergeStyles(var7.copy(), Style.EMPTY.withColor(var8)).append("\n").append(param2.getDescription());
         Component var10 = var7.copy().withStyle(l_param1 -> l_param1.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, var9)));
         this.chatComponent = ComponentUtils.wrapInSquareBrackets(var10).withStyle(var8);
      }
   }

   public Advancement.Builder deconstruct() {
      return new Advancement.Builder(this.parent == null ? null : this.parent.getId(), this.display, this.rewards, this.criteria, this.requirements);
   }

   @Nullable
   public Advancement getParent() {
      return this.parent;
   }

   public Advancement getRoot() {
      return getRoot(this);
   }

   public static Advancement getRoot(Advancement param0) {
      Advancement var1 = param0;

      while (true) {
         Advancement var2 = var1.getParent();
         if (var2 == null) {
            return var1;
         }

         var1 = var2;
      }
   }

   @Nullable
   public DisplayInfo getDisplay() {
      return this.display;
   }

   public AdvancementRewards getRewards() {
      return this.rewards;
   }

   @Override
   public String toString() {
      return "SimpleAdvancement{id="
         + this.getId()
         + ", parent="
         + (this.parent == null ? "null" : this.parent.getId())
         + ", display="
         + this.display
         + ", rewards="
         + this.rewards
         + ", criteria="
         + this.criteria
         + ", requirements="
         + Arrays.deepToString(this.requirements)
         + "}";
   }

   public Iterable<Advancement> getChildren() {
      return this.children;
   }

   public Map<String, Criterion> getCriteria() {
      return this.criteria;
   }

   public int getMaxCriteraRequired() {
      return this.requirements.length;
   }

   public void addChild(Advancement param0) {
      this.children.add(param0);
   }

   public ResourceLocation getId() {
      return this.id;
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else {
         return !(param0 instanceof Advancement var2) ? false : this.id.equals(var2.id);
      }
   }

   @Override
   public int hashCode() {
      return this.id.hashCode();
   }

   public String[][] getRequirements() {
      return this.requirements;
   }

   public Component getChatComponent() {
      return this.chatComponent;
   }

   public static class Builder {
      @Nullable
      private ResourceLocation parentId;
      @Nullable
      private Advancement parent;
      @Nullable
      private DisplayInfo display;
      private AdvancementRewards rewards = AdvancementRewards.EMPTY;
      private Map<String, Criterion> criteria = Maps.newLinkedHashMap();
      @Nullable
      private String[][] requirements;
      private RequirementsStrategy requirementsStrategy = RequirementsStrategy.AND;

      Builder(@Nullable ResourceLocation param0, @Nullable DisplayInfo param1, AdvancementRewards param2, Map<String, Criterion> param3, String[][] param4) {
         this.parentId = param0;
         this.display = param1;
         this.rewards = param2;
         this.criteria = param3;
         this.requirements = param4;
      }

      private Builder() {
      }

      public static Advancement.Builder advancement() {
         return new Advancement.Builder();
      }

      public Advancement.Builder parent(Advancement param0) {
         this.parent = param0;
         return this;
      }

      public Advancement.Builder parent(ResourceLocation param0) {
         this.parentId = param0;
         return this;
      }

      public Advancement.Builder display(
         ItemStack param0,
         Component param1,
         Component param2,
         @Nullable ResourceLocation param3,
         FrameType param4,
         boolean param5,
         boolean param6,
         boolean param7
      ) {
         return this.display(new DisplayInfo(param0, param1, param2, param3, param4, param5, param6, param7));
      }

      public Advancement.Builder display(
         ItemLike param0,
         Component param1,
         Component param2,
         @Nullable ResourceLocation param3,
         FrameType param4,
         boolean param5,
         boolean param6,
         boolean param7
      ) {
         return this.display(new DisplayInfo(new ItemStack(param0.asItem()), param1, param2, param3, param4, param5, param6, param7));
      }

      public Advancement.Builder display(DisplayInfo param0) {
         this.display = param0;
         return this;
      }

      public Advancement.Builder rewards(AdvancementRewards.Builder param0) {
         return this.rewards(param0.build());
      }

      public Advancement.Builder rewards(AdvancementRewards param0) {
         this.rewards = param0;
         return this;
      }

      public Advancement.Builder addCriterion(String param0, CriterionTriggerInstance param1) {
         return this.addCriterion(param0, new Criterion(param1));
      }

      public Advancement.Builder addCriterion(String param0, Criterion param1) {
         if (this.criteria.containsKey(param0)) {
            throw new IllegalArgumentException("Duplicate criterion " + param0);
         }

         this.criteria.put(param0, param1);
         return this;
      }

      public Advancement.Builder requirements(RequirementsStrategy param0) {
         this.requirementsStrategy = param0;
         return this;
      }

      public Advancement.Builder requirements(String[][] param0) {
         this.requirements = param0;
         return this;
      }

      public boolean canBuild(Function<ResourceLocation, Advancement> param0) {
         if (this.parentId == null) {
            return true;
         }

         if (this.parent == null) {
            this.parent = param0.apply(this.parentId);
         }

         return this.parent != null;
      }

      public Advancement build(ResourceLocation param0) {
         if (!this.canBuild(l_param0 -> null)) {
            throw new IllegalStateException("Tried to build incomplete advancement!");
         }

         if (this.requirements == null) {
            this.requirements = this.requirementsStrategy.createRequirements(this.criteria.keySet());
         }

         return new Advancement(param0, this.parent, this.display, this.rewards, this.criteria, this.requirements);
      }

      public Advancement save(Consumer<Advancement> param0, String param1) {
         Advancement var3 = this.build(new ResourceLocation(param1));
         param0.accept(var3);
         return var3;
      }

      public JsonObject serializeToJson() {
         if (this.requirements == null) {
            this.requirements = this.requirementsStrategy.createRequirements(this.criteria.keySet());
         }

         JsonObject var1 = new JsonObject();
         if (this.parent != null) {
            var1.addProperty("parent", this.parent.getId().toString());
         } else if (this.parentId != null) {
            var1.addProperty("parent", this.parentId.toString());
         }

         if (this.display != null) {
            var1.add("display", this.display.serializeToJson());
         }

         var1.add("rewards", this.rewards.serializeToJson());
         JsonObject var2 = new JsonObject();

         for (Entry<String, Criterion> var4 : this.criteria.entrySet()) {
            var2.add(var4.getKey(), var4.getValue().serializeToJson());
         }

         var1.add("criteria", var2);
         JsonArray var3 = new JsonArray();

         for (String[] var7 : this.requirements) {
            JsonArray var8 = new JsonArray();

            for (String var12 : var7) {
               var8.add(var12);
            }

            var3.add(var8);
         }

         var1.add("requirements", var3);
         return var1;
      }

      public void serializeToNetwork(FriendlyByteBuf param0) {
         if (this.requirements == null) {
            this.requirements = this.requirementsStrategy.createRequirements(this.criteria.keySet());
         }

         param0.writeNullable(this.parentId, FriendlyByteBuf::writeResourceLocation);
         param0.writeNullable(this.display, (l_param0, l_param1) -> l_param1.serializeToNetwork(l_param0));
         Criterion.serializeToNetwork(this.criteria, param0);
         param0.writeVarInt(this.requirements.length);

         for (String[] var5 : this.requirements) {
            param0.writeVarInt(var5.length);

            for (String var9 : var5) {
               param0.writeUtf(var9);
            }
         }
      }

      @Override
      public String toString() {
         return "Task Advancement{parentId="
            + this.parentId
            + ", display="
            + this.display
            + ", rewards="
            + this.rewards
            + ", criteria="
            + this.criteria
            + ", requirements="
            + Arrays.deepToString(this.requirements)
            + "}";
      }

      public static Advancement.Builder fromJson(JsonObject param0, DeserializationContext param1) {
         ResourceLocation var2 = param0.has("parent") ? new ResourceLocation(GsonHelper.getAsString(param0, "parent")) : null;
         DisplayInfo var3 = param0.has("display") ? DisplayInfo.fromJson(GsonHelper.getAsJsonObject(param0, "display")) : null;
         AdvancementRewards var4 = param0.has("rewards")
            ? AdvancementRewards.deserialize(GsonHelper.getAsJsonObject(param0, "rewards"))
            : AdvancementRewards.EMPTY;
         Map<String, Criterion> var5 = Criterion.criteriaFromJson(GsonHelper.getAsJsonObject(param0, "criteria"), param1);
         if (var5.isEmpty()) {
            throw new JsonSyntaxException("Advancement criteria cannot be empty");
         }

         JsonArray var6 = GsonHelper.getAsJsonArray(param0, "requirements", new JsonArray());
         String[][] var7 = new String[var6.size()][];

         for (int var8 = 0; var8 < var6.size(); var8++) {
            JsonArray var9 = GsonHelper.convertToJsonArray(var6.get(var8), "requirements[" + var8 + "]");
            var7[var8] = new String[var9.size()];

            for (int var10 = 0; var10 < var9.size(); var10++) {
               var7[var8][var10] = GsonHelper.convertToString(var9.get(var10), "requirements[" + var8 + "][" + var10 + "]");
            }
         }

         if (var7.length == 0) {
            var7 = new String[var5.size()][];
            int var8 = 0;

            for (String var10 : var5.keySet()) {
               var7[var8++] = new String[]{var10};
            }
         }

         for (String[] var11 : var7) {
            if (var11.length == 0 && var5.isEmpty()) {
               throw new JsonSyntaxException("Requirement entry cannot be empty");
            }

            for (String var15 : var11) {
               if (!var5.containsKey(var15)) {
                  throw new JsonSyntaxException("Unknown required criterion '" + var15 + "'");
               }
            }
         }

         for (String var9 : var5.keySet()) {
            boolean var10 = false;

            for (String[] var14 : var7) {
               if (ArrayUtils.contains(var14, var9)) {
                  var10 = true;
                  break;
               }
            }

            if (!var10) {
               throw new JsonSyntaxException(
                  "Criterion '" + var9 + "' isn't a requirement for completion. This isn't supported behaviour, all criteria must be required."
               );
            }
         }

         return new Advancement.Builder(var2, var3, var4, var5, var7);
      }

      public static Advancement.Builder fromNetwork(FriendlyByteBuf param0) {
         ResourceLocation var1 = param0.readNullable(FriendlyByteBuf::readResourceLocation);
         DisplayInfo var2 = param0.readNullable(DisplayInfo::fromNetwork);
         Map<String, Criterion> var3 = Criterion.criteriaFromNetwork(param0);
         String[][] var4 = new String[param0.readVarInt()][];

         for (int var5 = 0; var5 < var4.length; var5++) {
            var4[var5] = new String[param0.readVarInt()];

            for (int var6 = 0; var6 < var4[var5].length; var6++) {
               var4[var5][var6] = param0.readUtf();
            }
         }

         return new Advancement.Builder(var1, var2, AdvancementRewards.EMPTY, var3, var4);
      }

      public Map<String, Criterion> getCriteria() {
         return this.criteria;
      }
   }
}
