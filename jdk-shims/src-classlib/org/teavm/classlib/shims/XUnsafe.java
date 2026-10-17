package org.teavm.classlib.shims;

/*
 * Replacement for sun.misc.Unsafe, generated from a javap dump of the host JDK 17
 * class so that every public member -- all 88 methods and all 20 fields -- keeps
 * resolving. Nothing here works; nothing is meant to.
 *
 * WHY A REPLACEMENT IS NEEDED AT ALL, WHEN sun.misc.Unsafe RESOLVES FINE ON ITS OWN:
 *
 *   Method java.lang.Class.isHidden()Z was not found
 *       at sun.misc.Unsafe.objectFieldOffset(Unsafe.java:644)
 *       at io.netty.util.internal.PlatformDependent0.objectFieldOffset(...:628)
 *       at io.netty.util.internal.PlatformDependent0.<clinit>(...:331)
 *
 * and the same for isRecord() at Unsafe.java:647, which is staticFieldOffset, and
 * again in staticFieldBase. The host JDK's own objectFieldOffset does
 *
 *     field.getDeclaringClass().isHidden()   // line 644
 *     field.getDeclaringClass().isRecord()   // line 647
 *
 * and the classlib's java.lang.Class has neither method. Replacing java.lang.Class
 * is impossible -- see the note in META-INF/teavm.properties about XClass -- so the
 * only way to cut this edge is to replace the caller instead. Every body below is
 * a throw, so no body references Class.isHidden and the edge is gone.
 *
 * WHY netty still runs. PlatformDependent0 reads the instance reflectively, via a
 * privileged getDeclaredField("theUnsafe") on sun.misc.Unsafe. That field is the
 * one member deliberately NOT copied, so the lookup throws NoSuchFieldException,
 * PlatformDependent0 ends its <clinit> with UNSAFE == null, and netty takes its
 * pure-Java paths -- which is where it goes anyway once io.netty.noUnsafe is set.
 * getUnsafe() throws SecurityException, which is what every call site in the
 * program already expects from a class loaded off the bootstrap loader.
 *
 * Registered as
 *     mapClass|org.teavm.classlib.shims.XUnsafe=sun.misc.Unsafe
 * in this jar's META-INF/teavm.properties. A file at sun/misc/Unsafe.class would
 * never be read -- the module image answers first -- so the target has to live in
 * the shims package, where toUnmappedClassName leaves it alone and
 * reverseClassMappings matches it verbatim.
 *
 * A flat class: no inner classes, so there is nothing else to rename with it.
 */
public final class XUnsafe {


    public static final int INVALID_FIELD_OFFSET = -1;
    public static final int ARRAY_BOOLEAN_BASE_OFFSET = 16;
    public static final int ARRAY_BYTE_BASE_OFFSET = 16;
    public static final int ARRAY_SHORT_BASE_OFFSET = 16;
    public static final int ARRAY_CHAR_BASE_OFFSET = 16;
    public static final int ARRAY_INT_BASE_OFFSET = 16;
    public static final int ARRAY_LONG_BASE_OFFSET = 16;
    public static final int ARRAY_FLOAT_BASE_OFFSET = 16;
    public static final int ARRAY_DOUBLE_BASE_OFFSET = 16;
    public static final int ARRAY_OBJECT_BASE_OFFSET = 16;
    public static final int ARRAY_BOOLEAN_INDEX_SCALE = 1;
    public static final int ARRAY_BYTE_INDEX_SCALE = 1;
    public static final int ARRAY_SHORT_INDEX_SCALE = 2;
    public static final int ARRAY_CHAR_INDEX_SCALE = 2;
    public static final int ARRAY_INT_INDEX_SCALE = 4;
    public static final int ARRAY_LONG_INDEX_SCALE = 8;
    public static final int ARRAY_FLOAT_INDEX_SCALE = 4;
    public static final int ARRAY_DOUBLE_INDEX_SCALE = 8;
    public static final int ARRAY_OBJECT_INDEX_SCALE = 8;
    public static final int ADDRESS_SIZE = 8;

    private XUnsafe() {
    }


