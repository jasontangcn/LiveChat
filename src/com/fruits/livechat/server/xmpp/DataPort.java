/**
 * $Id: DataPort.java,v 1.0 Apr 9, 2009 10:31:44 PM Jason Exp $
 */
package com.fruits.livechat.server.xmpp;

import java.nio.ByteBuffer;

/**
 * @author Jason Tang
 */
public interface DataPort {
	public ByteBuffer getReadBuffer();

	public void finishReading();

	public boolean isReadBufferWritable();

	public boolean isReadBufferExpectWrite();

	public void setReadBufferExpectWrite(boolean readBufferExpectWrite);

	public void readyWriteReadBuffer();

	public void readyReadReadBufufer();

	public ByteBuffer getWriteBuffer();

	public void finishWriting();

	public boolean isWriteBufferReadable();

	public boolean isWriteBufferExpectRead();

	public void setWriteBufferExpectRead(boolean writeBufferExpectRead);

	public void readyReadWriteBuffer();

	public void readyWriteWriteBuffer();

	public void close();

	public boolean isClosed();

	public void addLister(DataPortListener listener);
}
