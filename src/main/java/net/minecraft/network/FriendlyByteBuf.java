package net.minecraft.network;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class FriendlyByteBuf extends ByteBuf {
   private static final int MAX_VARINT_SIZE = 5;
   private static final int MAX_VARLONG_SIZE = 10;
   public static final int DEFAULT_NBT_QUOTA = 2097152;
   private final ByteBuf source;
   public static final short MAX_STRING_LENGTH = 32767;
   public static final int MAX_COMPONENT_STRING_LENGTH = 262144;
   private static final int PUBLIC_KEY_SIZE = 256;
   private static final int MAX_PUBLIC_KEY_HEADER_SIZE = 256;
   private static final int MAX_PUBLIC_KEY_LENGTH = 512;
   private static final Gson GSON = new Gson();

   public FriendlyByteBuf(ByteBuf param0) {
      this.source = param0;
   }

   public static int getVarIntSize(int param0) {
      for (int var1 = 1; var1 < 5; var1++) {
         if ((param0 & -1 << var1 * 7) == 0) {
            return var1;
         }
      }

      return 5;
   }

   public static int getVarLongSize(long param0) {
      for (int var2 = 1; var2 < 10; var2++) {
         if ((param0 & -1L << var2 * 7) == 0L) {
            return var2;
         }
      }

      return 10;
   }

   @Deprecated
   public <T> T readWithCodec(DynamicOps<Tag> param0, Codec<T> param1) {
      CompoundTag var3 = this.readAnySizeNbt();
      return Util.getOrThrow(param1.parse(param0, var3), l_param1 -> new DecoderException("Failed to decode: " + l_param1 + " " + var3));
   }

   @Deprecated
   public <T> void writeWithCodec(DynamicOps<Tag> param0, Codec<T> param1, T param2) {
      Tag var4 = Util.getOrThrow(param1.encodeStart(param0, param2), l_param1 -> new EncoderException("Failed to encode: " + l_param1 + " " + param2));
      this.writeNbt((CompoundTag)var4);
   }

   public <T> T readJsonWithCodec(Codec<T> param0) {
      JsonElement var2 = GsonHelper.fromJson(GSON, this.readUtf(), JsonElement.class);
      DataResult<T> var3 = param0.parse(JsonOps.INSTANCE, var2);
      return Util.getOrThrow(var3, l_param0 -> new DecoderException("Failed to decode json: " + l_param0));
   }

   public <T> void writeJsonWithCodec(Codec<T> param0, T param1) {
      DataResult<JsonElement> var3 = param0.encodeStart(JsonOps.INSTANCE, param1);
      this.writeUtf(GSON.toJson(Util.getOrThrow(var3, l_param1 -> new EncoderException("Failed to encode: " + l_param1 + " " + param1))));
   }

   public <T> void writeId(IdMap<T> param0, T param1) {
      int var3 = param0.getId(param1);
      if (var3 == -1) {
         throw new IllegalArgumentException("Can't find id for '" + param1 + "' in map " + param0);
      }

      this.writeVarInt(var3);
   }

   public <T> void writeId(IdMap<Holder<T>> param0, Holder<T> param1, FriendlyByteBuf.Writer<T> param2) {
      switch (param1.kind()) {
         case REFERENCE:
            int var4 = param0.getId(param1);
            if (var4 == -1) {
               throw new IllegalArgumentException("Can't find id for '" + param1.value() + "' in map " + param0);
            }

            this.writeVarInt(var4 + 1);
            break;
         case DIRECT:
            this.writeVarInt(0);
            param2.accept(this, param1.value());
      }
   }

   @Nullable
   public <T> T readById(IdMap<T> param0) {
      int var2 = this.readVarInt();
      return param0.byId(var2);
   }

   public <T> Holder<T> readById(IdMap<Holder<T>> param0, FriendlyByteBuf.Reader<T> param1) {
      int var3 = this.readVarInt();
      if (var3 == 0) {
         return Holder.direct(param1.apply(this));
      } else {
         Holder<T> var4 = param0.byId(var3 - 1);
         if (var4 == null) {
            throw new IllegalArgumentException("Can't find element with id " + var3);
         } else {
            return var4;
         }
      }
   }

   public static <T> IntFunction<T> limitValue(IntFunction<T> param0, int param1) {
      return l_param2 -> {
         if (l_param2 > param1) {
            throw new DecoderException("Value " + l_param2 + " is larger than limit " + param1);
         } else {
            return param0.apply(l_param2);
         }
      };
   }

   public <T, C extends Collection<T>> C readCollection(IntFunction<C> param0, FriendlyByteBuf.Reader<T> param1) {
      int var3 = this.readVarInt();
      C var4 = (C)param0.apply(var3);

      for (int var5 = 0; var5 < var3; var5++) {
         var4.add(param1.apply(this));
      }

      return var4;
   }

   public <T> void writeCollection(Collection<T> param0, FriendlyByteBuf.Writer<T> param1) {
      this.writeVarInt(param0.size());

      for (T var4 : param0) {
         param1.accept(this, var4);
      }
   }

   public <T> List<T> readList(FriendlyByteBuf.Reader<T> param0) {
      return this.readCollection(Lists::newArrayListWithCapacity, param0);
   }

   public IntList readIntIdList() {
      int var1 = this.readVarInt();
      IntList var2 = new IntArrayList();

      for (int var3 = 0; var3 < var1; var3++) {
         var2.add(this.readVarInt());
      }

      return var2;
   }

   public void writeIntIdList(IntList param0) {
      this.writeVarInt(param0.size());
      param0.forEach(this::writeVarInt);
   }

   public <K, V, M extends Map<K, V>> M readMap(IntFunction<M> param0, FriendlyByteBuf.Reader<K> param1, FriendlyByteBuf.Reader<V> param2) {
      int var4 = this.readVarInt();
      M var5 = (M)param0.apply(var4);

      for (int var6 = 0; var6 < var4; var6++) {
         K var7 = param1.apply(this);
         V var8 = param2.apply(this);
         var5.put(var7, var8);
      }

      return var5;
   }

   public <K, V> Map<K, V> readMap(FriendlyByteBuf.Reader<K> param0, FriendlyByteBuf.Reader<V> param1) {
      return this.readMap(Maps::newHashMapWithExpectedSize, param0, param1);
   }

   public <K, V> void writeMap(Map<K, V> param0, FriendlyByteBuf.Writer<K> param1, FriendlyByteBuf.Writer<V> param2) {
      this.writeVarInt(param0.size());
      param0.forEach((l_param2, l_param3) -> {
         param1.accept(this, (K)l_param2);
         param2.accept(this, (V)l_param3);
      });
   }

   public void readWithCount(Consumer<FriendlyByteBuf> param0) {
      int var2 = this.readVarInt();

      for (int var3 = 0; var3 < var2; var3++) {
         param0.accept(this);
      }
   }

   public <E extends Enum<E>> void writeEnumSet(EnumSet<E> param0, Class<E> param1) {
      E[] var3 = (E[])param1.getEnumConstants();
      BitSet var4 = new BitSet(var3.length);

      for (int var5 = 0; var5 < var3.length; var5++) {
         var4.set(var5, param0.contains(var3[var5]));
      }

      this.writeFixedBitSet(var4, var3.length);
   }

   public <E extends Enum<E>> EnumSet<E> readEnumSet(Class<E> param0) {
      E[] var2 = (E[])param0.getEnumConstants();
      BitSet var3 = this.readFixedBitSet(var2.length);
      EnumSet<E> var4 = EnumSet.noneOf(param0);

      for (int var5 = 0; var5 < var2.length; var5++) {
         if (var3.get(var5)) {
            var4.add(var2[var5]);
         }
      }

      return var4;
   }

   public <T> void writeOptional(Optional<T> param0, FriendlyByteBuf.Writer<T> param1) {
      if (param0.isPresent()) {
         this.writeBoolean(true);
         param1.accept(this, param0.get());
      } else {
         this.writeBoolean(false);
      }
   }

   public <T> Optional<T> readOptional(FriendlyByteBuf.Reader<T> param0) {
      return this.readBoolean() ? Optional.of(param0.apply(this)) : Optional.empty();
   }

   @Nullable
   public <T> T readNullable(FriendlyByteBuf.Reader<T> param0) {
      return this.readBoolean() ? param0.apply(this) : null;
   }

   public <T> void writeNullable(@Nullable T param0, FriendlyByteBuf.Writer<T> param1) {
      if (param0 != null) {
         this.writeBoolean(true);
         param1.accept(this, param0);
      } else {
         this.writeBoolean(false);
      }
   }

   public <L, R> void writeEither(Either<L, R> param0, FriendlyByteBuf.Writer<L> param1, FriendlyByteBuf.Writer<R> param2) {
      param0.ifLeft(l_param1 -> {
         this.writeBoolean(true);
         param1.accept(this, (L)l_param1);
      }).ifRight(l_param1 -> {
         this.writeBoolean(false);
         param2.accept(this, (R)l_param1);
      });
   }

   public <L, R> Either<L, R> readEither(FriendlyByteBuf.Reader<L> param0, FriendlyByteBuf.Reader<R> param1) {
      return this.readBoolean() ? Either.left(param0.apply(this)) : Either.right(param1.apply(this));
   }

   public byte[] readByteArray() {
      return this.readByteArray(this.readableBytes());
   }

   public FriendlyByteBuf writeByteArray(byte[] param0) {
      this.writeVarInt(param0.length);
      this.writeBytes(param0);
      return this;
   }

   public byte[] readByteArray(int param0) {
      int var2 = this.readVarInt();
      if (var2 > param0) {
         throw new DecoderException("ByteArray with size " + var2 + " is bigger than allowed " + param0);
      }

      byte[] var3 = new byte[var2];
      this.readBytes(var3);
      return var3;
   }

   public FriendlyByteBuf writeVarIntArray(int[] param0) {
      this.writeVarInt(param0.length);

      for (int var5 : param0) {
         this.writeVarInt(var5);
      }

      return this;
   }

   public int[] readVarIntArray() {
      return this.readVarIntArray(this.readableBytes());
   }

   public int[] readVarIntArray(int param0) {
      int var2 = this.readVarInt();
      if (var2 > param0) {
         throw new DecoderException("VarIntArray with size " + var2 + " is bigger than allowed " + param0);
      }

      int[] var3 = new int[var2];

      for (int var4 = 0; var4 < var3.length; var4++) {
         var3[var4] = this.readVarInt();
      }

      return var3;
   }

   public FriendlyByteBuf writeLongArray(long[] param0) {
      this.writeVarInt(param0.length);

      for (long var5 : param0) {
         this.writeLong(var5);
      }

      return this;
   }

   public long[] readLongArray() {
      return this.readLongArray(null);
   }

   public long[] readLongArray(@Nullable long[] param0) {
      return this.readLongArray(param0, this.readableBytes() / 8);
   }

   public long[] readLongArray(@Nullable long[] param0, int param1) {
      int var3 = this.readVarInt();
      if (param0 == null || param0.length != var3) {
         if (var3 > param1) {
            throw new DecoderException("LongArray with size " + var3 + " is bigger than allowed " + param1);
         }

         param0 = new long[var3];
      }

      for (int var4 = 0; var4 < param0.length; var4++) {
         param0[var4] = this.readLong();
      }

      return param0;
   }

   @VisibleForTesting
   public byte[] accessByteBufWithCorrectSize() {
      int var1 = this.writerIndex();
      byte[] var2 = new byte[var1];
      this.getBytes(0, var2);
      return var2;
   }

   public BlockPos readBlockPos() {
      return BlockPos.of(this.readLong());
   }

   public FriendlyByteBuf writeBlockPos(BlockPos param0) {
      this.writeLong(param0.asLong());
      return this;
   }

   public ChunkPos readChunkPos() {
      return new ChunkPos(this.readLong());
   }

   public FriendlyByteBuf writeChunkPos(ChunkPos param0) {
      this.writeLong(param0.toLong());
      return this;
   }

   public SectionPos readSectionPos() {
      return SectionPos.of(this.readLong());
   }

   public FriendlyByteBuf writeSectionPos(SectionPos param0) {
      this.writeLong(param0.asLong());
      return this;
   }

   public GlobalPos readGlobalPos() {
      ResourceKey<Level> var1 = this.readResourceKey(Registries.DIMENSION);
      BlockPos var2 = this.readBlockPos();
      return GlobalPos.of(var1, var2);
   }

   public void writeGlobalPos(GlobalPos param0) {
      this.writeResourceKey(param0.dimension());
      this.writeBlockPos(param0.pos());
   }

   public Vector3f readVector3f() {
      return new Vector3f(this.readFloat(), this.readFloat(), this.readFloat());
   }

   public void writeVector3f(Vector3f param0) {
      this.writeFloat(param0.x());
      this.writeFloat(param0.y());
      this.writeFloat(param0.z());
   }

   public Quaternionf readQuaternion() {
      return new Quaternionf(this.readFloat(), this.readFloat(), this.readFloat(), this.readFloat());
   }

   public void writeQuaternion(Quaternionf param0) {
      this.writeFloat(param0.x);
      this.writeFloat(param0.y);
      this.writeFloat(param0.z);
      this.writeFloat(param0.w);
   }

   public Component readComponent() {
      Component var1 = Component.Serializer.fromJson(this.readUtf(262144));
      if (var1 == null) {
         throw new DecoderException("Received unexpected null component");
      } else {
         return var1;
      }
   }

   public FriendlyByteBuf writeComponent(Component param0) {
      return this.writeUtf(Component.Serializer.toJson(param0), 262144);
   }

   public <T extends Enum<T>> T readEnum(Class<T> param0) {
      return param0.getEnumConstants()[this.readVarInt()];
   }

   public FriendlyByteBuf writeEnum(Enum<?> param0) {
      return this.writeVarInt(param0.ordinal());
   }

   public int readVarInt() {
      int var1 = 0;
      int var2 = 0;

      byte var3;
      do {
         var3 = this.readByte();
         var1 |= (var3 & 127) << var2++ * 7;
         if (var2 > 5) {
            throw new RuntimeException("VarInt too big");
         }
      } while ((var3 & 128) == 128);

      return var1;
   }

   public long readVarLong() {
      long var1 = 0L;
      int var3 = 0;

      byte var4;
      do {
         var4 = this.readByte();
         var1 |= (long)(var4 & 127) << var3++ * 7;
         if (var3 > 10) {
            throw new RuntimeException("VarLong too big");
         }
      } while ((var4 & 128) == 128);

      return var1;
   }

   public FriendlyByteBuf writeUUID(UUID param0) {
      this.writeLong(param0.getMostSignificantBits());
      this.writeLong(param0.getLeastSignificantBits());
      return this;
   }

   public UUID readUUID() {
      return new UUID(this.readLong(), this.readLong());
   }

   public FriendlyByteBuf writeVarInt(int param0) {
      while ((param0 & -128) != 0) {
         this.writeByte(param0 & 127 | 128);
         param0 >>>= 7;
      }

      this.writeByte(param0);
      return this;
   }

   public FriendlyByteBuf writeVarLong(long param0) {
      while ((param0 & -128L) != 0L) {
         this.writeByte((int)(param0 & 127L) | 128);
         param0 >>>= 7;
      }

      this.writeByte((int)param0);
      return this;
   }

   public FriendlyByteBuf writeNbt(@Nullable CompoundTag param0) {
      if (param0 == null) {
         this.writeByte(0);
      } else {
         try {
            NbtIo.write(param0, new ByteBufOutputStream(this));
         } catch (IOException var2) {
            throw new EncoderException(var2);
         }
      }

      return this;
   }

   @Nullable
   public CompoundTag readNbt() {
      return this.readNbt(new NbtAccounter(2097152L));
   }

   @Nullable
   public CompoundTag readAnySizeNbt() {
      return this.readNbt(NbtAccounter.UNLIMITED);
   }

   @Nullable
   public CompoundTag readNbt(NbtAccounter param0) {
      int var2 = this.readerIndex();
      byte var3 = this.readByte();
      if (var3 == 0) {
         return null;
      }

      this.readerIndex(var2);

      try {
         return NbtIo.read(new ByteBufInputStream(this), param0);
      } catch (IOException var4) {
         throw new EncoderException(var4);
      }
   }

   public FriendlyByteBuf writeItem(ItemStack param0) {
      if (param0.isEmpty()) {
         this.writeBoolean(false);
      } else {
         this.writeBoolean(true);
         Item var2 = param0.getItem();
         this.writeId(BuiltInRegistries.ITEM, var2);
         this.writeByte(param0.getCount());
         CompoundTag var3 = null;
         if (var2.canBeDepleted() || var2.shouldOverrideMultiplayerNbt()) {
            var3 = param0.getTag();
         }

         this.writeNbt(var3);
      }

      return this;
   }

   public ItemStack readItem() {
      if (!this.readBoolean()) {
         return ItemStack.EMPTY;
      }

      Item var1 = this.readById(BuiltInRegistries.ITEM);
      int var2 = this.readByte();
      ItemStack var3 = new ItemStack(var1, var2);
      var3.setTag(this.readNbt());
      return var3;
   }

   public String readUtf() {
      return this.readUtf(32767);
   }

   public String readUtf(int param0) {
      int var2 = getMaxEncodedUtfLength(param0);
      int var3 = this.readVarInt();
      if (var3 > var2) {
         throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + var3 + " > " + var2 + ")");
      } else if (var3 < 0) {
         throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
      } else {
         String var4 = this.toString(this.readerIndex(), var3, StandardCharsets.UTF_8);
         this.readerIndex(this.readerIndex() + var3);
         if (var4.length() > param0) {
            throw new DecoderException("The received string length is longer than maximum allowed (" + var4.length() + " > " + param0 + ")");
         } else {
            return var4;
         }
      }
   }

   public FriendlyByteBuf writeUtf(String param0) {
      return this.writeUtf(param0, 32767);
   }

   public FriendlyByteBuf writeUtf(String param0, int param1) {
      if (param0.length() > param1) {
         throw new EncoderException("String too big (was " + param0.length() + " characters, max " + param1 + ")");
      }

      byte[] var3 = param0.getBytes(StandardCharsets.UTF_8);
      int var4 = getMaxEncodedUtfLength(param1);
      if (var3.length > var4) {
         throw new EncoderException("String too big (was " + var3.length + " bytes encoded, max " + var4 + ")");
      }

      this.writeVarInt(var3.length);
      this.writeBytes(var3);
      return this;
   }

   private static int getMaxEncodedUtfLength(int param0) {
      return param0 * 3;
   }

   public ResourceLocation readResourceLocation() {
      return new ResourceLocation(this.readUtf(32767));
   }

   public FriendlyByteBuf writeResourceLocation(ResourceLocation param0) {
      this.writeUtf(param0.toString());
      return this;
   }

   public <T> ResourceKey<T> readResourceKey(ResourceKey<? extends Registry<T>> param0) {
      ResourceLocation var2 = this.readResourceLocation();
      return ResourceKey.create(param0, var2);
   }

   public void writeResourceKey(ResourceKey<?> param0) {
      this.writeResourceLocation(param0.location());
   }

   public Date readDate() {
      return new Date(this.readLong());
   }

   public FriendlyByteBuf writeDate(Date param0) {
      this.writeLong(param0.getTime());
      return this;
   }

   public Instant readInstant() {
      return Instant.ofEpochMilli(this.readLong());
   }

   public void writeInstant(Instant param0) {
      this.writeLong(param0.toEpochMilli());
   }

   public PublicKey readPublicKey() {
      try {
         return Crypt.byteToPublicKey(this.readByteArray(512));
      } catch (CryptException var1) {
         throw new DecoderException("Malformed public key bytes", var1);
      }
   }

   public FriendlyByteBuf writePublicKey(PublicKey param0) {
      this.writeByteArray(param0.getEncoded());
      return this;
   }

   public BlockHitResult readBlockHitResult() {
      BlockPos var1 = this.readBlockPos();
      Direction var2 = this.readEnum(Direction.class);
      float var3 = this.readFloat();
      float var4 = this.readFloat();
      float var5 = this.readFloat();
      boolean var6 = this.readBoolean();
      return new BlockHitResult(new Vec3((double)var1.getX() + var3, (double)var1.getY() + var4, (double)var1.getZ() + var5), var2, var1, var6);
   }

   public void writeBlockHitResult(BlockHitResult param0) {
      BlockPos var2 = param0.getBlockPos();
      this.writeBlockPos(var2);
      this.writeEnum(param0.getDirection());
      Vec3 var3 = param0.getLocation();
      this.writeFloat((float)(var3.x - var2.getX()));
      this.writeFloat((float)(var3.y - var2.getY()));
      this.writeFloat((float)(var3.z - var2.getZ()));
      this.writeBoolean(param0.isInside());
   }

   public BitSet readBitSet() {
      return BitSet.valueOf(this.readLongArray());
   }

   public void writeBitSet(BitSet param0) {
      this.writeLongArray(param0.toLongArray());
   }

   public BitSet readFixedBitSet(int param0) {
      byte[] var2 = new byte[Mth.positiveCeilDiv(param0, 8)];
      this.readBytes(var2);
      return BitSet.valueOf(var2);
   }

   public void writeFixedBitSet(BitSet param0, int param1) {
      if (param0.length() > param1) {
         throw new EncoderException("BitSet is larger than expected size (" + param0.length() + ">" + param1 + ")");
      }

      byte[] var3 = param0.toByteArray();
      this.writeBytes(Arrays.copyOf(var3, Mth.positiveCeilDiv(param1, 8)));
   }

   public GameProfile readGameProfile() {
      UUID var1 = this.readUUID();
      String var2 = this.readUtf(16);
      GameProfile var3 = new GameProfile(var1, var2);
      var3.getProperties().putAll(this.readGameProfileProperties());
      return var3;
   }

   public void writeGameProfile(GameProfile param0) {
      this.writeUUID(param0.getId());
      this.writeUtf(param0.getName());
      this.writeGameProfileProperties(param0.getProperties());
   }

   public PropertyMap readGameProfileProperties() {
      PropertyMap var1 = new PropertyMap();
      this.readWithCount(l_param1 -> {
         Property l_var3 = this.readProperty();
         var1.put(l_var3.getName(), l_var3);
      });
      return var1;
   }

   public void writeGameProfileProperties(PropertyMap param0) {
      this.writeCollection(param0.values(), FriendlyByteBuf::writeProperty);
   }

   public Property readProperty() {
      String var1 = this.readUtf();
      String var2 = this.readUtf();
      if (this.readBoolean()) {
         String var3 = this.readUtf();
         return new Property(var1, var2, var3);
      } else {
         return new Property(var1, var2);
      }
   }

   public void writeProperty(Property param0) {
      this.writeUtf(param0.getName());
      this.writeUtf(param0.getValue());
      if (param0.hasSignature()) {
         this.writeBoolean(true);
         this.writeUtf(param0.getSignature());
      } else {
         this.writeBoolean(false);
      }
   }

   @Override
   public int capacity() {
      return this.source.capacity();
   }

   @Override
   public ByteBuf capacity(int param0) {
      return this.source.capacity(param0);
   }

   @Override
   public int maxCapacity() {
      return this.source.maxCapacity();
   }

   @Override
   public ByteBufAllocator alloc() {
      return this.source.alloc();
   }

   @Override
   public ByteOrder order() {
      return this.source.order();
   }

   @Override
   public ByteBuf order(ByteOrder param0) {
      return this.source.order(param0);
   }

   @Override
   public ByteBuf unwrap() {
      return this.source.unwrap();
   }

   @Override
   public boolean isDirect() {
      return this.source.isDirect();
   }

   @Override
   public boolean isReadOnly() {
      return this.source.isReadOnly();
   }

   @Override
   public ByteBuf asReadOnly() {
      return this.source.asReadOnly();
   }

   @Override
   public int readerIndex() {
      return this.source.readerIndex();
   }

   @Override
   public ByteBuf readerIndex(int param0) {
      return this.source.readerIndex(param0);
   }

   @Override
   public int writerIndex() {
      return this.source.writerIndex();
   }

   @Override
   public ByteBuf writerIndex(int param0) {
      return this.source.writerIndex(param0);
   }

   @Override
   public ByteBuf setIndex(int param0, int param1) {
      return this.source.setIndex(param0, param1);
   }

   @Override
   public int readableBytes() {
      return this.source.readableBytes();
   }

   @Override
   public int writableBytes() {
      return this.source.writableBytes();
   }

   @Override
   public int maxWritableBytes() {
      return this.source.maxWritableBytes();
   }

   @Override
   public boolean isReadable() {
      return this.source.isReadable();
   }

   @Override
   public boolean isReadable(int param0) {
      return this.source.isReadable(param0);
   }

   @Override
   public boolean isWritable() {
      return this.source.isWritable();
   }

   @Override
   public boolean isWritable(int param0) {
      return this.source.isWritable(param0);
   }

   @Override
   public ByteBuf clear() {
      return this.source.clear();
   }

   @Override
   public ByteBuf markReaderIndex() {
      return this.source.markReaderIndex();
   }

   @Override
   public ByteBuf resetReaderIndex() {
      return this.source.resetReaderIndex();
   }

   @Override
   public ByteBuf markWriterIndex() {
      return this.source.markWriterIndex();
   }

   @Override
   public ByteBuf resetWriterIndex() {
      return this.source.resetWriterIndex();
   }

   @Override
   public ByteBuf discardReadBytes() {
      return this.source.discardReadBytes();
   }

   @Override
   public ByteBuf discardSomeReadBytes() {
      return this.source.discardSomeReadBytes();
   }

   @Override
   public ByteBuf ensureWritable(int param0) {
      return this.source.ensureWritable(param0);
   }

   @Override
   public int ensureWritable(int param0, boolean param1) {
      return this.source.ensureWritable(param0, param1);
   }

   @Override
   public boolean getBoolean(int param0) {
      return this.source.getBoolean(param0);
   }

   @Override
   public byte getByte(int param0) {
      return this.source.getByte(param0);
   }

   @Override
   public short getUnsignedByte(int param0) {
      return this.source.getUnsignedByte(param0);
   }

   @Override
   public short getShort(int param0) {
      return this.source.getShort(param0);
   }

   @Override
   public short getShortLE(int param0) {
      return this.source.getShortLE(param0);
   }

   @Override
   public int getUnsignedShort(int param0) {
      return this.source.getUnsignedShort(param0);
   }

   @Override
   public int getUnsignedShortLE(int param0) {
      return this.source.getUnsignedShortLE(param0);
   }

   @Override
   public int getMedium(int param0) {
      return this.source.getMedium(param0);
   }

   @Override
   public int getMediumLE(int param0) {
      return this.source.getMediumLE(param0);
   }

   @Override
   public int getUnsignedMedium(int param0) {
      return this.source.getUnsignedMedium(param0);
   }

   @Override
   public int getUnsignedMediumLE(int param0) {
      return this.source.getUnsignedMediumLE(param0);
   }

   @Override
   public int getInt(int param0) {
      return this.source.getInt(param0);
   }

   @Override
   public int getIntLE(int param0) {
      return this.source.getIntLE(param0);
   }

   @Override
   public long getUnsignedInt(int param0) {
      return this.source.getUnsignedInt(param0);
   }

   @Override
   public long getUnsignedIntLE(int param0) {
      return this.source.getUnsignedIntLE(param0);
   }

   @Override
   public long getLong(int param0) {
      return this.source.getLong(param0);
   }

   @Override
   public long getLongLE(int param0) {
      return this.source.getLongLE(param0);
   }

   @Override
   public char getChar(int param0) {
      return this.source.getChar(param0);
   }

   @Override
   public float getFloat(int param0) {
      return this.source.getFloat(param0);
   }

   @Override
   public double getDouble(int param0) {
      return this.source.getDouble(param0);
   }

   @Override
   public ByteBuf getBytes(int param0, ByteBuf param1) {
      return this.source.getBytes(param0, param1);
   }

   @Override
   public ByteBuf getBytes(int param0, ByteBuf param1, int param2) {
      return this.source.getBytes(param0, param1, param2);
   }

   @Override
   public ByteBuf getBytes(int param0, ByteBuf param1, int param2, int param3) {
      return this.source.getBytes(param0, param1, param2, param3);
   }

   @Override
   public ByteBuf getBytes(int param0, byte[] param1) {
      return this.source.getBytes(param0, param1);
   }

   @Override
   public ByteBuf getBytes(int param0, byte[] param1, int param2, int param3) {
      return this.source.getBytes(param0, param1, param2, param3);
   }

   @Override
   public ByteBuf getBytes(int param0, ByteBuffer param1) {
      return this.source.getBytes(param0, param1);
   }

   @Override
   public ByteBuf getBytes(int param0, OutputStream param1, int param2) throws IOException {
      return this.source.getBytes(param0, param1, param2);
   }

   @Override
   public int getBytes(int param0, GatheringByteChannel param1, int param2) throws IOException {
      return this.source.getBytes(param0, param1, param2);
   }

   @Override
   public int getBytes(int param0, FileChannel param1, long param2, int param4) throws IOException {
      return this.source.getBytes(param0, param1, param2, param4);
   }

   @Override
   public CharSequence getCharSequence(int param0, int param1, Charset param2) {
      return this.source.getCharSequence(param0, param1, param2);
   }

   @Override
   public ByteBuf setBoolean(int param0, boolean param1) {
      return this.source.setBoolean(param0, param1);
   }

   @Override
   public ByteBuf setByte(int param0, int param1) {
      return this.source.setByte(param0, param1);
   }

   @Override
   public ByteBuf setShort(int param0, int param1) {
      return this.source.setShort(param0, param1);
   }

   @Override
   public ByteBuf setShortLE(int param0, int param1) {
      return this.source.setShortLE(param0, param1);
   }

   @Override
   public ByteBuf setMedium(int param0, int param1) {
      return this.source.setMedium(param0, param1);
   }

   @Override
   public ByteBuf setMediumLE(int param0, int param1) {
      return this.source.setMediumLE(param0, param1);
   }

   @Override
   public ByteBuf setInt(int param0, int param1) {
      return this.source.setInt(param0, param1);
   }

   @Override
   public ByteBuf setIntLE(int param0, int param1) {
      return this.source.setIntLE(param0, param1);
   }

   @Override
   public ByteBuf setLong(int param0, long param1) {
      return this.source.setLong(param0, param1);
   }

   @Override
   public ByteBuf setLongLE(int param0, long param1) {
      return this.source.setLongLE(param0, param1);
   }

   @Override
   public ByteBuf setChar(int param0, int param1) {
      return this.source.setChar(param0, param1);
   }

   @Override
   public ByteBuf setFloat(int param0, float param1) {
      return this.source.setFloat(param0, param1);
   }

   @Override
   public ByteBuf setDouble(int param0, double param1) {
      return this.source.setDouble(param0, param1);
   }

   @Override
   public ByteBuf setBytes(int param0, ByteBuf param1) {
      return this.source.setBytes(param0, param1);
   }

   @Override
   public ByteBuf setBytes(int param0, ByteBuf param1, int param2) {
      return this.source.setBytes(param0, param1, param2);
   }

   @Override
   public ByteBuf setBytes(int param0, ByteBuf param1, int param2, int param3) {
      return this.source.setBytes(param0, param1, param2, param3);
   }

   @Override
   public ByteBuf setBytes(int param0, byte[] param1) {
      return this.source.setBytes(param0, param1);
   }

   @Override
   public ByteBuf setBytes(int param0, byte[] param1, int param2, int param3) {
      return this.source.setBytes(param0, param1, param2, param3);
   }

   @Override
   public ByteBuf setBytes(int param0, ByteBuffer param1) {
      return this.source.setBytes(param0, param1);
   }

   @Override
   public int setBytes(int param0, InputStream param1, int param2) throws IOException {
      return this.source.setBytes(param0, param1, param2);
   }

   @Override
   public int setBytes(int param0, ScatteringByteChannel param1, int param2) throws IOException {
      return this.source.setBytes(param0, param1, param2);
   }

   @Override
   public int setBytes(int param0, FileChannel param1, long param2, int param4) throws IOException {
      return this.source.setBytes(param0, param1, param2, param4);
   }

   @Override
   public ByteBuf setZero(int param0, int param1) {
      return this.source.setZero(param0, param1);
   }

   @Override
   public int setCharSequence(int param0, CharSequence param1, Charset param2) {
      return this.source.setCharSequence(param0, param1, param2);
   }

   @Override
   public boolean readBoolean() {
      return this.source.readBoolean();
   }

   @Override
   public byte readByte() {
      return this.source.readByte();
   }

   @Override
   public short readUnsignedByte() {
      return this.source.readUnsignedByte();
   }

   @Override
   public short readShort() {
      return this.source.readShort();
   }

   @Override
   public short readShortLE() {
      return this.source.readShortLE();
   }

   @Override
   public int readUnsignedShort() {
      return this.source.readUnsignedShort();
   }

   @Override
   public int readUnsignedShortLE() {
      return this.source.readUnsignedShortLE();
   }

   @Override
   public int readMedium() {
      return this.source.readMedium();
   }

   @Override
   public int readMediumLE() {
      return this.source.readMediumLE();
   }

   @Override
   public int readUnsignedMedium() {
      return this.source.readUnsignedMedium();
   }

   @Override
   public int readUnsignedMediumLE() {
      return this.source.readUnsignedMediumLE();
   }

   @Override
   public int readInt() {
      return this.source.readInt();
   }

   @Override
   public int readIntLE() {
      return this.source.readIntLE();
   }

   @Override
   public long readUnsignedInt() {
      return this.source.readUnsignedInt();
   }

   @Override
   public long readUnsignedIntLE() {
      return this.source.readUnsignedIntLE();
   }

   @Override
   public long readLong() {
      return this.source.readLong();
   }

   @Override
   public long readLongLE() {
      return this.source.readLongLE();
   }

   @Override
   public char readChar() {
      return this.source.readChar();
   }

   @Override
   public float readFloat() {
      return this.source.readFloat();
   }

   @Override
   public double readDouble() {
      return this.source.readDouble();
   }

   @Override
   public ByteBuf readBytes(int param0) {
      return this.source.readBytes(param0);
   }

   @Override
   public ByteBuf readSlice(int param0) {
      return this.source.readSlice(param0);
   }

   @Override
   public ByteBuf readRetainedSlice(int param0) {
      return this.source.readRetainedSlice(param0);
   }

   @Override
   public ByteBuf readBytes(ByteBuf param0) {
      return this.source.readBytes(param0);
   }

   @Override
   public ByteBuf readBytes(ByteBuf param0, int param1) {
      return this.source.readBytes(param0, param1);
   }

   @Override
   public ByteBuf readBytes(ByteBuf param0, int param1, int param2) {
      return this.source.readBytes(param0, param1, param2);
   }

   @Override
   public ByteBuf readBytes(byte[] param0) {
      return this.source.readBytes(param0);
   }

   @Override
   public ByteBuf readBytes(byte[] param0, int param1, int param2) {
      return this.source.readBytes(param0, param1, param2);
   }

   @Override
   public ByteBuf readBytes(ByteBuffer param0) {
      return this.source.readBytes(param0);
   }

   @Override
   public ByteBuf readBytes(OutputStream param0, int param1) throws IOException {
      return this.source.readBytes(param0, param1);
   }

   @Override
   public int readBytes(GatheringByteChannel param0, int param1) throws IOException {
      return this.source.readBytes(param0, param1);
   }

   @Override
   public CharSequence readCharSequence(int param0, Charset param1) {
      return this.source.readCharSequence(param0, param1);
   }

   @Override
   public int readBytes(FileChannel param0, long param1, int param3) throws IOException {
      return this.source.readBytes(param0, param1, param3);
   }

   @Override
   public ByteBuf skipBytes(int param0) {
      return this.source.skipBytes(param0);
   }

   @Override
   public ByteBuf writeBoolean(boolean param0) {
      return this.source.writeBoolean(param0);
   }

   @Override
   public ByteBuf writeByte(int param0) {
      return this.source.writeByte(param0);
   }

   @Override
   public ByteBuf writeShort(int param0) {
      return this.source.writeShort(param0);
   }

   @Override
   public ByteBuf writeShortLE(int param0) {
      return this.source.writeShortLE(param0);
   }

   @Override
   public ByteBuf writeMedium(int param0) {
      return this.source.writeMedium(param0);
   }

   @Override
   public ByteBuf writeMediumLE(int param0) {
      return this.source.writeMediumLE(param0);
   }

   @Override
   public ByteBuf writeInt(int param0) {
      return this.source.writeInt(param0);
   }

   @Override
   public ByteBuf writeIntLE(int param0) {
      return this.source.writeIntLE(param0);
   }

   @Override
   public ByteBuf writeLong(long param0) {
      return this.source.writeLong(param0);
   }

   @Override
   public ByteBuf writeLongLE(long param0) {
      return this.source.writeLongLE(param0);
   }

   @Override
   public ByteBuf writeChar(int param0) {
      return this.source.writeChar(param0);
   }

   @Override
   public ByteBuf writeFloat(float param0) {
      return this.source.writeFloat(param0);
   }

   @Override
   public ByteBuf writeDouble(double param0) {
      return this.source.writeDouble(param0);
   }

   @Override
   public ByteBuf writeBytes(ByteBuf param0) {
      return this.source.writeBytes(param0);
   }

   @Override
   public ByteBuf writeBytes(ByteBuf param0, int param1) {
      return this.source.writeBytes(param0, param1);
   }

   @Override
   public ByteBuf writeBytes(ByteBuf param0, int param1, int param2) {
      return this.source.writeBytes(param0, param1, param2);
   }

   @Override
   public ByteBuf writeBytes(byte[] param0) {
      return this.source.writeBytes(param0);
   }

   @Override
   public ByteBuf writeBytes(byte[] param0, int param1, int param2) {
      return this.source.writeBytes(param0, param1, param2);
   }

   @Override
   public ByteBuf writeBytes(ByteBuffer param0) {
      return this.source.writeBytes(param0);
   }

   @Override
   public int writeBytes(InputStream param0, int param1) throws IOException {
      return this.source.writeBytes(param0, param1);
   }

   @Override
   public int writeBytes(ScatteringByteChannel param0, int param1) throws IOException {
      return this.source.writeBytes(param0, param1);
   }

   @Override
   public int writeBytes(FileChannel param0, long param1, int param3) throws IOException {
      return this.source.writeBytes(param0, param1, param3);
   }

   @Override
   public ByteBuf writeZero(int param0) {
      return this.source.writeZero(param0);
   }

   @Override
   public int writeCharSequence(CharSequence param0, Charset param1) {
      return this.source.writeCharSequence(param0, param1);
   }

   @Override
   public int indexOf(int param0, int param1, byte param2) {
      return this.source.indexOf(param0, param1, param2);
   }

   @Override
   public int bytesBefore(byte param0) {
      return this.source.bytesBefore(param0);
   }

   @Override
   public int bytesBefore(int param0, byte param1) {
      return this.source.bytesBefore(param0, param1);
   }

   @Override
   public int bytesBefore(int param0, int param1, byte param2) {
      return this.source.bytesBefore(param0, param1, param2);
   }

   @Override
   public int forEachByte(ByteProcessor param0) {
      return this.source.forEachByte(param0);
   }

   @Override
   public int forEachByte(int param0, int param1, ByteProcessor param2) {
      return this.source.forEachByte(param0, param1, param2);
   }

   @Override
   public int forEachByteDesc(ByteProcessor param0) {
      return this.source.forEachByteDesc(param0);
   }

   @Override
   public int forEachByteDesc(int param0, int param1, ByteProcessor param2) {
      return this.source.forEachByteDesc(param0, param1, param2);
   }

   @Override
   public ByteBuf copy() {
      return this.source.copy();
   }

   @Override
   public ByteBuf copy(int param0, int param1) {
      return this.source.copy(param0, param1);
   }

   @Override
   public ByteBuf slice() {
      return this.source.slice();
   }

   @Override
   public ByteBuf retainedSlice() {
      return this.source.retainedSlice();
   }

   @Override
   public ByteBuf slice(int param0, int param1) {
      return this.source.slice(param0, param1);
   }

   @Override
   public ByteBuf retainedSlice(int param0, int param1) {
      return this.source.retainedSlice(param0, param1);
   }

   @Override
   public ByteBuf duplicate() {
      return this.source.duplicate();
   }

   @Override
   public ByteBuf retainedDuplicate() {
      return this.source.retainedDuplicate();
   }

   @Override
   public int nioBufferCount() {
      return this.source.nioBufferCount();
   }

   @Override
   public ByteBuffer nioBuffer() {
      return this.source.nioBuffer();
   }

   @Override
   public ByteBuffer nioBuffer(int param0, int param1) {
      return this.source.nioBuffer(param0, param1);
   }

   @Override
   public ByteBuffer internalNioBuffer(int param0, int param1) {
      return this.source.internalNioBuffer(param0, param1);
   }

   @Override
   public ByteBuffer[] nioBuffers() {
      return this.source.nioBuffers();
   }

   @Override
   public ByteBuffer[] nioBuffers(int param0, int param1) {
      return this.source.nioBuffers(param0, param1);
   }

   @Override
   public boolean hasArray() {
      return this.source.hasArray();
   }

   @Override
   public byte[] array() {
      return this.source.array();
   }

   @Override
   public int arrayOffset() {
      return this.source.arrayOffset();
   }

   @Override
   public boolean hasMemoryAddress() {
      return this.source.hasMemoryAddress();
   }

   @Override
   public long memoryAddress() {
      return this.source.memoryAddress();
   }

   @Override
   public String toString(Charset param0) {
      return this.source.toString(param0);
   }

   @Override
   public String toString(int param0, int param1, Charset param2) {
      return this.source.toString(param0, param1, param2);
   }

   @Override
   public int hashCode() {
      return this.source.hashCode();
   }

   @Override
   public boolean equals(Object param0) {
      return this.source.equals(param0);
   }

   @Override
   public int compareTo(ByteBuf param0) {
      return this.source.compareTo(param0);
   }

   @Override
   public String toString() {
      return this.source.toString();
   }

   @Override
   public ByteBuf retain(int param0) {
      return this.source.retain(param0);
   }

   @Override
   public ByteBuf retain() {
      return this.source.retain();
   }

   @Override
   public ByteBuf touch() {
      return this.source.touch();
   }

   @Override
   public ByteBuf touch(Object param0) {
      return this.source.touch(param0);
   }

   @Override
   public int refCnt() {
      return this.source.refCnt();
   }

   @Override
   public boolean release() {
      return this.source.release();
   }

   @Override
   public boolean release(int param0) {
      return this.source.release(param0);
   }

   @FunctionalInterface
   public interface Reader<T> extends Function<FriendlyByteBuf, T> {
      default FriendlyByteBuf.Reader<Optional<T>> asOptional() {
         return l_param0 -> l_param0.readOptional(this);
      }
   }

   @FunctionalInterface
   public interface Writer<T> extends BiConsumer<FriendlyByteBuf, T> {
      default FriendlyByteBuf.Writer<Optional<T>> asOptional() {
         return (l_param0, l_param1) -> l_param0.writeOptional(l_param1, this);
      }
   }
}
