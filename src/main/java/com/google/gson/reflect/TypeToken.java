/*
 * Copyright (C) 2008 Google Inc.
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

package com.google.gson.reflect;

import com.google.gson.internal.$Gson$Types;
import com.google.gson.internal.GenericType;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Represents a generic type {@code T}. Java doesn't yet provide a way to represent generic types,
 * so this class does. Instances are obtained by calling {@link #get}.
 *
 * <h2>Port note</h2>
 *
 * <p>Upstream's anonymous-subclass spelling
 * <pre>{@code new TypeToken<List<String>>() {}}</pre>
 * works by reading the anonymous class's generic superclass signature, i.e.
 * {@code getClass().getGenericSuperclass()}, and picking {@code T} out of it. On this target that
 * method is absent from the TeaVM classlib, as are {@code Class.getTypeParameters},
 * {@code TypeVariable} and {@code ParameterizedType} -- see
 * {@code .decompile_work/classlib-model.txt} and the comment on
 * {@link com.google.gson.internal.$Gson$Types}. The argument is genuinely unrecoverable here, so
 * the anonymous form cannot be supported.
 *
 * <p>Everything it did is available through explicit factories instead:
 *
 * <pre>{@code
 * // instead of new TypeToken<Map<String, Foo>>() {}
 * TypeToken<Map<String, Foo>> t = TypeToken.getParameterized(Map.class, String.class, Foo.class);
 *
 * // instead of new TypeToken<List<String>>() {}
 * TypeToken<List<String>> t = TypeToken.getParameterized(List.class, String.class);
 * }</pre>
 *
 * <p>Three call sites in Minecraft needed this (resource packs, the sound event registry and
 * player advancements); they use the factories above.
 *
 * <p>The protected no-arg constructor is kept so that any remaining anonymous subclass still
 * compiles, but it now fails immediately with an explanation instead of returning a token for the
 * wrong type. Failing loudly here is deliberate: the alternative -- defaulting to {@code Object} --
 * would silently produce {@code List<LinkedTreeMap>} where {@code List<String>} was asked for, and
 * the corruption would surface far away from its cause.
 *
 * <p>Assignability checking is correspondingly reduced. Upstream walks generic supertypes to prove
 * {@code List<String>} assignable from {@code Collection<String>}; without generic signatures only
 * raw-class assignability is decidable, so that is what this checks. It is a strictly coarser
 * answer, and it is the only answer the platform can support.
 *
 * @author Jesse Wilson
 */
public final class TypeToken<T> {

  private final Type type;
  private final Class<? super T> rawType;
  private final int hashCode;

  /**
   * Constructs a new type literal from an anonymous subclass.
   *
   * @throws IllegalStateException always on this platform -- see the class comment. Use
   *     {@link #get(Class)}, {@link #get(Type)}, {@link #getParameterized(Type, Type...)} or
   *     {@link #getArray(Type)} instead.
   */
  protected TypeToken() {
    throw new IllegalStateException(
        "new TypeToken<...>() {} cannot recover its type argument on this platform:"
            + " Class.getGenericSuperclass() is not available in the WASM-GC TeaVM classlib."
            + " Use TypeToken.getParameterized(Raw.class, Arg.class, ...) instead,"
            + " or TypeToken.get(Foo.class) for a non-generic type.");
  }

  /**
   * Unsafe. Constructs a type literal manually.
   *
   * @throws IllegalArgumentException if {@code type} is neither a {@code Class} nor a
   *     {@link GenericType}.
   */
  @SuppressWarnings("unchecked")
  private TypeToken(Type type) {
    this.type = $Gson$Types.canonicalize(Objects.requireNonNull(type));
    this.rawType = (Class<? super T>) $Gson$Types.getRawType(this.type);
    this.hashCode = this.type.hashCode();
  }

  /**
   * Returns the raw (non-generic) type for this type.
   */
  public final Class<? super T> getRawType() {
    return rawType;
  }

  /**
   * Gets underlying {@code Type} instance. Either a {@code Class} or a {@link GenericType}.
   */
  public final Type getType() {
    return type;
  }

  /**
   * Check if this type is assignable from the given class object.
   *
   * <p>Raw-class assignability only; see the class comment.
   */
  public boolean isAssignableFrom(Class<?> cls) {
    return rawType.isAssignableFrom(cls);
  }

  /**
   * Check if this type is assignable from the given {@code Type}.
   *
   * <p>Exact match, or raw-class assignability if this token has no type arguments. See the class
   * comment.
   */
  public boolean isAssignableFrom(Type from) {
    if (from == null) {
      return false;
    }
    if (type.equals(from)) {
      return true;
    }
    if (type instanceof GenericType) {
      // A parameterized target only accepts a source with equal arguments, which the equality
      // check above has already rejected. Anything else is not provably assignable.
      return false;
    }
    return rawType.isAssignableFrom($Gson$Types.getRawType(from));
  }

