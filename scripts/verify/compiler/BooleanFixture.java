public final class BooleanFixture {
 static final class Cache {
  boolean field;
  boolean[] array;
  Cache(int n) { array = new boolean[n]; field = n > 0; }
  boolean get(int i) { return array[i]; }
  boolean getField() { return field; }
 }
 static boolean choose(Cache c, int i, java.util.function.IntPredicate fallback) {
  return c != null ? c.get(i) : fallback.test(i);
 }
 public static void main(String[] args) {
  int n = args.length + 2;
  Cache c = new Cache(n);
  c.array[0] = args.length > 0;
  boolean result = choose(c,0,i->i>0) ^ c.getField();
  if(result && !c.get(0)) c.array[1] = true;
  it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap<Object> flags = new it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap<>();
  flags.put(args, result);
  flags.put("x", !result);
  it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap longs = new it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap();
  longs.put(n, flags.getBoolean(args));
  if(flags.getBoolean("x") && longs.get(n)) throw new AssertionError();
  Boolean boxed = flags.get(args);
  if(boxed != null && boxed.booleanValue() && args.length > 3) throw new AssertionError();
 }
}
