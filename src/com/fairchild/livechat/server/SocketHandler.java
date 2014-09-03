/**
 * $Id: SocketHandler.java,v 1.0 Mar 28, 2009 3:40:00 PM Jason Exp $
 */
package com.fairchild.livechat.server;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

/**
 * @author Jason Tang
 */
public interface SocketHandler {
	public Selector getSelector();

	public void setSelector(Selector selector);

	public void handle() throws Exception;

	public void close() throws Exception;
}
