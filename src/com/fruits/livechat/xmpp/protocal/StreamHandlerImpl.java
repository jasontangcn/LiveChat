/**
 * $Id: StreamHandlerImpl.java,v 1.0 Apr 12, 2009 12:02:19 PM Jason Exp $
 */
package com.fruits.livechat.xmpp.protocal;

import org.gjt.xpp.XmlEndTag;
import org.gjt.xpp.XmlStartTag;
import org.gjt.xpp.XmlTag;

import com.fruits.livechat.Constants;
import com.fruits.livechat.xmpp.Package;
import com.fruits.livechat.xmpp.PackageDeliver;
import com.fruits.livechat.xmpp.Session;

/**
 * @author Jason Tang
 */
public class StreamHandlerImpl implements PackageHandler {
	private PackageHandler nextHandler = null;

	public StreamHandlerImpl() {
	}

	public StreamHandlerImpl(PackageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}

	public void handle(Package pkg) throws Exception {
		System.out.println("Processing a stream package.");

		Session session = pkg.getSession();
		XmlTag streamTag = pkg.getXmlTag();

		if (streamTag instanceof XmlStartTag) {
			StringBuilder responseXml = new StringBuilder();
			if (!SaslHandlerImpl.SASL_AUTHED.equalsIgnoreCase((String) session.getAttribute(SaslHandlerImpl.SASL_STATE))) {
				responseXml.append("<stream:stream xmlns=\"jabber:client\" xmlns:stream=\"http://etherx.jabber.org/streams\"").append(" id=\"").append(pkg.getSession().getSessionId()).append("\"").append(" from=\"").append(Constants.SERVER_NAME).append("\"").append(" version=\"1.0\">").append("<stream:features>").append("<mechanisms xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\"><mechanism>DIGEST-MD5</mechanism><mechanism>PLAIN</mechanism></mechanisms>").append("</stream:features>");

			} else {
				responseXml.append("<stream:stream xmlns=\"jabber:client\" xmlns:stream=\"http://etherx.jabber.org/streams\"").append(" id=\"").append(pkg.getSession().getSessionId()).append("\"").append(" from=\"").append(Constants.SERVER_NAME).append("\"").append(" version=\"1.0\">").append("<stream:features>").append("<bind xmlns=\"urn:ietf:params:xml:ns:xmpp-bind\"/><session xmlns=\"urn:ietf:params:xml:ns:xmpp-session\"/>").append("</stream:features>");
			}
			PackageDeliver.responseClient(pkg.getSession(), responseXml.toString());
		} else if (streamTag instanceof XmlEndTag) {
		}

		if (null != nextHandler) {
			nextHandler.handle(pkg);
		}
	}

	public PackageHandler nextHandler() {
		return null;
	}
}
