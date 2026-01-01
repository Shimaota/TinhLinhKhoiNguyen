package com.tlnt.net;

/**
 * KryoNet TCP network layer, backward compatible with the old version.
 * To switch to WebSocket (Web/TeaVM), use {@link com.tlnt.net.task.ServerPacketSender#setNetworkClientFactory}.
 */
public class NetworkClient extends KryoNetworkClient {
}
