package net.minecraft.network.protocol.game;

import com.google.common.collect.Queues;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiPredicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

public class ClientboundCommandsPacket implements Packet<ClientGamePacketListener> {
   private static final byte MASK_TYPE = 3;
   private static final byte FLAG_EXECUTABLE = 4;
   private static final byte FLAG_REDIRECT = 8;
   private static final byte FLAG_CUSTOM_SUGGESTIONS = 16;
   private static final byte TYPE_ROOT = 0;
   private static final byte TYPE_LITERAL = 1;
   private static final byte TYPE_ARGUMENT = 2;
   private final int rootIndex;
   private final List<ClientboundCommandsPacket.Entry> entries;

   public ClientboundCommandsPacket(RootCommandNode<SharedSuggestionProvider> param0) {
      Object2IntMap<CommandNode<SharedSuggestionProvider>> var2 = enumerateNodes(param0);
      this.entries = createEntries(var2);
      this.rootIndex = var2.getInt(param0);
   }

   public ClientboundCommandsPacket(FriendlyByteBuf param0) {
      this.entries = param0.readList(ClientboundCommandsPacket::readNode);
      this.rootIndex = param0.readVarInt();
      validateEntries(this.entries);
   }

   @Override
   public void write(FriendlyByteBuf param0) {
      param0.writeCollection(this.entries, (l_param0, l_param1) -> l_param1.write(l_param0));
      param0.writeVarInt(this.rootIndex);
   }

   private static void validateEntries(List<ClientboundCommandsPacket.Entry> param0, BiPredicate<ClientboundCommandsPacket.Entry, IntSet> param1) {
      IntSet var2 = new IntOpenHashSet(IntSets.fromTo(0, param0.size()));

      while (!var2.isEmpty()) {
         boolean var3 = var2.removeIf(l_param3 -> param1.test(param0.get(l_param3), var2));
         if (!var3) {
            throw new IllegalStateException("Server sent an impossible command tree");
         }
      }
   }

   private static void validateEntries(List<ClientboundCommandsPacket.Entry> param0) {
      validateEntries(param0, ClientboundCommandsPacket.Entry::canBuild);
      validateEntries(param0, ClientboundCommandsPacket.Entry::canResolve);
   }

   private static Object2IntMap<CommandNode<SharedSuggestionProvider>> enumerateNodes(RootCommandNode<SharedSuggestionProvider> param0) {
      Object2IntMap<CommandNode<SharedSuggestionProvider>> var1 = new Object2IntOpenHashMap<>();
      Queue<CommandNode<SharedSuggestionProvider>> var2 = Queues.newArrayDeque();
      var2.add(param0);

      CommandNode<SharedSuggestionProvider> var3;
      while ((var3 = var2.poll()) != null) {
         if (!var1.containsKey(var3)) {
            int var4 = var1.size();
            var1.put(var3, var4);
            var2.addAll(var3.getChildren());
            if (var3.getRedirect() != null) {
               var2.add(var3.getRedirect());
            }
         }
      }

      return var1;
   }

   private static List<ClientboundCommandsPacket.Entry> createEntries(Object2IntMap<CommandNode<SharedSuggestionProvider>> param0) {
      ObjectArrayList<ClientboundCommandsPacket.Entry> var1 = new ObjectArrayList<>(param0.size());
      var1.size(param0.size());

      for (Object2IntMap.Entry<CommandNode<SharedSuggestionProvider>> var3 : Object2IntMaps.fastIterable(param0)) {
         var1.set(var3.getIntValue(), createEntry(var3.getKey(), param0));
      }

      return var1;
   }

   private static ClientboundCommandsPacket.Entry readNode(FriendlyByteBuf param0) {
      byte var1 = param0.readByte();
      int[] var2 = param0.readVarIntArray();
      int var3 = (var1 & 8) != 0 ? param0.readVarInt() : 0;
      ClientboundCommandsPacket.NodeStub var4 = read(param0, var1);
      return new ClientboundCommandsPacket.Entry(var4, var1, var3, var2);
   }

