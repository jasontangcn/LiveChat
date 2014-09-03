package com.fairchild.livechat.test;

import java.io.IOException;
import java.io.StringReader;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.gjt.xpp.XmlEndTag;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserException;
import org.gjt.xpp.XmlPullParserFactory;
import org.gjt.xpp.XmlStartTag;

/**
 * Some tests to implementation of XML Pull PullProtocalParser (xpp).
 *
 */
public class XMLParserTestcase extends TestCase {

    private int type;
    private XmlStartTag stag;
    private XmlEndTag etag;
    private XmlPullParserFactory factory;

    private static final String HEADER_STR = "####";
    private static final String TRAILER_STR = "%%%%%XXX";
    private static final String XML_STR = HEADER_STR +
        "<?xml version=\"1.0\"?>\n"+
        "<m:PlaceOrder xmlns:m='Some-Namespace-URI'"+
        " xmlns:xsd=\"http://w3.org/ns/data\""+
        " xmlns:xsi=\"http://w3.org/ns/instances\""+
        ">\n"+
        "<DaysToDelivery xsd:type=\"xsi:integer\">7</DaysToDelivery>\n"+
        "<tree><subtree1><a1>aaa</a1><a2><a3>X</a3></a2></subtree1><subtree2/>"+
        "</tree>"+
        "<empty/>\n"+
        "<top xmlns=\"http://my.top.com\">\n"+
        "<c:codebase xsi:type= 'xsd:string' xmlns:c='http://foo'>"+
        " </c:codebase>\n"+
        "<codebase2 xsi:type= 'xsd:string' ></codebase2>\n"+
        "<new-line>\r\r\n\n\r\n\n\n&#13;</new-line>"+
        "</top>"+
        "</m:PlaceOrder>\n"+
        ""+ TRAILER_STR;

    public XMLParserTestcase(String name) {
        super(name);
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
        System.exit(0);
    }

    public static Test suite() {
        return new TestSuite(XMLParserTestcase.class);
    }

    protected void setUp() throws XmlPullParserException {
        factory = XmlPullParserFactory.newInstance(
            System.getProperty(XmlPullParserFactory.DEFAULT_PROPERTY_NAME) );
        factory.setNamespaceAware(true);
        stag = factory.newStartTag();
        etag = factory.newEndTag();
    }

    protected void tearDown() {
    }

    private void testSimple(XmlPullParser pp, boolean mixedContent)
        throws IOException, XmlPullParserException
    {
        assertEquals(XmlPullParser.START_TAG, pp.next());pp.readStartTag(stag);
        assertEquals("t1", stag.getLocalName());
        assertEquals("t1", stag.getRawName());
        if(mixedContent) {
            assertEquals(XmlPullParser.CONTENT, pp.next());
            assertEquals(printable("\n "), printable(pp.readContent()));
        }

        int countNs = pp.getNamespacesLength(pp.getDepth());
        assertEquals(1, countNs);
        String[] namespaces = new String[countNs];
        pp.readNamespacesUris(pp.getDepth(), namespaces, 0, countNs);
        assertEquals("test", namespaces[0]);
        String[] prefixes = new String[countNs];
        pp.readNamespacesPrefixes(pp.getDepth(), prefixes, 0, countNs);
        assertEquals("ns", prefixes[0]);


        assertEquals(XmlPullParser.START_TAG, pp.next());pp.readStartTag(stag);
        assertEquals("t2", stag.getLocalName());
        assertEquals(pp.next(), XmlPullParser.END_TAG); pp.readEndTag(etag);
        assertEquals("t2", etag.getLocalName());
        if(mixedContent) {
            assertEquals(XmlPullParser.CONTENT, pp.next());
            assertEquals(" ", pp.readContent());
        }
        assertEquals(XmlPullParser.START_TAG, pp.next());pp.readStartTag(stag);
        assertEquals("x2", stag.getLocalName());
        assertEquals(XmlPullParser.CONTENT, pp.next());
        assertEquals("Hello", pp.readContent());
        assertEquals(pp.next(), XmlPullParser.END_TAG); pp.readEndTag(etag);
        assertEquals("x2", etag.getLocalName());
        if(mixedContent) {
            assertEquals(XmlPullParser.CONTENT, pp.next());
            assertEquals(printable("\n"), printable(pp.readContent()));
        }
        assertEquals(pp.next(), XmlPullParser.END_TAG); pp.readEndTag(etag);
        assertEquals("t1", etag.getLocalName());

        countNs = pp.getNamespacesLength(pp.getDepth());
        assertEquals(1, countNs);
        pp.readNamespacesUris(pp.getDepth(), namespaces, 0, countNs);
        assertEquals("test", namespaces[0]);
        pp.readNamespacesPrefixes(pp.getDepth(), prefixes, 0, countNs);
        assertEquals("ns", prefixes[0]);

    }

