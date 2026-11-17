package net.lax1dude.eaglercraft.v1_8.internal;

/**
 * How a server answered an Eaglercraft query socket: normally, a short block,
 * or a full lockout. The server list uses this to stop hammering a server that
 * has already told us to go away.
 */
public enum EnumServerRateLimit {
    OK,
    BLOCKED,
    LOCKED_OUT
}
