package net.minecraft.world.entity.ai.village.poi;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.SectionTracker;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.storage.SectionStorage;

public class PoiManager extends SectionStorage<PoiSection> {
   public static final int MAX_VILLAGE_DISTANCE = 6;
   public static final int VILLAGE_SECTION_SIZE = 1;
   private final PoiManager.DistanceTracker distanceTracker;
   private final LongSet loadedChunks = new LongOpenHashSet();

   public PoiManager(Path param0, DataFixer param1, boolean param2, RegistryAccess param3, LevelHeightAccessor param4) {
      super(param0, PoiSection::codec, PoiSection::new, param1, DataFixTypes.POI_CHUNK, param2, param3, param4);
      this.distanceTracker = new PoiManager.DistanceTracker();
   }

   public void add(BlockPos param0, Holder<PoiType> param1) {
      this.getOrCreate(SectionPos.asLong(param0)).add(param0, param1);
   }

   public void remove(BlockPos param0) {
      this.getOrLoad(SectionPos.asLong(param0)).ifPresent(l_param1 -> l_param1.remove(param0));
   }

   public long getCountInRange(Predicate<Holder<PoiType>> param0, BlockPos param1, int param2, PoiManager.Occupancy param3) {
      return this.getInRange(param0, param1, param2, param3).count();
   }

   public boolean existsAtPosition(ResourceKey<PoiType> param0, BlockPos param1) {
      return this.exists(param1, l_param1 -> l_param1.is(param0));
   }

   public Stream<PoiRecord> getInSquare(Predicate<Holder<PoiType>> param0, BlockPos param1, int param2, PoiManager.Occupancy param3) {
      int var5 = Math.floorDiv(param2, 16) + 1;
      return ChunkPos.rangeClosed(new ChunkPos(param1), var5).flatMap(l_param2 -> this.getInChunk(param0, l_param2, param3)).filter(l_param2 -> {
         BlockPos l_var3 = l_param2.getPos();
         return Math.abs(l_var3.getX() - param1.getX()) <= param2 && Math.abs(l_var3.getZ() - param1.getZ()) <= param2;
      });
   }

   public Stream<PoiRecord> getInRange(Predicate<Holder<PoiType>> param0, BlockPos param1, int param2, PoiManager.Occupancy param3) {
      int var5 = param2 * param2;
      return this.getInSquare(param0, param1, param2, param3).filter(l_param2 -> l_param2.getPos().distSqr(param1) <= var5);
   }

   @VisibleForDebug
   public Stream<PoiRecord> getInChunk(Predicate<Holder<PoiType>> param0, ChunkPos param1, PoiManager.Occupancy param2) {
      return IntStream.range(this.levelHeightAccessor.getMinSection(), this.levelHeightAccessor.getMaxSection())
         .boxed()
         .map(l_param1 -> this.getOrLoad(SectionPos.of(param1, l_param1).asLong()))
         .filter(Optional::isPresent)
         .flatMap(l_param2 -> l_param2.get().getRecords(param0, param2));
   }

   public Stream<BlockPos> findAll(Predicate<Holder<PoiType>> param0, Predicate<BlockPos> param1, BlockPos param2, int param3, PoiManager.Occupancy param4) {
      return this.getInRange(param0, param2, param3, param4).map(PoiRecord::getPos).filter(param1);
   }

   public Stream<Pair<Holder<PoiType>, BlockPos>> findAllWithType(
      Predicate<Holder<PoiType>> param0, Predicate<BlockPos> param1, BlockPos param2, int param3, PoiManager.Occupancy param4
   ) {
      return this.getInRange(param0, param2, param3, param4)
         .filter(l_param1 -> param1.test(l_param1.getPos()))
         .map(l_param0 -> Pair.of(l_param0.getPoiType(), l_param0.getPos()));
   }

   public Stream<Pair<Holder<PoiType>, BlockPos>> findAllClosestFirstWithType(
      Predicate<Holder<PoiType>> param0, Predicate<BlockPos> param1, BlockPos param2, int param3, PoiManager.Occupancy param4
   ) {
      return this.findAllWithType(param0, param1, param2, param3, param4).sorted(Comparator.comparingDouble(l_param1 -> l_param1.getSecond().distSqr(param2)));
   }

