package net.minecraft.world.level.entity;

import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.stream.Stream;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

public class EntitySection<T extends EntityAccess> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ClassInstanceMultiMap<T> storage;
   private Visibility chunkStatus;

   public EntitySection(Class<T> param0, Visibility param1) {
      this.chunkStatus = param1;
      this.storage = new ClassInstanceMultiMap<>(param0);
   }

   public void add(T param0) {
      this.storage.add(param0);
   }

   public boolean remove(T param0) {
      return this.storage.remove(param0);
   }

   public AbortableIterationConsumer.Continuation getEntities(AABB param0, AbortableIterationConsumer<T> param1) {
      for (T var4 : this.storage) {
         if (var4.getBoundingBox().intersects(param0) && param1.accept(var4).shouldAbort()) {
            return AbortableIterationConsumer.Continuation.ABORT;
         }
      }

      return AbortableIterationConsumer.Continuation.CONTINUE;
   }

   public <U extends T> AbortableIterationConsumer.Continuation getEntities(
      EntityTypeTest<T, U> param0, AABB param1, AbortableIterationConsumer<? super U> param2
   ) {
      Collection<? extends T> var4 = this.storage.find(param0.getBaseClass());
      if (var4.isEmpty()) {
         return AbortableIterationConsumer.Continuation.CONTINUE;
      }

      for (T var6 : var4) {
         U var7 = (U)param0.tryCast(var6);
         if (var7 != null && var6.getBoundingBox().intersects(param1) && param2.accept(var7).shouldAbort()) {
            return AbortableIterationConsumer.Continuation.ABORT;
         }
      }

      return AbortableIterationConsumer.Continuation.CONTINUE;
   }

   public boolean isEmpty() {
      return this.storage.isEmpty();
   }

   public Stream<T> getEntities() {
      return this.storage.stream();
   }

   public Visibility getStatus() {
      return this.chunkStatus;
   }

   public Visibility updateChunkStatus(Visibility param0) {
      Visibility var2 = this.chunkStatus;
      this.chunkStatus = param0;
      return var2;
   }

   @VisibleForDebug
   public int size() {
      return this.storage.size();
   }
}
