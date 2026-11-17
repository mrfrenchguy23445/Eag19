package net.lax1dude.eaglercraft.v1_8.socket;

import net.lax1dude.eaglercraft.v1_8.internal.IServerQuery;
import net.lax1dude.eaglercraft.v1_8.internal.IWebSocketClient;
import net.lax1dude.eaglercraft.v1_8.internal.PlatformNetworking;
import net.lax1dude.eaglercraft.v1_8.log4j.LogManager;
import net.lax1dude.eaglercraft.v1_8.log4j.Logger;

/**
 * Opens the short-lived WebSocket used by the server list. Eaglercraft servers
 * answer a text "Accept: &lt;type&gt;" line on this socket with the server's
 * MOTD data, which is not available over the vanilla status protocol.
 */
public class ServerQueryDispatch {

    private static final Logger logger = LogManager.getLogger("QueryDispatch");

    public static IServerQuery sendServerQuery(String uri, String accept) {
        logger.info("Sending {} query to: \"{}\"", accept, uri);
        IWebSocketClient sockClient = PlatformNetworking.openWebSocket(uri);
        return sockClient != null ? new ServerQueryImpl(sockClient, accept) : null;
    }

}
