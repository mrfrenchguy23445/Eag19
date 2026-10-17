/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.lang;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.classlib.impl.Base64Impl;
import org.teavm.interop.NoSideEffects;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSObject;

public abstract class XClassLoader extends TObject {
    private XClassLoader parent;
    private static TSystemClassLoader systemClassLoader = new TSystemClassLoader();
    private static ResourceContainer resources;

    protected XClassLoader() {
        this(null);
    }

    protected XClassLoader(XClassLoader parent) {
        this.parent = parent;
    }

    public XClassLoader getParent() {
        return parent;
    }

    public static XClassLoader getSystemClassLoader() {
        // The cast looks like it is papering over a real mistake, but nothing is lost by it.
        // TSystemClassLoader extends the classlib's TClassLoader, not this file's XClassLoader,
        // because the classlib cannot know this jar renamed its superclass. The two are one
        // type by the time anything links: toUnmappedClassName strips the X here and the T
        // there, and both land on java.lang.ClassLoader. javac sees two types, the linker
        // sees one, and this is where the two views are reconciled.
        return (XClassLoader) (Object) systemClassLoader;
    }

    public InputStream getResourceAsStream(String name) {
        if (resources == null) {
            resources = supplyResources();
        }
        JSObject data = resources.getResource(name);
        String dataString = resourceToString(data);
        if (dataString == null) {
            return null;
        }
        byte[] bytes = new byte[dataString.length()];
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte) dataString.charAt(i);
        }
        return new ByteArrayInputStream(Base64Impl.decode(bytes));
    }

    public static InputStream getSystemResourceAsStream(String name) {
        return getSystemClassLoader().getResourceAsStream(name);
    }

    @JSBody(params = "resource", script = "return resource !== null && resource !== void 0 ? resource : null;")
    private static native String resourceToString(JSObject resource);

    @InjectedBy(ClassLoaderNativeGenerator.class)
    @NoSideEffects
    private static native ResourceContainer supplyResources();

    interface ResourceContainer extends JSObject {
        @JSIndexer
        JSObject getResource(String name);
    }


    /*
     * Port note: loadClass(String) and loadClass(String, boolean) are the only members added.
     *
     * The classlib's ClassLoader has no loadClass at all -- it serves resources and nothing
     * else -- so every class is resolved by the linker rather than by a running program. The one
     * caller on this target is netty's PlatformDependent0$7, and its whole body sits inside a
     * single catch (Throwable):
     *
     *   Class<?> c = getClassLoader(PlatformDependent0.class).loadClass("jdk.internal.misc.Unsafe");
     *   return c.getDeclaredMethod("getUnsafe", new Class[0]).invoke(null, new Object[0]);
     *   catch (Throwable t) { return t; }
     *
     * so it is *expecting* this to fail: jdk.internal.misc.Unsafe does not exist in a browser
     * and never will. Throwing ClassNotFoundException is therefore not a degraded answer, it is
     * the correct one, and netty handles it on the next instruction.
     *
     * So the body throws and never resolves anything. That is also why it does not defer to
     * TClass.forName: deferring would make the method succeed for a name that does exist, and
     * nothing on this target wants that -- the one call site above is a probe that only works
     * when it fails. TClassNotFoundException is the same checked exception the real
     * ClassLoader.loadClass declares, so the descriptor matches exactly.
     */
    public TClass<?> loadClass(TString name) throws TClassNotFoundException {
        return loadClass(name, false);
    }

    public TClass<?> loadClass(TString name, boolean resolve) throws TClassNotFoundException {
        throw new TClassNotFoundException();
    }

}
