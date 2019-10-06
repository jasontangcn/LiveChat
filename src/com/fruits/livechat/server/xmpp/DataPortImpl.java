/**
 * $Id: DataPortImpl.java,v 1.0 Apr 11, 2009 3:34:50 PM Jason Exp $
 */
package com.fruits.livechat.server.xmpp;

import java.nio.ByteBuffer;
import java.util.*;

import com.fruits.livechat.Constants;

/**
 * @author Jason Tang
 */
public class DataPortImpl implements DataPort {
	// replace them with self-increasing-buffer
	private ByteBuffer readBuffer = null;
	private ByteBuffer writeBuffer = null;
	private List<DataPortListener> listeners = new ArrayList<DataPortListener>();
	private boolean closed = false;
	private boolean readBufferExpectWrite;
	private boolean writeBufferExpectRead;

	public DataPortImpl() {
		readBuffer = ByteBuffer.allocate(Constants.SESSION_INCOMING_BUFFER_INITIAL_CAPACITY);
		readBuffer.clear();
		this.setReadBufferExpectWrite(true);

		writeBuffer = ByteBuffer.allocate(Constants.SESSION_OUTCOMING_BUFFER_INITIAL_CAPACITY);
		writeBuffer.clear();
		this.setWriteBufferExpectRead(false);
	}

	public void addLister(DataPortListener listener) {
		listeners.add(listener);
	}

	public ByteBuffer getReadBuffer() {
		return readBuffer;
	}

	public void finishReading() {
		readBuffer.flip();
		setReadBufferExpectWrite(false);

		EventObject event = new EventObject(Integer.valueOf(DataPortListener.DATA_PORT_FINISH_READING));
		for (DataPortListener listener : listeners) {
			listener.emitEvent(event);
		}
	}

	public boolean isReadBufferWritable() {
		if (!isReadBufferExpectWrite()) {
			if (readBuffer.limit() == readBuffer.capacity()) {
				return false;
			} else {
				return true;
			}
		}

		return writeBuffer.hasRemaining();
	}

	public boolean isReadBufferExpectWrite() {
		return this.readBufferExpectWrite;
	}

	public void setReadBufferExpectWrite(boolean readBufferExpectWrite) {
		this.readBufferExpectWrite = readBufferExpectWrite;
	}

	public void readyWriteReadBuffer() {
		if (!isReadBufferExpectWrite()) {
			readBuffer.compact();
			setReadBufferExpectWrite(true);
		}
	}

	public void readyReadReadBufufer() {
		if (isReadBufferExpectWrite()) {
			readBuffer.flip();
			setReadBufferExpectWrite(false);
		}
	}

	public ByteBuffer getWriteBuffer() {
		return writeBuffer;
	}

	public void finishWriting() {
		writeBuffer.compact();
		setWriteBufferExpectRead(false);

		EventObject event = new EventObject(Integer.valueOf(DataPortListener.DATA_PORT_FINISH_WRITING));
		for (DataPortListener listener : listeners) {
			listener.emitEvent(event);
		}
	}

	public boolean isWriteBufferReadable() {
		if (!isWriteBufferExpectRead()) {
			if (0 == writeBuffer.position()) {
				return false;
			} else {
				return true;
			}
		}

		return writeBuffer.hasRemaining();
	}

	public boolean isWriteBufferExpectRead() {
		return this.writeBufferExpectRead;
	}

	public void setWriteBufferExpectRead(boolean writeBufferExpectRead) {
		this.writeBufferExpectRead = writeBufferExpectRead;
	}

	public void readyReadWriteBuffer() {
		if (!isWriteBufferExpectRead()) {
			writeBuffer.flip();
			setWriteBufferExpectRead(true);
		}
	}

	public void readyWriteWriteBuffer() {
		if (isWriteBufferExpectRead()) {
			writeBuffer.compact();
			setWriteBufferExpectRead(false);
		}
	}

	public void close() {
		this.closed = true;
	}

	public boolean isClosed() {
		return this.closed;
	}
}
