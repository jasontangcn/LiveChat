package com.fairchild.livechat.server;

import com.fairchild.livechat.server.xmpp.*;

public class Server {
	public static void main(String[] args) throws Exception {
		Configuration config = new Configuration("localhost", 5222);
		final Service xmpp = new ServiceImpl(config);
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				try {
					xmpp.stop();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}));
		xmpp.start();
	}
}
