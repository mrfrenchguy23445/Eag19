package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.Message;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.network.chat.contents.BlockDataSource;
import net.minecraft.network.chat.contents.DataSource;
import net.minecraft.network.chat.contents.EntityDataSource;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.network.chat.contents.StorageDataSource;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LowerCaseEnumTypeAdapterFactory;

public interface Component extends Message, FormattedText {
   Style getStyle();

   ComponentContents getContents();

   @Override
   default String getString() {
      return FormattedText.super.getString();
   }

   default String getString(int param0) {
      StringBuilder var2 = new StringBuilder();
      this.visit(l_param2 -> {
         int l_var3 = param0 - var2.length();
         if (l_var3 <= 0) {
            return STOP_ITERATION;
         }

         var2.append(l_param2.length() <= l_var3 ? l_param2 : l_param2.substring(0, l_var3));
         return Optional.empty();
      });
      return var2.toString();
   }

   List<Component> getSiblings();

   default MutableComponent plainCopy() {
      return MutableComponent.create(this.getContents());
   }

   default MutableComponent copy() {
      return new MutableComponent(this.getContents(), new ArrayList<>(this.getSiblings()), this.getStyle());
   }

   FormattedCharSequence getVisualOrderText();

   @Override
   default <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> param0, Style param1) {
      Style var3 = this.getStyle().applyTo(param1);
      Optional<T> var4 = this.getContents().visit(param0, var3);
      if (var4.isPresent()) {
         return var4;
      }

      for (Component var6 : this.getSiblings()) {
         Optional<T> var7 = var6.visit(param0, var3);
         if (var7.isPresent()) {
            return var7;
         }
      }

      return Optional.empty();
   }

   @Override
   default <T> Optional<T> visit(FormattedText.ContentConsumer<T> param0) {
      Optional<T> var2 = this.getContents().visit(param0);
      if (var2.isPresent()) {
         return var2;
      }

      for (Component var4 : this.getSiblings()) {
         Optional<T> var5 = var4.visit(param0);
         if (var5.isPresent()) {
            return var5;
         }
      }

      return Optional.empty();
   }

   default List<Component> toFlatList() {
      return this.toFlatList(Style.EMPTY);
   }

   default List<Component> toFlatList(Style param0) {
      List<Component> var2 = Lists.newArrayList();
      this.visit((l_param1, l_param2) -> {
         if (!l_param2.isEmpty()) {
            var2.add(literal(l_param2).withStyle(l_param1));
         }

         return Optional.empty();
      }, param0);
      return var2;
   }

   default boolean contains(Component param0) {
      if (this.equals(param0)) {
         return true;
      }

      List<Component> var2 = this.toFlatList();
      List<Component> var3 = param0.toFlatList(this.getStyle());
      return Collections.indexOfSubList(var2, var3) != -1;
   }

   static Component nullToEmpty(@Nullable String param0) {
      return param0 != null ? literal(param0) : CommonComponents.EMPTY;
   }

   static MutableComponent literal(String param0) {
      return MutableComponent.create(new LiteralContents(param0));
   }

   static MutableComponent translatable(String param0) {
      return MutableComponent.create(new TranslatableContents(param0, null, TranslatableContents.NO_ARGS));
   }

   static MutableComponent translatable(String param0, Object... param1) {
      return MutableComponent.create(new TranslatableContents(param0, null, param1));
   }

   static MutableComponent translatableWithFallback(String param0, @Nullable String param1) {
      return MutableComponent.create(new TranslatableContents(param0, param1, TranslatableContents.NO_ARGS));
   }

   static MutableComponent translatableWithFallback(String param0, @Nullable String param1, Object... param2) {
      return MutableComponent.create(new TranslatableContents(param0, param1, param2));
   }

   static MutableComponent empty() {
      return MutableComponent.create(ComponentContents.EMPTY);
   }

   static MutableComponent keybind(String param0) {
      return MutableComponent.create(new KeybindContents(param0));
   }

   static MutableComponent nbt(String param0, boolean param1, Optional<Component> param2, DataSource param3) {
      return MutableComponent.create(new NbtContents(param0, param1, param2, param3));
   }

   static MutableComponent score(String param0, String param1) {
      return MutableComponent.create(new ScoreContents(param0, param1));
   }

   static MutableComponent selector(String param0, Optional<Component> param1) {
      return MutableComponent.create(new SelectorContents(param0, param1));
   }

   class Serializer implements JsonDeserializer<MutableComponent>, JsonSerializer<Component> {
      private static final Gson GSON = Util.make(() -> {
         GsonBuilder var0 = new GsonBuilder();
         var0.disableHtmlEscaping();
         var0.registerTypeHierarchyAdapter(Component.class, new Component.Serializer());
         var0.registerTypeHierarchyAdapter(Style.class, new Style.Serializer());
         var0.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
         return var0.create();
      });
      private static final Field JSON_READER_POS = Util.make(() -> {
         try {
            new JsonReader(new StringReader(""));
            Field var0 = JsonReader.class.getDeclaredField("pos");
            var0.setAccessible(true);
            return var0;
         } catch (NoSuchFieldException var0) {
            throw new IllegalStateException("Couldn't get field 'pos' for JsonReader", var0);
         }
      });
      private static final Field JSON_READER_LINESTART = Util.make(() -> {
         try {
            new JsonReader(new StringReader(""));
            Field var0 = JsonReader.class.getDeclaredField("lineStart");
            var0.setAccessible(true);
            return var0;
         } catch (NoSuchFieldException var0) {
            throw new IllegalStateException("Couldn't get field 'lineStart' for JsonReader", var0);
         }
      });

      public MutableComponent deserialize(JsonElement param0, Type param1, JsonDeserializationContext param2) throws JsonParseException {
         if (param0.isJsonPrimitive()) {
            return Component.literal(param0.getAsString());
         }

         if (!param0.isJsonObject()) {
            if (param0.isJsonArray()) {
               JsonArray var4 = param0.getAsJsonArray();
               MutableComponent var5 = null;

               for (JsonElement var7 : var4) {
                  MutableComponent var8 = this.deserialize(var7, var7.getClass(), param2);
                  if (var5 == null) {
                     var5 = var8;
                  } else {
                     var5.append(var8);
                  }
               }

               return var5;
            } else {
               throw new JsonParseException("Don't know how to turn " + param0 + " into a Component");
            }
         } else {
            JsonObject var4 = param0.getAsJsonObject();
            MutableComponent var5;
            if (var4.has("text")) {
               String var6 = GsonHelper.getAsString(var4, "text");
               var5 = var6.isEmpty() ? Component.empty() : Component.literal(var6);
            } else if (var4.has("translate")) {
               String var6 = GsonHelper.getAsString(var4, "translate");
               String var7 = GsonHelper.getAsString(var4, "fallback", null);
               if (var4.has("with")) {
                  JsonArray var8 = GsonHelper.getAsJsonArray(var4, "with");
                  Object[] var9 = new Object[var8.size()];

                  for (int var10 = 0; var10 < var9.length; var10++) {
                     var9[var10] = unwrapTextArgument(this.deserialize(var8.get(var10), param1, param2));
                  }

                  var5 = Component.translatableWithFallback(var6, var7, var9);
               } else {
                  var5 = Component.translatableWithFallback(var6, var7);
               }
            } else if (var4.has("score")) {
               JsonObject var6 = GsonHelper.getAsJsonObject(var4, "score");
               if (!var6.has("name") || !var6.has("objective")) {
                  throw new JsonParseException("A score component needs a least a name and an objective");
               }

               var5 = Component.score(GsonHelper.getAsString(var6, "name"), GsonHelper.getAsString(var6, "objective"));
            } else if (var4.has("selector")) {
               Optional<Component> var6 = this.parseSeparator(param1, param2, var4);
               var5 = Component.selector(GsonHelper.getAsString(var4, "selector"), var6);
            } else if (var4.has("keybind")) {
               var5 = Component.keybind(GsonHelper.getAsString(var4, "keybind"));
            } else {
               if (!var4.has("nbt")) {
                  throw new JsonParseException("Don't know how to turn " + param0 + " into a Component");
               }

               String var6 = GsonHelper.getAsString(var4, "nbt");
               Optional<Component> var7 = this.parseSeparator(param1, param2, var4);
               boolean var8 = GsonHelper.getAsBoolean(var4, "interpret", false);
               DataSource var9;
               if (var4.has("block")) {
                  var9 = new BlockDataSource(GsonHelper.getAsString(var4, "block"));
               } else if (var4.has("entity")) {
                  var9 = new EntityDataSource(GsonHelper.getAsString(var4, "entity"));
               } else {
                  if (!var4.has("storage")) {
                     throw new JsonParseException("Don't know how to turn " + param0 + " into a Component");
                  }

                  var9 = new StorageDataSource(new ResourceLocation(GsonHelper.getAsString(var4, "storage")));
               }

               var5 = Component.nbt(var6, var8, var7, var9);
            }

            if (var4.has("extra")) {
               JsonArray var6 = GsonHelper.getAsJsonArray(var4, "extra");
               if (var6.size() <= 0) {
                  throw new JsonParseException("Unexpected empty array of components");
               }

               for (int var7 = 0; var7 < var6.size(); var7++) {
                  var5.append(this.deserialize(var6.get(var7), param1, param2));
               }
            }

            var5.setStyle(param2.deserialize(param0, Style.class));
            return var5;
         }
      }

      private static Object unwrapTextArgument(Object param0) {
         return param0 instanceof Component var1
               && var1.getStyle().isEmpty()
               && var1.getSiblings().isEmpty()
               && var1.getContents() instanceof LiteralContents var3
            ? var3.text()
            : param0;
      }

      private Optional<Component> parseSeparator(Type param0, JsonDeserializationContext param1, JsonObject param2) {
         return param2.has("separator") ? Optional.of(this.deserialize(param2.get("separator"), param0, param1)) : Optional.empty();
      }

      private void serializeStyle(Style param0, JsonObject param1, JsonSerializationContext param2) {
         JsonElement var4 = param2.serialize(param0);
         if (var4.isJsonObject()) {
            JsonObject var5 = (JsonObject)var4;

            for (Entry<String, JsonElement> var7 : var5.entrySet()) {
               param1.add(var7.getKey(), var7.getValue());
            }
         }
      }

      public JsonElement serialize(Component param0, Type param1, JsonSerializationContext param2) {
         JsonObject var4 = new JsonObject();
         if (!param0.getStyle().isEmpty()) {
            this.serializeStyle(param0.getStyle(), var4, param2);
         }

         if (!param0.getSiblings().isEmpty()) {
            JsonArray var5 = new JsonArray();

            for (Component var7 : param0.getSiblings()) {
               var5.add(this.serialize(var7, Component.class, param2));
            }

            var4.add("extra", var5);
         }

         ComponentContents var5 = param0.getContents();
         if (var5 == ComponentContents.EMPTY) {
            var4.addProperty("text", "");
         } else if (var5 instanceof LiteralContents var7) {
            var4.addProperty("text", var7.text());
         } else if (var5 instanceof TranslatableContents var8) {
            var4.addProperty("translate", var8.getKey());
            String var12 = var8.getFallback();
            if (var12 != null) {
               var4.addProperty("fallback", var12);
            }

            if (var8.getArgs().length > 0) {
               JsonArray var13 = new JsonArray();

               for (Object var17 : var8.getArgs()) {
                  if (var17 instanceof Component) {
                     var13.add(this.serialize((Component)var17, var17.getClass(), param2));
                  } else {
                     var13.add(new JsonPrimitive(String.valueOf(var17)));
                  }
               }

               var4.add("with", var13);
            }
         } else if (var5 instanceof ScoreContents var9) {
            JsonObject var12 = new JsonObject();
            var12.addProperty("name", var9.getName());
            var12.addProperty("objective", var9.getObjective());
            var4.add("score", var12);
         } else if (var5 instanceof SelectorContents var10) {
            var4.addProperty("selector", var10.getPattern());
            this.serializeSeparator(param2, var4, var10.getSeparator());
         } else if (var5 instanceof KeybindContents var11) {
            var4.addProperty("keybind", var11.getName());
         } else {
            if (!(var5 instanceof NbtContents var6)) {
               throw new IllegalArgumentException("Don't know how to serialize " + var5 + " as a Component");
            }

            var4.addProperty("nbt", var6.getNbtPath());
            var4.addProperty("interpret", var6.isInterpreting());
            this.serializeSeparator(param2, var4, var6.getSeparator());
            DataSource var12 = var6.getDataSource();
            if (var12 instanceof BlockDataSource var14) {
               var4.addProperty("block", var14.posPattern());
            } else if (var12 instanceof EntityDataSource var15) {
               var4.addProperty("entity", var15.selectorPattern());
            } else {
               if (!(var12 instanceof StorageDataSource var13)) {
                  throw new IllegalArgumentException("Don't know how to serialize " + var5 + " as a Component");
               }

               var4.addProperty("storage", var13.id().toString());
            }
         }

         return var4;
      }

      private void serializeSeparator(JsonSerializationContext param0, JsonObject param1, Optional<Component> param2) {
         param2.ifPresent(l_param2 -> param1.add("separator", this.serialize(l_param2, l_param2.getClass(), param0)));
      }

      public static String toJson(Component param0) {
         return GSON.toJson(param0);
      }

      public static String toStableJson(Component param0) {
         return GsonHelper.toStableString(toJsonTree(param0));
      }

      public static JsonElement toJsonTree(Component param0) {
         return GSON.toJsonTree(param0);
      }

      @Nullable
      public static MutableComponent fromJson(String param0) {
         return GsonHelper.fromNullableJson(GSON, param0, MutableComponent.class, false);
      }

      @Nullable
      public static MutableComponent fromJson(JsonElement param0) {
         return GSON.fromJson(param0, MutableComponent.class);
      }

      @Nullable
      public static MutableComponent fromJsonLenient(String param0) {
         return GsonHelper.fromNullableJson(GSON, param0, MutableComponent.class, true);
      }

      public static MutableComponent fromJson(com.mojang.brigadier.StringReader param0) {
         try {
            JsonReader var1 = new JsonReader(new StringReader(param0.getRemaining()));
            var1.setLenient(false);
            MutableComponent var2 = GSON.getAdapter(MutableComponent.class).read(var1);
            param0.setCursor(param0.getCursor() + getPos(var1));
            return var2;
         } catch (IOException | StackOverflowError var1) {
            throw new JsonParseException(var1);
         }
      }

      private static int getPos(JsonReader param0) {
         try {
            return (Integer)JSON_READER_POS.get(param0) - (Integer)JSON_READER_LINESTART.get(param0) + 1;
         } catch (IllegalAccessException var1) {
            throw new IllegalStateException("Couldn't read position of JsonReader", var1);
         }
      }
   }
}
