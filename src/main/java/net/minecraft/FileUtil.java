package net.minecraft;

import com.mojang.serialization.DataResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;

public class FileUtil {
   /**
    * Matches the trailing " (n)" that a previous copy of a file was given, so that
    * copying the same file again increments the counter instead of colliding.
    *
    * <p>The pattern in this tree read {@code "(<name>.*) \\((<count>\\d*)\\)"}, which is a
    * plain capturing group around the literal characters {@code <name>} followed by
    * {@code .*} -- so it matched nothing a filename could contain, and the two
    * {@code group("name")} / {@code group("count")} calls below would have thrown
    * {@code IllegalArgumentException: No group with name} had it ever matched. The named
    * group markers were lost somewhere upstream of here.
    *
    * <p>Numbered groups are used rather than restoring the {@code (?<name>...)} syntax,
    * because restoring it would still not link: the TeaVM classlib's matcher has no
    * {@code group(String)}, which is what the link reported. Numbering is also simply the
    * upstream form.
    *
    * <p>The flag argument is left as it was found, {@code 66} =
    * {@code UNICODE_CHARACTER_CLASS | CASE_INSENSITIVE}. Only the literal-group bug is
    * being corrected; no flag is touched.
    */
   private static final Pattern COPY_COUNTER_PATTERN = Pattern.compile("(.*) \\((\\d*)\\)", 66);
   private static final int MAX_FILE_NAME = 255;
   private static final Pattern RESERVED_WINDOWS_FILENAMES = Pattern.compile(".*\\.|(?:COM|CLOCK\\$|CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?", 2);
   private static final Pattern STRICT_PATH_SEGMENT_CHECK = Pattern.compile("[-._a-z0-9]+");

   public static String findAvailableName(Path param0, String param1, String param2) throws IOException {
      for (char var6 : SharedConstants.ILLEGAL_FILE_CHARACTERS) {
         param1 = param1.replace(var6, '_');
      }

      param1 = param1.replaceAll("[./\"]", "_");
      if (RESERVED_WINDOWS_FILENAMES.matcher(param1).matches()) {
         param1 = "_" + param1 + "_";
      }

      Matcher var3 = COPY_COUNTER_PATTERN.matcher(param1);
      int var4 = 0;
      if (var3.matches()) {
         param1 = var3.group(1);
         var4 = Integer.parseInt(var3.group(2));
      }

      if (param1.length() > 255 - param2.length()) {
         param1 = param1.substring(0, 255 - param2.length());
      }

      while (true) {
         String var5 = param1;
         if (var4 != 0) {
            String var6 = " (" + var4 + ")";
            int var7 = 255 - var6.length();
            if (var5.length() > var7) {
               var5 = var5.substring(0, var7);
            }

            var5 = var5 + var6;
         }

         var5 = var5 + param2;
         Path var6 = param0.resolve(var5);

         try {
            Path var7 = Files.createDirectory(var6);
            Files.deleteIfExists(var7);
            return param0.relativize(var7).toString();
         } catch (FileAlreadyExistsException var7) {
            var4++;
         }
      }
   }

   public static boolean isPathNormalized(Path param0) {
      Path var1 = param0.normalize();
      return var1.equals(param0);
   }

   public static boolean isPathPortable(Path param0) {
      for (Path var2 : param0) {
         if (RESERVED_WINDOWS_FILENAMES.matcher(var2.toString()).matches()) {
            return false;
         }
      }

      return true;
   }

   public static Path createPathToResource(Path param0, String param1, String param2) {
      String var3 = param1 + param2;
      Path var4 = Paths.get(var3);
      if (var4.endsWith(param2)) {
         throw new InvalidPathException(var3, "empty resource name");
      } else {
         return param0.resolve(var4);
      }
   }

   public static String getFullResourcePath(String param0) {
      return FilenameUtils.getFullPath(param0).replace(File.separator, "/");
   }

   public static String normalizeResourcePath(String param0) {
      return FilenameUtils.normalize(param0).replace(File.separator, "/");
   }

   public static DataResult<List<String>> decomposePath(String param0) {
      int var1 = param0.indexOf(47);
      if (var1 == -1) {
         return switch (param0) {
            case "", ".", ".." -> DataResult.error(() -> "Invalid path '" + param0 + "'");
            default -> !isValidStrictPathSegment(param0) ? DataResult.error(() -> "Invalid path '" + param0 + "'") : DataResult.success(List.of(param0));
         };
      } else {
         List<String> var2 = new ArrayList<>();
         int var3 = 0;
         boolean var4 = false;

         while (true) {
            String var5 = param0.substring(var3, var1);
            switch (var5) {
               case "":
               case ".":
               case "..":
                  return DataResult.error(() -> "Invalid segment '" + var5 + "' in path '" + param0 + "'");
            }

            if (!isValidStrictPathSegment(var5)) {
               return DataResult.error(() -> "Invalid segment '" + var5 + "' in path '" + param0 + "'");
            }

            var2.add(var5);
            if (var4) {
               return DataResult.success(var2);
            }

            var3 = var1 + 1;
            var1 = param0.indexOf(47, var3);
            if (var1 == -1) {
               var1 = param0.length();
               var4 = true;
            }
         }
      }
   }

   public static Path resolvePath(Path param0, List<String> param1) {
      int var2 = param1.size();

      return switch (var2) {
         case 0 -> param0;
         case 1 -> param0.resolve(param1.get(0));
         default -> {
            String[] var3 = new String[var2 - 1];

            for (int var4 = 1; var4 < var2; var4++) {
               var3[var4 - 1] = param1.get(var4);
            }

            yield param0.resolve(param0.getFileSystem().getPath(param1.get(0), var3));
         }
      };
   }

   public static boolean isValidStrictPathSegment(String param0) {
      return STRICT_PATH_SEGMENT_CHECK.matcher(param0).matches();
   }

   public static void validatePath(String... param0) {
      if (param0.length == 0) {
         throw new IllegalArgumentException("Path must have at least one element");
      }

      for (String var4 : param0) {
         if (var4.equals("..") || var4.equals(".") || !isValidStrictPathSegment(var4)) {
            throw new IllegalArgumentException("Illegal segment " + var4 + " in path " + Arrays.toString(param0));
         }
      }
   }

   public static void createDirectoriesSafe(Path param0) throws IOException {
      Files.createDirectories(Files.exists(param0) ? param0.toRealPath() : param0);
   }
}