   public Optional<BlockPos> find(Predicate<Holder<PoiType>> param0, Predicate<BlockPos> param1, BlockPos param2, int param3, PoiManager.Occupancy param4) {
      return this.findAll(param0, param1, param2, param3, param4).findFirst();
   }

   public Optional<BlockPos> findClosest(Predicate<Holder<PoiType>> param0, BlockPos param1, int param2, PoiManager.Occupancy param3) {
      return this.getInRange(param0, param1, param2, param3).map(PoiRecord::getPos).min(Comparator.comparingDouble(l_param1 -> l_param1.distSqr(param1)));
   }

   public Optional<Pair<Holder<PoiType>, BlockPos>> findClosestWithType(
      Predicate<Holder<PoiType>> param0, BlockPos param1, int param2, PoiManager.Occupancy param3
   ) {
      return this.getInRange(param0, param1, param2, param3)
         .min(Comparator.comparingDouble(l_param1 -> l_param1.getPos().distSqr(param1)))
         .map(l_param0 -> Pair.of(l_param0.getPoiType(), l_param0.getPos()));
   }

   public Optional<BlockPos> findClosest(
      Predicate<Holder<PoiType>> param0, Predicate<BlockPos> param1, BlockPos param2, int param3, PoiManager.Occupancy param4
   ) {
      return this.getInRange(param0, param2, param3, param4)
         .map(PoiRecord::getPos)
         .filter(param1)
         .min(Comparator.comparingDouble(l_param1 -> l_param1.distSqr(param2)));
   }

   public Optional<BlockPos> take(Predicate<Holder<PoiType>> param0, BiPredicate<Holder<PoiType>, BlockPos> param1, BlockPos param2, int param3) {
      return this.getInRange(param0, param2, param3, PoiManager.Occupancy.HAS_SPACE)
         .filter(l_param1 -> param1.test(l_param1.getPoiType(), l_param1.getPos()))
         .findFirst()
         .map(l_param0 -> {
            l_param0.acquireTicket();
            return l_param0.getPos();
         });
   }

   public Optional<BlockPos> getRandom(
      Predicate<Holder<PoiType>> param0, Predicate<BlockPos> param1, PoiManager.Occupancy param2, BlockPos param3, int param4, RandomSource param5
   ) {
      List<PoiRecord> var7 = Util.toShuffledList(this.getInRange(param0, param3, param4, param2), param5);
      return var7.stream().filter(l_param1 -> param1.test(l_param1.getPos())).findFirst().map(PoiRecord::getPos);
   }

   public boolean release(BlockPos param0) {
      return this.getOrLoad(SectionPos.asLong(param0))
         .map(l_param1 -> l_param1.release(param0))
         .orElseThrow(() -> Util.pauseInIde(new IllegalStateException("POI never registered at " + param0)));
   }

   public boolean exists(BlockPos param0, Predicate<Holder<PoiType>> param1) {
      return this.getOrLoad(SectionPos.asLong(param0)).map(l_param2 -> l_param2.exists(param0, param1)).orElse(false);
   }

   public Optional<Holder<PoiType>> getType(BlockPos param0) {
      return this.getOrLoad(SectionPos.asLong(param0)).flatMap(l_param1 -> l_param1.getType(param0));
   }

   @Deprecated
   @VisibleForDebug
   public int getFreeTickets(BlockPos param0) {
      return this.getOrLoad(SectionPos.asLong(param0)).map(l_param1 -> l_param1.getFreeTickets(param0)).orElse(0);
   }

   public int sectionsToVillage(SectionPos param0) {
      this.distanceTracker.runAllUpdates();
      return this.distanceTracker.getLevel(param0.asLong());
   }

   boolean isVillageCenter(long param0) {
      Optional<PoiSection> var3 = this.get(param0);
      return var3 == null
         ? false
         : var3.<Boolean>map(
               l_param0 -> l_param0.getRecords(l2_param0 -> l2_param0.is(PoiTypeTags.VILLAGE), PoiManager.Occupancy.IS_OCCUPIED).findAny().isPresent()
            )
            .orElse(false);
   }

