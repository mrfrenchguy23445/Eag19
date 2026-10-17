package net.minecraft.server.rcon.thread;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.rcon.NetworkDataOutputStream;
import net.minecraft.server.rcon.PktUtils;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;

public class QueryThreadGs4 extends GenericThread {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String GAME_TYPE = "SMP";
   private static final String GAME_ID = "MINECRAFT";
   private static final long CHALLENGE_CHECK_INTERVAL = 30000L;
   private static final long RESPONSE_CACHE_TIME = 5000L;
   private long lastChallengeCheck;
   private final int port;
   private final int serverPort;
   private final int maxPlayers;
   private final String serverName;
   private final String worldName;
   private DatagramSocket socket;
   private final byte[] buffer = new byte[1460];
   private String hostIp;
   private String serverIp;
   private final Map<SocketAddress, QueryThreadGs4.RequestChallenge> validChallenges;
   private final NetworkDataOutputStream rulesResponse;
   private long lastRulesResponse;
   private final ServerInterface serverInterface;

   private QueryThreadGs4(ServerInterface param0, int param1) {
      super("Query Listener");
      this.serverInterface = param0;
      this.port = param1;
      this.serverIp = param0.getServerIp();
      this.serverPort = param0.getServerPort();
      this.serverName = param0.getServerName();
      this.maxPlayers = param0.getMaxPlayers();
      this.worldName = param0.getLevelIdName();
      this.lastRulesResponse = 0L;
      this.hostIp = "0.0.0.0";
      if (!this.serverIp.isEmpty() && !this.hostIp.equals(this.serverIp)) {
         this.hostIp = this.serverIp;
      } else {
         this.serverIp = "0.0.0.0";

         try {
            InetAddress var3 = InetAddress.getLocalHost();
            this.hostIp = var3.getHostAddress();
         } catch (UnknownHostException var3) {
            LOGGER.warn("Unable to determine local host IP, please set server-ip in server.properties", var3);
         }
      }

      this.rulesResponse = new NetworkDataOutputStream(1460);
      this.validChallenges = Maps.newHashMap();
   }

   @Nullable
   public static QueryThreadGs4 create(ServerInterface param0) {
      int var1 = param0.getProperties().queryPort;
      if (0 < var1 && 65535 >= var1) {
         QueryThreadGs4 var2 = new QueryThreadGs4(param0, var1);
         return !var2.start() ? null : var2;
      } else {
         LOGGER.warn("Invalid query port {} found in server.properties (queries disabled)", var1);
         return null;
      }
   }

   private void sendTo(byte[] param0, DatagramPacket param1) throws IOException {
      this.socket.send(new DatagramPacket(param0, param0.length, param1.getSocketAddress()));
   }

   private boolean processPacket(DatagramPacket param0) throws IOException {
      byte[] var2 = param0.getData();
      int var3 = param0.getLength();
      SocketAddress var4 = param0.getSocketAddress();
      LOGGER.debug("Packet len {} [{}]", var3, var4);
      if (3 <= var3 && -2 == var2[0] && -3 == var2[1]) {
         LOGGER.debug("Packet '{}' [{}]", PktUtils.toHexString(var2[2]), var4);
         switch (var2[2]) {
            case 0:
               if (!this.validChallenge(param0)) {
                  LOGGER.debug("Invalid challenge [{}]", var4);
                  return false;
               } else if (15 == var3) {
                  this.sendTo(this.buildRuleResponse(param0), param0);
                  LOGGER.debug("Rules [{}]", var4);
               } else {
                  NetworkDataOutputStream var5 = new NetworkDataOutputStream(1460);
                  var5.write(0);
                  var5.writeBytes(this.getIdentBytes(param0.getSocketAddress()));
                  var5.writeString(this.serverName);
                  var5.writeString("SMP");
                  var5.writeString(this.worldName);
                  var5.writeString(Integer.toString(this.serverInterface.getPlayerCount()));
                  var5.writeString(Integer.toString(this.maxPlayers));
                  var5.writeShort((short)this.serverPort);
                  var5.writeString(this.hostIp);
                  this.sendTo(var5.toByteArray(), param0);
                  LOGGER.debug("Status [{}]", var4);
               }
            default:
               return true;
            case 9:
               this.sendChallenge(param0);
               LOGGER.debug("Challenge [{}]", var4);
               return true;
         }
      } else {
         LOGGER.debug("Invalid packet [{}]", var4);
         return false;
      }
   }

