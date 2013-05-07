package org.sandrop.webscarab.plugin.proxy;

import java.net.Socket;

import org.sandrop.webscarab.model.ClientDescriptor;

public interface IClientResolver {
    ClientDescriptor getClientDescriptorBySocket(Socket socket);
}
