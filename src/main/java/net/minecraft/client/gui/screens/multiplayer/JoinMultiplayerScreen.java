package net.minecraft.client.gui.screens.multiplayer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.EditServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.LanServer;
import net.minecraft.client.server.LanServerDetection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class JoinMultiplayerScreen extends Screen {
   public static final int BUTTON_ROW_WIDTH = 308;
   public static final int TOP_ROW_BUTTON_WIDTH = 100;
   public static final int LOWER_ROW_BUTTON_WIDTH = 74;
   public static final int FOOTER_HEIGHT = 64;
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ServerStatusPinger pinger = new ServerStatusPinger();
   private final Screen lastScreen;
   protected ServerSelectionList serverSelectionList;
   private ServerList servers;
   private Button editButton;
   private Button selectButton;
   private Button deleteButton;
   @Nullable
   private List<Component> toolTip;
   private ServerData editingServer;
   private LanServerDetection.LanServerList lanServerList;
   @Nullable
   private LanServerDetection.LanServerDetector lanServerDetector;
   private boolean initedOnce;

   public JoinMultiplayerScreen(Screen param0) {
      super(Component.translatable("multiplayer.title"));
      this.lastScreen = param0;
   }

   @Override
   protected void init() {
      if (this.initedOnce) {
         this.serverSelectionList.updateSize(this.width, this.height, 32, this.height - 64);
      } else {
         this.initedOnce = true;
         this.servers = new ServerList(this.minecraft);
         this.servers.load();
         this.lanServerList = new LanServerDetection.LanServerList();

         try {
            this.lanServerDetector = new LanServerDetection.LanServerDetector(this.lanServerList);
            this.lanServerDetector.start();
         } catch (Exception var1) {
            LOGGER.warn("Unable to start LAN server detection: {}", var1.getMessage());
         }

         this.serverSelectionList = new ServerSelectionList(this, this.minecraft, this.width, this.height, 32, this.height - 64, 36);
         this.serverSelectionList.updateOnlineServers(this.servers);
      }

      this.addWidget(this.serverSelectionList);
      this.selectButton = this.addRenderableWidget(
         Button.builder(Component.translatable("selectServer.select"), l_param0 -> this.joinSelectedServer()).width(100).build()
      );
      Button var1 = this.addRenderableWidget(Button.builder(Component.translatable("selectServer.direct"), l_param0 -> {
         this.editingServer = new ServerData(I18n.get("selectServer.defaultName"), "", false);
         this.minecraft.setScreen(new DirectJoinServerScreen(this, this::directJoinCallback, this.editingServer));
      }).width(100).build());
      Button var2 = this.addRenderableWidget(Button.builder(Component.translatable("selectServer.add"), l_param0 -> {
         this.editingServer = new ServerData(I18n.get("selectServer.defaultName"), "", false);
         this.minecraft.setScreen(new EditServerScreen(this, this::addServerCallback, this.editingServer));
      }).width(100).build());
      this.editButton = this.addRenderableWidget(Button.builder(Component.translatable("selectServer.edit"), l_param0 -> {
         ServerSelectionList.Entry l_var2 = this.serverSelectionList.getSelected();
         if (l_var2 instanceof ServerSelectionList.OnlineServerEntry) {
            ServerData l_var3 = ((ServerSelectionList.OnlineServerEntry)l_var2).getServerData();
            this.editingServer = new ServerData(l_var3.name, l_var3.ip, false);
            this.editingServer.copyFrom(l_var3);
            this.minecraft.setScreen(new EditServerScreen(this, this::editServerCallback, this.editingServer));
         }
      }).width(74).build());
      this.deleteButton = this.addRenderableWidget(Button.builder(Component.translatable("selectServer.delete"), l_param0 -> {
         ServerSelectionList.Entry l_var2 = this.serverSelectionList.getSelected();
         if (l_var2 instanceof ServerSelectionList.OnlineServerEntry) {
            String l_var3 = ((ServerSelectionList.OnlineServerEntry)l_var2).getServerData().name;
            if (l_var3 != null) {
               Component l_var4 = Component.translatable("selectServer.deleteQuestion");
               Component l_var5 = Component.translatable("selectServer.deleteWarning", l_var3);
               Component l_var6 = Component.translatable("selectServer.deleteButton");
               Component l_var7 = CommonComponents.GUI_CANCEL;
               this.minecraft.setScreen(new ConfirmScreen(this::deleteCallback, l_var4, l_var5, l_var6, l_var7));
            }
         }
      }).width(74).build());
      Button var3 = this.addRenderableWidget(
         Button.builder(Component.translatable("selectServer.refresh"), l_param0 -> this.refreshServerList()).width(74).build()
      );
      Button var4 = this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, l_param0 -> this.minecraft.setScreen(this.lastScreen)).width(74).build()
      );
      GridLayout var5 = new GridLayout();
      GridLayout.RowHelper var6 = var5.createRowHelper(1);
      LinearLayout var7 = var6.addChild(new LinearLayout(308, 20, LinearLayout.Orientation.HORIZONTAL));
      var7.addChild(this.selectButton);
      var7.addChild(var1);
      var7.addChild(var2);
      var6.addChild(SpacerElement.height(4));
      LinearLayout var8 = var6.addChild(new LinearLayout(308, 20, LinearLayout.Orientation.HORIZONTAL));
      var8.addChild(this.editButton);
      var8.addChild(this.deleteButton);
      var8.addChild(var3);
      var8.addChild(var4);
      var5.arrangeElements();
      FrameLayout.centerInRectangle(var5, 0, this.height - 64, this.width, 64);
      this.onSelectedChange();
   }

   @Override
   public void tick() {
      super.tick();
      List<LanServer> var1 = this.lanServerList.takeDirtyServers();
      if (var1 != null) {
         this.serverSelectionList.updateNetworkServers(var1);
      }

      this.pinger.tick();
   }

   @Override
   public void removed() {
      if (this.lanServerDetector != null) {
         this.lanServerDetector.interrupt();
         this.lanServerDetector = null;
      }

      this.pinger.removeAll();
   }

   private void refreshServerList() {
      this.minecraft.setScreen(new JoinMultiplayerScreen(this.lastScreen));
   }

   private void deleteCallback(boolean param0) {
      ServerSelectionList.Entry var2 = this.serverSelectionList.getSelected();
      if (param0 && var2 instanceof ServerSelectionList.OnlineServerEntry) {
         this.servers.remove(((ServerSelectionList.OnlineServerEntry)var2).getServerData());
         this.servers.save();
         this.serverSelectionList.setSelected((ServerSelectionList.Entry)null);
         this.serverSelectionList.updateOnlineServers(this.servers);
      }

      this.minecraft.setScreen(this);
   }

   private void editServerCallback(boolean param0) {
      ServerSelectionList.Entry var2 = this.serverSelectionList.getSelected();
      if (param0 && var2 instanceof ServerSelectionList.OnlineServerEntry) {
         ServerData var3 = ((ServerSelectionList.OnlineServerEntry)var2).getServerData();
         var3.name = this.editingServer.name;
         var3.ip = this.editingServer.ip;
         var3.copyFrom(this.editingServer);
         this.servers.save();
         this.serverSelectionList.updateOnlineServers(this.servers);
      }

      this.minecraft.setScreen(this);
   }

   private void addServerCallback(boolean param0) {
      if (param0) {
         ServerData var2 = this.servers.unhide(this.editingServer.ip);
         if (var2 != null) {
            var2.copyNameIconFrom(this.editingServer);
            this.servers.save();
         } else {
            this.servers.add(this.editingServer, false);
            this.servers.save();
         }

         this.serverSelectionList.setSelected((ServerSelectionList.Entry)null);
         this.serverSelectionList.updateOnlineServers(this.servers);
      }

      this.minecraft.setScreen(this);
   }

   private void directJoinCallback(boolean param0) {
      if (param0) {
         ServerData var2 = this.servers.get(this.editingServer.ip);
         if (var2 == null) {
            this.servers.add(this.editingServer, true);
            this.servers.save();
            this.join(this.editingServer);
         } else {
            this.join(var2);
         }
      } else {
         this.minecraft.setScreen(this);
      }
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (super.keyPressed(param0, param1, param2)) {
         return true;
      }

      if (param0 == 294) {
         this.refreshServerList();
         return true;
      }

      if (this.serverSelectionList.getSelected() != null) {
         if (param0 != 257 && param0 != 335) {
            return this.serverSelectionList.keyPressed(param0, param1, param2);
         }

         this.joinSelectedServer();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.toolTip = null;
      this.renderBackground(param0);
      this.serverSelectionList.render(param0, param1, param2, param3);
      drawCenteredString(param0, this.font, this.title, this.width / 2, 20, 16777215);
      super.render(param0, param1, param2, param3);
      if (this.toolTip != null) {
         this.renderComponentTooltip(param0, this.toolTip, param1, param2);
      }
   }

   public void joinSelectedServer() {
      ServerSelectionList.Entry var1 = this.serverSelectionList.getSelected();
      if (var1 instanceof ServerSelectionList.OnlineServerEntry) {
         this.join(((ServerSelectionList.OnlineServerEntry)var1).getServerData());
      } else if (var1 instanceof ServerSelectionList.NetworkServerEntry) {
         LanServer var2 = ((ServerSelectionList.NetworkServerEntry)var1).getServerData();
         this.join(new ServerData(var2.getMotd(), var2.getAddress(), true));
      }
   }

   private void join(ServerData param0) {
      ConnectScreen.startConnecting(this, this.minecraft, ServerAddress.parseString(param0.ip), param0);
   }

   public void setSelected(ServerSelectionList.Entry param0) {
      this.serverSelectionList.setSelected(param0);
      this.onSelectedChange();
   }

   protected void onSelectedChange() {
      this.selectButton.active = false;
      this.editButton.active = false;
      this.deleteButton.active = false;
      ServerSelectionList.Entry var1 = this.serverSelectionList.getSelected();
      if (var1 != null && !(var1 instanceof ServerSelectionList.LANHeader)) {
         this.selectButton.active = true;
         if (var1 instanceof ServerSelectionList.OnlineServerEntry) {
            this.editButton.active = true;
            this.deleteButton.active = true;
         }
      }
   }

   public ServerStatusPinger getPinger() {
      return this.pinger;
   }

   public void setToolTip(List<Component> param0) {
      this.toolTip = param0;
   }

   public ServerList getServers() {
      return this.servers;
   }
}
