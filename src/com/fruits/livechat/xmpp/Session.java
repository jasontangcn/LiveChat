/**
 * $Id: Session.java,v 1.0 Apr 9, 2009 10:29:05 PM Jason Exp $
 */
package com.fruits.livechat.xmpp;

import java.nio.ByteBuffer;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gjt.xpp.XmlTag;

import com.fruits.livechat.server.xmpp.DataPort;
import com.fruits.livechat.server.xmpp.DataPortImpl;
import com.fruits.livechat.server.xmpp.DataPortListener;
import com.fruits.livechat.util.BufferUtil;

/**
 * @author Jason Tang
 */
public class Session implements DataPortListener {
	private String sessionId;
	private Map<String, Object> attributes = new HashMap<String, Object>();
	private boolean isClosed = false;

	DataPort dataPort;

	private ProtocalParser parser;

	public Session(String sessionId) throws Exception {
		this.sessionId = sessionId;
		dataPort = new DataPortImpl();
		dataPort.addLister(this);
		this.parser = new ProtocalParserImpl();
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Object getAttribute(String name) {
		return this.attributes.get(name);
	}

	public Object setAttribute(String name, Object o) {
		return this.attributes.put(name, o);
	}

	public Object removeAttribute(String name) {
		return this.attributes.remove(name);
	}

	public void close() {
		// do something here.

		dataPort.close();
		this.isClosed = true;
	}

	public boolean isClosed() {
		return this.isClosed;
	}

	public DataPort getDataPort() {
		return this.dataPort;
	}

	public void emitEvent(EventObject event) {
		int eventType = ((Integer) event.getSource()).intValue();
		if (eventType == DataPortListener.DATA_PORT_FINISH_READING) {
			System.out.println("Data port read event...\nData port read:");

			ByteBuffer bb = dataPort.getReadBuffer();

			// debugging
			BufferUtil.debug(bb);

			dataPort.readyReadReadBufufer();
			byte[] barr = null;
			if (bb.hasArray() && bb.hasRemaining()) {
				barr = bb.array();
				bb.clear();
				dataPort.setReadBufferExpectWrite(true);
			}
			if (null != barr) {
				List<XmlTag> tags = parser.parser(barr);
				for (XmlTag tag : tags) {
					Package pkg = new Package(this);
					pkg.setXmlTag(tag);
					try {
						PackageQueue.getPackageQ().put(pkg);
					} catch (Exception e) {
						System.out.println(e);
					}
				}
			}
		} else if (eventType == DataPortListener.DATA_PORT_FINISH_WRITING) {

		} else if (eventType == DataPortListener.DATA_PORT_OPENED) {

		} else if (eventType == DataPortListener.DATA_PORT_CLOSED) {

		}
	}
}