   private byte[] buildRuleResponse(DatagramPacket param0) throws IOException {
      long var2 = Util.getMillis();
      if (var2 < this.lastRulesResponse + 5000L) {
         byte[] var4 = this.rulesResponse.toByteArray();
         byte[] var5 = this.getIdentBytes(param0.getSocketAddress());
         var4[1] = var5[0];
         var4[2] = var5[1];
         var4[3] = var5[2];
         var4[4] = var5[3];
         return var4;
      }

      this.lastRulesResponse = var2;
      this.rulesResponse.reset();
      this.rulesResponse.write(0);
      this.rulesResponse.writeBytes(this.getIdentBytes(param0.getSocketAddress()));
      this.rulesResponse.writeString("splitnum");
      this.rulesResponse.write(128);
      this.rulesResponse.write(0);
      this.rulesResponse.writeString("hostname");
      this.rulesResponse.writeString(this.serverName);
      this.rulesResponse.writeString("gametype");
      this.rulesResponse.writeString("SMP");
      this.rulesResponse.writeString("game_id");
      this.rulesResponse.writeString("MINECRAFT");
      this.rulesResponse.writeString("version");
      this.rulesResponse.writeString(this.serverInterface.getServerVersion());
      this.rulesResponse.writeString("plugins");
      this.rulesResponse.writeString(this.serverInterface.getPluginNames());
      this.rulesResponse.writeString("map");
      this.rulesResponse.writeString(this.worldName);
      this.rulesResponse.writeString("numplayers");
      this.rulesResponse.writeString(this.serverInterface.getPlayerCount() + "");
      this.rulesResponse.writeString("maxplayers");
      this.rulesResponse.writeString(this.maxPlayers + "");
      this.rulesResponse.writeString("hostport");
      this.rulesResponse.writeString(this.serverPort + "");
      this.rulesResponse.writeString("hostip");
      this.rulesResponse.writeString(this.hostIp);
      this.rulesResponse.write(0);
      this.rulesResponse.write(1);
      this.rulesResponse.writeString("player_");
      this.rulesResponse.write(0);
      String[] var4 = this.serverInterface.getPlayerNames();

      for (String var8 : var4) {
         this.rulesResponse.writeString(var8);
      }

      this.rulesResponse.write(0);
      return this.rulesResponse.toByteArray();
   }

   private byte[] getIdentBytes(SocketAddress param0) {
      return this.validChallenges.get(param0).getIdentBytes();
   }

   private Boolean validChallenge(DatagramPacket param0) {
      SocketAddress var2 = param0.getSocketAddress();
      if (!this.validChallenges.containsKey(var2)) {
         return false;
      }

      byte[] var3 = param0.getData();
      return this.validChallenges.get(var2).getChallenge() == PktUtils.intFromNetworkByteArray(var3, 7, param0.getLength());
   }

   private void sendChallenge(DatagramPacket param0) throws IOException {
      QueryThreadGs4.RequestChallenge var2 = new QueryThreadGs4.RequestChallenge(param0);
      this.validChallenges.put(param0.getSocketAddress(), var2);
      this.sendTo(var2.getChallengeBytes(), param0);
   }

   private void pruneChallenges() {
      if (this.running) {
         long var1 = Util.getMillis();
         if (var1 >= this.lastChallengeCheck + 30000L) {
            this.lastChallengeCheck = var1;
            this.validChallenges.values().removeIf(l_param2 -> l_param2.before(var1));
         }
      }
   }

   @Override
   public void run() {
      LOGGER.info("Query running on {}:{}", this.serverIp, this.port);
      this.lastChallengeCheck = Util.getMillis();
      DatagramPacket var1 = new DatagramPacket(this.buffer, this.buffer.length);

      try {
         while (this.running) {
            try {
               this.socket.receive(var1);
               this.pruneChallenges();
               this.processPacket(var1);
            } catch (SocketTimeoutException var2) {
               this.pruneChallenges();
            } catch (PortUnreachableException var9) {
            } catch (IOException var2) {
               this.recoverSocketError(var2);
            }
         }
      } finally {
         LOGGER.debug("closeSocket: {}:{}", this.serverIp, this.port);
         this.socket.close();
      }
   }

   @Override
   public boolean start() {
      if (this.running) {
         return true;
      } else {
         return !this.initSocket() ? false : super.start();
      }
   }

   private void recoverSocketError(Exception param0) {
      if (this.running) {
         LOGGER.warn("Unexpected exception", param0);
         if (!this.initSocket()) {
            LOGGER.error("Failed to recover from exception, shutting down!");
            this.running = false;
         }
      }
   }

   private boolean initSocket() {
      try {
         this.socket = new DatagramSocket(this.port, InetAddress.getByName(this.serverIp));
         this.socket.setSoTimeout(500);
         return true;
      } catch (Exception var1) {
         LOGGER.warn("Unable to initialise query system on {}:{}", this.serverIp, this.port, var1);
         return false;
      }
   }

   static class RequestChallenge {
      private final long time = new Date().getTime();
      private final int challenge;
      private final byte[] identBytes;
      private final byte[] challengeBytes;
      private final String ident;

      public RequestChallenge(DatagramPacket param0) {
         byte[] var2 = param0.getData();
         this.identBytes = new byte[4];
         this.identBytes[0] = var2[3];
         this.identBytes[1] = var2[4];
         this.identBytes[2] = var2[5];
         this.identBytes[3] = var2[6];
         this.ident = new String(this.identBytes, StandardCharsets.UTF_8);
         this.challenge = RandomSource.create().nextInt(16777216);
         this.challengeBytes = String.format(Locale.ROOT, "\t%s%d\u0000", this.ident, this.challenge).getBytes(StandardCharsets.UTF_8);
      }

      public Boolean before(long param0) {
         return this.time < param0;
      }

      public int getChallenge() {
         return this.challenge;
      }

      public byte[] getChallengeBytes() {
         return this.challengeBytes;
      }

      public byte[] getIdentBytes() {
         return this.identBytes;
      }

      public String getIdent() {
         return this.ident;
      }
   }
}
