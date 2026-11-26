package net.minecraft.world.level.chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;

public class HashMapPalette<T> implements Palette<T> {
   private final IdMap<T> registry;
   private final CrudeIncrementalIntIdentityHashBiMap<T> values;
   private final PaletteResize<T> resizeHandler;
   private final int bits;

   public HashMapPalette(IdMap<T> param0, int param1, PaletteResize<T> param2, List<T> param3) {
      this(param0, param1, param2);
      param3.forEach(this.values::add);
   }

   public HashMapPalette(IdMap<T> param0, int param1, PaletteResize<T> param2) {
      this(param0, param1, param2, CrudeIncrementalIntIdentityHashBiMap.create(1 << param1));
   }

   private HashMapPalette(IdMap<T> param0, int param1, PaletteResize<T> param2, CrudeIncrementalIntIdentityHashBiMap<T> param3) {
      this.registry = param0;
      this.bits = param1;
      this.resizeHandler = param2;
      this.values = param3;
   }

   public static <A> Palette<A> create(int param0, IdMap<A> param1, PaletteResize<A> param2, List<A> param3) {
      return new HashMapPalette<>(param1, param0, param2, param3);
   }

   @Override
   public int idFor(T param0) {
      int var2 = this.values.getId(param0);
      if (var2 == -1) {
         var2 = this.values.add(param0);
         if (var2 >= 1 << this.bits) {
            var2 = this.resizeHandler.onResize(this.bits + 1, param0);
         }
      }

      return var2;
   }

   @Override
   public boolean maybeHas(Predicate<T> param0) {
      for (int var2 = 0; var2 < this.getSize(); var2++) {
         if (param0.test(this.values.byId(var2))) {
            return true;
         }
      }

      return false;
   }

   @Override
   public T valueFor(int param0) {
      T var2 = this.values.byId(param0);
      if (var2 == null) {
         throw new MissingPaletteEntryException(param0);
      } else {
         return var2;
      }
   }

   @Override
   public void read(FriendlyByteBuf param0) {
      this.values.clear();
      int var2 = param0.readVarInt();

      for (int var3 = 0; var3 < var2; var3++) {
         this.values.add(this.registry.byIdOrThrow(param0.readVarInt()));
      }
   }

   @Override
   public void write(FriendlyByteBuf param0) {
      int var2 = this.getSize();
      param0.writeVarInt(var2);

      for (int var3 = 0; var3 < var2; var3++) {
         param0.writeVarInt(this.registry.getId(this.values.byId(var3)));
      }
   }

   @Override
   public int getSerializedSize() {
      int var1 = FriendlyByteBuf.getVarIntSize(this.getSize());

      for (int var2 = 0; var2 < this.getSize(); var2++) {
         var1 += FriendlyByteBuf.getVarIntSize(this.registry.getId(this.values.byId(var2)));
      }

      return var1;
   }

   public List<T> getEntries() {
      ArrayList<T> var1 = new ArrayList<>();
      this.values.iterator().forEachRemaining(var1::add);
      return var1;
   }

   @Override
   public int getSize() {
      return this.values.size();
   }

   @Override
   public Palette<T> copy() {
      return new HashMapPalette<>(this.registry, this.bits, this.resizeHandler, this.values.copy());
   }
}
