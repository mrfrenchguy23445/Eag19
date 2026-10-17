package net.minecraft.commands.arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignableCommand;

public record ArgumentSignatures(List<ArgumentSignatures.Entry> entries) {
   public static final ArgumentSignatures EMPTY = new ArgumentSignatures(List.of());
   private static final int MAX_ARGUMENT_COUNT = 8;
   private static final int MAX_ARGUMENT_NAME_LENGTH = 16;

   public ArgumentSignatures(FriendlyByteBuf param0) {
      // Delegating through a helper with an EXACT return type avoids two problems:
      //  * 'ArgumentSignatures.Entry::new' is ambiguous (Entry has 2 constructors).
      //  * inlining the readCollection call here made this(...) match BOTH
      //    constructors, and pinning readCollection's <T, C> inline sent javac's
      //    type inference into a multi-minute blowup.
      this(readEntries(param0));
   }

   private static List<Entry> readEntries(FriendlyByteBuf buf) {
      return buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, MAX_ARGUMENT_COUNT), b -> new Entry(b));
   }

   @Nullable
   public MessageSignature get(String param0) {
      for (ArgumentSignatures.Entry var3 : this.entries) {
         if (var3.name.equals(param0)) {
            return var3.signature;
         }
      }

      return null;
   }

   public void write(FriendlyByteBuf param0) {
      param0.writeCollection(this.entries, (l_param0, l_param1) -> l_param1.write(l_param0));
   }

   public static ArgumentSignatures signCommand(SignableCommand<?> param0, ArgumentSignatures.Signer param1) {
      List<ArgumentSignatures.Entry> var2 = param0.arguments().stream().map(l_param1 -> {
         MessageSignature l_var2 = param1.sign(l_param1.value());
         return l_var2 != null ? new ArgumentSignatures.Entry(l_param1.name(), l_var2) : null;
      }).filter(Objects::nonNull).toList();
      return new ArgumentSignatures(var2);
   }

   public record Entry(String name, MessageSignature signature) {

      public Entry(FriendlyByteBuf param0) {
         this(param0.readUtf(16), MessageSignature.read(param0));
      }

      public void write(FriendlyByteBuf param0) {
         param0.writeUtf(this.name, 16);
         MessageSignature.write(param0, this.signature);
      }
   }

   @FunctionalInterface
   public interface Signer {
      @Nullable
      MessageSignature sign(String var1);
   }
}
