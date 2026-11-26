package net.minecraft.server.level;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.ChunkPos;

public class TickingTracker extends ChunkTracker {
   private static final int INITIAL_TICKET_LIST_CAPACITY = 4;
   protected final Long2ByteMap chunks = new Long2ByteOpenHashMap();
   private final Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets = new Long2ObjectOpenHashMap<>();

   public TickingTracker() {
      super(34, 16, 256);
      this.chunks.defaultReturnValue((byte)33);
   }

   private SortedArraySet<Ticket<?>> getTickets(long param0) {
      return this.tickets.computeIfAbsent(param0, l_param0 -> SortedArraySet.create(4));
   }

   private int getTicketLevelAt(SortedArraySet<Ticket<?>> param0) {
      return param0.isEmpty() ? 34 : param0.first().getTicketLevel();
   }

   public void addTicket(long param0, Ticket<?> param2) {
      SortedArraySet<Ticket<?>> var4 = this.getTickets(param0);
      int var5 = this.getTicketLevelAt(var4);
      var4.add(param2);
      if (param2.getTicketLevel() < var5) {
         this.update(param0, param2.getTicketLevel(), true);
      }
   }

   public void removeTicket(long param0, Ticket<?> param2) {
      SortedArraySet<Ticket<?>> var4 = this.getTickets(param0);
      var4.remove(param2);
      if (var4.isEmpty()) {
         this.tickets.remove(param0);
      }

      this.update(param0, this.getTicketLevelAt(var4), false);
   }

   public <T> void addTicket(TicketType<T> param0, ChunkPos param1, int param2, T param3) {
      this.addTicket(param1.toLong(), new Ticket<>(param0, param2, param3));
   }

   public <T> void removeTicket(TicketType<T> param0, ChunkPos param1, int param2, T param3) {
      Ticket<T> var5 = new Ticket<>(param0, param2, param3);
      this.removeTicket(param1.toLong(), var5);
   }

   public void replacePlayerTicketsLevel(int param0) {
      List<Pair<Ticket<ChunkPos>, Long>> var2 = new ArrayList<>();

      for (Entry<SortedArraySet<Ticket<?>>> var4 : this.tickets.long2ObjectEntrySet()) {
         for (Ticket<?> var6 : var4.getValue()) {
            if (var6.getType() == TicketType.PLAYER) {
               var2.add(Pair.of((Ticket<ChunkPos>)var6, var4.getLongKey()));
            }
         }
      }

      for (Pair<Ticket<ChunkPos>, Long> var4 : var2) {
         Long var5 = var4.getSecond();
         Ticket<ChunkPos> var6 = var4.getFirst();
         this.removeTicket(var5, var6);
         ChunkPos var7 = new ChunkPos(var5);
         TicketType<ChunkPos> var8 = var6.getType();
         this.addTicket(var8, var7, param0, var7);
      }
   }

   @Override
   protected int getLevelFromSource(long param0) {
      SortedArraySet<Ticket<?>> var3 = this.tickets.get(param0);
      return var3 != null && !var3.isEmpty() ? var3.first().getTicketLevel() : Integer.MAX_VALUE;
   }

   public int getLevel(ChunkPos param0) {
      return this.getLevel(param0.toLong());
   }

   @Override
   protected int getLevel(long param0) {
      return this.chunks.get(param0);
   }

   @Override
   protected void setLevel(long param0, int param2) {
      if (param2 > 33) {
         this.chunks.remove(param0);
      } else {
         this.chunks.put(param0, (byte)param2);
      }
   }

   public void runAllUpdates() {
      this.runUpdates(Integer.MAX_VALUE);
   }

   public String getTicketDebugString(long param0) {
      SortedArraySet<Ticket<?>> var3 = this.tickets.get(param0);
      return var3 != null && !var3.isEmpty() ? var3.first().toString() : "no_ticket";
   }
}
