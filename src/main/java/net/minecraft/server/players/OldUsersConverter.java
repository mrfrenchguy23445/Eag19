package net.minecraft.server.players;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

public class OldUsersConverter {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final File OLD_IPBANLIST = new File("banned-ips.txt");
   public static final File OLD_USERBANLIST = new File("banned-players.txt");
   public static final File OLD_OPLIST = new File("ops.txt");
   public static final File OLD_WHITELIST = new File("white-list.txt");

   static List<String> readOldListFormat(File param0, Map<String, String[]> param1) throws IOException {
      List<String> var2 = Files.readLines(param0, StandardCharsets.UTF_8);

      for (String var4 : var2) {
         var4 = var4.trim();
         if (!var4.startsWith("#") && var4.length() >= 1) {
            String[] var5 = var4.split("\\|");
            param1.put(var5[0].toLowerCase(Locale.ROOT), var5);
         }
      }

      return var2;
   }

   private static void lookupPlayers(MinecraftServer param0, Collection<String> param1, ProfileLookupCallback param2) {
      String[] var3 = param1.stream().filter(l_param0 -> !StringUtil.isNullOrEmpty(l_param0)).toArray(String[]::new);
      if (param0.usesAuthentication()) {
         param0.getProfileRepository().findProfilesByNames(var3, Agent.MINECRAFT, param2);
      } else {
         for (String var7 : var3) {
            UUID var8 = UUIDUtil.getOrCreatePlayerUUID(new GameProfile(null, var7));
            GameProfile var9 = new GameProfile(var8, var7);
            param2.onProfileLookupSucceeded(var9);
         }
      }
   }

   public static boolean convertUserBanlist(final MinecraftServer param0) {
      final UserBanList var1 = new UserBanList(PlayerList.USERBANLIST_FILE);
      if (OLD_USERBANLIST.exists() && OLD_USERBANLIST.isFile()) {
         if (var1.getFile().exists()) {
            try {
               var1.load();
            } catch (IOException var2) {
               LOGGER.warn("Could not load existing file {}", var1.getFile().getName(), var2);
            }
         }

         try {
            final Map<String, String[]> var2 = Maps.newHashMap();
            readOldListFormat(OLD_USERBANLIST, var2);
            ProfileLookupCallback var3 = new ProfileLookupCallback() {
               @Override
               public void onProfileLookupSucceeded(GameProfile l_param0) {
                  param0.getProfileCache().add(l_param0);
                  String[] l_var2 = var2.get(l_param0.getName().toLowerCase(Locale.ROOT));
                  if (l_var2 == null) {
                     OldUsersConverter.LOGGER.warn("Could not convert user banlist entry for {}", l_param0.getName());
                     throw new OldUsersConverter.ConversionError("Profile not in the conversionlist");
                  }

                  Date l_var3 = l_var2.length > 1 ? OldUsersConverter.parseDate(l_var2[1], null) : null;
                  String l_var4 = l_var2.length > 2 ? l_var2[2] : null;
                  Date l_var5 = l_var2.length > 3 ? OldUsersConverter.parseDate(l_var2[3], null) : null;
                  String l_var6 = l_var2.length > 4 ? l_var2[4] : null;
                  var1.add(new UserBanListEntry(l_param0, l_var3, l_var4, l_var5, l_var6));
               }

               @Override
               public void onProfileLookupFailed(GameProfile l_param0, Exception l_param1) {
                  OldUsersConverter.LOGGER.warn("Could not lookup user banlist entry for {}", l_param0.getName(), l_param1);
                  if (!(l_param1 instanceof ProfileNotFoundException)) {
                     throw new OldUsersConverter.ConversionError("Could not request user " + l_param0.getName() + " from backend systems", l_param1);
                  }
               }
            };
            lookupPlayers(param0, var2.keySet(), var3);
            var1.save();
            renameOldFile(OLD_USERBANLIST);
            return true;
         } catch (IOException var2) {
            LOGGER.warn("Could not read old user banlist to convert it!", var2);
            return false;
         } catch (OldUsersConverter.ConversionError var2) {
            LOGGER.error("Conversion failed, please try again later", var2);
            return false;
         }
      } else {
         return true;
      }
   }

