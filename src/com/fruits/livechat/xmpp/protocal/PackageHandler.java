/**
 * $Id: PackageHandler.java,v 1.0 Apr 11, 2009 9:54:32 AM Jason Exp $
 */
package com.fruits.livechat.xmpp.protocal;

import com.fruits.livechat.xmpp.Package;

/**
 * @author Jason Tang
 */
public interface PackageHandler {
	public void handle(Package pkg) throws Exception;

	public PackageHandler nextHandler();
}
