package net.minecraft.network.chat.contents;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.logging.LogUtils;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

public class NbtContents implements ComponentContents {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final boolean interpreting;
   private final Optional<Component> separator;
   private final String nbtPathPattern;
   private final DataSource dataSource;
   @Nullable
   protected final NbtPathArgument.NbtPath compiledNbtPath;

   public NbtContents(String param0, boolean param1, Optional<Component> param2, DataSource param3) {
      this(param0, compileNbtPath(param0), param1, param2, param3);
   }

   private NbtContents(String param0, @Nullable NbtPathArgument.NbtPath param1, boolean param2, Optional<Component> param3, DataSource param4) {
      this.nbtPathPattern = param0;
      this.compiledNbtPath = param1;
      this.interpreting = param2;
      this.separator = param3;
      this.dataSource = param4;
   }

   @Nullable
   private static NbtPathArgument.NbtPath compileNbtPath(String param0) {
      try {
         return new NbtPathArgument().parse(new StringReader(param0));
      } catch (CommandSyntaxException var1) {
         return null;
      }
   }

   public String getNbtPath() {
      return this.nbtPathPattern;
   }

   public boolean isInterpreting() {
      return this.interpreting;
   }

   public Optional<Component> getSeparator() {
      return this.separator;
   }

   public DataSource getDataSource() {
      return this.dataSource;
   }

   @Override
   public boolean equals(Object param0) {
      return this == param0
         ? true
         : param0 instanceof NbtContents var2
            && this.dataSource.equals(var2.dataSource)
            && this.separator.equals(var2.separator)
            && this.interpreting == var2.interpreting
            && this.nbtPathPattern.equals(var2.nbtPathPattern);
   }

   @Override
   public int hashCode() {
      int var1 = this.interpreting ? 1 : 0;
      var1 = 31 * var1 + this.separator.hashCode();
      var1 = 31 * var1 + this.nbtPathPattern.hashCode();
      return 31 * var1 + this.dataSource.hashCode();
   }

   @Override
   public String toString() {
      return "nbt{" + this.dataSource + ", interpreting=" + this.interpreting + ", separator=" + this.separator + "}";
   }

   @Override
   public MutableComponent resolve(@Nullable CommandSourceStack param0, @Nullable Entity param1, int param2) throws CommandSyntaxException {
      if (param0 != null && this.compiledNbtPath != null) {
         Stream<String> var4 = this.dataSource.getData(param0).flatMap(l_param0 -> {
            try {
               return this.compiledNbtPath.get(l_param0).stream();
            } catch (CommandSyntaxException l_var2) {
               return Stream.empty();
            }
         }).map(Tag::getAsString);
         if (this.interpreting) {
            Component var5 = DataFixUtils.orElse(
               ComponentUtils.updateForEntity(param0, this.separator, param1, param2), ComponentUtils.DEFAULT_NO_STYLE_SEPARATOR
            );
            return var4.flatMap(l_param3 -> {
               try {
                  MutableComponent l_var4 = Component.Serializer.fromJson(l_param3);
                  return Stream.of(ComponentUtils.updateForEntity(param0, l_var4, param1, param2));
               } catch (Exception l_var4) {
                  LOGGER.warn("Failed to parse component: {}", l_param3, l_var4);
                  return Stream.of();
               }
            }).reduce((l_param1, l_param2) -> l_param1.append(var5).append(l_param2)).orElseGet(Component::empty);
         } else {
            return ComponentUtils.updateForEntity(param0, this.separator, param1, param2)
               .map(
                  l_param1 -> var4.map(Component::literal)
                     .reduce((l2_param1, l2_param2) -> l2_param1.append(l_param1).append(l2_param2))
                     .orElseGet(Component::empty)
               )
               .orElseGet(() -> Component.literal(var4.collect(Collectors.joining(", "))));
         }
      } else {
         return Component.empty();
      }
   }
}
