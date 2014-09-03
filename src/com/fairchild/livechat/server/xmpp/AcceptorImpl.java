/**
 * $Id: XMPPAcceptor.java,v 1.0 Mar 28, 2009 11:44:08 PM Jason Exp $
 */
package com.fairchild.livechat.server.xmpp;

import java.io.IOException;
import java.util.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.fairchild.livechat.server.*;

/**
 * @author Jason Tang
 */
public class AcceptorImpl implements Acceptor {
	private ServerSocketChannel serverChannel;
	private Selector selector;
	private ServiceImpl xmppService;

	public AcceptorImpl(ServiceImpl xmppService) {
		this.xmppService = xmppService;
	}

	public void handle() throws Exception {
		// handle exception
		SocketChannel socketChannel = null;
		try {
			socketChannel = serverChannel.accept();
			System.out.println("Accepted connection from: " + socketChannel.socket().getRemoteSocketAddress());

			new IOHandlerImpl(selector, socketChannel, xmppService.newDataPort());
		} catch (Exception e) {
			try {
				if ((null != socketChannel) && socketChannel.isOpen()) {
					socketChannel.close();
				}
			} catch (IOException ioe) {
				System.out.println(ioe);
			}
			throw new RuntimeException(e);
		}
	}

	public ServerSocketChannel getServerChannel() {
		return serverChannel;
	}

	public void setServerChannel(ServerSocketChannel serverChannel) {
		this.serverChannel = serverChannel;
	}

	public Selector getSelector() {
		return selector;
	}

	public void setSelector(Selector selector) {
		this.selector = selector;
	}

	public void close() throws Exception {
		// ignore
	}
}
