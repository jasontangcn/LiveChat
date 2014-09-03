/**
 * $Id: ProtocalParserImpl.java,v 1.0 Apr 9, 2009 10:50:23 PM Jason Exp $
 */
package com.fairchild.livechat.xmpp;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.gjt.xpp.XmlEndTag;
import org.gjt.xpp.XmlNode;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserFactory;
import org.gjt.xpp.XmlStartTag;
import org.gjt.xpp.XmlTag;

import com.fairchild.livechat.util.BufferUtil;

/**
 * @author Jason Tang
 */
public class ProtocalParserImpl implements ProtocalParser {
	private String charset = "iso-8859-1";

	private XmlPullParserFactory xppFactory;
	private XmlPullParser xpp;

	public ProtocalParserImpl() throws Exception {
		xppFactory = XmlPullParserFactory.newInstance();
		xpp = xppFactory.newPullParser();
		xpp.setAllowedMixedContent(false);
	}

	public List<XmlTag> parser(byte[] data) {
		List<XmlTag> tags = new ArrayList<XmlTag>();
		try {
			synchronized (data) {
				if ((null != data) && (0 < data.length)) {
					System.out.println("Start to parse...");
					xpp.setInput(new InputStreamReader(new ByteArrayInputStream(data), Charset.forName(charset)));

					byte elType;
					while ((elType = xpp.next()) != XmlPullParser.END_DOCUMENT) {
						if (XmlPullParser.START_TAG == elType) {
							XmlStartTag startTag = xppFactory.newStartTag();
							xpp.readStartTag(startTag);
							if ("stream:stream".equalsIgnoreCase(startTag.getRawName())) {
								tags.add(startTag);
							} else {
								XmlNode node = xppFactory.newNode();
								xpp.readNode(node);
								tags.add(node);
							}
						} else if (XmlPullParser.END_TAG == elType) {
							XmlEndTag endTag = xppFactory.newEndTag();
							xpp.readEndTag(endTag);
							if ("stream:stream".equalsIgnoreCase(endTag.getRawName())) {
								tags.add(endTag);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e);
		}

		System.out.println("Parsed nodes: " + tags.size());
		return tags;
	}

	public void setCharset(String charset) {
	}

	public String getCharset() {
		return this.charset;
	}

}
