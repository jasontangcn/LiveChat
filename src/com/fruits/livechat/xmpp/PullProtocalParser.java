/**
 * $Id: PullProtocalParser.java,v 1.0 Mar 29, 2009 2:46:17 PM Jason Exp $
 */
package com.fruits.livechat.xmpp;

import java.io.IOException;
import java.io.StringReader;

import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserException;
import org.gjt.xpp.XmlPullParserFactory;
import org.gjt.xpp.XmlStartTag;

/**
 * @author Jason Tang
 */
public class PullProtocalParser {
	public static PullProtocalParser pullProtocalParser = null;

	static {
		try {
			pullProtocalParser = new PullProtocalParser();
		} catch (XmlPullParserException xppe) {
			throw new RuntimeException(xppe);
		}
	}

	private XmlPullParserFactory xppFactory;
	private XmlPullParser parser;

	private PullProtocalParser() throws XmlPullParserException {
		xppFactory = XmlPullParserFactory.newInstance();
		parser = xppFactory.newPullParser();
		parser.setAllowedMixedContent(false);
	}

	// bottleneck
	public synchronized String getContent(String xml, String elementName) throws XmlPullParserException, IOException {
		parser.setInput(new StringReader(xml));

		byte type;
		XmlStartTag startTag = xppFactory.newStartTag();

		boolean meetStart = false;
		while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (type == XmlPullParser.START_TAG) {
				parser.readStartTag(startTag);
				if (startTag.getRawName().equalsIgnoreCase(elementName)) {
					meetStart = true;
				}
			} else {
				if (type == XmlPullParser.CONTENT) {
					if (meetStart) {
						return parser.readContent();
					}
				}
				meetStart = false;
			}
		}

		return null;
	}
}