    public static sun.misc.Unsafe getUnsafe() {
        throw new SecurityException("not available");
    }
    public int getInt(java.lang.Object p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putInt(java.lang.Object p0, long p1, int p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public java.lang.Object getObject(java.lang.Object p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putObject(java.lang.Object p0, long p1, java.lang.Object p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public boolean getBoolean(java.lang.Object p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putBoolean(java.lang.Object p0, long p1, boolean p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public byte getByte(java.lang.Object p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putByte(java.lang.Object p0, long p1, byte p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public short getShort(java.lang.Object p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putShort(java.lang.Object p0, long p1, short p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public char getChar(java.lang.Object p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putChar(java.lang.Object p0, long p1, char p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public long getLong(java.lang.Object p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putLong(java.lang.Object p0, long p1, long p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public float getFloat(java.lang.Object p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putFloat(java.lang.Object p0, long p1, float p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public double getDouble(java.lang.Object p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putDouble(java.lang.Object p0, long p1, double p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public byte getByte(long p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putByte(long p0, byte p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public short getShort(long p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putShort(long p0, short p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public char getChar(long p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putChar(long p0, char p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public int getInt(long p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putInt(long p0, int p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public long getLong(long p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putLong(long p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public float getFloat(long p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putFloat(long p0, float p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public double getDouble(long p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putDouble(long p0, double p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public long getAddress(long p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putAddress(long p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public long allocateMemory(long p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public long reallocateMemory(long p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void setMemory(java.lang.Object p0, long p1, long p2, byte p3) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void setMemory(long p0, long p1, byte p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void copyMemory(java.lang.Object p0, long p1, java.lang.Object p2, long p3, long p4) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void copyMemory(long p0, long p1, long p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void freeMemory(long p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public long objectFieldOffset(java.lang.reflect.Field p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public long staticFieldOffset(java.lang.reflect.Field p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public java.lang.Object staticFieldBase(java.lang.reflect.Field p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public boolean shouldBeInitialized(java.lang.Class<?> p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void ensureClassInitialized(java.lang.Class<?> p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public int arrayBaseOffset(java.lang.Class<?> p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public int arrayIndexScale(java.lang.Class<?> p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public int addressSize() {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public int pageSize() {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public java.lang.Object allocateInstance(java.lang.Class<?> p0) throws java.lang.InstantiationException {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void throwException(java.lang.Throwable p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public final boolean compareAndSwapObject(java.lang.Object p0, long p1, java.lang.Object p2, java.lang.Object p3) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public final boolean compareAndSwapInt(java.lang.Object p0, long p1, int p2, int p3) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public final boolean compareAndSwapLong(java.lang.Object p0, long p1, long p2, long p3) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public java.lang.Object getObjectVolatile(java.lang.Object p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putObjectVolatile(java.lang.Object p0, long p1, java.lang.Object p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public int getIntVolatile(java.lang.Object p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putIntVolatile(java.lang.Object p0, long p1, int p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public boolean getBooleanVolatile(java.lang.Object p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putBooleanVolatile(java.lang.Object p0, long p1, boolean p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public byte getByteVolatile(java.lang.Object p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putByteVolatile(java.lang.Object p0, long p1, byte p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public short getShortVolatile(java.lang.Object p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putShortVolatile(java.lang.Object p0, long p1, short p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public char getCharVolatile(java.lang.Object p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putCharVolatile(java.lang.Object p0, long p1, char p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public long getLongVolatile(java.lang.Object p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putLongVolatile(java.lang.Object p0, long p1, long p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public float getFloatVolatile(java.lang.Object p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putFloatVolatile(java.lang.Object p0, long p1, float p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public double getDoubleVolatile(java.lang.Object p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putDoubleVolatile(java.lang.Object p0, long p1, double p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putOrderedObject(java.lang.Object p0, long p1, java.lang.Object p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putOrderedInt(java.lang.Object p0, long p1, int p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void putOrderedLong(java.lang.Object p0, long p1, long p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void unpark(java.lang.Object p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void park(boolean p0, long p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public int getLoadAverage(double[] p0, int p1) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public final int getAndAddInt(java.lang.Object p0, long p1, int p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public final long getAndAddLong(java.lang.Object p0, long p1, long p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public final int getAndSetInt(java.lang.Object p0, long p1, int p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public final long getAndSetLong(java.lang.Object p0, long p1, long p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public final java.lang.Object getAndSetObject(java.lang.Object p0, long p1, java.lang.Object p2) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void loadFence() {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void storeFence() {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void fullFence() {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
    public void invokeCleaner(java.nio.ByteBuffer p0) {
        throw new UnsupportedOperationException("sun.misc.Unsafe");
    }
}
