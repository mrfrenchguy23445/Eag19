package net.minecraft.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

/** Regression checks for the integrated server's queued, bidirectional transport. */
public final class LocalConnectionVerify {
   private static final class Listener implements PacketListener {
      final List<Integer> received = new ArrayList<>();
      int disconnected;
      public void onDisconnect(Component reason) { disconnected++; }
      public boolean isAcceptingMessages() { return true; }
   }
   private record Message(int value) implements Packet<Listener> {
      public void write(FriendlyByteBuf buffer) { throw new AssertionError("Local packets must not serialize"); }
      public void handle(Listener listener) { listener.received.add(value); }
   }
   private static void check(boolean condition, String message) {
      if (!condition) throw new AssertionError(message);
   }
   public static void main(String[] args) {
      Connection[] pair = Connection.createLocalPair();
      Listener client = new Listener(), server = new Listener();
      pair[0].setListener(client);
      pair[1].setListener(server);
      check(pair[0].isMemoryConnection() && pair[1].isConnected(), "local connection flags");
      check(pair[0].getRemoteAddress() != null, "address required by login ban checks");
      pair[0].send(new Message(1));
      pair[0].send(new Message(2));
      check(server.received.isEmpty(), "send must not reenter listeners");
      pair[1].tick();
      check(server.received.equals(List.of(1, 2)), "ordered delivery");
      pair[1].send(new Message(3));
      pair[0].tick();
      check(client.received.equals(List.of(3)), "reply delivery");
      pair[1].setReadOnly();
      pair[0].send(new Message(4));
      pair[1].tick();
      check(server.received.size() == 2, "read-only connection");
      pair[0].disconnect(Component.literal("Done"));
      check(!pair[0].isConnected() && !pair[1].isConnected(), "close both endpoints");
      pair[0].tick(); pair[1].tick(); pair[0].tick();
      check(client.disconnected == 1 && server.disconnected == 1, "disconnect notification exactly once");
      check(pair[1].getDisconnectedReason().getString().equals("Done"), "disconnect reason");
      System.out.println("Local connection verification passed");
   }
}
