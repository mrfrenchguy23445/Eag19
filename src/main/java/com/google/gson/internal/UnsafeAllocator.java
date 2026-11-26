/*
 * Copyright (C) 2007 Google Inc.
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

/**
 * Creates instances of a class without calling any of its constructors.
 *
 * <h2>Port note: this allocator always refuses</h2>
 *
 * <p>Upstream tries three backdoors in {@code create()}, in order:
 *
 * <ol>
 *   <li>{@code sun.misc.Unsafe.allocateInstance(Class)} -- {@code sun.misc.Unsafe} does not exist
 *       in this target's TeaVM classlib at all;</li>
 *   <li>{@code ObjectStreamClass.newInstance} (Dalvik, post-gingerbread) --
 *       {@code java.io.ObjectStreamClass} is not modelled;</li>
 *   <li>{@code ObjectInputStream.newInstance} (Dalvik, pre-gingerbread) --
 *       {@code java.io.ObjectInputStream} is not modelled.</li>
 * </ol>
 *
 * <p>None of those three types is available, so every branch would fall through to upstream's
 * final "give up" allocator anyway -- except that each branch is a hard <i>compile</i> error here,
 * because TeaVM resolves the referenced type while building the call graph, long before any
 * {@code catch} could swallow the failure. Referencing them at all is what broke the build; three
 * distinct classes, three errors.
 *
 * <p>So the backdoors are gone rather than left to fail. The behaviour on use is upstream's own
 * last-resort behaviour: throw, and say what would actually have worked. {@code
 * ConstructorConstructor.newUnsafeAllocator} catches this and re-throws with the same guidance, so
 * the caller sees one message.
 *
 * <p><b>What this costs.</b> Gson can no longer instantiate a type that has no accessible no-args
 * constructor and no registered {@code InstanceCreator}/{@code TypeAdapter}. On a JVM that path
 * was rare; here it is the only path. In practice Minecraft's Gson targets are all records,
 * {@code Component} (which has a registered serializer), or plain beans with a no-args or
 * all-args constructor, so the fallback is not reached -- and when it is, a precise exception is
 * far better than the alternative, which would be a silently zero-filled object.
 */
public abstract class UnsafeAllocator {

  public abstract <T> T newInstance(Class<T> c) throws Exception;

  /**
   * Asserts that the class is instantiable. This check should have already occurred
   * in {@link ConstructorConstructor}; this check here acts as safeguard since trying
   * to use Unsafe for non-instantiable classes might crash the JVM on some devices.
   */
  private static void assertInstantiable(Class<?> c) {
    String exceptionMessage = ConstructorConstructor.checkInstantiable(c);
    if (exceptionMessage != null) {
      throw new AssertionError("UnsafeAllocator is used for non-instantiable type: " + exceptionMessage);
    }
  }

  public static final UnsafeAllocator INSTANCE = new UnsafeAllocator() {
    @Override public <T> T newInstance(Class<T> c) {
      assertInstantiable(c);
      throw new UnsupportedOperationException(
          "Cannot allocate an instance of " + c.getName()
              + " without calling a constructor on this platform. sun.misc.Unsafe,"
              + " java.io.ObjectStreamClass and java.io.ObjectInputStream -- the three backdoors"
              + " Gson would normally use -- are all absent from the WASM-GC TeaVM classlib."
              + " Give the class a no-args constructor, or register an InstanceCreator or a"
              + " TypeAdapter for it. (For the common case of a type with only an all-args"
              + " constructor, Gson can construct it reflectively: see ConstructorConstructor"
              + " .newDefaultConstructor, which handles that without this class.)");
    }
  };
}
