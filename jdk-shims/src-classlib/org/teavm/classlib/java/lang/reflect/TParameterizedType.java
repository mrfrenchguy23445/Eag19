package org.teavm.classlib.java.lang.reflect;

/*
 * See TGenericDeclaration.java. Signatures mirror the JDK interface exactly so
 * the shaded Gson bytecode resolves after the T->java classlib mapping.
 */
public interface TParameterizedType extends TType {

	TType[] getActualTypeArguments();

	TType getRawType();

	TType getOwnerType();

}
