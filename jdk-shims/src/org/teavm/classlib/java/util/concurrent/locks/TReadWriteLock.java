package org.teavm.classlib.java.util.concurrent.locks;

/*
 * ViaVersion's ProtocolManagerImpl declares
 *   private final ReadWriteLock mappingLoaderLock = new ReentrantReadWriteLock();
 * and the classlib shipped ReentrantReadWriteLock but not the interface. The
 * lock accessors are the only members Via calls; see TReentrantReadWriteLock.
 */
public interface TReadWriteLock {

	TLock readLock();

	TLock writeLock();

}
