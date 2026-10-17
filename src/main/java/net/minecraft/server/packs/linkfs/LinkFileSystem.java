package net.minecraft.server.packs.linkfs;

import com.google.common.base.Splitter;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class LinkFileSystem extends FileSystem {
   private static final Set<String> VIEWS = Set.of("basic");
   public static final String PATH_SEPARATOR = "/";
   private static final Splitter PATH_SPLITTER = Splitter.on('/');
   private final FileStore store;
   private final FileSystemProvider provider = new LinkFSProvider();
   private final LinkFSPath root;

   LinkFileSystem(String param0, LinkFileSystem.DirectoryEntry param1) {
      this.store = new LinkFSFileStore(param0);
      this.root = buildPath(param1, this, "", null);
   }

   private static LinkFSPath buildPath(LinkFileSystem.DirectoryEntry param0, LinkFileSystem param1, String param2, @Nullable LinkFSPath param3) {
      Object2ObjectOpenHashMap<String, LinkFSPath> var4 = new Object2ObjectOpenHashMap<>();
      LinkFSPath var5 = new LinkFSPath(param1, param2, param3, new PathContents.DirectoryContents(var4));
      param0.files.forEach((l_param3, l_param4) -> var4.put(l_param3, new LinkFSPath(param1, l_param3, var5, new PathContents.FileContents(l_param4))));
      param0.children.forEach((l_param3, l_param4) -> var4.put(l_param3, buildPath(l_param4, param1, l_param3, var5)));
      var4.trim();
      return var5;
   }

   @Override
   public FileSystemProvider provider() {
      return this.provider;
   }

   @Override
   public void close() {
   }

   @Override
   public boolean isOpen() {
      return true;
   }

   @Override
   public boolean isReadOnly() {
      return true;
   }

   @Override
   public String getSeparator() {
      return "/";
   }

   @Override
   public Iterable<Path> getRootDirectories() {
      return List.of(this.root);
   }

   @Override
   public Iterable<FileStore> getFileStores() {
      return List.of(this.store);
   }

   @Override
   public Set<String> supportedFileAttributeViews() {
      return VIEWS;
   }

   @Override
   public Path getPath(String param0, String... param1) {
      Stream<String> var3 = Stream.of(param0);
      if (param1.length > 0) {
         var3 = Stream.concat(var3, Stream.of(param1));
      }

      String var4 = var3.collect(Collectors.joining("/"));
      if (var4.equals("/")) {
         return this.root;
      }

      if (var4.startsWith("/")) {
         LinkFSPath var5 = this.root;

         for (String var7 : PATH_SPLITTER.split(var4.substring(1))) {
            if (var7.isEmpty()) {
               throw new IllegalArgumentException("Empty paths not allowed");
            }

            var5 = var5.resolveName(var7);
         }

         return var5;
      } else {
         LinkFSPath var5 = null;

         for (String var7 : PATH_SPLITTER.split(var4)) {
            if (var7.isEmpty()) {
               throw new IllegalArgumentException("Empty paths not allowed");
            }

            var5 = new LinkFSPath(this, var7, var5, PathContents.RELATIVE);
         }

         if (var5 == null) {
            throw new IllegalArgumentException("Empty paths not allowed");
         } else {
            return var5;
         }
      }
   }

   @Override
   public PathMatcher getPathMatcher(String param0) {
      throw new UnsupportedOperationException();
   }

   @Override
   public UserPrincipalLookupService getUserPrincipalLookupService() {
      throw new UnsupportedOperationException();
   }

   @Override
   public WatchService newWatchService() {
      throw new UnsupportedOperationException();
   }

   public FileStore store() {
      return this.store;
   }

   public LinkFSPath rootPath() {
      return this.root;
   }

   public static LinkFileSystem.Builder builder() {
      return new LinkFileSystem.Builder();
   }

   public static class Builder {
      private final LinkFileSystem.DirectoryEntry root = new LinkFileSystem.DirectoryEntry();

      public LinkFileSystem.Builder put(List<String> param0, String param1, Path param2) {
         LinkFileSystem.DirectoryEntry var4 = this.root;

         for (String var6 : param0) {
            var4 = var4.children.computeIfAbsent(var6, l_param0 -> new LinkFileSystem.DirectoryEntry());
         }

         var4.files.put(param1, param2);
         return this;
      }

      public LinkFileSystem.Builder put(List<String> param0, Path param1) {
         if (param0.isEmpty()) {
            throw new IllegalArgumentException("Path can't be empty");
         }

         int var3 = param0.size() - 1;
         return this.put(param0.subList(0, var3), param0.get(var3), param1);
      }

      public FileSystem build(String param0) {
         return new LinkFileSystem(param0, this.root);
      }
   }

   record DirectoryEntry(Map<String, LinkFileSystem.DirectoryEntry> children, Map<String, Path> files) {

      public DirectoryEntry() {
         this(new HashMap<>(), new HashMap<>());
      }
   }
}
