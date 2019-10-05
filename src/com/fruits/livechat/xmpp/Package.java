/**
 * $Id: Package.java,v 1.0 Apr 9, 2009 10:27:38 PM Jason Exp $
 */
package com.fruits.livechat.xmpp;

import org.gjt.xpp.XmlTag;

/**
 * @author Jason Tang
 */
public class Package {
	private String xmlString;

	private XmlTag xmlTag;
	private Session session;

	public Package(Session session) {
		this.session = session;
	}

	public XmlTag getXmlTag() {
		return xmlTag;
	}

	public void setXmlTag(XmlTag tag) {
		this.xmlTag = tag;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public String getXmlString() {
		return xmlString;
	}

	public void setXmlString(String xmlString) {
		this.xmlString = xmlString;
	}
}
