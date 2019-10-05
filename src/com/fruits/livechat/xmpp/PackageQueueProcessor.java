/**
 * $Id: PackageQueueProcessor.java,v 1.0 Apr 11, 2009 9:03:51 AM Jason Exp $
 */
package com.fruits.livechat.xmpp;

import java.util.HashMap;
import java.util.Map;

import com.fruits.livechat.xmpp.protocal.PackageHandler;
import com.fruits.livechat.xmpp.protocal.SaslHandlerImpl;
import com.fruits.livechat.xmpp.protocal.StreamHandlerImpl;
import org.gjt.xpp.XmlNode;
import org.gjt.xpp.XmlTag;

/**
 * @author Jason Tang
 */
public class PackageQueueProcessor implements Runnable {
	private static PackageQueueProcessor processor = new PackageQueueProcessor(PackageQueue.getPackageQ());

	public static PackageQueueProcessor getProcessor() {
		return processor;
	}

	private PackageQueue packageQ = null;
	private Map<String, PackageHandler> handlers = new HashMap<String, PackageHandler>();

	public PackageQueueProcessor(PackageQueue packageQ) {
		this.packageQ = packageQ;
	}

	public void addPackageHandler(String packageType, PackageHandler packageHandler) {
		if (!(packageHandler instanceof SaslHandlerImpl) && !(packageHandler instanceof StreamHandlerImpl)) {
			handlers.put(packageType, new SaslHandlerImpl(packageHandler));
		} else {
			handlers.put(packageType, packageHandler);
		}
	}

	public void run() {
		for (;;) {
			Package pkg = null;
			try {
				pkg = packageQ.take();
			} catch (Exception e) {
				System.out.println(e);
			}

			// handle this package
			XmlTag tag = pkg.getXmlTag();
			String tagName = tag.getLocalName();

			System.out.println("Processing a package of type: " + tagName);

			PackageHandler handler = handlers.get(tagName);

			String ns = null;

			if (null == handler) {
				ns = tag.getNamespaceUri();
				if (null != ns)
					handler = handlers.get(ns);
				if (null == handler) {
					if (tag instanceof XmlNode) {
						XmlNode node = (XmlNode) tag;
						ns = node.getAttributeValueFromRawName("xmlns");
						handler = handlers.get(ns);
					}
				}
			}

			if (null == handler)
				System.out.println("No handler found for package of type: " + tagName);

			if (null != handler) {
				try {
					System.out.println("Handler: " + handler + " selected for package of type " + tagName + " or namespace of " + ns + ".");
					handler.handle(pkg);
				} catch (Exception e) {
					System.out.println(e);
				}
			}
		}
	}
}
