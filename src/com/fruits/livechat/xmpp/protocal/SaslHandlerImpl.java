/**
 * $Id: SaslHandlerImpl.java,v 1.0 Apr 11, 2009 11:02:20 AM Jason Exp $
 */
package com.fruits.livechat.xmpp.protocal;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

import org.gjt.xpp.XmlNode;
import org.gjt.xpp.XmlTag;

import com.fruits.livechat.Constants;
import com.fruits.livechat.server.xmpp.CallbackHandlerImpl;
import com.fruits.livechat.xmpp.Package;
import com.fruits.livechat.xmpp.PackageDeliver;
import com.fruits.livechat.xmpp.Session;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/**
 * @author Jason Tang
 */
public class SaslHandlerImpl implements PackageHandler {
	public final static String SASL_MECHANISHM_DIGEST_MD5 = "DIGEST-MD5";
	public final static String SASL_PROTOCAL = "XMPP";
	public final static String SASL_QOP = "auth";

	public final static String SASL_STATE = "SASL_STATE";
	public final static String SASL_AUTHING = "SASL_AUTHING";
	public final static String SASL_AUTHED = "SASL_AUTHED";
	public final static String SASL_CURRENT_STEP = "SASL_CURRENT_STEP";
	public final static String SASL_STEP_ONE = "SASL_STEP_ONE";
	public final static String SASL_STEP_THREE = "SASL_STEP_THREE";
	public final static String SASL_STEP_FIVE = "SASL_STEP_FIVE";
	public final static String SASL_SERVER = "SASL_SERVER";

	private PackageHandler nextHandler = null;

	public SaslHandlerImpl() {
	}

	public SaslHandlerImpl(PackageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}

	public void handle(Package pkg) throws Exception {
		Session session = pkg.getSession();

		String authState = (String) session.getAttribute(SASL_STATE);
		if ((null == authState) || (!SASL_AUTHED.equalsIgnoreCase(authState))) {
			System.out.println("SASL performing.");

			XmlTag tag = pkg.getXmlTag();
			String tagName = tag.getLocalName();

			if ("auth".equalsIgnoreCase(tagName)) {
				XmlNode node = (XmlNode) tag;
				String mecchanism = node.getAttributeValueFromRawName("mechanism");
				if (SASL_MECHANISHM_DIGEST_MD5.equalsIgnoreCase(mecchanism)) {
					resetSaslState(session);

					Map<String, String> props = new HashMap<String, String>();
					props.put(Sasl.QOP, SASL_QOP);
					CallbackHandler callbackHandler = new CallbackHandlerImpl();
					SaslServer saslServer = Sasl.createSaslServer(SASL_MECHANISHM_DIGEST_MD5, SASL_PROTOCAL, Constants.SERVER_NAME, props, callbackHandler);
					session.setAttribute(SASL_STATE, SASL_AUTHING);
					session.setAttribute(SASL_CURRENT_STEP, SASL_STEP_THREE);
					session.setAttribute(SASL_SERVER, saslServer);
					StringBuilder responseXml = new StringBuilder();
					responseXml.append("<challenge xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">").append(Base64.encode(saslServer.evaluateResponse(new byte[0]))).append("</challenge>");
					PackageDeliver.responseClient(session, responseXml.toString());
				}
			} else if ("response".equalsIgnoreCase(tagName)) {
				String currentStep = (String) session.getAttribute(SASL_CURRENT_STEP);
				SaslServer saslServer = (SaslServer) session.getAttribute(SASL_SERVER);

				if (tag instanceof XmlNode) {
					XmlNode node = (XmlNode) tag;
					if ((0 < node.getChildrenCount()) && SASL_STEP_THREE.equalsIgnoreCase(currentStep)) {
						String clientResponse = (String) ((XmlNode) tag).getChildAt(0);
						StringBuilder responseXml = new StringBuilder();
						responseXml.append("<challenge xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">").append(Base64.encode(saslServer.evaluateResponse(Base64.decode(clientResponse)))).append("</challenge>");
						session.setAttribute(SASL_CURRENT_STEP, SASL_STEP_FIVE);
						PackageDeliver.responseClient(session, responseXml.toString());
					} else if (SASL_STEP_FIVE.equalsIgnoreCase(currentStep)) {
						StringBuilder responseXml = new StringBuilder();
						responseXml.append("<success xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\"/>");
						session.removeAttribute(SASL_CURRENT_STEP);
						session.removeAttribute(SASL_SERVER);
						PackageDeliver.responseClient(session, responseXml.toString());
						try {
							saslServer.dispose();
						} catch (SaslException se) {
							System.out.println(se);
						}
						session.setAttribute(SASL_STATE, SASL_AUTHED);
						System.out.println("SASL succeeded.");
					}
				}
			}
		} else {
			if (null != nextHandler) {
				nextHandler.handle(pkg);
			}
		}
	}

	private void resetSaslState(Session session) {
		session.removeAttribute(SASL_STATE);
		session.removeAttribute(SASL_CURRENT_STEP);
		SaslServer saslServer = (SaslServer) session.removeAttribute(SASL_SERVER);
		if (null != saslServer) {
			try {
				saslServer.dispose();
			} catch (SaslException se) {
				System.out.println(se);
			}
		}
	}

	public PackageHandler nextHandler() {
		return this.nextHandler;
	}

}