   @Nullable
   private static ClientboundCommandsPacket.NodeStub read(FriendlyByteBuf param0, byte param1) {
      int var2 = param1 & 3;
      if (var2 == 2) {
         String var3 = param0.readUtf();
         int var4 = param0.readVarInt();
         ArgumentTypeInfo<?, ?> var5 = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.byId(var4);
         if (var5 == null) {
            return null;
         }

         ArgumentTypeInfo.Template<?> var6 = var5.deserializeFromNetwork(param0);
         ResourceLocation var7 = (param1 & 16) != 0 ? param0.readResourceLocation() : null;
         return new ClientboundCommandsPacket.ArgumentNodeStub(var3, var6, var7);
      } else if (var2 == 1) {
         String var3 = param0.readUtf();
         return new ClientboundCommandsPacket.LiteralNodeStub(var3);
      } else {
         return null;
      }
   }

   private static ClientboundCommandsPacket.Entry createEntry(
      CommandNode<SharedSuggestionProvider> param0, Object2IntMap<CommandNode<SharedSuggestionProvider>> param1
   ) {
      int var2 = 0;
      int var3;
      if (param0.getRedirect() != null) {
         var2 |= 8;
         var3 = param1.getInt(param0.getRedirect());
      } else {
         var3 = 0;
      }

      if (param0.getCommand() != null) {
         var2 |= 4;
      }

      ClientboundCommandsPacket.NodeStub var4;
      if (param0 instanceof RootCommandNode) {
         var2 |= 0;
         var4 = null;
      } else if (param0 instanceof ArgumentCommandNode<SharedSuggestionProvider, ?> var6) {
         var4 = new ClientboundCommandsPacket.ArgumentNodeStub(var6);
         var2 |= 2;
         if (var6.getCustomSuggestions() != null) {
            var2 |= 16;
         }
      } else {
         if (!(param0 instanceof LiteralCommandNode var5)) {
            throw new UnsupportedOperationException("Unknown node type " + param0);
         }

         var4 = new ClientboundCommandsPacket.LiteralNodeStub(var5.getLiteral());
         var2 |= 1;
      }

      int[] var6 = param0.getChildren().stream().mapToInt(param1::getInt).toArray();
      return new ClientboundCommandsPacket.Entry(var4, var2, var3, var6);
   }

   public void handle(ClientGamePacketListener param0) {
      param0.handleCommands(this);
   }

   public RootCommandNode<SharedSuggestionProvider> getRoot(CommandBuildContext param0) {
      return (RootCommandNode<SharedSuggestionProvider>)new ClientboundCommandsPacket.NodeResolver(param0, this.entries).resolve(this.rootIndex);
   }

   static class ArgumentNodeStub implements ClientboundCommandsPacket.NodeStub {
      private final String id;
      private final ArgumentTypeInfo.Template<?> argumentType;
      @Nullable
      private final ResourceLocation suggestionId;

      @Nullable
      private static ResourceLocation getSuggestionId(@Nullable SuggestionProvider<SharedSuggestionProvider> param0) {
         return param0 != null ? SuggestionProviders.getName(param0) : null;
      }

      ArgumentNodeStub(String param0, ArgumentTypeInfo.Template<?> param1, @Nullable ResourceLocation param2) {
         this.id = param0;
         this.argumentType = param1;
         this.suggestionId = param2;
      }

      public ArgumentNodeStub(ArgumentCommandNode<SharedSuggestionProvider, ?> param0) {
         this(param0.getName(), ArgumentTypeInfos.unpack(param0.getType()), getSuggestionId(param0.getCustomSuggestions()));
      }

      @Override
      public ArgumentBuilder<SharedSuggestionProvider, ?> build(CommandBuildContext param0) {
         ArgumentType<?> var2 = this.argumentType.instantiate(param0);
         RequiredArgumentBuilder<SharedSuggestionProvider, ?> var3 = RequiredArgumentBuilder.argument(this.id, var2);
         if (this.suggestionId != null) {
            var3.suggests(SuggestionProviders.getProvider(this.suggestionId));
         }

         return var3;
      }

