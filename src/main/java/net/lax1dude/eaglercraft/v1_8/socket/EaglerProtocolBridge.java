package net.lax1dude.eaglercraft.v1_8.socket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.network.ConnectionProtocol;

/**
 * Platform-neutral seam between the Eaglercraft connection code and the
 * client-side protocol translator.
 *
 * <p>The desktop and legacy TeaVM targets have no translator, so this class is
 * inert there. The WASM-GC target installs a Via-backed delegate at startup;
 * when that happens the V5 handshake advertises 1.20.2/1.18.2 game protocols
 * instead of 1.19.4 and every play packet is translated around the existing
 * compression/encryption layer in {@code Connection}.
 */
public final class EaglerProtocolBridge {

	public static final int CLIENT_GAME_PROTOCOL = 762;
	public static final int SERVER_GAME_PROTOCOL_1_18_2 = 758;
	public static final int SERVER_GAME_PROTOCOL_1_20_2 = 764;

	/**
	 * Implemented by the WASM-GC target. Kept free of Via types so the shared
	 * client source can still be compiled by the other targets.
	 */
	public interface Delegate {
		boolean initialize(String username, UUID uuid, int clientProtocolVersion, int serverProtocolVersion);

		@Nullable
		byte[] transformServerbound(byte[] packet, ConnectionProtocol state);

		@Nullable
		byte[] transformClientbound(byte[] packet, ConnectionProtocol state);

		void close();
	}

	@Nullable
	private static Delegate delegate;

	/**
	 * Packets synthesized by the translator (for example the 1.20.2
	 * configuration acknowledgements). They are already in the server's
	 * protocol and must be written by {@code Connection} after the packet
	 * currently being translated.
	 */
	private static final List<byte[]> generatedServerboundPackets = new ArrayList<>();

	/**
	 * Packets synthesized by the translator for the client. They are already in
	 * the client's protocol and must be parsed by {@code Connection} without a
	 * second translation pass.
	 */
	private static final List<byte[]> generatedClientboundPackets = new ArrayList<>();

	private EaglerProtocolBridge() {
	}

	public static void setDelegate(@Nullable Delegate value) {
		delegate = value;
	}

	public static boolean isAvailable() {
		return delegate != null;
	}

	public static boolean supportsServerProtocol(int serverProtocolVersion) {
		return serverProtocolVersion == SERVER_GAME_PROTOCOL_1_20_2
				|| serverProtocolVersion == SERVER_GAME_PROTOCOL_1_18_2;
	}

	public static boolean initialize(String username, UUID uuid, int clientProtocolVersion, int serverProtocolVersion) {
		Delegate value = delegate;
		return value != null && value.initialize(username, uuid, clientProtocolVersion, serverProtocolVersion);
	}

	@Nullable
	public static byte[] transformServerbound(byte[] packet, ConnectionProtocol state) {
		Delegate value = delegate;
		return value == null ? packet : value.transformServerbound(packet, state);
	}

	@Nullable
	public static byte[] transformClientbound(byte[] packet, ConnectionProtocol state) {
		Delegate value = delegate;
		return value == null ? packet : value.transformClientbound(packet, state);
	}

	public static void addGeneratedServerbound(byte[] packet) {
		if (packet != null && packet.length > 0) {
			generatedServerboundPackets.add(packet);
		}
	}

	public static List<byte[]> drainGeneratedServerbound() {
		if (generatedServerboundPackets.isEmpty()) {
			return Collections.emptyList();
		}
		List<byte[]> result = new ArrayList<>(generatedServerboundPackets);
		generatedServerboundPackets.clear();
		return result;
	}

	public static void addGeneratedClientbound(byte[] packet) {
		if (packet != null && packet.length > 0) {
			generatedClientboundPackets.add(packet);
		}
	}

	public static List<byte[]> drainGeneratedClientbound() {
		if (generatedClientboundPackets.isEmpty()) {
			return Collections.emptyList();
		}
		List<byte[]> result = new ArrayList<>(generatedClientboundPackets);
		generatedClientboundPackets.clear();
		return result;
	}

	public static void close() {
		generatedServerboundPackets.clear();
		generatedClientboundPackets.clear();
		Delegate value = delegate;
		if (value != null) {
			value.close();
		}
	}

}
