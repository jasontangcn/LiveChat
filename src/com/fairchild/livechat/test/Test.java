/**
 * $Id: Test.java,v 1.0 Apr 6, 2009 10:54:18 AM Jason Exp $
 */
package com.fairchild.livechat.test;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslServer;

import org.gjt.xpp.XmlEndTag;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserFactory;
import org.gjt.xpp.XmlStartTag;

import com.fairchild.livechat.util.BufferUtil;
import com.fairchild.livechat.xmpp.PullProtocalParser;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/**
 * @author Jason Tang
 */
public class Test
{
	public static void main(String[] args) throws Exception{
		//test1();
		//test2();
		//test3();
		//test4();
		test5();
	}
	
  public static void test1(){
  	ByteBuffer bb = ByteBuffer.allocate(1024);
  	bb.put("Iam ByteBuffer".getBytes());
  	bb.flip();
  	System.out.println(bb);
  	System.out.println(BufferUtil.buffer2String(bb.asReadOnlyBuffer(),"UTF-8"));
  	System.out.println(bb);
  }
  
  public static void test2(){
  	System.out.println(new String(Base64.decode("dXNlcm5hbWU9Ikphc29uIixyZWFsbT0iamFzb250YW5nIixub25jZT0ib3Zyd296RGFZSzA2c1puUi8xNWNRRWdYTFg1STd6STNlaThFN3pLNyIsY25vbmNlPSJHU0pYZEM5Q0RuUVpRbE00V0FzS0NRTXRKMWtTTWdoV1hCbFlUbE5GREVVPSIsbmM9MDAwMDAwMDIscW9wPWF1dGgsbWF4YnVmPTQwOTYsZGlnZXN0LXVyaT0ieG1wcC9qYXNvbnRhbmciLHJlc3BvbnNlPWVkMjI1YTE3MGQzMWZiZGU0YjhhMTgxZGZjOTk4ZmEy")));
  	//System.out.println(Base64.encode("realm=\"jasontang\",nonce=\"OA6MG9tEQGm2hh\",qop=\"auth\",charset=utf-8,algorithm=md5-sess".getBytes()));
  }
  
  public static void test3() throws Exception{
  	Map props = new HashMap();
  	props.put(Sasl.QOP,"auth");
  	
  	CallbackHandler cbh = new CallbackHandler(){
      public void handle(Callback[] callbacks) throws java.io.IOException, UnsupportedCallbackException{
      	
      }
  	};
  	SaslServer server = Sasl.createSaslServer("DIGEST-MD5","XMPP","jasontang",props,cbh);

  	byte[] response = new byte[0];
  	byte[] rsp = server.evaluateResponse(response);
  	System.out.println(new String(rsp));
  }
  
  public static void test4() throws Exception{
  	System.out.println(PullProtocalParser.pullProtocalParser.getContent(null,"response"));
  }
  
  public static void test5() throws Exception{
  	/*
  	 * <?xml version='1.0' ?>
			  <stream:stream to='jasontang' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' version='1.0'>
			    <auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='DIGEST-MD5' xmlns:ga='http://www.google.com/talk/protocol/auth' ga:client-uses-full-bind-result='true'/>
			    <response xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>dXNlcm5hbWU9Ikphc29uIixyZWFsbT0iamFzb250YW5nIixub25jZT0iMWpkeXZibmVUNnpBSWN5TlRZRFF5Yzg5N3Y1VXk0Z0llRyt0M0VBQyIsY25vbmNlPSJZRmRGQ1RjWldTVTNIVWxFVUJSQWUxbDJHQ3NXZnpwL0RGQU1PSGNHY2lVPSIsbmM9MDAwMDAwMDEscW9wPWF1dGgsbWF4YnVmPTQwOTYsZGlnZXN0LXVyaT0ieG1wcC9qYXNvbnRhbmciLHJlc3BvbnNlPTMwOGU4NWRhNDQ4Y2M2ZDIyZmM4MjFmZTNmZDdjOGNj</response>
			  </stream:stream>
  	 */
  	String xml = "<a/>";
  	XmlPullParserFactory xppFactory = XmlPullParserFactory.newInstance();
		XmlPullParser parser = xppFactory.newPullParser();
		
		parser.setAllowedMixedContent(false);
		parser.setNamespaceAware(true);
		parser.setNamespaceAttributesReporting(true);
		parser.setInput(xml.toCharArray());
		
		byte type;
		XmlStartTag startTag = xppFactory.newStartTag();
		XmlEndTag endTag = xppFactory.newEndTag();
		/*
		parser.next();
		
		XmlPullNode pullNode = xppFactory.newPullNode(parser);
		
		Enumeration e = pullNode.children();
		while(e.hasMoreElements()){
			;
			System.out.println(e.nextElement());
		}
		*/

		//System.out.println(parser.getQNameLocal("stream:stream"));
		//System.out.println(parser.getQNameUri("stream:stream"));
		
		while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
			System.out.println("-----------------------------------------");
			if (type == XmlPullParser.CONTENT) {
				String s = parser.readContent();
				System.out.println("ContentLength: " + parser.getContentLength());
				System.out.println("Content={'" + s + "'}");
				System.out.println("NamespaceLength: " + parser.getNamespacesLength(parser.getDepth()));
				System.out.println("Depth: " + parser.getDepth());
			} else if (type == XmlPullParser.END_TAG) {
				System.out.println("NamespaceLength: " + parser.getNamespacesLength(parser.getDepth()));
				parser.readEndTag(endTag);
				System.out.println(endTag);
			} else if (type == XmlPullParser.START_TAG) {
				parser.readStartTag(startTag);
				System.out.println(startTag);
				System.out.println("NamespaceLength: " + parser.getNamespacesLength(parser.getDepth()));
				System.out.println("Depth: " + parser.getDepth());
				System.out.println("PosDesc: " + parser.getPosDesc());
				System.out.println("LocalName: " + parser.getLocalName());
				System.out.println("RawName: " + parser.getRawName());
				System.out.println("ColumnNumber: " + parser.getColumnNumber());
			}
		}

  }
}
