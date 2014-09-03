/**
 * $Id: ProtocalParser.java,v 1.0 Apr 9, 2009 10:49:18 PM Jason Exp $
 */
package com.fairchild.livechat.xmpp;

import java.util.List;

import org.gjt.xpp.XmlTag;

/**
 * @author Jason Tang
 */
public interface ProtocalParser {
	public List<XmlTag> parser(byte[] data);

	public void setCharset(String charset);

	public String getCharset();
}
