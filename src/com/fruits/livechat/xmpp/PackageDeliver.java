/**
 * $Id: PackageDeliver.java,v 1.0 Apr 9, 2009 10:28:47 PM Jason Exp $
 */
package com.fruits.livechat.xmpp;

import java.nio.ByteBuffer;

import com.fruits.livechat.server.xmpp.DataPort;

/**
 * @author Jason Tang
 */
public class PackageDeliver {
	private static PackageDeliver packageDeliver = new PackageDeliver();

	public static PackageDeliver getPackageDeliver() {
		return packageDeliver;
	}

	public PackageDeliver() {
	}

	public void deliver(Package pkg) {
		Session session = pkg.getSession();
		DataPort dataPort = session.getDataPort();
		ByteBuffer writeBuffer = dataPort.getWriteBuffer();
		synchronized (writeBuffer) {
			dataPort.readyWriteWriteBuffer();
			writeBuffer.put(pkg.getXmlString().getBytes());
		}
	}

	public static void responseClient(Session session, String responseXml) {
		Package response = new Package(session);
		response.setXmlString(responseXml.toString());
		getPackageDeliver().deliver(response);
		System.out.println("Responsing to client: " + responseXml);
	}
}
