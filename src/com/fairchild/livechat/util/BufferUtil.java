/**
 * $Id: BufferUtil.java,v 1.0 Apr 6, 2009 10:10:31 AM Jason Exp $
 */
package com.fairchild.livechat.util;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/**
 * @author Jason Tang
 */
public class BufferUtil {
	public static String buffer2String(ByteBuffer buff, String charsetName) {
		return Charset.forName(charsetName).decode(buff).toString();
	}

	public static void debug(ByteBuffer bb) {
		System.out.println(buffer2String(bb.asReadOnlyBuffer(), "UTF-8"));
	}

	public static void debug(String s) {
		System.out.println(s);
	}
}
