package org.teavm.classlib.java.lang.reflect;

/*
 * See TGenericDeclaration.java. Gson resolves type variables through
 * getGenericDeclaration() and getName(); getBounds() is included to keep the
 * interface's JDK shape intact.
 */
public interface TTypeVariable<D extends TGenericDeclaration> extends TType {

	TType[] getBounds();

	D getGenericDeclaration();

	String getName();

}
