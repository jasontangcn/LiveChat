/**
 * $Id: DataPortListener.java,v 1.0 Apr 11, 2009 3:44:37 PM Jason Exp $
 */
package com.fruits.livechat.server.xmpp;

import java.util.EventListener;
import java.util.EventObject;

/**
 * @author Jason Tang
 */
public interface DataPortListener extends EventListener {
	public static final int DATA_PORT_OPENED = 1;
	public static final int DATA_PORT_FINISH_READING = 1 << 1;
	public static final int DATA_PORT_FINISH_WRITING = 1 << 2;
	public static final int DATA_PORT_CLOSED = 1 << 3;

	public void emitEvent(EventObject event);
}
