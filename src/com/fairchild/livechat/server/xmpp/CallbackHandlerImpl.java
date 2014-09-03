/**
 * $Id: CallbackHandlerImpl.java,v 1.0 Apr 6, 2009 6:31:39 PM Jason Exp $
 */
package com.fairchild.livechat.server.xmpp;

import javax.security.auth.callback.*;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;

/**
 * @author Jason Tang
 */
public class CallbackHandlerImpl implements CallbackHandler {
	private String username;

	public CallbackHandlerImpl() {
	}

	public void handle(Callback[] callbacks) throws java.io.IOException, UnsupportedCallbackException {
		for (Callback cb : callbacks) {
			if (cb instanceof NameCallback) {
				username = ((NameCallback) cb).getDefaultName();
			} else if (cb instanceof PasswordCallback) {
				// TODO:
				// Query the password from db.
				// Here hardcode a password.
				String testPwd = "123456";
				((PasswordCallback) cb).setPassword(testPwd.toCharArray());
			} else if (cb instanceof AuthorizeCallback) {
				// TODO:
				((AuthorizeCallback) cb).setAuthorized(true);
			}
		}
	}
}