   public static boolean convertIpBanlist(MinecraftServer param0) {
      IpBanList var1 = new IpBanList(PlayerList.IPBANLIST_FILE);
      if (OLD_IPBANLIST.exists() && OLD_IPBANLIST.isFile()) {
         if (var1.getFile().exists()) {
            try {
               var1.load();
            } catch (IOException var2) {
               LOGGER.warn("Could not load existing file {}", var1.getFile().getName(), var2);
            }
         }

         try {
            Map<String, String[]> var2 = Maps.newHashMap();
            readOldListFormat(OLD_IPBANLIST, var2);

            for (String var4 : var2.keySet()) {
               String[] var5 = var2.get(var4);
               Date var6 = var5.length > 1 ? parseDate(var5[1], null) : null;
               String var7 = var5.length > 2 ? var5[2] : null;
               Date var8 = var5.length > 3 ? parseDate(var5[3], null) : null;
               String var9 = var5.length > 4 ? var5[4] : null;
               var1.add(new IpBanListEntry(var4, var6, var7, var8, var9));
            }

            var1.save();
            renameOldFile(OLD_IPBANLIST);
            return true;
         } catch (IOException var2) {
            LOGGER.warn("Could not parse old ip banlist to convert it!", var2);
            return false;
         }
      } else {
         return true;
      }
   }

   public static boolean convertOpsList(final MinecraftServer param0) {
      final ServerOpList var1 = new ServerOpList(PlayerList.OPLIST_FILE);
      if (OLD_OPLIST.exists() && OLD_OPLIST.isFile()) {
         if (var1.getFile().exists()) {
            try {
               var1.load();
            } catch (IOException var2) {
               LOGGER.warn("Could not load existing file {}", var1.getFile().getName(), var2);
            }
         }

         try {
            List<String> var2 = Files.readLines(OLD_OPLIST, StandardCharsets.UTF_8);
            ProfileLookupCallback var3 = new ProfileLookupCallback() {
               @Override
               public void onProfileLookupSucceeded(GameProfile l_param0) {
                  param0.getProfileCache().add(l_param0);
                  var1.add(new ServerOpListEntry(l_param0, param0.getOperatorUserPermissionLevel(), false));
               }

               @Override
               public void onProfileLookupFailed(GameProfile l_param0, Exception l_param1) {
                  OldUsersConverter.LOGGER.warn("Could not lookup oplist entry for {}", l_param0.getName(), l_param1);
                  if (!(l_param1 instanceof ProfileNotFoundException)) {
                     throw new OldUsersConverter.ConversionError("Could not request user " + l_param0.getName() + " from backend systems", l_param1);
                  }
               }
            };
            lookupPlayers(param0, var2, var3);
            var1.save();
            renameOldFile(OLD_OPLIST);
            return true;
         } catch (IOException var2) {
            LOGGER.warn("Could not read old oplist to convert it!", var2);
            return false;
         } catch (OldUsersConverter.ConversionError var2) {
            LOGGER.error("Conversion failed, please try again later", var2);
            return false;
         }
      } else {
         return true;
      }
   }

   public static boolean convertWhiteList(final MinecraftServer param0) {
      final UserWhiteList var1 = new UserWhiteList(PlayerList.WHITELIST_FILE);
      if (OLD_WHITELIST.exists() && OLD_WHITELIST.isFile()) {
         if (var1.getFile().exists()) {
            try {
               var1.load();
            } catch (IOException var2) {
               LOGGER.warn("Could not load existing file {}", var1.getFile().getName(), var2);
            }
         }

         try {
            List<String> var2 = Files.readLines(OLD_WHITELIST, StandardCharsets.UTF_8);
            ProfileLookupCallback var3 = new ProfileLookupCallback() {
               @Override
               public void onProfileLookupSucceeded(GameProfile l_param0) {
                  param0.getProfileCache().add(l_param0);
                  var1.add(new UserWhiteListEntry(l_param0));
               }

               @Override
               public void onProfileLookupFailed(GameProfile l_param0, Exception l_param1) {
                  OldUsersConverter.LOGGER.warn("Could not lookup user whitelist entry for {}", l_param0.getName(), l_param1);
                  if (!(l_param1 instanceof ProfileNotFoundException)) {
                     throw new OldUsersConverter.ConversionError("Could not request user " + l_param0.getName() + " from backend systems", l_param1);
                  }
               }
            };
            lookupPlayers(param0, var2, var3);
            var1.save();
            renameOldFile(OLD_WHITELIST);
            return true;
         } catch (IOException var2) {
            LOGGER.warn("Could not read old whitelist to convert it!", var2);
            return false;
         } catch (OldUsersConverter.ConversionError var2) {
            LOGGER.error("Conversion failed, please try again later", var2);
            return false;
         }
      } else {
         return true;
      }
   }

