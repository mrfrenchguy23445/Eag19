package net.minecraft.network.syncher;

import com.mojang.logging.LogUtils;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;

public class SynchedEntityData {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Object2IntMap<Class<? extends Entity>> ENTITY_ID_POOL = new Object2IntOpenHashMap<>();
   private static final int MAX_ID_VALUE = 254;
   private final Entity entity;
   private final Int2ObjectMap<SynchedEntityData.DataItem<?>> itemsById = new Int2ObjectOpenHashMap<>();
   private final Lock lock = new Lock();
   private boolean isDirty;

   public SynchedEntityData(Entity param0) {
      this.entity = param0;
   }

   public static <T> EntityDataAccessor<T> defineId(Class<? extends Entity> param0, EntityDataSerializer<T> param1) {
      if (LOGGER.isDebugEnabled()) {
         try {
            Class<?> var2 = Class.forName(Thread.currentThread().getStackTrace()[2].getClassName());
            if (!var2.equals(param0)) {
               LOGGER.debug("defineId called for: {} from {}", param0, var2, new RuntimeException());
            }
         } catch (ClassNotFoundException var5) {
         }
      }

      int var2;
      if (ENTITY_ID_POOL.containsKey(param0)) {
         var2 = ENTITY_ID_POOL.getInt(param0) + 1;
      } else {
         int var3 = 0;
         Class<?> var4 = param0;

         while (var4 != Entity.class) {
            var4 = var4.getSuperclass();
            if (ENTITY_ID_POOL.containsKey(var4)) {
               var3 = ENTITY_ID_POOL.getInt(var4) + 1;
               break;
            }
         }

         var2 = var3;
      }

      if (var2 > 254) {
         throw new IllegalArgumentException("Data value id is too big with " + var2 + "! (Max is 254)");
      }

      ENTITY_ID_POOL.put(param0, var2);
      return param1.createAccessor(var2);
   }

   public <T> void define(EntityDataAccessor<T> param0, T param1) {
      int var3 = param0.getId();
      if (var3 > 254) {
         throw new IllegalArgumentException("Data value id is too big with " + var3 + "! (Max is 254)");
      }

      if (this.itemsById.containsKey(var3)) {
         throw new IllegalArgumentException("Duplicate id value for " + var3 + "!");
      }

      if (EntityDataSerializers.getSerializedId(param0.getSerializer()) < 0) {
         throw new IllegalArgumentException("Unregistered serializer " + param0.getSerializer() + " for " + var3 + "!");
      }

      this.createDataItem(param0, param1);
   }

   private <T> void createDataItem(EntityDataAccessor<T> param0, T param1) {
      SynchedEntityData.DataItem<T> var3 = new SynchedEntityData.DataItem<>(param0, param1);
      this.lock.lock();
      this.itemsById.put(param0.getId(), var3);
      this.lock.unlock();
   }

   private <T> SynchedEntityData.DataItem<T> getItem(EntityDataAccessor<T> param0) {
      this.lock.lock();

      SynchedEntityData.DataItem<T> var2;
      try {
         var2 = (SynchedEntityData.DataItem<T>)this.itemsById.get(param0.getId());
      } catch (Throwable var3) {
         CrashReport var4 = CrashReport.forThrowable(var3, "Getting synched entity data");
         CrashReportCategory var5 = var4.addCategory("Synched entity data");
         var5.setDetail("Data ID", param0);
         throw new ReportedException(var4);
      } finally {
         this.lock.unlock();
      }

      return var2;
   }

   public <T> T get(EntityDataAccessor<T> param0) {
      return this.getItem(param0).getValue();
   }

   public <T> void set(EntityDataAccessor<T> param0, T param1) {
      this.set(param0, param1, false);
   }

   public <T> void set(EntityDataAccessor<T> param0, T param1, boolean param2) {
      SynchedEntityData.DataItem<T> var4 = this.getItem(param0);
      if (param2 || ObjectUtils.notEqual(param1, var4.getValue())) {
         var4.setValue(param1);
         this.entity.onSyncedDataUpdated(param0);
         var4.setDirty(true);
         this.isDirty = true;
      }
   }

   public boolean isDirty() {
      return this.isDirty;
   }

