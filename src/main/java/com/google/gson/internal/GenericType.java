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
import java.util.Arrays;

/**
 * A concrete parameterized type: a raw {@link Class} plus its type arguments.
 *
 * <h2>Why this class exists</h2>
 *
 * <p>Upstream Gson represents {@code Map<String, Foo>} as a
 * {@code java.lang.reflect.ParameterizedType}. That interface is not in this target's TeaVM
 * classlib, and {@code java.*} types cannot be supplied from this project's source tree, so
 * implementing it is not an option. See {@code .decompile_work/classlib-model.txt}: under
 * {@code java.lang.reflect} only {@code Type} itself is modelled, and it has no members.
 *
 * <p>The information itself is still representable, though -- it is just a raw class and an array
 * of arguments -- so this class carries it, and {@link $Gson$Types} learns to read it. Nothing
 * outside Gson ever needs to see this type: callers name it through
 * {@link com.google.gson.reflect.TypeToken#getParameterized}.
 *
 * <p>The important consequence is that generic container deserialization keeps working when the
 * caller asks for it explicitly. It is the <i>anonymous</i> form
 * {@code new TypeToken<Map<String, Foo>>() {}} that cannot work, because recovering that argument
 * requires {@code Class.getGenericSuperclass()}, which is absent for exactly the same reason.
 *
 * @see $Gson$Types
 */
public final class GenericType implements Type {

  private final Class<?> rawType;
  private final Type[] typeArguments;

  public GenericType(Class<?> rawType, Type... typeArguments) {
    if (rawType == null) {
      throw new NullPointerException("rawType");
    }
    if (typeArguments == null) {
      throw new NullPointerException("typeArguments");
    }
    for (Type argument : typeArguments) {
      if (argument == null) {
        throw new NullPointerException("typeArguments must not contain null");
      }
    }
    this.rawType = rawType;
    this.typeArguments = typeArguments.clone();
  }

  public Class<?> getRawType() {
    return rawType;
  }

  /** A fresh copy, so callers cannot mutate this type's arguments. */
  public Type[] getTypeArguments() {
    return typeArguments.clone();
  }

  public int getTypeArgumentCount() {
    return typeArguments.length;
  }

  public Type getTypeArgument(int index) {
    return typeArguments[index];
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof GenericType)) {
      return false;
    }
    GenericType other = (GenericType) o;
    return rawType.equals(other.rawType) && Arrays.equals(typeArguments, other.typeArguments);
  }

  @Override
  public int hashCode() {
    return rawType.hashCode() ^ Arrays.hashCode(typeArguments);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(rawType.getName());
    if (typeArguments.length > 0) {
      sb.append('<');
      for (int i = 0; i < typeArguments.length; i++) {
        if (i > 0) {
          sb.append(", ");
        }
        sb.append(typeArguments[i] instanceof Class
            ? ((Class<?>) typeArguments[i]).getName()
            : String.valueOf(typeArguments[i]));
      }
      sb.append('>');
    }
    return sb.toString();
  }
}
