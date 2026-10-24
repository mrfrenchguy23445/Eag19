import org.teavm.backend.wasm.gc.PreciseTypeInference;
import org.teavm.backend.wasm.gc.PreciseValueType;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;

public final class InferenceJoinVerify {
 private static final class Inference extends PreciseTypeInference {
  Inference() { super(new Program(), new MethodReference("Fixture", "test", ValueType.VOID), null); }
  ValueType join(ValueType left, ValueType right) {
   return merge(new PreciseValueType(left, false), new PreciseValueType(right, false)).valueType;
  }
 }
 public static void main(String[] args) {
  Inference inference = new Inference();
  ValueType[] ints = {ValueType.BOOLEAN, ValueType.BYTE, ValueType.SHORT, ValueType.CHARACTER, ValueType.INTEGER};
  for (ValueType left : ints) for (ValueType right : ints) {
   ValueType result = inference.join(left, right);
   if (!(result instanceof ValueType.Primitive)) throw new AssertionError("integer merge became a reference");
   if (left != right && result != ValueType.INTEGER) throw new AssertionError("mixed integer widths must use i32");
  }
  ValueType boolArray = ValueType.arrayOf(ValueType.BOOLEAN);
  if (!inference.join(boolArray, boolArray).equals(boolArray)) throw new AssertionError("same array type changed");
  if (!inference.join(boolArray, ValueType.arrayOf(ValueType.BYTE)).equals(ValueType.object("java.lang.Object"))) {
   throw new AssertionError("primitive arrays must remain invariant");
  }
  System.out.println("WASM inference join verification passed");
 }
}