      @Override
      public void write(FriendlyByteBuf param0) {
         param0.writeUtf(this.id);
         serializeCap(param0, this.argumentType);
         if (this.suggestionId != null) {
            param0.writeResourceLocation(this.suggestionId);
         }
      }

      private static <A extends ArgumentType<?>> void serializeCap(FriendlyByteBuf param0, ArgumentTypeInfo.Template<A> param1) {
         serializeCap(param0, param1.type(), param1);
      }

      private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeCap(
         FriendlyByteBuf param0, ArgumentTypeInfo<A, T> param1, ArgumentTypeInfo.Template<A> param2
      ) {
         param0.writeVarInt(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getId(param1));
         param1.serializeToNetwork((T)param2, param0);
      }
   }

   static class Entry {
      @Nullable
      final ClientboundCommandsPacket.NodeStub stub;
      final int flags;
      final int redirect;
      final int[] children;

      Entry(@Nullable ClientboundCommandsPacket.NodeStub param0, int param1, int param2, int[] param3) {
         this.stub = param0;
         this.flags = param1;
         this.redirect = param2;
         this.children = param3;
      }

      public void write(FriendlyByteBuf param0) {
         param0.writeByte(this.flags);
         param0.writeVarIntArray(this.children);
         if ((this.flags & 8) != 0) {
            param0.writeVarInt(this.redirect);
         }

         if (this.stub != null) {
            this.stub.write(param0);
         }
      }

      public boolean canBuild(IntSet param0) {
         return (this.flags & 8) != 0 ? !param0.contains(this.redirect) : true;
      }

      public boolean canResolve(IntSet param0) {
         for (int var5 : this.children) {
            if (param0.contains(var5)) {
               return false;
            }
         }

         return true;
      }
   }

   static class LiteralNodeStub implements ClientboundCommandsPacket.NodeStub {
      private final String id;

      LiteralNodeStub(String param0) {
         this.id = param0;
      }

      @Override
      public ArgumentBuilder<SharedSuggestionProvider, ?> build(CommandBuildContext param0) {
         return LiteralArgumentBuilder.literal(this.id);
      }

      @Override
      public void write(FriendlyByteBuf param0) {
         param0.writeUtf(this.id);
      }
   }

   static class NodeResolver {
      private final CommandBuildContext context;
      private final List<ClientboundCommandsPacket.Entry> entries;
      private final List<CommandNode<SharedSuggestionProvider>> nodes;

      NodeResolver(CommandBuildContext param0, List<ClientboundCommandsPacket.Entry> param1) {
         this.context = param0;
         this.entries = param1;
         ObjectArrayList<CommandNode<SharedSuggestionProvider>> var3 = new ObjectArrayList<>();
         var3.size(param1.size());
         this.nodes = var3;
      }

      public CommandNode<SharedSuggestionProvider> resolve(int param0) {
         CommandNode<SharedSuggestionProvider> var2 = this.nodes.get(param0);
         if (var2 != null) {
            return var2;
         }

         ClientboundCommandsPacket.Entry var3 = this.entries.get(param0);
         CommandNode<SharedSuggestionProvider> var4;
         if (var3.stub == null) {
            var4 = new RootCommandNode<>();
         } else {
            ArgumentBuilder<SharedSuggestionProvider, ?> var5 = var3.stub.build(this.context);
            if ((var3.flags & 8) != 0) {
               var5.redirect(this.resolve(var3.redirect));
            }

            if ((var3.flags & 4) != 0) {
               var5.executes(l_param0 -> 0);
            }

            var4 = var5.build();
         }

         this.nodes.set(param0, var4);

         for (int var8 : var3.children) {
            CommandNode<SharedSuggestionProvider> var9 = this.resolve(var8);
            if (!(var9 instanceof RootCommandNode)) {
               var4.addChild(var9);
            }
         }

         return var4;
      }
   }

   interface NodeStub {
      ArgumentBuilder<SharedSuggestionProvider, ?> build(CommandBuildContext var1);

      void write(FriendlyByteBuf var1);
   }
}