    public void testSimple() throws IOException, XmlPullParserException
    {
        final String SIMPLE_XML = "<t1 xmlns:ns=\"test\">\n <t2/> <x2>Hello</x2>\r\n</t1>";
        XmlPullParser pp = factory.newPullParser();
        pp.setInput(new StringReader(SIMPLE_XML));
        testSimple(pp, true);
        // now repeat parsing but ignoring mixed content
        pp.setInput(new StringReader(SIMPLE_XML));
        pp.setAllowedMixedContent(false);
        testSimple(pp, false);

    }

    public void testCDATA() throws IOException, XmlPullParserException
    {
        final String CDATA = "<F3><![CDATA[[Esc][Esc][Esc]]]></F3>";
        XmlPullParser pp = factory.newPullParser();
        pp.setInput(new StringReader(CDATA));
        assertEquals(XmlPullParser.START_TAG, pp.next());pp.readStartTag(stag);
        assertEquals("F3", stag.getLocalName());
        assertEquals(XmlPullParser.CONTENT, pp.next());
        assertEquals("[Esc][Esc][Esc]", pp.readContent());
        assertEquals(pp.next(), XmlPullParser.END_TAG); pp.readEndTag(etag);
        assertEquals("F3", etag.getLocalName());

    }
    public void testXpp() throws IOException, XmlPullParserException
    {
        //if(Log.ON) l.log(Level.FINE, "starting test");

        char[] buf = XML_STR.toCharArray();
        XmlPullParser pp = factory.newPullParser();
        pp.setInput(buf, HEADER_STR.length() ,
                    XML_STR.length() - HEADER_STR.length() - TRAILER_STR.length());
        pp.setAllowedMixedContent(false);


        assertEquals(XmlPullParser.START_TAG, pp.next());pp.readStartTag(stag);
        assertEquals("PlaceOrder", stag.getLocalName());
        String uri = stag.getNamespaceUri();
        assertEquals("Some-Namespace-URI", uri);

        assertEquals(XmlPullParser.START_TAG, pp.next());pp.readStartTag(stag);
        assertEquals("DaysToDelivery", stag.getLocalName());

        // check utility functions
        String local = pp.getQNameLocal("ns:alek");
        assertEquals("alek", local);
        uri = pp.getQNameUri("ns:alek");
        assertEquals(null, uri);
        uri = pp.getQNameUri("alek");
        assertEquals("", uri);
        uri = pp.getQNameUri("m:alek");
        assertEquals("Some-Namespace-URI", uri);

        assertEquals(XmlPullParser.CONTENT, pp.next());
        assertEquals("7", pp.readContent());

        assertEquals(pp.next(), XmlPullParser.END_TAG);
        pp.readEndTag(etag);
        assertEquals("DaysToDelivery", etag.getLocalName());

        assertEquals(XmlPullParser.START_TAG, pp.next());pp.readStartTag(stag);
        assertEquals("tree", stag.getLocalName());
        pp.skipNode();
        pp.readEndTag(etag);
        assertEquals("tree", etag.getLocalName());

        assertEquals(XmlPullParser.START_TAG, pp.next());pp.readStartTag(stag);
        assertEquals("empty", stag.getLocalName());
        pp.skipNode();
        pp.readEndTag(etag);
        assertEquals("empty", etag.getLocalName());

        assertEquals(XmlPullParser.START_TAG, pp.next());pp.readStartTag(stag);
        assertEquals("top", stag.getLocalName());

        assertEquals(XmlPullParser.START_TAG, pp.next());pp.readStartTag(stag);
        assertEquals("codebase", stag.getLocalName());
        assertEquals(XmlPullParser.CONTENT, pp.next());
        assertEquals(" ", pp.readContent());
        assertEquals(pp.next(), XmlPullParser.END_TAG);
        pp.readEndTag(etag);
        assertEquals("codebase", etag.getLocalName());

        assertEquals(XmlPullParser.START_TAG, pp.next());pp.readStartTag(stag);
        assertEquals("codebase2", stag.getLocalName());
        //assertEquals(XmlPullParser.CONTENT, pp.next());
        //assertEquals("", pp.readContent());
        assertEquals(pp.next(), XmlPullParser.END_TAG);
        pp.readEndTag(etag);
        assertEquals("codebase2", etag.getLocalName());

        assertEquals(XmlPullParser.START_TAG, pp.next());pp.readStartTag(stag);
        assertEquals("new-line", stag.getLocalName());
        assertEquals(XmlPullParser.CONTENT, pp.next());
        //assertEquals("", pp.readContent());
        assertEquals(pp.next(), XmlPullParser.END_TAG);
        pp.readEndTag(etag);
        assertEquals("new-line", etag.getLocalName());

        assertEquals(pp.next(), XmlPullParser.END_TAG);
        pp.readEndTag(etag);
        assertEquals("top", etag.getLocalName());

        assertEquals(XmlPullParser.END_TAG, pp.next());pp.readEndTag(etag);
        assertEquals("PlaceOrder", etag.getLocalName());
        uri = etag.getNamespaceUri();
        assertEquals("Some-Namespace-URI", uri);

        assertEquals(XmlPullParser.END_DOCUMENT, pp.next());

        // make sure that if tried to read beyond that is excpetion ...

    }

