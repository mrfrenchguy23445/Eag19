package net.minecraft.util.profiling;

public final class ResultField implements Comparable<ResultField> {
   public final double percentage;
   public final double globalPercentage;
   public final long count;
   public final String name;

   public ResultField(String param0, double param1, double param3, long param5) {
      this.name = param0;
      this.percentage = param1;
      this.globalPercentage = param3;
      this.count = param5;
   }

   public int compareTo(ResultField param0) {
      if (param0.percentage < this.percentage) {
         return -1;
      } else {
         return param0.percentage > this.percentage ? 1 : param0.name.compareTo(this.name);
      }
   }

   public int getColor() {
      return (this.name.hashCode() & 11184810) + 4473924;
   }
}
