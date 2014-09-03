/**
 * $Id: PackageQueue.java,v 1.0 Apr 9, 2009 10:28:21 PM Jason Exp $
 */
package com.fairchild.livechat.xmpp;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.fairchild.livechat.Constants;

/**
 * @author Jason Tang
 */
public class PackageQueue {
	private static PackageQueue packageQ = new PackageQueue();

	public static PackageQueue getPackageQ() {
		return packageQ;
	}

	private BlockingQueue<Package> packages = new LinkedBlockingQueue<Package>(Constants.PACKAGE_QUEUE_INITIAL_CAPACITY);

	public void put(Package pkg) throws Exception {
		this.packages.put(pkg);
	}

	public void put(List<Package> pkgs) throws Exception {
		// lock problem
		for (Package pkg : pkgs) {
			this.packages.put(pkg);
		}
	}

	public Package take() throws Exception {
		return packages.take();
	}
}