   @Nullable
   public List<SynchedEntityData.DataValue<?>> packDirty() {
      List<SynchedEntityData.DataValue<?>> var1 = null;
      if (this.isDirty) {
         this.lock.lock();

         for (SynchedEntityData.DataItem<?> var3 : this.itemsById.values()) {
            if (var3.isDirty()) {
               var3.setDirty(false);
               if (var1 == null) {
                  var1 = new ArrayList<>();
               }

               var1.add(var3.value());
            }
         }

         this.lock.unlock();
      }

      this.isDirty = false;
      return var1;
   }

   @Nullable
   public List<SynchedEntityData.DataValue<?>> getNonDefaultValues() {
      List<SynchedEntityData.DataValue<?>> var1 = null;
      this.lock.lock();

      for (SynchedEntityData.DataItem<?> var3 : this.itemsById.values()) {
         if (!var3.isSetToDefault()) {
            if (var1 == null) {
               var1 = new ArrayList<>();
            }

            var1.add(var3.value());
         }
      }

      this.lock.unlock();
      return var1;
   }

   public void assignValues(List<SynchedEntityData.DataValue<?>> param0) {
      this.lock.lock();

      try {
         for (SynchedEntityData.DataValue<?> var3 : param0) {
            SynchedEntityData.DataItem<?> var4 = this.itemsById.get(var3.id);
            if (var4 != null) {
               this.assignValue(var4, var3);
               this.entity.onSyncedDataUpdated(var4.getAccessor());
            }
         }
      } finally {
         this.lock.unlock();
      }

      this.entity.onSyncedDataUpdated(param0);
   }

   private <T> void assignValue(SynchedEntityData.DataItem<T> param0, SynchedEntityData.DataValue<?> param1) {
      if (!Objects.equals(param1.serializer(), param0.accessor.getSerializer())) {
         throw new IllegalStateException(
            String.format(
               Locale.ROOT,
               "Invalid entity data item type for field %d on entity %s: old=%s(%s), new=%s(%s)",
               param0.accessor.getId(),
               this.entity,
               param0.value,
               param0.value.getClass(),
               param1.value,
               param1.value.getClass()
            )
         );
      }

      param0.setValue((T)param1.value);
   }

   public boolean isEmpty() {
      return this.itemsById.isEmpty();
   }

   private static final class Lock {
      void lock() {
      }

      void unlock() {
      }
   }

   public static class DataItem<T> {
      final EntityDataAccessor<T> accessor;
      T value;
      private final T initialValue;
      private boolean dirty;

      public DataItem(EntityDataAccessor<T> param0, T param1) {
         this.accessor = param0;
         this.initialValue = param1;
         this.value = param1;
      }

      public EntityDataAccessor<T> getAccessor() {
         return this.accessor;
      }

      public void setValue(T param0) {
         this.value = param0;
      }

      public T getValue() {
         return this.value;
      }

      public boolean isDirty() {
         return this.dirty;
      }

      public void setDirty(boolean param0) {
         this.dirty = param0;
      }

      public boolean isSetToDefault() {
         return this.initialValue.equals(this.value);
      }

      public SynchedEntityData.DataValue<T> value() {
         return SynchedEntityData.DataValue.create(this.accessor, this.value);
      }
   }

   public record DataValue<T>(int id, EntityDataSerializer<T> serializer, T value) {

      public static <T> SynchedEntityData.DataValue<T> create(EntityDataAccessor<T> param0, T param1) {
         EntityDataSerializer<T> var2 = param0.getSerializer();
         return new SynchedEntityData.DataValue<>(param0.getId(), var2, var2.copy(param1));
      }

      public void write(FriendlyByteBuf param0) {
         int var2 = EntityDataSerializers.getSerializedId(this.serializer);
         if (var2 < 0) {
            throw new EncoderException("Unknown serializer type " + this.serializer);
         }

         param0.writeByte(this.id);
         param0.writeVarInt(var2);
         this.serializer.write(param0, this.value);
      }

      public static SynchedEntityData.DataValue<?> read(FriendlyByteBuf param0, int param1) {
         int var2 = param0.readVarInt();
         EntityDataSerializer<?> var3 = EntityDataSerializers.getSerializer(var2);
         if (var3 == null) {
            throw new DecoderException("Unknown serializer type " + var2);
         } else {
            return read(param0, param1, var3);
         }
      }

      private static <T> SynchedEntityData.DataValue<T> read(FriendlyByteBuf param0, int param1, EntityDataSerializer<T> param2) {
         return new SynchedEntityData.DataValue<>(param1, param2, param2.read(param0));
      }
   }
}
