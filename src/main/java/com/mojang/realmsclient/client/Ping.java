package com.mojang.realmsclient.client;

import com.google.common.collect.Lists;
import com.mojang.realmsclient.dto.RegionPingResult;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Comparator;
import java.util.List;
import net.minecraft.Util;
import org.apache.commons.io.IOUtils;

public class Ping {
   public static List<RegionPingResult> ping(Ping.Region... param0) {
      for (Ping.Region var4 : param0) {
         ping(var4.endpoint);
      }

      List<RegionPingResult> var1 = Lists.newArrayList();

      for (Ping.Region var5 : param0) {
         var1.add(new RegionPingResult(var5.name, ping(var5.endpoint)));
      }

      var1.sort(Comparator.comparingInt(RegionPingResult::ping));
      return var1;
   }

   private static int ping(String param0) {
      int var1 = 700;
      long var2 = 0L;
      Socket var4 = null;

      for (int var5 = 0; var5 < 5; var5++) {
         try {
            SocketAddress var6 = new InetSocketAddress(param0, 80);
            var4 = new Socket();
            long var7 = now();
            var4.connect(var6, 700);
            var2 += now() - var7;
         } catch (Exception var6) {
            var2 += 700L;
         } finally {
            IOUtils.closeQuietly(var4);
         }
      }

      return (int)(var2 / 5.0);
   }

   private static long now() {
      return Util.getMillis();
   }

   /**
    * Port note (no platform networking): refused, returns no regions.
    *
    * <p>Each region was timed by opening a raw {@link Socket} to it on port 80 and measuring how
    * long the connect took, five times, averaging. The classlib has no {@code java.net} -- no
    * {@link Socket}, no {@link SocketAddress}, no {@link InetSocketAddress} -- and a browser
    * cannot open a raw TCP connection to an arbitrary host, which is the whole operation.
    *
    * <p>There is also nothing to report to. The one caller is
    * {@code RealmsMainScreen.pingRegions}, which times the regions so it can post the result back
    * to Mojang's Realms service; this port has no Mojang account and no Realms service to post to.
    *
    * <p>An empty list is the truthful answer rather than a placeholder: it says the regions were
    * not measured, and leaves the region picker empty, which is what it already shows when Realms
    * is unavailable. The timing code below it is left in place for when there is a transport that
    * can reach a host; nothing links it, so it costs the build nothing.
    */
   public static List<RegionPingResult> pingAllRegions() {
      return List.of();
   }

   enum Region {
      US_EAST_1("us-east-1", "ec2.us-east-1.amazonaws.com"),
      US_WEST_2("us-west-2", "ec2.us-west-2.amazonaws.com"),
      US_WEST_1("us-west-1", "ec2.us-west-1.amazonaws.com"),
      EU_WEST_1("eu-west-1", "ec2.eu-west-1.amazonaws.com"),
      AP_SOUTHEAST_1("ap-southeast-1", "ec2.ap-southeast-1.amazonaws.com"),
      AP_SOUTHEAST_2("ap-southeast-2", "ec2.ap-southeast-2.amazonaws.com"),
      AP_NORTHEAST_1("ap-northeast-1", "ec2.ap-northeast-1.amazonaws.com"),
      SA_EAST_1("sa-east-1", "ec2.sa-east-1.amazonaws.com");

      final String name;
      final String endpoint;

      Region(String param2, String param3) {
         this.name = param2;
         this.endpoint = param3;
      }
   }
}
