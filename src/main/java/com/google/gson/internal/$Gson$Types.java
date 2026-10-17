/*
 * Copyright (C) 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gson.internal;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

/**
 * Utilities for working with Gson type tokens.
 *
 * <h2>Port note: this file is a deliberate, bounded reduction of upstream Gson 2.10</h2>
 *
 * <p>Upstream, every type Gson can represent is one of six shapes: {@code Class},
 * {@code java.lang.reflect.ParameterizedType}, {@code GenericArrayType}, {@code WildcardType},
 * {@code TypeVariable} and the bare {@code Type} interface. The four subtypes do not exist in this
 * target's TeaVM classlib. That is not a packaging gap this project's source tree can close --
 * {@code java.*} types are resolved by TeaVM from its own internal model, not from a classpath --
 * and it is verifiable against {@code .decompile_work/classlib-model.txt}, where
 * {@code java.lang.reflect.Type} is modelled with no members and the other four are absent.
 * {@code Class.getGenericSuperclass}, {@code Class.getGenericInterfaces} and
 * {@code Field.getGenericType} are absent for the same reason.
 *
 * <p>What survives is a two-shape world: a {@code Class}, or a {@link GenericType} (raw class plus
 * arguments) that a caller builds <i>explicitly</i>. Every function below therefore has two jobs:
 * handle the plain case exactly as upstream does, and read type arguments off a {@code GenericType}
 * when one is present.
 *
 * <p><b>What is genuinely lost.</b> The <i>anonymous</i> form
 * {@code new TypeToken<Map<String, Foo>>() {}} recovers its argument from
 * {@code Class.getGenericSuperclass()}, which is absent. That specific spelling therefore cannot
 * work here, and callers must use {@link com.google.gson.reflect.TypeToken#getParameterized}
 * instead. That is the one behavioural change, and it is a platform limitation rather than a
 * simplification: the information is not lost by this file, only the one syntax that could extract
 * it is unavailable.
 *
 * <p>Everything else -- the tree model, streaming readers and writers, reflective field access, all
 * the built-in adapters -- is unchanged from upstream.
 *
 * @author Jesse Wilson
 * @author Swapna Burugupalli
 */
public final class $Gson$Types {

  private $Gson$Types() {}

  /**
   * Returns {@code type} in canonical form.
   *
   * <p>Upstream rebuilt an equivalent tree so that two structurally identical parameterized types
   * compared equal. {@link GenericType} is already canonical by construction -- it is immutable,
   * its arguments are immutable, and {@link GenericType#equals} is structural -- so identity is
   * canonical form here and this is the identity function.
   */
  public static Type canonicalize(Type type) {
    return type;
  }

  /**
   * Returns the raw {@link Class} behind {@code type}.
   *
   * @throws IllegalArgumentException if {@code type} is neither a {@code Class} nor a
   *     {@code GenericType}, which on this target means a foreign {@code Type} implementation
   *     leaked in from somewhere.
   */
  public static Class<?> getRawType(Type type) {
    if (type instanceof Class) {
      return (Class<?>) type;
    }
    if (type instanceof GenericType) {
      return ((GenericType) type).getRawType();
    }
    throw new IllegalArgumentException(
        "Unsupported type on this platform: " + type
            + " (a " + (type == null ? "null" : type.getClass().getName()) + ")."
            + " java.lang.reflect.ParameterizedType / WildcardType / GenericArrayType / TypeVariable"
            + " are absent from the WASM-GC TeaVM classlib and cannot be implemented from source."
            + " Use TypeToken.getParameterized(rawClass, args...) to express a generic type.");
  }

  /** Structural equality, across both representable shapes. */
  public static boolean equals(Type a, Type b) {
    return a == b || (a != null && a.equals(b));
  }

  /** A readable rendering of a type, used in error messages and {@code TypeToken.toString()}. */
  public static String typeToString(Type type) {
    if (type instanceof Class) {
      return ((Class<?>) type).getName();
    }
    return String.valueOf(type);
  }

  /**
   * The component type of an array type, or {@code null} if {@code array} is not an array.
   *
   * <p>Upstream also accepts {@code GenericArrayType}, which describes {@code T[]} where the
   * component is a type variable. Such a type cannot be represented here, so only real array
   * classes are accepted.
   */
  public static Type getArrayComponentType(Type array) {
    return array instanceof Class && ((Class<?>) array).isArray()
        ? ((Class<?>) array).getComponentType()
        : null;
  }

  /**
   * The element type of a {@code Collection}, or {@code Object.class} when it is not knowable.
   *
   * <p>For a raw {@code Collection} upstream also returns {@code Object.class}, so that fallback
   * is upstream behaviour rather than a guess.
   */
  public static Type getCollectionElementType(Type context, Class<?> contextRawType) {
    if (context instanceof GenericType) {
      GenericType generic = (GenericType) context;
      if (Collection.class.isAssignableFrom(generic.getRawType())
          && generic.getTypeArgumentCount() == 1) {
        return generic.getTypeArgument(0);
      }
    }
    return Object.class;
  }

  /**
   * The key and value types of a {@code Map}, or {@code {String.class, Object.class}} when they are
   * not knowable.
   *
   * <p>The key default is {@code String} because a JSON object always has string keys, so that is
   * not a guess.
   */
  public static Type[] getMapKeyAndValueTypes(Type context, Class<?> contextRawType) {
    if (context instanceof GenericType) {
      GenericType generic = (GenericType) context;
      if (Map.class.isAssignableFrom(generic.getRawType()) && generic.getTypeArgumentCount() == 2) {
        return new Type[] {generic.getTypeArgument(0), generic.getTypeArgument(1)};
      }
    }
    return new Type[] {String.class, Object.class};
  }

  /**
   * Substitutes type variables in {@code toResolve} against {@code contextRawType}.
   *
   * <p>Reduced to the identity function. There are no {@code TypeVariable}s to substitute and no
   * {@code getGenericSuperclass} to resolve them against, so there is nothing this could do beyond
   * return the declared type -- which is the most specific type information that still exists
   * here.
   */
  public static Type resolve(Type context, Class<?> contextRawType, Type toResolve) {
    return toResolve;
  }

  /**
   * Throws if {@code type} is a primitive, which Gson cannot serialize meaningfully.
   *
   * <p>Public because {@link com.google.gson.reflect.TypeToken#getParameterized} enforces it on
   * each argument, mirroring upstream, where this check lives in
   * {@code newParameterizedTypeWithOwner}.
   */
  public static void checkNotPrimitive(Type type) {
    Class<?> raw = rawIfClass(type);
    if (raw != null && raw.isPrimitive()) {
      throw new IllegalArgumentException("Primitive " + type + " is not a valid Gson Type");
    }
  }

  /** The type's arguments if it has any, otherwise {@code null}. */
  static Type[] getTypeArguments(Type type) {
    return type instanceof GenericType ? ((GenericType) type).getTypeArguments() : null;
  }

  /** {@code type} itself if it is a {@code Class}, else {@code null}. */
  private static Class<?> rawIfClass(Type type) {
    return type instanceof Class ? (Class<?>) type : null;
  }
}
