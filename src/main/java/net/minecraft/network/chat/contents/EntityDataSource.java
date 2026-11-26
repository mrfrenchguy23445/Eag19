package net.minecraft.network.chat.contents;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public record EntityDataSource(String selectorPattern, @Nullable EntitySelector compiledSelector) implements DataSource {
   public EntityDataSource(String param0) {
      this(param0, compileSelector(param0));
   }

   @Nullable
   private static EntitySelector compileSelector(String param0) {
      try {
         EntitySelectorParser var1 = new EntitySelectorParser(new StringReader(param0));
         return var1.parse();
      } catch (CommandSyntaxException var1) {
         return null;
      }
   }

   @Override
   public Stream<CompoundTag> getData(CommandSourceStack param0) throws CommandSyntaxException {
      if (this.compiledSelector != null) {
         List<? extends Entity> var2 = this.compiledSelector.findEntities(param0);
         return var2.stream().map(NbtPredicate::getEntityTagToCompare);
      } else {
         return Stream.empty();
      }
   }

   @Override
   public String toString() {
      return "entity=" + this.selectorPattern;
   }

   @Override
   public boolean equals(Object param0) {
      return this == param0 ? true : param0 instanceof EntityDataSource var2 && this.selectorPattern.equals(var2.selectorPattern);
   }

   @Override
   public int hashCode() {
      return this.selectorPattern.hashCode();
   }
}