  /**
   * Check if this type is assignable from the given type token.
   */
  public boolean isAssignableFrom(TypeToken<?> token) {
    return isAssignableFrom(token.getType());
  }

  @Override public final int hashCode() {
    return this.hashCode;
  }

  @Override public final boolean equals(Object o) {
    return o instanceof TypeToken<?>
        && $Gson$Types.equals(type, ((TypeToken<?>) o).type);
  }

  @Override public final String toString() {
    return $Gson$Types.typeToString(type);
  }

  /**
   * Gets type literal for the given {@code Type} instance.
   */
  public static TypeToken<?> get(Type type) {
    return new TypeToken<>(type);
  }

  /**
   * Gets type literal for the given {@code Class} instance.
   */
  public static <T> TypeToken<T> get(Class<T> type) {
    return new TypeToken<>(type);
  }

  /**
   * Gets a type literal for the parameterized type formed by applying {@code typeArguments} to
   * {@code rawType}. This is the replacement for the anonymous-subclass spelling
   * {@code new TypeToken<Map<K, V>>() {}}, which cannot work on this target:
   *
   * <pre>{@code
   * TypeToken<Map<String, Foo>> t = TypeToken.getParameterized(Map.class, String.class, Foo.class);
   * }</pre>
   *
   * <p><b>The returned type parameter is unchecked and is taken from the assignment context.</b>
   * Nothing here can check it -- the arguments are {@code Type}s, and the type variables they would
   * have to satisfy are exactly what this platform cannot express -- so a wrong argument list
   * produces a {@code ClassCastException} at the first use instead of an error at the call. That is
   * the same bargain upstream strikes, except that upstream returns {@code TypeToken<?>} and forces
   * an explicit cast at every call site. Overloading on {@link Class} (below) lets the common
   * spelling assign directly, which removes the cast without adding any unsoundness.
   *
   * <p><b>Validation is reduced relative to upstream.</b> Upstream compares the argument count
   * against {@code Class.getTypeParameters()} and checks each argument against its type variable's
   * bounds; both need {@code getTypeParameters}/{@code TypeVariable.getBounds}, which are absent
   * here. What remains is a check that no argument is a primitive, which is what upstream's own
   * {@code newParameterizedTypeWithOwner} enforces. A wrong argument count degrades to the
   * behaviour of a raw {@code Map}/{@code Collection} rather than throwing, so callers should
   * double-check their own arity.
   *
   * @throws IllegalArgumentException if an argument is a primitive type
   * @throws NullPointerException if {@code rawType}, {@code typeArguments} or any argument is null
   */
  public static <T> TypeToken<T> getParameterized(Class<?> rawType, Type... typeArguments) {
    Objects.requireNonNull(rawType);
    validateArguments(typeArguments);
    return new TypeToken<>(new GenericType(rawType, typeArguments));
  }

  /**
   * Same as {@link #getParameterized(Class, Type...)}, but for a {@code rawType} that is only
   * statically known to be a {@link Type}.
   *
   * <p>This overload keeps upstream's exact signature and so its {@code TypeToken<?>} result,
   * including upstream's check that {@code rawType} really is a {@code Class}. It exists so that
   * existing callers still compile; prefer the {@link Class} overload where you can.
   *
   * @throws IllegalArgumentException if {@code rawType} is not a {@code Class}, or an argument is a
   *     primitive type
   */
  public static TypeToken<?> getParameterized(Type rawType, Type... typeArguments) {
    Objects.requireNonNull(rawType);
    // Perform basic validation here because this is the only public API where users
    // can create malformed parameterized types
    if (!(rawType instanceof Class)) {
      // See also https://bugs.openjdk.org/browse/JDK-8250659
      throw new IllegalArgumentException("rawType must be of type Class, but was " + rawType);
    }
    validateArguments(typeArguments);
    return new TypeToken<>(new GenericType((Class<?>) rawType, typeArguments));
  }

  private static void validateArguments(Type[] typeArguments) {
    Objects.requireNonNull(typeArguments);
    for (Type typeArgument : typeArguments) {
      Objects.requireNonNull(typeArgument, "typeArguments must not contain null");
      $Gson$Types.checkNotPrimitive(typeArgument);
    }
  }

  /**
   * Gets type literal for the array type whose elements are all instances of {@code componentType}.
   *
   * <p>Upstream returns a {@code GenericArrayType} when the component is itself generic. Such a
   * type cannot exist here, so {@code componentType} must be a {@code Class}; the resulting token
   * is then an ordinary array class, which {@code ArrayTypeAdapter} handles unchanged.
   *
   * @throws IllegalArgumentException if {@code componentType} is not a {@code Class}
   */
  public static TypeToken<?> getArray(Type componentType) {
    if (!(componentType instanceof Class)) {
      throw new IllegalArgumentException("componentType must be of type Class, but was "
          + componentType + ". Array types with a generic component (T[]) cannot be represented"
          + " on this platform; pass the concrete element class instead.");
    }
    return new TypeToken<>(Array.newInstance((Class<?>) componentType, 0).getClass());
  }
}
