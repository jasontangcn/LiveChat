/**
 * $Id: IOHandler.java,v 1.0 Mar 29, 2009 9:29:15 AM Jason Exp $
 */
package com.fruits.livechat.server;

import java.nio.channels.SocketChannel;

/**
 * @author Jason Tang
 */
public interface IOHandler extends SocketHandler {
	public SocketChannel getSocketChannel();

	public void setSocketChannel(SocketChannel socketChannel);
}