   @Nullable
   public static UUID convertMobOwnerIfNecessary(final MinecraftServer param0, String param1) {
      if (!StringUtil.isNullOrEmpty(param1) && param1.length() <= 16) {
         Optional<UUID> var2 = param0.getProfileCache().get(param1).map(GameProfile::getId);
         if (var2.isPresent()) {
            return var2.get();
         } else if (!param0.isSingleplayer() && param0.usesAuthentication()) {
            final List<GameProfile> var3 = Lists.newArrayList();
            ProfileLookupCallback var4 = new ProfileLookupCallback() {
               @Override
               public void onProfileLookupSucceeded(GameProfile l_param0) {
                  param0.getProfileCache().add(l_param0);
                  var3.add(l_param0);
               }

               @Override
               public void onProfileLookupFailed(GameProfile l_param0, Exception l_param1) {
                  OldUsersConverter.LOGGER.warn("Could not lookup user whitelist entry for {}", l_param0.getName(), l_param1);
               }
            };
            lookupPlayers(param0, Lists.newArrayList(param1), var4);
            return !var3.isEmpty() && var3.get(0).getId() != null ? var3.get(0).getId() : null;
         } else {
            return UUIDUtil.getOrCreatePlayerUUID(new GameProfile(null, param1));
         }
      } else {
         try {
            return UUID.fromString(param1);
         } catch (IllegalArgumentException var2) {
            return null;
         }
      }
   }

   public static boolean convertPlayers(final DedicatedServer param0) {
      final File var1 = getWorldPlayersDirectory(param0);
      final File var2 = new File(var1.getParentFile(), "playerdata");
      final File var3 = new File(var1.getParentFile(), "unknownplayers");
      if (var1.exists() && var1.isDirectory()) {
         File[] var4 = var1.listFiles();
         List<String> var5 = Lists.newArrayList();

         for (File var9 : var4) {
            String var10 = var9.getName();
            if (var10.toLowerCase(Locale.ROOT).endsWith(".dat")) {
               String var11 = var10.substring(0, var10.length() - ".dat".length());
               if (!var11.isEmpty()) {
                  var5.add(var11);
               }
            }
         }

         try {
            final String[] var6 = var5.toArray(new String[var5.size()]);
            ProfileLookupCallback var7 = new ProfileLookupCallback() {
               @Override
               public void onProfileLookupSucceeded(GameProfile l_param0) {
                  param0.getProfileCache().add(l_param0);
                  UUID l_var2 = l_param0.getId();
                  if (l_var2 == null) {
                     throw new OldUsersConverter.ConversionError("Missing UUID for user profile " + l_param0.getName());
                  }

                  this.movePlayerFile(var2, this.getFileNameForProfile(l_param0), l_var2.toString());
               }

               @Override
               public void onProfileLookupFailed(GameProfile l_param0, Exception l_param1) {
                  OldUsersConverter.LOGGER.warn("Could not lookup user uuid for {}", l_param0.getName(), l_param1);
                  if (l_param1 instanceof ProfileNotFoundException) {
                     String l_var3 = this.getFileNameForProfile(l_param0);
                     this.movePlayerFile(var3, l_var3, l_var3);
                  } else {
                     throw new OldUsersConverter.ConversionError("Could not request user " + l_param0.getName() + " from backend systems", l_param1);
                  }
               }

               private void movePlayerFile(File l_param0, String l_param1, String l_param2) {
                  File l_var4 = new File(var1, l_param1 + ".dat");
                  File l_var5 = new File(l_param0, l_param2 + ".dat");
                  OldUsersConverter.ensureDirectoryExists(l_param0);
                  if (!l_var4.renameTo(l_var5)) {
                     throw new OldUsersConverter.ConversionError("Could not convert file for " + l_param1);
                  }
               }

               private String getFileNameForProfile(GameProfile l_param0) {
                  String l_var2 = null;

                  for (String l_var6 : var6) {
                     if (l_var6 != null && l_var6.equalsIgnoreCase(l_param0.getName())) {
                        l_var2 = l_var6;
                        break;
                     }
                  }

                  if (l_var2 == null) {
                     throw new OldUsersConverter.ConversionError("Could not find the filename for " + l_param0.getName() + " anymore");
                  } else {
                     return l_var2;
                  }
               }
            };
            lookupPlayers(param0, Lists.newArrayList(var6), var7);
            return true;
         } catch (OldUsersConverter.ConversionError var6) {
            LOGGER.error("Conversion failed, please try again later", var6);
            return false;
         }
      } else {
         return true;
      }
   }