    public void testAttribs() throws IOException, XmlPullParserException
    {
        final String XML_ATTRS =
            "<event xmlns:xsi='http://www.w3.org/1999/XMLSchema/instance' encodingStyle=\"test\">"+
            "<type>my-event</type>"+
            "<handback xsi:type='ns2:string' xmlns:ns2='http://www.w3.org/1999/XMLSchema' xsi:null='1'/>"+
            "</event>";

        XmlPullParser pp = factory.newPullParser();
        pp.setInput(new StringReader(XML_ATTRS));

        assertEquals(XmlPullParser.START_TAG, pp.next());pp.readStartTag(stag);
        assertEquals("event", stag.getLocalName());
        assertEquals("event", stag.getRawName());
        assertEquals(XmlPullParser.START_TAG, pp.next());pp.readStartTag(stag);
        assertEquals("type", stag.getLocalName());
        assertEquals(XmlPullParser.CONTENT, pp.next());
        assertEquals("my-event", pp.readContent());
        assertEquals(pp.next(), XmlPullParser.END_TAG); pp.readEndTag(etag);
        assertEquals("type", etag.getLocalName());
        assertEquals(XmlPullParser.START_TAG, pp.next());pp.readStartTag(stag);
        assertEquals("handback", stag.getLocalName());
        //assertEquals(XmlPullParser.CONTENT, pp.next());
        //assertEquals("", pp.readContent());

        String xsiNull = stag.getAttributeValueFromName(
            "http://www.w3.org/1999/XMLSchema/instance", "null");
        assertEquals("1", xsiNull);

        String xsiType = stag.getAttributeValueFromName(
            "http://www.w3.org/1999/XMLSchema/instance", "type");
        assertEquals("ns2:string", xsiType);


        String typeName = pp.getQNameLocal(xsiType);
        assertEquals("string", typeName);
        String typeNS = pp.getQNameUri(xsiType);
        assertEquals("http://www.w3.org/1999/XMLSchema", typeNS);

        // check if removing works OK
        boolean removed = stag.removeAttributeByRawName("xsi:null");
        assertTrue(removed);
        removed = stag.removeAttributeByName(
            "http://www.w3.org/1999/XMLSchema/instance", "null");
        assertEquals(false, removed);
        removed = stag.removeAttributeByName(
            "http://www.w3.org/1999/XMLSchema/instance", "null");
        assertEquals(false, removed);
        xsiNull = stag.getAttributeValueFromName(
            "http://www.w3.org/1999/XMLSchema/instance", "null");
        assertEquals(null, xsiNull);
        xsiType = stag.getAttributeValueFromName(
            "http://www.w3.org/1999/XMLSchema/instance", "type");
        assertEquals("ns2:string", xsiType);

        // remove second attribute as well
        removed = stag.removeAttributeByName(
            "http://www.w3.org/1999/XMLSchema/instance", "type");
        assertTrue(removed);
        xsiNull = stag.getAttributeValueFromName(
            "http://www.w3.org/1999/XMLSchema/instance", "null");
        assertEquals(null, xsiNull);
        xsiType = stag.getAttributeValueFromName(
            "http://www.w3.org/1999/XMLSchema/instance", "type");
        assertEquals(null, xsiType);


        assertEquals(pp.next(), XmlPullParser.END_TAG); pp.readEndTag(etag);
        assertEquals("handback", etag.getLocalName());
        assertEquals(pp.next(), XmlPullParser.END_TAG); pp.readEndTag(etag);
        assertEquals("event", etag.getLocalName());

        assertEquals(pp.next(), XmlPullParser.END_DOCUMENT);

    }

