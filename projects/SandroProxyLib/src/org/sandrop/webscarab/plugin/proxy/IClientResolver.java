package org.sandrop.webscarab.plugin.proxy;

import java.net.Socket;

import org.sandrop.webscarab.model.ConnectionDescriptor;

public interface IClientResolver {
    ConnectionDescriptor getClientDescriptorBySocket(Socket socket);
}
