/**
 * $Id: ReactorImpl.java,v 1.0 2009-3-28 am 02:50:02 Jason Exp $
 */
package com.fruits.livechat.server;

import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;

/**
 * @author Jason Tang
 */
public class ReactorImpl implements Reactor, Runnable {
	private Thread reactorThread = null;
	private SocketAddress addr;
	private ServerSocketChannel serverChannel = null;
	private Selector selector = null;
	private Acceptor acceptor;

	public ReactorImpl(SocketAddress addr, Acceptor acceptor) {
		this.addr = addr;
		this.acceptor = acceptor;
	}

	public void start() throws Exception {
		reactorThread = new Thread(this);
		reactorThread.start();
	}

	public void run() {
		try {
			selector = Selector.open();
			serverChannel = ServerSocketChannel.open();
			serverChannel.socket().bind(addr);
			serverChannel.configureBlocking(false);
			acceptor.setSelector(selector);
			acceptor.setServerChannel(serverChannel);
			SelectionKey key = serverChannel.register(selector, SelectionKey.OP_ACCEPT, acceptor);
			System.out.println(key);

			while ((!Thread.interrupted()) && (selector.select() >= 0)) {
				Set<SelectionKey> selKeys = selector.selectedKeys();

				System.out.println("Selected keys: " + selKeys.size());

				for (SelectionKey selKey : selKeys) {
					try {
						((SocketHandler) selKey.attachment()).handle();
					} catch (Exception e) {
						System.out.println(e);
					}
				}
				selKeys.clear();
				Thread.sleep(3000);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void stop() throws Exception {
		try {
			reactorThread.interrupt();
		} catch (Exception e) {
			// ignore
		}

		try {
			if ((null != selector) && (selector.isOpen())) {
				selector.close();
			}
		} catch (Exception e) {
			System.out.println(e);
		}

		try {
			if ((null != serverChannel) && (serverChannel.isOpen())) {
				serverChannel.close();
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}