    public void testAttribUniq() throws IOException, XmlPullParserException
    {

        final String attribsOk =
            "<m:test xmlns:m='Some-Namespace-URI' xmlns:n='Some-Namespace-URI'"+
            " a='a' b='b' m:a='c' n:b='d' n:x='e'"+
            "/>\n"+
            "";

        final String duplicateAttribs =
            "<m:test xmlns:m='Some-Namespace-URI' xmlns:n='Some-Namespace-URI'"+
            " a='a' b='b' m:a='a' n:b='b' a='x'"+
            "/>\n"+
            "";

        final String duplicateNsAttribs =
            "<m:test xmlns:m='Some-Namespace-URI' xmlns:n='Some-Namespace-URI'"+
            " a='a' b='b' m:a='a' n:b='b' n:a='a'"+
            "/>\n"+
            "";

        final String duplicateXmlns =
            "<m:test xmlns:m='Some-Namespace-URI' xmlns:m='Some-Namespace-URI'"+
            ""+
            "/>\n"+
            "";

        final String duplicateAttribXmlnsDefault =
            "<m:test xmlns='Some-Namespace-URI' xmlns:m='Some-Namespace-URI'"+
            " a='a' b='b' m:b='b' m:a='x'"+
            "/>\n"+
            "";

        XmlPullParser pp = factory.newPullParser();
        parseOneElement(pp, attribsOk, false);
        assertEquals("a", stag.getAttributeValueFromRawName("a"));
        assertEquals("b", stag.getAttributeValueFromRawName("b"));
        assertEquals("c", stag.getAttributeValueFromRawName("m:a"));
        assertEquals("d", stag.getAttributeValueFromRawName("n:b"));
        assertEquals("e", stag.getAttributeValueFromRawName("n:x"));

        parseOneElement(pp, attribsOk, true);

        assertEquals("a", stag.getAttributeValueFromRawName("a"));
        assertEquals("b", stag.getAttributeValueFromRawName("b"));
        assertEquals("c", stag.getAttributeValueFromRawName("m:a"));
        assertEquals("d", stag.getAttributeValueFromRawName("n:b"));
        assertEquals("e", stag.getAttributeValueFromRawName("n:x"));

        assertEquals("c", stag.getAttributeValueFromName("Some-Namespace-URI", "a"));
        assertEquals("d", stag.getAttributeValueFromName("Some-Namespace-URI", "b"));
        assertEquals("e", stag.getAttributeValueFromName("Some-Namespace-URI", "x"));
        assertEquals("a", stag.getAttributeValueFromName(null, "a"));
        assertEquals("b", stag.getAttributeValueFromName(null, "b"));

        parseOneElement(pp, duplicateNsAttribs, false);
        parseOneElement(pp, duplicateAttribXmlnsDefault, false);
        parseOneElement(pp, duplicateAttribXmlnsDefault, true);

        Exception ex;

        ex = null;
        try {
            parseOneElement(pp, duplicateAttribs, true);
        } catch(XmlPullParserException rex) {
            ex = rex;
        }
        assertNotNull(ex);

        ex = null;
        try {
            parseOneElement(pp, duplicateAttribs, false);
        } catch(XmlPullParserException rex) {
            ex = rex;
        }
        assertNotNull(ex);

        ex = null;
        try {
            parseOneElement(pp, duplicateXmlns, false);
        } catch(XmlPullParserException rex) {
            ex = rex;
        }
        assertNotNull(ex);

        // disable this test for Xerces 2.0.1 or NullPointer Exception is thrown ...
        ex = null;
        try {
            parseOneElement(pp, duplicateXmlns, true);
        } catch(XmlPullParserException rex) {
            ex = rex;
        }
        assertNotNull(ex);

        ex = null;
        try {
            parseOneElement(pp, duplicateNsAttribs, true);
        } catch(XmlPullParserException rex) {
            ex = rex;
        }
        assertNotNull(ex);

        final String declaringEmptyNs  =
            "<m:test xmlns:m='' />";

        // allowed when namespaces disabled
        parseOneElement(pp, declaringEmptyNs, false);

        // otherwise it is error to declare '' for non-default NS as described in
        //   http://www.w3.org/TR/1999/REC-xml-names-19990114/#ns-decl
        ex = null;
        try {
            parseOneElement(pp, declaringEmptyNs, true);
        } catch(XmlPullParserException rex) {
            ex = rex;
        }
        assertNotNull(ex);

    }

