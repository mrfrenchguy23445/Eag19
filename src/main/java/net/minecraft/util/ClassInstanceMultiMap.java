package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class ClassInstanceMultiMap<T> extends AbstractCollection<T> {
   private final Map<Class<?>, List<T>> byClass = Maps.newHashMap();
   private final Map<Class<?>, Collection<?>> classListView = Maps.newHashMap();
   private final Class<T> baseClass;
   private final List<T> allInstances = Lists.newArrayList();

   public ClassInstanceMultiMap(Class<T> param0) {
      this.baseClass = param0;
      this.byClass.put(param0, this.allInstances);
   }

   @Override
   public boolean add(T param0) {
      boolean var2 = false;

      for (Entry<Class<?>, List<T>> var4 : this.byClass.entrySet()) {
         if (var4.getKey().isInstance(param0)) {
            var2 |= var4.getValue().add(param0);
         }
      }

      return var2;
   }

   @Override
   public boolean remove(Object param0) {
      boolean var2 = false;

      for (Entry<Class<?>, List<T>> var4 : this.byClass.entrySet()) {
         if (var4.getKey().isInstance(param0)) {
            List<T> var5 = var4.getValue();
            var2 |= var5.remove(param0);
         }
      }

      return var2;
   }

   @Override
   public boolean contains(Object param0) {
      return this.find(param0.getClass()).contains(param0);
   }

   public <S> Collection<S> find(Class<S> param0) {
      if (!this.baseClass.isAssignableFrom(param0)) {
         throw new IllegalArgumentException("Don't know how to search for " + param0);
      }

      List<? extends T> var2 = this.byClass
         .computeIfAbsent(param0, l_param0 -> this.allInstances.stream().filter(l_param0::isInstance).collect(Collectors.toList()));
      // Lithium port (Phase B): the unmodifiable wrapper was allocated on every find() call,
      // i.e. once per section hit per type-filtered entity query (per-frame AI/collision
      // paths). It is immutable and the backing lists are only replaced wholesale, so the
      // wrapper is cached alongside the list instead.
      Collection<?> var3 = this.classListView.get(param0);
      if (var3 == null) {
         var3 = Collections.unmodifiableCollection(var2);
         this.classListView.put(param0, var3);
      }

      return (Collection<S>)var3;
   }

   @Override
   public Iterator<T> iterator() {
      return this.allInstances.isEmpty() ? Collections.emptyIterator() : Iterators.unmodifiableIterator(this.allInstances.iterator());
   }

   public List<T> getAllInstances() {
      return ImmutableList.copyOf(this.allInstances);
   }

   @Override
   public int size() {
      return this.allInstances.size();
   }
}
