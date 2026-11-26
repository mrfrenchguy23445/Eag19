package org.teavm.classlib.java.lang.reflect;

/*
 * See TGenericDeclaration.java.
 */
public interface TWildcardType extends TType {

	TType[] getUpperBounds();

	TType[] getLowerBounds();

}
