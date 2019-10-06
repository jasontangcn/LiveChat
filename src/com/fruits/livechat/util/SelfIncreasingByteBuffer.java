/**
 * $Id: SelfIncreasingByteBuffer.java,v 1.0 Mar 28, 2009 7:36:53 PM Jason Exp $
 */
package com.fruits.livechat.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * @author Jason Tang
 */
public class SelfIncreasingByteBuffer {
	private Charset charset;
	private ByteBuffer buff;

	public SelfIncreasingByteBuffer(String charsetName, int capacity) {
		charset = Charset.forName(charsetName);
		buff = ByteBuffer.allocate(capacity);
	}

	public SelfIncreasingByteBuffer put(ByteBuffer src) {
		if (src.limit() > buff.remaining()) {
			ByteBuffer buff2 = ByteBuffer.allocate(buff.position() + src.limit() + 1024);
			buff.flip();
			buff2.put(buff).put(src);
			buff = buff2;
		} else {
			buff.put(src);
		}
		return this;
	}

	public SelfIncreasingByteBuffer flip() {
		buff.flip();
		return this;
	}

	public SelfIncreasingByteBuffer clear() {
		buff.clear();
		return this;
	}

	public ByteBuffer viewBuffer() {
		return buff.asReadOnlyBuffer();
	}

	public String toString() {
		ByteBuffer bb = buff.asReadOnlyBuffer();
		bb.flip();
		CharBuffer cb = charset.decode(bb);
		return cb.toString();
	}
}
