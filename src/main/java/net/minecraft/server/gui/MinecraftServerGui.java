package net.minecraft.server.gui;

import com.google.common.collect.Lists;
import com.mojang.logging.LogQueues;
import com.mojang.logging.LogUtils;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.server.dedicated.DedicatedServer;
import org.slf4j.Logger;

public class MinecraftServerGui extends JComponent {
   private static final Font MONOSPACED = new Font("Monospaced", 0, 12);
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String TITLE = "Minecraft server";
   private static final String SHUTDOWN_TITLE = "Minecraft server - shutting down!";
   private final DedicatedServer server;
   private Thread logAppenderThread;
   private final Collection<Runnable> finalizers = Lists.newArrayList();
   final AtomicBoolean isClosing = new AtomicBoolean();

   public static MinecraftServerGui showFrameFor(final DedicatedServer param0) {
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (Exception var3) {
      }

      final JFrame var1 = new JFrame("Minecraft server");
      final MinecraftServerGui var2 = new MinecraftServerGui(param0);
      var1.setDefaultCloseOperation(2);
      var1.add(var2);
      var1.pack();
      var1.setLocationRelativeTo(null);
      var1.setVisible(true);
      var1.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent l_param0) {
            if (!var2.isClosing.getAndSet(true)) {
               var1.setTitle("Minecraft server - shutting down!");
               param0.halt(true);
               var2.runFinalizers();
            }
         }
      });
      var2.addFinalizer(var1::dispose);
      var2.start();
      return var2;
   }

   private MinecraftServerGui(DedicatedServer param0) {
      this.server = param0;
      this.setPreferredSize(new Dimension(854, 480));
      this.setLayout(new BorderLayout());

      try {
         this.add(this.buildChatPanel(), "Center");
         this.add(this.buildInfoPanel(), "West");
      } catch (Exception var2) {
         LOGGER.error("Couldn't build server GUI", var2);
      }
   }

   public void addFinalizer(Runnable param0) {
      this.finalizers.add(param0);
   }

   private JComponent buildInfoPanel() {
      JPanel var1 = new JPanel(new BorderLayout());
      StatsComponent var2 = new StatsComponent(this.server);
      this.finalizers.add(var2::close);
      var1.add(var2, "North");
      var1.add(this.buildPlayerPanel(), "Center");
      var1.setBorder(new TitledBorder(new EtchedBorder(), "Stats"));
      return var1;
   }

   private JComponent buildPlayerPanel() {
      JList<?> var1 = new PlayerListComponent(this.server);
      JScrollPane var2 = new JScrollPane(var1, 22, 30);
      var2.setBorder(new TitledBorder(new EtchedBorder(), "Players"));
      return var2;
   }

   private JComponent buildChatPanel() {
      JPanel var1 = new JPanel(new BorderLayout());
      JTextArea var2 = new JTextArea();
      JScrollPane var3 = new JScrollPane(var2, 22, 30);
      var2.setEditable(false);
      var2.setFont(MONOSPACED);
      JTextField var4 = new JTextField();
      var4.addActionListener(l_param1 -> {
         String l_var3 = var4.getText().trim();
         if (!l_var3.isEmpty()) {
            this.server.handleConsoleInput(l_var3, this.server.createCommandSourceStack());
         }

         var4.setText("");
      });
      var2.addFocusListener(new FocusAdapter() {
         @Override
         public void focusGained(FocusEvent l_param0) {
         }
      });
      var1.add(var3, "Center");
      var1.add(var4, "South");
      var1.setBorder(new TitledBorder(new EtchedBorder(), "Log and chat"));
      this.logAppenderThread = new Thread(() -> {
         String l_var3;
         while ((l_var3 = LogQueues.getNextLogEvent("ServerGuiConsole")) != null) {
            this.print(var2, var3, l_var3);
         }
      });
      this.logAppenderThread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
      this.logAppenderThread.setDaemon(true);
      return var1;
   }

   public void start() {
      this.logAppenderThread.start();
   }

   public void close() {
      if (!this.isClosing.getAndSet(true)) {
         this.runFinalizers();
      }
   }

   void runFinalizers() {
      this.finalizers.forEach(Runnable::run);
   }

   public void print(JTextArea param0, JScrollPane param1, String param2) {
      if (!SwingUtilities.isEventDispatchThread()) {
         SwingUtilities.invokeLater(() -> this.print(param0, param1, param2));
      } else {
         Document var4 = param0.getDocument();
         JScrollBar var5 = param1.getVerticalScrollBar();
         boolean var6 = false;
         if (param1.getViewport().getView() == param0) {
            var6 = var5.getValue() + var5.getSize().getHeight() + MONOSPACED.getSize() * 4 > var5.getMaximum();
         }

         try {
            var4.insertString(var4.getLength(), param2, null);
         } catch (BadLocationException var8) {
         }

         if (var6) {
            var5.setValue(Integer.MAX_VALUE);
         }
      }
   }
}
