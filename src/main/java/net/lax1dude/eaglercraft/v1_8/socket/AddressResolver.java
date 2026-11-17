package net.lax1dude.eaglercraft.v1_8.socket;

import net.lax1dude.eaglercraft.v1_8.EagRuntime;
import net.minecraft.client.multiplayer.ServerData;

/**
 * Turns the address a player typed into the WebSocket URI used for the
 * Eaglercraft query socket. Joining a server goes through
 * {@code Connection.normalizeWebSocketURI} instead; this exists so the ping
 * path can respect the page's transport (ws on http, wss on https).
 */
public class AddressResolver {

    public static String resolveURI(ServerData input) {
        return resolveURI(input.ip);
    }

    public static String resolveURI(String input) {
        String lc = input.toLowerCase();
        if (!lc.startsWith("ws://") && !lc.startsWith("wss://")) {
            input = EagRuntime.requireSSL() ? "wss://" + input : "ws://" + input;
        }
        return input;
    }

}
