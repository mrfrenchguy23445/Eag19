import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class NestedCallbackFixture {
 enum Type { SKIN, CAPE }
 interface RenderCall { void execute(); }
 static final Queue<RenderCall> renderQueue = new ArrayDeque<>();
 static final class MainThread implements Executor {
  final Queue<Runnable> queue = new ArrayDeque<>();
  boolean defer;
  public void execute(Runnable task) { if (defer) queue.add(task); else task.run(); }
 }
 final MainThread main;
 final Map<Type,String> session;
 NestedCallbackFixture(MainThread main, Map<Type,String> session) { this.main = main; this.session = session; }
 void register(Consumer<String> callback, boolean secure) {
  Runnable task = () -> {
   Map<Type,String> textures = new HashMap<>();
   try { textures.putAll(session); } catch (IllegalArgumentException ignored) { }
   if (textures.isEmpty() && !secure) textures.put(Type.SKIN, "default");
   main.execute(() -> renderQueue.add(() -> Arrays.asList(Type.SKIN, Type.CAPE).forEach(type -> {
    if (textures.containsKey(type)) callback.accept(textures.get(type));
   })));
  };
  ((Executor) Runnable::run).execute(task);
 }
 public static void main(String[] args) {
  MainThread main = new MainThread(); main.defer = args.length > 0;
  Map<Type,String> session = new HashMap<>(); session.put(Type.SKIN, "skin");
  List<String> result = new ArrayList<>();
  new NestedCallbackFixture(main,session).register(result::add, args.length > 1);
  while (!main.queue.isEmpty()) main.queue.remove().run();
  while (!renderQueue.isEmpty()) renderQueue.remove().execute();
  if (result.isEmpty()) throw new AssertionError();
 }
}
