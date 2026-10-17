package net.minecraft.commands;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;

public interface CommandBuildContext {
   <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<T>> var1);

   static CommandBuildContext simple(final HolderLookup.Provider param0, final FeatureFlagSet param1) {
      return new CommandBuildContext() {
         @Override
         public <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<T>> l_param0) {
            return param0.lookupOrThrow(l_param0).filterFeatures(param1);
         }
      };
   }

   static CommandBuildContext.Configurable configurable(final RegistryAccess param0, final FeatureFlagSet param1) {
      return new CommandBuildContext.Configurable() {
         CommandBuildContext.MissingTagAccessPolicy missingTagAccessPolicy = CommandBuildContext.MissingTagAccessPolicy.FAIL;

         @Override
         public void missingTagAccessPolicy(CommandBuildContext.MissingTagAccessPolicy l_param0) {
            this.missingTagAccessPolicy = l_param0;
         }

         @Override
         public <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<T>> l_param0) {
            Registry<T> l_var2 = param0.registryOrThrow(l_param0);
            final HolderLookup.RegistryLookup<T> l_var3 = l_var2.asLookup();
            final HolderLookup.RegistryLookup<T> l_var4 = l_var2.asTagAddingLookup();
            HolderLookup.RegistryLookup<T> l_var5 = new HolderLookup.RegistryLookup.Delegate<T>() {
               @Override
               protected HolderLookup.RegistryLookup<T> parent() {
                  return switch (missingTagAccessPolicy) {
                     case FAIL -> l_var3;
                     case CREATE_NEW -> l_var4;
                  };
               }
            };
            return l_var5.filterFeatures(param1);
         }
      };
   }

   interface Configurable extends CommandBuildContext {
      void missingTagAccessPolicy(CommandBuildContext.MissingTagAccessPolicy var1);
   }

   enum MissingTagAccessPolicy {
      CREATE_NEW,
      FAIL;
   }
}
