package org.teavm.classlib.java.lang;

/*
 * Shim for java.lang.ThreadGroup. The classlib does not model it at all, so unlike
 * the classes this jar relocates into org.teavm.classlib.java.lang as X* overrides,
 * there is no classlib original here to shadow -- this is simply the missing type,
 * and the classlib's own mapPackageHierarchy + stripPrefix rules pick it up with no
 * properties entry needed:
 *
 *   java.lang.ThreadGroup -> org.teavm.classlib.java.lang.ThreadGroup
 *                         -> prefix T inserted -> TThreadGroup (this file)
 *
 * Exactly one class on this target touches it. DefaultThreadFactory passes a group
 * straight through to FastThreadLocalThread's three-argument constructor, and that
 * constructor discards it -- it exists so the thread has a parent for ThreadGroup's
 * bookkeeping, which nothing on this target does. The group is stored for the same
 * reason TThread's own copy is: so a future getThreadGroup has something to return.
 *
 * It is deliberately NOT an override of a classlib type, so it carries no
 * mapClass| rule. Adding one would be wrong -- there is nothing to override.
 */
public class TThreadGroup extends TObject {

	private String name;

	public TThreadGroup() {
		this(null);
	}

	public TThreadGroup(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return getClass().getName() + "[name=" + name + "]";
	}

}
