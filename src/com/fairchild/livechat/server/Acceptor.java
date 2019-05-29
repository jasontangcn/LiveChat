/**
 * $Id: Acceptor.java,v 1.0 Mar 29, 2009 9:25:26 AM Jason Exp $
 */
package com.fairchild.livechat.server;

import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

/**
 * @author TomHornson@hotmail.com
 */
public interface Acceptor extends SocketHandler {
	public ServerSocketChannel getServerChannel();

	public void setServerChannel(ServerSocketChannel serverChannel);
}
