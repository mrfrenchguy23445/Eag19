/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson.internal;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.Since;
import com.google.gson.annotations.Until;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class selects which fields and types to omit. It is configurable,
 * supporting version attributes {@link Since} and {@link Until}, modifiers,
 * synthetic fields, anonymous and local classes, inner classes, and fields with
 * the {@link Expose} annotation.
 *
 * <p>This class is a type adapter factory; types that are excluded will be
 * adapted to null. It may delegate to another type adapter if only one
 * direction is excluded.
 *
 * @author Joel Leitch
 * @author Jesse Wilson
 */
public final class Excluder implements TypeAdapterFactory, Cloneable {
  private static final double IGNORE_VERSIONS = -1.0d;
  public static final Excluder DEFAULT = new Excluder();

  private double version = IGNORE_VERSIONS;
  private int modifiers = Modifier.TRANSIENT | Modifier.STATIC;
  private boolean serializeInnerClasses = true;
  private boolean requireExpose;
  private List<ExclusionStrategy> serializationStrategies = Collections.emptyList();
  private List<ExclusionStrategy> deserializationStrategies = Collections.emptyList();

  @Override protected Excluder clone() {
    try {
      return (Excluder) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError(e);
    }
  }

  public Excluder withVersion(double ignoreVersionsAfter) {
    Excluder result = clone();
    result.version = ignoreVersionsAfter;
    return result;
  }

  public Excluder withModifiers(int... modifiers) {
    Excluder result = clone();
    result.modifiers = 0;
    for (int modifier : modifiers) {
      result.modifiers |= modifier;
    }
    return result;
  }

  public Excluder disableInnerClassSerialization() {
    Excluder result = clone();
    result.serializeInnerClasses = false;
    return result;
  }

  public Excluder excludeFieldsWithoutExposeAnnotation() {
    Excluder result = clone();
    result.requireExpose = true;
    return result;
  }

  public Excluder withExclusionStrategy(ExclusionStrategy exclusionStrategy,
      boolean serialization, boolean deserialization) {
    Excluder result = clone();
    if (serialization) {
      result.serializationStrategies = new ArrayList<>(serializationStrategies);
      result.serializationStrategies.add(exclusionStrategy);
    }
    if (deserialization) {
      result.deserializationStrategies = new ArrayList<>(deserializationStrategies);
      result.deserializationStrategies.add(exclusionStrategy);
    }
    return result;
  }

  @Override public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
    Class<?> rawType = type.getRawType();
    boolean excludeClass = excludeClassChecks(rawType);

    final boolean skipSerialize = excludeClass || excludeClassInStrategy(rawType, true);
    final boolean skipDeserialize = excludeClass ||  excludeClassInStrategy(rawType, false);

    if (!skipSerialize && !skipDeserialize) {
      return null;
    }

