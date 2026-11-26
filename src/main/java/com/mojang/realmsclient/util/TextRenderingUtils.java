package com.mojang.realmsclient.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

public class TextRenderingUtils {
   private TextRenderingUtils() {
   }

   @VisibleForTesting
   protected static List<String> lineBreak(String param0) {
      return Arrays.asList(param0.split("\\n"));
   }

   public static List<TextRenderingUtils.Line> decompose(String param0, TextRenderingUtils.LineSegment... param1) {
      return decompose(param0, Arrays.asList(param1));
   }

   private static List<TextRenderingUtils.Line> decompose(String param0, List<TextRenderingUtils.LineSegment> param1) {
      List<String> var2 = lineBreak(param0);
      return insertLinks(var2, param1);
   }

   private static List<TextRenderingUtils.Line> insertLinks(List<String> param0, List<TextRenderingUtils.LineSegment> param1) {
      int var2 = 0;
      List<TextRenderingUtils.Line> var3 = Lists.newArrayList();

      for (String var5 : param0) {
         List<TextRenderingUtils.LineSegment> var6 = Lists.newArrayList();

         for (String var9 : split(var5, "%link")) {
            if ("%link".equals(var9)) {
               var6.add(param1.get(var2++));
            } else {
               var6.add(TextRenderingUtils.LineSegment.text(var9));
            }
         }

         var3.add(new TextRenderingUtils.Line(var6));
      }

      return var3;
   }

   public static List<String> split(String param0, String param1) {
      if (param1.isEmpty()) {
         throw new IllegalArgumentException("Delimiter cannot be the empty string");
      }

      List<String> var2 = Lists.newArrayList();
      int var3 = 0;

      int var4;
      while ((var4 = param0.indexOf(param1, var3)) != -1) {
         if (var4 > var3) {
            var2.add(param0.substring(var3, var4));
         }

         var2.add(param1);
         var3 = var4 + param1.length();
      }

      if (var3 < param0.length()) {
         var2.add(param0.substring(var3));
      }

      return var2;
   }

   public static class Line {
      public final List<TextRenderingUtils.LineSegment> segments;

      Line(TextRenderingUtils.LineSegment... param0) {
         this(Arrays.asList(param0));
      }

      Line(List<TextRenderingUtils.LineSegment> param0) {
         this.segments = param0;
      }

      @Override
      public String toString() {
         return "Line{segments=" + this.segments + "}";
      }

      @Override
      public boolean equals(Object param0) {
         if (this == param0) {
            return true;
         } else if (param0 != null && this.getClass() == param0.getClass()) {
            TextRenderingUtils.Line var2 = (TextRenderingUtils.Line)param0;
            return Objects.equals(this.segments, var2.segments);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.segments);
      }
   }

   public static class LineSegment {
      private final String fullText;
      @Nullable
      private final String linkTitle;
      @Nullable
      private final String linkUrl;

      private LineSegment(String param0) {
         this.fullText = param0;
         this.linkTitle = null;
         this.linkUrl = null;
      }

      private LineSegment(String param0, @Nullable String param1, @Nullable String param2) {
         this.fullText = param0;
         this.linkTitle = param1;
         this.linkUrl = param2;
      }

      @Override
      public boolean equals(Object param0) {
         if (this == param0) {
            return true;
         } else if (param0 != null && this.getClass() == param0.getClass()) {
            TextRenderingUtils.LineSegment var2 = (TextRenderingUtils.LineSegment)param0;
            return Objects.equals(this.fullText, var2.fullText) && Objects.equals(this.linkTitle, var2.linkTitle) && Objects.equals(this.linkUrl, var2.linkUrl);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.fullText, this.linkTitle, this.linkUrl);
      }

      @Override
      public String toString() {
         return "Segment{fullText='" + this.fullText + "', linkTitle='" + this.linkTitle + "', linkUrl='" + this.linkUrl + "'}";
      }

      public String renderedText() {
         return this.isLink() ? this.linkTitle : this.fullText;
      }

      public boolean isLink() {
         return this.linkTitle != null;
      }

      public String getLinkUrl() {
         if (!this.isLink()) {
            throw new IllegalStateException("Not a link: " + this);
         } else {
            return this.linkUrl;
         }
      }

      public static TextRenderingUtils.LineSegment link(String param0, String param1) {
         return new TextRenderingUtils.LineSegment(null, param0, param1);
      }

      @VisibleForTesting
      protected static TextRenderingUtils.LineSegment text(String param0) {
         return new TextRenderingUtils.LineSegment(param0);
      }
   }
}
