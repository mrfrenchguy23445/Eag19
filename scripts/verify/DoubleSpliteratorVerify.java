import org.teavm.classlib.java.util.TSpliterators;
import org.teavm.classlib.java.util.TSpliterator;
public class DoubleSpliteratorVerify {
 public static void main(String[] args) {
  TSpliterator.OfDouble all=TSpliterators.spliterator(new double[]{99,1,2,3,4,99},1,5,TSpliterator.ORDERED);
  TSpliterator.OfDouble first=all.trySplit();
  double[] sum={0}; int[] count={0};
  java.util.function.DoubleConsumer add=x->{sum[0]+=x;count[0]++;};
  while(first.tryAdvance(add)){} while(all.tryAdvance(add)){}
  if(sum[0]!=10 || count[0]!=4 || all.estimateSize()!=0 || first.estimateSize()!=0)throw new AssertionError();
  if((all.characteristics() & (TSpliterator.SIZED|TSpliterator.SUBSIZED|TSpliterator.ORDERED))!=(TSpliterator.SIZED|TSpliterator.SUBSIZED|TSpliterator.ORDERED))throw new AssertionError();
  System.out.println("Double spliterator verification passed");
 }
}
