package net.minecraft.client.gui.screens.packs;

import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.lax1dude.eaglercraft.v1_8.EagRuntime;
import net.lax1dude.eaglercraft.v1_8.internal.EnumPlatformType;
import net.lax1dude.eaglercraft.v1_8.internal.FileChooserResult;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

public class PackSelectionScreen extends Screen {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int LIST_WIDTH = 200;
   private static final Component DRAG_AND_DROP = Component.translatable("pack.dropInfo").withStyle(ChatFormatting.GRAY);
   private static final Component DIRECTORY_BUTTON_TOOLTIP = Component.translatable("pack.folderInfo");
   /**
    * The browser has no filesystem to open in an explorer window, so the same button imports a
    * pack file through the page's file chooser instead. See {@link #isBrowser()}.
    */
   private static final Component IMPORT_BUTTON = Component.literal("Import Pack...");
   private static final Component IMPORT_BUTTON_TOOLTIP = Component.literal("Select a .zip resource pack from your computer");
   private static final int RELOAD_COOLDOWN = 20;
   private static final ResourceLocation DEFAULT_ICON = new ResourceLocation("textures/misc/unknown_pack.png");
   private final PackSelectionModel model;
   @Nullable
   private PackSelectionScreen.Watcher watcher;
   private long ticksToReload;
   private TransferableSelectionList availablePackList;
   private TransferableSelectionList selectedPackList;
   private final Path packDir;
   private Button doneButton;
   private final Map<String, ResourceLocation> packIcons = Maps.newHashMap();

   public PackSelectionScreen(PackRepository param0, Consumer<PackRepository> param1, Path param2, Component param3) {
      super(param3);
      this.model = new PackSelectionModel(this::populateLists, this::getPackIcon, param0, param1);
      this.packDir = param2;
      this.watcher = PackSelectionScreen.Watcher.create(param2);
   }

   @Override
   public void onClose() {
      this.model.commit();
      this.closeWatcher();
   }

   private void closeWatcher() {
      if (this.watcher != null) {
         try {
            this.watcher.close();
            this.watcher = null;
         } catch (Exception var2) {
         }
      }
   }

