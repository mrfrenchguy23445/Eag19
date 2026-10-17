package org.teavm.classlib.java.net;

/*
 * Reached from net.minecraft.client.Minecraft.<init> (line 401), which assigns
 * Proxy.NO_PROXY to the field returned by getProxy(). Neither Proxy nor its
 * SocketAddress parameter type is modelled by the classlib.
 *
 * Direct is the only meaningful value under WASM-GC: the client opens no sockets,
 * because every transport netty and Minecraft use goes through the JSO layer and
 * out to the browser's own fetch stack. The HTTP and SOCKS constructors are kept
 * so that javac still type-checks the upstream code, but nothing constructs them
 * -- Minecraft's only call site is the NO_PROXY assignment.
 */
public class TProxy {

	public enum Type {
		DIRECT,
		HTTP,
		SOCKS
	}

	public static final TProxy NO_PROXY = new TProxy(Type.DIRECT, null, -1);

	private final Type type;
	private final TSocketAddress socketAddress;
	private final int port;

	public TProxy(Type param0) {
		this(param0, null, -1);
	}

	public TProxy(Type param0, TSocketAddress param1, int param2) {
		this.type = param0;
		this.socketAddress = param1;
		this.port = param2;
	}

	public Type type() {
		return this.type;
	}

	public TSocketAddress address() {
		return this.socketAddress;
	}

	public int port() {
		return this.port;
	}
}