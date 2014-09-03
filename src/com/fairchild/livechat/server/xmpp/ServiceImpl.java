/**
 * $Id: XMPPService.java,v 1.0 Apr 5, 2009 4:48:53 PM Jason Exp $
 */
package com.fairchild.livechat.server.xmpp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fairchild.livechat.server.Acceptor;
import com.fairchild.livechat.server.Reactor;
import com.fairchild.livechat.server.ReactorImpl;
import com.fairchild.livechat.server.Service;
import com.fairchild.livechat.xmpp.PackageQueueProcessor;
import com.fairchild.livechat.xmpp.Session;
import com.fairchild.livechat.xmpp.protocal.PackageHandler;
import com.fairchild.livechat.xmpp.protocal.SaslHandlerImpl;
import com.fairchild.livechat.xmpp.protocal.StreamHandlerImpl;

/**
 * @author Jason Tang
 */
public class ServiceImpl implements Service {
	private Configuration config = null;
	private Reactor reactor = null;
	private Map<String, Session> sessions = new HashMap<String, Session>();

	public ServiceImpl(Configuration config) {
		this.config = config;
		init();
	}

	private void init() {
		PackageQueueProcessor pqp = PackageQueueProcessor.getProcessor();
		PackageHandler saslHander = new SaslHandlerImpl();
		PackageHandler streamHandler = new StreamHandlerImpl();
		pqp.addPackageHandler("urn:ietf:params:xml:ns:xmpp-sasl", saslHander);
		pqp.addPackageHandler("stream:stream", streamHandler);
		Thread pqpThread = new Thread(pqp);
		pqpThread.start();
	}

	public DataPort newDataPort() throws Exception {
		String sessionId = UUID.randomUUID().toString();
		Session session = null;
		session = new Session(sessionId);

		sessions.put(sessionId, session);
		return session.getDataPort();
	}

	public void start() throws Exception {
		Acceptor acceptor = new AcceptorImpl(this);
		// SocketAddress addr = new InetSocketAddress(config.getHostname(),config.getPort());
		InetSocketAddress addr = new InetSocketAddress(InetAddress.getLocalHost(), 5222);

		System.out.println(addr.getHostName());
		System.out.println(addr.getAddress());

		reactor = new ReactorImpl(addr, acceptor);
		reactor.start();
	}

	public void stop() throws Exception {
		reactor.stop();

		for (Session session : sessions.values()) {
			session.close();
		}
	}
}
