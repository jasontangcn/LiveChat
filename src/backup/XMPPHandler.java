/**
 * $Id: XMPPAcceptor.java,v 1.0 2009-3-28 am 03:20:36 Jason Exp $
 */
package backup;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import com.fruits.livechat.server.IOHandler;
import com.fruits.livechat.util.SelfIncreasingByteBuffer;

/**
 * @author Jason Tang
 */
public class XMPPHandler implements IOHandler {
	private Selector selector;
	private SocketChannel socketChannel;
	private InputHandler input = new InputHandler();
	private OutputHandler output = new OutputHandler();

	private SelfIncreasingByteBuffer data = new SelfIncreasingByteBuffer("UTF-8", 1024);
	ByteBuffer buffer = ByteBuffer.allocate(1024);

	public XMPPHandler(Selector selector, SocketChannel socketChannel) throws Exception {
		this.selector = selector;
		this.socketChannel = socketChannel;
		socketChannel.configureBlocking(false);
		socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
	}

	public void handle() throws Exception {
		SelectionKey selKey = socketChannel.keyFor(selector);
		// Is it reasonable to start a new thread for input/output handling.
		if (selKey.isReadable()) {
			selKey.interestOps(selKey.interestOps() & (~SelectionKey.OP_READ));
			new Thread(input).start();
		} else if (selKey.isWritable()) {
			selKey.interestOps(selKey.interestOps() & (~SelectionKey.OP_WRITE));
			new Thread(output).start();
		}
	}

	public Selector getSelector() {
		return selector;
	}

	public void setSelector(Selector selector) {
		this.selector = selector;
	}

	public SocketChannel getSocketChannel() {
		return socketChannel;
	}

	public void setSocketChannel(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

	public void close() throws Exception {
		// how to handle it.
	}

	private class InputHandler implements Runnable {
		public void run() {
			try {
				// Number of bytes read, possibly zero, or -1 if the channel has reached end of stream.
				System.out.println("I'am reading.");

				while (socketChannel.read(buffer) > 0) {
					buffer.flip();

					System.out.println("Read " + buffer.limit() + " bytes.");

					data.put(buffer);
					buffer.clear();
				}

				System.out.println(data);

				SelectionKey key = socketChannel.keyFor(selector);
				key.interestOps(key.interestOps() | SelectionKey.OP_READ);
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}
	}

	private class OutputHandler implements Runnable {
		public void run() {
			/*
			 * try { 
			 *   ByteBuffer buffer = ByteBuffer.allocate(1024);
			 *   socketChannel.write(buffer); 
			 *   SelectionKey key = socketChannel.keyFor(selector);
			 *   key.interestOps(key.interestOps()|SelectionKey.OP_WRITE); 
			 * } catch(IOException ioe) {
			 *  throw new RuntimeException(ioe); 
			 * }
			 */
		}
	}
}
