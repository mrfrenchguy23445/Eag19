/**
 * Copyright (c) 2004-2011 QOS.ch
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.slf4j.helpers;

/**
 * An internal utility class.
 *
 * @author Alexander Dorokhine
 * @author Ceki G&uuml;lc&uuml;
 */
public final class Util {

    private Util() {
    }

    public static String safeGetSystemProperty(String key) {
        if (key == null)
            throw new IllegalArgumentException("null input");

        String result = null;
        try {
            result = System.getProperty(key);
        } catch (java.lang.SecurityException sm) {
            ; // ignore
        }
        return result;
    }

    public static boolean safeGetBooleanSystemProperty(String key) {
        String value = safeGetSystemProperty(key);
        if (value == null)
            return false;
        else
            return value.equalsIgnoreCase("true");
    }

    /**
     * Always {@code null} on this platform.
     *
     * <p>Upstream recovers the calling class by installing a {@code SecurityManager} and
     * reading {@code getClassContext()}, because slf4j has no other way to see its caller's
     * stack. {@code java.lang.SecurityManager} does not exist on the WASM-GC TeaVM classlib
     * -- it is gone from the JDK as of 18 for exactly the reason it does not exist in a
     * browser tab, so there is nothing to install -- and the link reported
     * {@code Method java.lang.SecurityManager.getClassContext()}.
     *
     * <p>{@code null} is not a new behaviour invented here; it is what upstream already
     * produces whenever {@code new SecurityManager()} throws {@code SecurityException},
     * which {@link #safeCreateSecurityManager()} catches. That is the case on every JVM 18
     * and later unless the property {@code java.security.manager=allow} is set, so the null
     * branch is a path slf4j itself takes in normal operation.
     *
     * <p>The one caller, {@code LoggerFactory.getLogger(Class)}, already null-checks and
     * skips the check when it sees null, so nothing changes. That check is off by default
     * anyway: {@code DETECT_LOGGER_NAME_MISMATCH} is only true when the system property
     * {@code slf4j.detectLoggerNameMismatch} is set, which nothing in this port does. It
     * exists to warn slf4j's own users that they named a logger after a class that does not
     * match; Minecraft names every logger after its own class, correctly.
     *
     * <p>Retained as a method rather than deleted because it is {@code public} on a class
     * third-party code can reach.
     */
    public static Class<?> getCallingClass() {
        return null;
    }

    static final public void report(String msg, Throwable t) {
        System.err.println(msg);
        System.err.println("Reported exception:");
        t.printStackTrace();
    }

    static final public void report(String msg) {
        System.err.println("SLF4J: " + msg);
    }

}