   @Override
   protected void init() {
      this.availablePackList = new TransferableSelectionList(this.minecraft, this, 200, this.height, Component.translatable("pack.available.title"));
      this.availablePackList.setLeftPos(this.width / 2 - 4 - 200);
      this.addWidget(this.availablePackList);
      this.selectedPackList = new TransferableSelectionList(this.minecraft, this, 200, this.height, Component.translatable("pack.selected.title"));
      this.selectedPackList.setLeftPos(this.width / 2 + 4);
      this.addWidget(this.selectedPackList);
      boolean browser = this.isBrowser();
      this.addRenderableWidget(
         Button.builder(
               browser ? IMPORT_BUTTON : Component.translatable("pack.openFolder"),
               l_param0 -> {
                  if (browser) {
                     // Null mime means the runtime filters by extension, which is the only filter
                     // browsers agree on for .zip ("application/zip" is not used by every OS).
                     EagRuntime.clearFileChooserResult();
                     EagRuntime.displayFileChooser(null, "zip");
                  } else {
                     Util.getPlatform().openUri(this.packDir.toUri());
                  }
               }
            )
            .bounds(this.width / 2 - 154, this.height - 48, 150, 20)
            .tooltip(Tooltip.create(browser ? IMPORT_BUTTON_TOOLTIP : DIRECTORY_BUTTON_TOOLTIP))
            .build()
      );
      this.doneButton = this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, l_param0 -> this.onClose()).bounds(this.width / 2 + 4, this.height - 48, 150, 20).build()
      );
      if (browser) {
         // Packs dropped onto the page before this screen opened are stale, discard them.
         EagRuntime.clearFileChooserResult();
      }
      this.reload();
   }

   /**
    * True for the JavaScript and WASM targets, which can only receive files through the page's
    * file chooser / drag-and-drop rather than by reading a directory the user put files in.
    */
   private boolean isBrowser() {
      return EagRuntime.getPlatformType() != EnumPlatformType.DESKTOP;
   }

   @Override
   public void tick() {
      if (this.isBrowser() && EagRuntime.fileChooserHasResult()) {
         FileChooserResult var1 = EagRuntime.getFileChooserResult();
         if (var1 != null) {
            this.importPackFile(var1);
         }
      }

      if (this.watcher != null) {
         try {
            if (this.watcher.pollForChanges()) {
               this.ticksToReload = 20L;
            }
         } catch (IOException var1) {
            LOGGER.warn("Failed to poll for directory {} changes, stopping", this.packDir);
            this.closeWatcher();
         }
      }

      if (this.ticksToReload > 0L && --this.ticksToReload == 0L) {
         this.reload();
      }
   }

   private void populateLists() {
      this.updateList(this.selectedPackList, this.model.getSelected());
      this.updateList(this.availablePackList, this.model.getUnselected());
      this.doneButton.active = !this.selectedPackList.children().isEmpty();
   }

   private void updateList(TransferableSelectionList param0, Stream<PackSelectionModel.Entry> param1) {
      param0.children().clear();
      TransferableSelectionList.PackEntry var3 = param0.getSelected();
      String var4 = var3 == null ? "" : var3.getPackId();
      param0.setSelected(null);
      param1.forEach(l_param2 -> {
         TransferableSelectionList.PackEntry l_var4 = new TransferableSelectionList.PackEntry(this.minecraft, param0, l_param2);
         param0.children().add(l_var4);
         if (l_param2.getId().equals(var4)) {
            param0.setSelected(l_var4);
         }
      });
   }

   public void updateFocus(TransferableSelectionList param0) {
      TransferableSelectionList var2 = this.selectedPackList == param0 ? this.availablePackList : this.selectedPackList;
      this.changeFocus(ComponentPath.path(var2.getFirstElement(), var2, this));
   }

   public void clearSelected() {
      this.selectedPackList.setSelected(null);
      this.availablePackList.setSelected(null);
   }

   private void reload() {
      this.model.findNewPacks();
      this.populateLists();
      this.ticksToReload = 0L;
      this.packIcons.clear();
   }

   private void importPackFile(FileChooserResult param0) {
      String var2 = param0.fileName;
      if (var2 == null || param0.fileData == null) {
         return;
      }

      int var3 = Math.max(var2.lastIndexOf('/'), var2.lastIndexOf('\\'));
      if (var3 >= 0) {
         var2 = var2.substring(var3 + 1);
      }

      if (var2.isEmpty()) {
         return;
      }

      if (var2.equals(".") || var2.equals("..") || var2.indexOf(':') >= 0 || var2.indexOf('\0') >= 0) {
         LOGGER.warn("Ignoring resource pack with an unsafe file name: '{}'", var2);
         SystemToast.onPackCopyFailure(this.minecraft, var2);
         return;
      }

      if (!var2.toLowerCase(Locale.ROOT).endsWith(".zip")) {
         LOGGER.warn("Ignoring dropped resource pack '{}': only .zip packs can be imported", var2);
         SystemToast.onPackCopyFailure(this.minecraft, var2);
         return;
      }

      try {
         Files.createDirectories(this.packDir);
         Files.write(this.packDir.resolve(var2), param0.fileData);
         LOGGER.info("Imported resource pack '{}' ({} bytes)", var2, param0.fileData.length);
         this.reload();
      } catch (IOException var4) {
         LOGGER.warn("Failed to import resource pack '{}'", var2, var4);
         SystemToast.onPackCopyFailure(this.minecraft, var2);
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderDirtBackground(param0);
      this.availablePackList.render(param0, param1, param2, param3);
      this.selectedPackList.render(param0, param1, param2, param3);
      drawCenteredString(param0, this.font, this.title, this.width / 2, 8, 16777215);
      drawCenteredString(param0, this.font, DRAG_AND_DROP, this.width / 2, 20, 16777215);
      super.render(param0, param1, param2, param3);
   }

   protected static void copyPacks(Minecraft param0, List<Path> param1, Path param2) {
      MutableBoolean var3 = new MutableBoolean();
      param1.forEach(l_param2 -> {
         try (Stream<Path> l_var3 = Files.walk(l_param2)) {
            l_var3.forEach(l2_param3 -> {
               try {
                  Util.copyBetweenDirs(l_param2.getParent(), param2, l2_param3);
               } catch (IOException l2_var4) {
                  LOGGER.warn("Failed to copy datapack file  from {} to {}", l2_param3, param2, l2_var4);
                  var3.setTrue();
               }
            });
         } catch (IOException l_var3) {
            LOGGER.warn("Failed to copy datapack file from {} to {}", l_param2, param2);
            var3.setTrue();
         }
      });
      if (var3.isTrue()) {
         SystemToast.onPackCopyFailure(param0, param2.toString());
      }
   }

   @Override
   public void onFilesDrop(List<Path> param0) {
      String var2 = param0.stream().map(Path::getFileName).map(Path::toString).collect(Collectors.joining(", "));
      this.minecraft.setScreen(new ConfirmScreen(l_param1 -> {
         if (l_param1) {
            copyPacks(this.minecraft, param0, this.packDir);
            this.reload();
         }

         this.minecraft.setScreen(this);
      }, Component.translatable("pack.dropConfirm"), Component.literal(var2)));
   }

   private ResourceLocation loadPackIcon(TextureManager param0, Pack param1) {
      try (PackResources var3 = param1.open()) {
         IoSupplier<InputStream> var4 = var3.getRootResource("pack.png");
         if (var4 == null) {
            return DEFAULT_ICON;
         }

         String var5 = param1.getId();
         ResourceLocation var6 = new ResourceLocation(
            "minecraft", "pack/" + Util.sanitizeName(var5, ResourceLocation::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(var5) + "/icon"
         );

         try (InputStream var7 = var4.get()) {
            NativeImage var8 = NativeImage.read(var7);
            param0.register(var6, new DynamicTexture(var8));
            return var6;
         }
      } catch (Exception var3) {
         LOGGER.warn("Failed to load icon from pack {}", param1.getId(), var3);
         return DEFAULT_ICON;
      }
   }

   private ResourceLocation getPackIcon(Pack param0) {
      return this.packIcons.computeIfAbsent(param0.getId(), l_param1 -> this.loadPackIcon(this.minecraft.getTextureManager(), param0));
   }

   static class Watcher implements AutoCloseable {
      private final WatchService watcher;
      private final Path packPath;

      public Watcher(Path param0) throws IOException {
         this.packPath = param0;
         this.watcher = param0.getFileSystem().newWatchService();

         try {
            this.watchDir(param0);

            try (DirectoryStream<Path> var2 = Files.newDirectoryStream(param0)) {
               for (Path var4 : var2) {
                  if (Files.isDirectory(var4, LinkOption.NOFOLLOW_LINKS)) {
                     this.watchDir(var4);
                  }
               }
            }
         } catch (Exception var2) {
            this.watcher.close();
            throw var2;
         }
      }

      @Nullable
      public static PackSelectionScreen.Watcher create(Path param0) {
         try {
            return new PackSelectionScreen.Watcher(param0);
         } catch (IOException var1) {
            PackSelectionScreen.LOGGER.warn("Failed to initialize pack directory {} monitoring", param0, var1);
            return null;
         }
      }

      private void watchDir(Path param0) throws IOException {
         param0.register(this.watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
      }

      public boolean pollForChanges() throws IOException {
         boolean var1 = false;

         WatchKey var2;
         while ((var2 = this.watcher.poll()) != null) {
            for (WatchEvent<?> var5 : var2.pollEvents()) {
               var1 = true;
               if (var2.watchable() == this.packPath && var5.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                  Path var6 = this.packPath.resolve((Path)var5.context());
                  if (Files.isDirectory(var6, LinkOption.NOFOLLOW_LINKS)) {
                     this.watchDir(var6);
                  }
               }
            }

            var2.reset();
         }

         return var1;
      }

      @Override
      public void close() throws IOException {
         this.watcher.close();
      }
   }
}
