/**
 * $Id: XMPPIOHandler.java,v 1.0 Apr 11, 2009 11:28:57 AM Jason Exp $
 */
package com.fairchild.livechat.server.xmpp;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import com.fairchild.livechat.server.IOHandler;

/**
 * @author Jason Tang
 */
public class IOHandlerImpl implements IOHandler {
	private Selector selector = null;
	private SocketChannel socketChannel = null;
	private DataPort dataPort = null;

	public IOHandlerImpl(Selector selector, SocketChannel socketChannel, DataPort dataPort) throws Exception {
		this.selector = selector;
		this.socketChannel = socketChannel;
		this.dataPort = dataPort;
		socketChannel.configureBlocking(false);
		socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
	}

	public void handle() throws Exception {
		// socket exception to close the data port.
		SelectionKey selKey = socketChannel.keyFor(selector);
		if (!dataPort.isClosed() && selKey.isReadable()) {
			System.out.println("Reading...");
			// To avoid buffer resize I prefer a big enough buffer to a self-increasing-buffer.
			ByteBuffer readBuffer = dataPort.getReadBuffer();

			// synchronization problem
			synchronized (readBuffer) {
				dataPort.readyWriteReadBuffer();

				System.out.println("ReadBuffer:" + readBuffer);

				int i = socketChannel.read(readBuffer);
				System.out.println("Read " + i + " bytes.");
				dataPort.finishReading();
			}
		} else if (selKey.isWritable()) {
			System.out.println("Writing...");
			ByteBuffer writeBuffer = dataPort.getWriteBuffer();
			// synchronization problem
			synchronized (writeBuffer) {
				dataPort.readyReadWriteBuffer();

				if (dataPort.isWriteBufferReadable()) {
					System.out.println("WriteBuffer:" + writeBuffer);
					int i = socketChannel.write(writeBuffer);
					System.out.println("Wrote " + i + " bytes");
					dataPort.finishWriting();
				} else {
					if (dataPort.isClosed()) {
						socketChannel.close();
					}
				}
			}
		}
	}

	public SocketChannel getSocketChannel() {
		return this.socketChannel;
	}

	public void setSocketChannel(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

	public void close() throws Exception {
	}

	public Selector getSelector() {
		return this.selector;
	}

	public void setSelector(Selector selector) {

	}
}