    return new TypeAdapter<T>() {
      /** The delegate is lazily created because it may not be needed, and creating it may fail. */
      private TypeAdapter<T> delegate;

      @Override public T read(JsonReader in) throws IOException {
        if (skipDeserialize) {
          in.skipValue();
          return null;
        }
        return delegate().read(in);
      }

      @Override public void write(JsonWriter out, T value) throws IOException {
        if (skipSerialize) {
          out.nullValue();
          return;
        }
        delegate().write(out, value);
      }

      private TypeAdapter<T> delegate() {
        TypeAdapter<T> d = delegate;
        return d != null
            ? d
            : (delegate = gson.getDelegateAdapter(Excluder.this, type));
      }
    };
  }

  public boolean excludeField(Field field, boolean serialize) {
    if ((modifiers & field.getModifiers()) != 0) {
      return true;
    }

    if (version != Excluder.IGNORE_VERSIONS
        && !isValidVersion(field.getAnnotation(Since.class), field.getAnnotation(Until.class))) {
      return true;
    }

    if (field.isSynthetic()) {
      return true;
    }

    if (requireExpose) {
      Expose annotation = field.getAnnotation(Expose.class);
      if (annotation == null || (serialize ? !annotation.serialize() : !annotation.deserialize())) {
        return true;
      }
    }

    if (!serializeInnerClasses && isInnerClass(field.getType())) {
      return true;
    }

    if (isAnonymousOrNonStaticLocal(field.getType())) {
      return true;
    }

    List<ExclusionStrategy> list = serialize ? serializationStrategies : deserializationStrategies;
    if (!list.isEmpty()) {
      FieldAttributes fieldAttributes = new FieldAttributes(field);
      for (ExclusionStrategy exclusionStrategy : list) {
        if (exclusionStrategy.shouldSkipField(fieldAttributes)) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean excludeClassChecks(Class<?> clazz) {
      // Port note (wasm_gc): the @Since/@Until class check is replaced by "annotation absent",
      // which is literally the truth on this target -- Class.getAnnotation() is what tells TeaVM
      // that class metadata annotations must be materialized (ClassMetadataRequirements
      // hasGetAnnotations), and once that flag is set, WasmGCAnnotationsGenerator.addClassAnnotations
      // reads classAnnotationsOffset out of java.lang.Class before WasmStructure.init() has ever
      // run fillSimpleClassFields, so it gets -1 and dies with
      //     Failed generating method body due to internal exception:
      //     java.lang.IllegalArgumentException: Field index must be >= 0
      //         at org.teavm.runtime.heap.Heap.init
      // Passing null is not a stub of a missing library feature; it is the answer this target
      // gives for every class, so the version gate keeps behaving as it does at runtime here.
      if (version != Excluder.IGNORE_VERSIONS && !isValidVersion(null, null)) {
          return true;
      }

      if (!serializeInnerClasses && isInnerClass(clazz)) {
          return true;
      }

      if (isAnonymousOrNonStaticLocal(clazz)) {
          return true;
      }

      return false;
  }

  public boolean excludeClass(Class<?> clazz, boolean serialize) {
      return excludeClassChecks(clazz) ||
              excludeClassInStrategy(clazz, serialize);
  }

  private boolean excludeClassInStrategy(Class<?> clazz, boolean serialize) {
      List<ExclusionStrategy> list = serialize ? serializationStrategies : deserializationStrategies;
      for (ExclusionStrategy exclusionStrategy : list) {
          if (exclusionStrategy.shouldSkipClass(clazz)) {
              return true;
          }
      }
      return false;
  }

  /**
   * Whether this class is an anonymous class or a non-static local class, which Gson excludes
   * because such classes normally capture enclosing state that is not part of the JSON shape.
   *
   * <p>Port note: upstream asks {@code clazz.isAnonymousClass() || clazz.isLocalClass()}.
   * {@code Class.isAnonymousClass()} is absent from this target's TeaVM classlib -- the only
   * modelled {@code Class} predicates are isAnnotationPresent, isArray, isAssignableFrom, isEnum,
   * isInstance, isInterface, isLocalClass, isMemberClass and isPrimitive (see
   * {@code .decompile_work/classlib-model.txt}). {@code Class.isSynthetic()} is absent too, so
   * widening to it is not an option; {@code Field.isSynthetic} existing does not help.
   *
   * <p>Two available signals stand in for {@code isAnonymousClass()}, and both are specified
   * rather than guessed:
   *
   * <ul>
   *   <li>An anonymous class's simple name is the empty string, so
   *       {@code getSimpleName().isEmpty()} identifies one. This is the standard idiom for
   *       recovering anonymous-ness from a {@code Class} that will not say so.</li>
   *   <li>For a <i>named</i> local class, {@code isLocalClass()} is true and
   *       {@code isMemberClass()} is false. Upstream's {@code isLocalClass()} alone would also
   *       accept a member class on some class-file shapes; the extra negation keeps the two
   *       conditions disjoint, which is what upstream means by "anonymous or non-static local".</li>
   * </ul>
   */
  private boolean isAnonymousOrNonStaticLocal(Class<?> clazz) {
    return !Enum.class.isAssignableFrom(clazz) && !isStatic(clazz)
        && (clazz.getSimpleName().isEmpty()
            || (clazz.isLocalClass() && !clazz.isMemberClass()));
  }

  private boolean isInnerClass(Class<?> clazz) {
    return clazz.isMemberClass() && !isStatic(clazz);
  }

  private boolean isStatic(Class<?> clazz) {
    return (clazz.getModifiers() & Modifier.STATIC) != 0;
  }

  private boolean isValidVersion(Since since, Until until) {
    return isValidSince(since) && isValidUntil(until);
  }

  private boolean isValidSince(Since annotation) {
    if (annotation != null) {
      double annotationVersion = annotation.value();
      return version >= annotationVersion;
    }
    return true;
  }

  private boolean isValidUntil(Until annotation) {
    if (annotation != null) {
      double annotationVersion = annotation.value();
      return version < annotationVersion;
    }
    return true;
  }
}
