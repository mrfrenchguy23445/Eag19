package org.teavm.classlib.java.net;

/*
 * Reached from java.net.Proxy's own constructor. The classlib models java.net for
 * URL and URI handling only, and Minecraft reaches Proxy.NO_PROXY from
 * Minecraft.<init> to initialise the field it hands back from getProxy(). A
 * browser has no proxy and no socket layer, so the only value that ever exists is
 * the DIRECT one.
 */
public class TSocketAddress {
}