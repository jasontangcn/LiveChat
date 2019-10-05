/**
 * $Id: SASLHandler.java,v 1.0 Apr 6, 2009 7:13:34 AM Jason Exp $
 */
package backup;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslServer;

import com.fruits.livechat.server.IOHandler;
import com.fruits.livechat.server.xmpp.CallbackHandlerImpl;
import com.fruits.livechat.util.BufferUtil;
import com.fruits.livechat.util.SelfIncreasingByteBuffer;
import com.fruits.livechat.xmpp.PullProtocalParser;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/**
 * @author Jason Tang
 */
public class SASLHandler implements IOHandler {
	public final static String saslMechanism = "DIGEST-MD5";
	public final static String saslProtocal = "XMPP";
	public final static String saslServerName = "jasontang";
	public final static String saslQOP = "auth";

	private static String step1rsp = "<?xml version=\"1.0\"?>"
	+ "<stream:stream xmlns=\"jabber:client\" xmlns:stream=\"http://etherx.jabber.org/streams\" id=\"c2s_234\" from=\"jasontang\" version=\"1.0\">" 
			+ "<stream:features>" 
	+ "<mechanisms xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\"><mechanism>DIGEST-MD5</mechanism><mechanism>PLAIN</mechanism></mechanisms>" 
			+ "</stream:features>";

	// Base64 is cmVhbG09Imphc29udGFuZyIsbm9uY2U9Ik9BNk1HOXRFUUdtMmhoIixxb3A9ImF1dGgiLGNoYXJzZXQ9dXRmLTgsYWxnb3JpdGhtPW1kNS1zZXNz

	private Selector selector = null;
	private SocketChannel socketChannel = null;

	private Reader reader = new Reader();
	private Writer writer = new Writer();

	// it's determined by the code of client's xml doc.
	private String charset = "UTF-8";
	private SelfIncreasingByteBuffer read = new SelfIncreasingByteBuffer("UTF-8", 1024);

	private ByteBuffer readBuff = ByteBuffer.allocate(1024);
	private ByteBuffer writeBuff = ByteBuffer.allocate(10240);

	private SaslServer saslServer;
	private int step = 1;

	public SASLHandler(Selector selector, SocketChannel socketChannel) throws Exception {
		this.selector = selector;
		this.socketChannel = socketChannel;
		socketChannel.configureBlocking(false);
		socketChannel.register(selector, SelectionKey.OP_READ, this);

		Map<String, String> props = new HashMap<String, String>();
		props.put(Sasl.QOP, saslQOP);

		CallbackHandler cbh = new CallbackHandlerImpl();
		this.saslServer = Sasl.createSaslServer(saslMechanism, saslProtocal, saslServerName, props, cbh);
	}

	public void handle() throws Exception {
		SelectionKey selKey = socketChannel.keyFor(selector);
		if (selKey.isReadable()) {
			selKey.interestOps(selKey.interestOps() & (~SelectionKey.OP_READ));
			new Thread(reader).start();
		} else if (selKey.isWritable()) {
			selKey.interestOps(selKey.interestOps() & (~SelectionKey.OP_WRITE));
			new Thread(writer).start();
		}
	}

	public void close() throws Exception {
		socketChannel.close();
	}

	public Selector getSelector() {
		return this.selector;
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

	private class Reader implements Runnable {
		public void run() {
			try {
				// The number of bytes read, possibly zero, or -1 if the channel has reached end-of-stream.
				System.out.println("Reading data.");

				// Actually I believe its better to use a big enough buffer.
				// Buffer duplicating is degrading performance.
				int i;
				while ((i = socketChannel.read(readBuff)) > 0) {
					System.out.println("Read " + i + " bytes.");
					readBuff.flip();
					read.put(readBuff);
					readBuff.clear();
				}

				System.out.println(read);

				writeBuff.clear();

				if (1 == step) {
					writeBuff.put(step1rsp.getBytes(charset));
					step += 2;
					// client performes 2th step.
				} else if (3 == step) {
					// JDK implement indicates DIGEST-MD5 must not have an initial response.
					// But in rfc 3920,its recommend that client send a initial response.
					// String crsp = PullProtocalParser.pullProtocalParser.getContent(read.toString(),"auth");
					StringBuilder rsp = new StringBuilder().append("<challenge xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">").append(Base64.encode(saslServer.evaluateResponse(new byte[0]))).append("</challenge>");

					writeBuff.put(rsp.toString().getBytes());
					step += 2;
				} else if (5 == step) {
					String crsp = PullProtocalParser.pullProtocalParser.getContent(read.toString(), "response");
					System.out.println(crsp);
					StringBuilder rsp = new StringBuilder().append("<challenge xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">").append(Base64.encode(saslServer.evaluateResponse(Base64.decode(crsp)))).append("</challenge>");

					writeBuff.put(rsp.toString().getBytes());

					step += 2;
				} else if (7 == step) {
					String rsp = "<success xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\"/>";
					writeBuff.put(rsp.getBytes());
				}

				writeBuff.flip();
				read.clear();

				SelectionKey key = socketChannel.keyFor(selector);
				key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
				selector.wakeup();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private class Writer implements Runnable {
		public void run() {
			try {
				System.out.println("Responding the request.");
				BufferUtil.debug(writeBuff);

				socketChannel.write(writeBuff);

				SelectionKey key = socketChannel.keyFor(selector);
				key.interestOps(key.interestOps() | SelectionKey.OP_READ);
				selector.wakeup();
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}
	}
}