   @Override
   public void tick(BooleanSupplier param0) {
      super.tick(param0);
      this.distanceTracker.runAllUpdates();
   }

   @Override
   protected void setDirty(long param0) {
      super.setDirty(param0);
      this.distanceTracker.update(param0, this.distanceTracker.getLevelFromSource(param0), false);
   }

   @Override
   protected void onSectionLoad(long param0) {
      this.distanceTracker.update(param0, this.distanceTracker.getLevelFromSource(param0), false);
   }

   public void checkConsistencyWithBlocks(ChunkPos param0, LevelChunkSection param1) {
      SectionPos var3 = SectionPos.of(param0, SectionPos.blockToSectionCoord(param1.bottomBlockY()));
      Util.ifElse(this.getOrLoad(var3.asLong()), l_param2 -> l_param2.refresh(l2_param2 -> {
         if (mayHavePoi(param1)) {
            this.updateFromSection(param1, var3, l2_param2);
         }
      }), () -> {
         if (mayHavePoi(param1)) {
            PoiSection l_var3 = this.getOrCreate(var3.asLong());
            this.updateFromSection(param1, var3, l_var3::add);
         }
      });
   }

   private static boolean mayHavePoi(LevelChunkSection param0) {
      return param0.maybeHas(PoiTypes::hasPoi);
   }

   private void updateFromSection(LevelChunkSection param0, SectionPos param1, BiConsumer<BlockPos, Holder<PoiType>> param2) {
      param1.blocksInside()
         .forEach(
            l_param2 -> {
               BlockState l_var3 = param0.getBlockState(
                  SectionPos.sectionRelative(l_param2.getX()), SectionPos.sectionRelative(l_param2.getY()), SectionPos.sectionRelative(l_param2.getZ())
               );
               PoiTypes.forState(l_var3).ifPresent(l2_param2 -> param2.accept(l_param2, (Holder<PoiType>)l2_param2));
            }
         );
   }

   public void ensureLoadedAndValid(LevelReader param0, BlockPos param1, int param2) {
      SectionPos.aroundChunk(
            new ChunkPos(param1), Math.floorDiv(param2, 16), this.levelHeightAccessor.getMinSection(), this.levelHeightAccessor.getMaxSection()
         )
         .map(l_param0 -> Pair.of(l_param0, this.getOrLoad(l_param0.asLong())))
         .filter(l_param0 -> !l_param0.getSecond().map(PoiSection::isValid).orElse(false))
         .map(l_param0 -> l_param0.getFirst().chunk())
         .filter(l_param0 -> this.loadedChunks.add(l_param0.toLong()))
         .forEach(l_param1 -> param0.getChunk(l_param1.x, l_param1.z, ChunkStatus.EMPTY));
   }

   final class DistanceTracker extends SectionTracker {
      private final Long2ByteMap levels = new Long2ByteOpenHashMap();

      protected DistanceTracker() {
         super(7, 16, 256);
         this.levels.defaultReturnValue((byte)7);
      }

      @Override
      protected int getLevelFromSource(long param0) {
         return PoiManager.this.isVillageCenter(param0) ? 0 : 7;
      }

      @Override
      protected int getLevel(long param0) {
         return this.levels.get(param0);
      }

      @Override
      protected void setLevel(long param0, int param2) {
         if (param2 > 6) {
            this.levels.remove(param0);
         } else {
            this.levels.put(param0, (byte)param2);
         }
      }

      public void runAllUpdates() {
         super.runUpdates(Integer.MAX_VALUE);
      }
   }

   public enum Occupancy {
      HAS_SPACE(PoiRecord::hasSpace),
      IS_OCCUPIED(PoiRecord::isOccupied),
      ANY(param0 -> true);

      private final Predicate<? super PoiRecord> test;

      Occupancy(Predicate<? super PoiRecord> param2) {
         this.test = param2;
      }

      public Predicate<? super PoiRecord> getTest() {
         return this.test;
      }
   }
}
