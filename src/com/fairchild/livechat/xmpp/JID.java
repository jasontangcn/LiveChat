/**
 * $Id: JID.java,v 1.0 Apr 9, 2009 10:22:10 PM Jason Exp $
 */
package com.fairchild.livechat.xmpp;

/**
 * @author Jason Tang
 */
public class JID {
	private String username;
	private String domain;
	private String resource;

	public JID(String jid) {
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}
}
