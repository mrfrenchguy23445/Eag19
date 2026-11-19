package org.teavm.classlib.java.lang.reflect;

/*
 * Via's shaded Gson reaches java.lang.reflect.GenericDeclaration through
 * TypeVariable.getGenericDeclaration(). The classlib ships TType but not the
 * rest of the generic-reflection interfaces, so canonicalize() and equals()
 * fail to link. This is the minimal JDK-shaped surface Gson actually uses.
 */
public interface TGenericDeclaration {

	TTypeVariable<?>[] getTypeParameters();

}