    public void testNormalizeLine()
        throws IOException, XmlPullParserException
    {

        XmlPullParser pp = factory.newPullParser();

        //-----------------------
        // ---- simple tests for end of line normalization

        final String simpleR = "-\n-\r-\r\n-\n\r-";

        // element content EOL normalizaton

        final String tagSimpleR = "<test>"+simpleR+"</test>";

        final String expectedSimpleN = "-\n-\n-\n-\n\n-";

        parseOneElement(pp, tagSimpleR, true);
        assertEquals(XmlPullParser.CONTENT, pp.next());
        assertEquals(printable(expectedSimpleN), printable(pp.readContent()));

        // attribute content normalization

        final String attrSimpleR = "<test a=\""+simpleR+"\"/>";

        final String normalizedSimpleN = "- - - -  -";

        parseOneElement(pp, attrSimpleR, true);
        pp.readStartTag(stag);
        String attrVal = stag.getAttributeValueFromRawName("a");

        //TODO Xerces2
        assertEquals(printable(normalizedSimpleN), printable(attrVal));

        //-----------------------
        // --- more complex example with more line engins together

        final String firstR =
            "\r \r\n \n\r \n\n \r\n\r \r\r \r\n\n \n\r\r\n\r"+
            "";

        // element content

        final String tagR =
            "<m:test xmlns:m='Some-Namespace-URI'>"+
            firstR+
            "</m:test>\r\n";

        final String expectedN =
            "\n \n \n\n \n\n \n\n \n\n \n\n \n\n\n\n";

        parseOneElement(pp, tagR, true);
        assertEquals(XmlPullParser.CONTENT, pp.next());
        assertEquals(printable(expectedN), printable(pp.readContent()));

        // attribute value

        final String attrR =
            "<m:test xmlns:m='Some-Namespace-URI' fifi='"+firstR+"'/>";

        final String normalizedN =
            "                       ";

        parseOneElement(pp, attrR, true);
        pp.readStartTag(stag);
        attrVal = stag.getAttributeValueFromRawName("fifi");
        //System.err.println("attrNormalized.len="+normalizedN.length());
        //System.err.println("attrVal.len="+attrVal.length());

        //TODO Xerces2
        assertEquals(printable(normalizedN), printable(attrVal));


        //-----------------------
        // --- even more complex

        final String manyLineBreaks =
            "fifi\r&amp;\r&amp;\r\n foo &amp;\r bar \n\r\n&quot;"+
            firstR;

        final String manyTag =
            "<m:test xmlns:m='Some-Namespace-URI'>"+
            manyLineBreaks+
            "</m:test>\r\n";

        final String manyExpected =
            "fifi\n&\n&\n foo &\n bar \n\n\""+
            expectedN;
        //"\r \r\n \n\r \n\n \r\n\r \r\r \r\n\n \n\r\r\n\r";

        parseOneElement(pp, manyTag, true);
        assertEquals(XmlPullParser.CONTENT, pp.next());
        assertEquals(manyExpected, pp.readContent());

        assertEquals(pp.next(), XmlPullParser.END_TAG);
        pp.readEndTag(etag);
        assertEquals("test", etag.getLocalName());

        // having \r\n as last characters is the hardest case
        //assertEquals(XmlPullParser.CONTENT, pp.next());
        //assertEquals("\n", pp.readContent());
        assertEquals(pp.next(), XmlPullParser.END_DOCUMENT);


        final String manyAttr =
            "<m:test xmlns:m='Some-Namespace-URI' fifi='"+manyLineBreaks+"'/>";

        final String manyNormalized =
            "fifi & &  foo &  bar   \""+
            normalizedN;

        parseOneElement(pp, manyAttr, true);
        pp.readStartTag(stag);
        attrVal = stag.getAttributeValueFromRawName("fifi");
        //TODO Xerces2
        assertEquals(printable(manyNormalized), printable(attrVal));

    }

    private void parseOneElement(
        final XmlPullParser pp,
        final String buf,
        final boolean supportNamespaces)
        throws IOException, XmlPullParserException
    {
        //pp.setInput(buf.toCharArray());
        pp.setInput(new StringReader(buf));
        pp.setNamespaceAware(supportNamespaces);
        pp.setAllowedMixedContent(false);
        pp.next();
        pp.readStartTag(stag);
        if(supportNamespaces) {
            assertEquals("test", stag.getLocalName());
        } else {
            assertEquals("m:test", stag.getLocalName());
        }
    }

    private static String printable(char ch) {
        if(ch == '\n') {
            return "\\n";
        } else if(ch == '\r') {
            return "\\r";
        } else if(ch == '\t') {
            return "\\t";
        }
        return ""+ch;
    }

    private static String printable(String s) {
        int iN = s.indexOf('\n');
        int iR = s.indexOf('\r');
        int iT = s.indexOf('\t');
        if((iN != -1) || (iR != -1) || (iT != -1)) {
            StringBuffer buf = new StringBuffer();
            for(int i = 0; i < s.length(); ++i) {
                buf.append(printable(s.charAt(i)));
            }
            s = buf.toString();
        }
        return s;
    }

}


