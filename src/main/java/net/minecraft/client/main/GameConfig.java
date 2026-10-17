package net.minecraft.client.main;

import com.mojang.blaze3d.platform.DisplayData;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nullable;
import net.minecraft.client.User;

public class GameConfig {
   public final GameConfig.UserData user;
   public final DisplayData display;
   public final GameConfig.FolderData location;
   public final GameConfig.GameData game;
   public final GameConfig.ServerData server;

   public GameConfig(GameConfig.UserData param0, DisplayData param1, GameConfig.FolderData param2, GameConfig.GameData param3, GameConfig.ServerData param4) {
      this.user = param0;
      this.display = param1;
      this.location = param2;
      this.game = param3;
      this.server = param4;
   }

   public static class FolderData {
      public final File gameDirectory;
      public final File resourcePackDirectory;
      public final File assetDirectory;
      @Nullable
      public final String assetIndex;

      public FolderData(File param0, File param1, File param2, @Nullable String param3) {
         this.gameDirectory = param0;
         this.resourcePackDirectory = param1;
         this.assetDirectory = param2;
         this.assetIndex = param3;
      }

      public Path getExternalAssetSource() {
         // Port note: IndexedAssetSource.createIndexFs is no longer called, so the configured
         // asset index is ignored. That branch built a LinkFileSystem -- Mojang's read-only
         // filesystem that maps resource paths through the asset index's hash table to loose
         // object files -- and LinkFileSystem's constructor reaches two JDK types the classlib
         // does not model at all, java.nio.file.spi.FileSystemProvider and
         // java.nio.file.FileStore. Neither is shimmed: FileSystemProvider is an abstract class
         // with a dozen protected hooks that javac insists on seeing when it type-checks
         // LinkFSProvider's extends clause, and building a working FileStore for a virtual
         // filesystem needs the four attribute-view types the classlib also lacks. Six shim files
         // for three errors was the wrong trade, and the answer was here rather than there.
         //
         // The index cannot work on this platform anyway. It describes Mojang's asset CDN layout:
         // an index JSON on disk plus content-addressed objects beside it, fetched over HTTPS.
         // This port serves assets out of assets.epk against its own virtual filesystem and has
         // no CDN to ask. Returning the asset directory itself is Mojang's own documented
         // alternative -- it is what the launcher gives when no index is configured, and what a
         // hand-unpacked assets folder looks like -- so a launcher config that still names an
         // index now degrades to reading the directory rather than failing to start.
         //
         // java.io.File.toPath() also does not exist on the WASM-GC classlib's File (TFile has
         // getAbsolutePath but no toPath), so the asset directory is converted by name.
         // Eaglercraft serves assets out of assets.epk against its own virtual filesystem
         // either way, so nothing depends on this being a real filesystem path.
         //
         // ClientPackSource treats a null path as "no external source"; returning the directory
         // keeps the pre-existing behaviour for the null-index case, which is the one that
         // actually occurs.
         return Paths.get(this.assetDirectory.getAbsolutePath());
      }
   }

   public static class GameData {
      public final boolean demo;
      public final String launchVersion;
      public final String versionType;
      public final boolean disableMultiplayer;
      public final boolean disableChat;

      public GameData(boolean param0, String param1, String param2, boolean param3, boolean param4) {
         this.demo = param0;
         this.launchVersion = param1;
         this.versionType = param2;
         this.disableMultiplayer = param3;
         this.disableChat = param4;
      }
   }

   public static class ServerData {
      @Nullable
      public final String hostname;
      public final int port;

      public ServerData(@Nullable String param0, int param1) {
         this.hostname = param0;
         this.port = param1;
      }
   }

   /** Browser build: no authlib PropertyMap, no java.net.Proxy. */
   public static class UserData {
      public final User user;

      public UserData(User param0) {
         this.user = param0;
      }
   }
}