   static void ensureDirectoryExists(File param0) {
      if (param0.exists()) {
         if (!param0.isDirectory()) {
            throw new OldUsersConverter.ConversionError("Can't create directory " + param0.getName() + " in world save directory.");
         }
      } else {
         if (!param0.mkdirs()) {
            throw new OldUsersConverter.ConversionError("Can't create directory " + param0.getName() + " in world save directory.");
         }
      }
   }

   public static boolean serverReadyAfterUserconversion(MinecraftServer param0) {
      boolean var1 = areOldUserlistsRemoved();
      return var1 && areOldPlayersConverted(param0);
   }

   private static boolean areOldUserlistsRemoved() {
      boolean var0 = false;
      if (OLD_USERBANLIST.exists() && OLD_USERBANLIST.isFile()) {
         var0 = true;
      }

      boolean var1 = false;
      if (OLD_IPBANLIST.exists() && OLD_IPBANLIST.isFile()) {
         var1 = true;
      }

      boolean var2 = false;
      if (OLD_OPLIST.exists() && OLD_OPLIST.isFile()) {
         var2 = true;
      }

      boolean var3 = false;
      if (OLD_WHITELIST.exists() && OLD_WHITELIST.isFile()) {
         var3 = true;
      }

      if (!var0 && !var1 && !var2 && !var3) {
         return true;
      }

      LOGGER.warn("**** FAILED TO START THE SERVER AFTER ACCOUNT CONVERSION!");
      LOGGER.warn("** please remove the following files and restart the server:");
      if (var0) {
         LOGGER.warn("* {}", OLD_USERBANLIST.getName());
      }

      if (var1) {
         LOGGER.warn("* {}", OLD_IPBANLIST.getName());
      }

      if (var2) {
         LOGGER.warn("* {}", OLD_OPLIST.getName());
      }

      if (var3) {
         LOGGER.warn("* {}", OLD_WHITELIST.getName());
      }

      return false;
   }

   private static boolean areOldPlayersConverted(MinecraftServer param0) {
      File var1 = getWorldPlayersDirectory(param0);
      if (!var1.exists() || !var1.isDirectory() || var1.list().length <= 0 && var1.delete()) {
         return true;
      }

      LOGGER.warn("**** DETECTED OLD PLAYER DIRECTORY IN THE WORLD SAVE");
      LOGGER.warn("**** THIS USUALLY HAPPENS WHEN THE AUTOMATIC CONVERSION FAILED IN SOME WAY");
      LOGGER.warn("** please restart the server and if the problem persists, remove the directory '{}'", var1.getPath());
      return false;
   }

   private static File getWorldPlayersDirectory(MinecraftServer param0) {
      return param0.getWorldPath(LevelResource.PLAYER_OLD_DATA_DIR).toFile();
   }

   private static void renameOldFile(File param0) {
      File var1 = new File(param0.getName() + ".converted");
      param0.renameTo(var1);
   }

   static Date parseDate(String param0, Date param1) {
      Date var2;
      try {
         var2 = BanListEntry.DATE_FORMAT.parse(param0);
      } catch (ParseException var3) {
         var2 = param1;
      }

      return var2;
   }

   static class ConversionError extends RuntimeException {
      ConversionError(String param0, Throwable param1) {
         super(param0, param1);
      }

      ConversionError(String param0) {
         super(param0);
      }
   }
}
