package com.paulmandal.atak.forwarder.xmlutils;

import android.util.Log;

import com.amazonaws.util.StringInputStream;
import com.paulmandal.atak.forwarder.Config;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XmlComparer {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + XmlComparer.class.getSimpleName();

    public boolean compareXmls(String messageType, String lhs, String rhs) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setCoalescing(true);
            dbf.setIgnoringElementContentWhitespace(true);
            dbf.setIgnoringComments(true);
            DocumentBuilder db = dbf.newDocumentBuilder();

            Document original = db.parse(new StringInputStream(lhs));
            original.normalizeDocument();

            Document converted = db.parse(new StringInputStream(rhs));
            converted.normalizeDocument();

            boolean matched = compare(original, converted);

            if (!matched) {
                Log.e(TAG, "messageType: " + messageType + " mismatch, o: " + lhs);
                Log.e(TAG, "messageType: " + messageType + " mismatch, c: " + rhs);
            } else {
                return true;
            }
        } catch (ParserConfigurationException | IOException | SAXException | UnsupportedOperationException e) {
            Log.e(TAG, "messageType: " + messageType + " matched: false");
            e.printStackTrace();
        }
        return false;
    }

    private boolean compare(Document lhs, Document rhs) {
        NodeList lhsNodes = lhs.getChildNodes();
        NodeList rhsNodes = rhs.getChildNodes();

        return match(lhsNodes.item(0), rhsNodes.item(0), "");
    }

    boolean match(Node lhs, Node rhs, String path) {
        // Compare innertext (i think?)
        String lhsNodeValue = lhs.getNodeValue();
        String rhsNodeValue = rhs.getNodeValue();
        if (lhsNodeValue != null && !lhsNodeValue.equals(rhsNodeValue)) {
            Log.e(TAG, "  " + path + "." + lhs.getNodeName() + ": didn't match inner value: " + lhsNodeValue);
            return false;
        }

        // Compare attributes
        NamedNodeMap lhsAttributes = lhs.getAttributes();
        NamedNodeMap rhsAttributes = rhs.getAttributes();

        if (lhsAttributes != null) {

            Map<String, Node> lhsAttributeMap = new HashMap<>();
            for (int i = 0; i < lhsAttributes.getLength(); i++) {
                Node lhsAttribute = lhsAttributes.item(i);
                lhsAttributeMap.put(lhsAttribute.getNodeName(), lhsAttribute);
            }

            Map<String, Node> rhsAttributeMap = new HashMap<>();
            for (int i = 0; i < rhsAttributes.getLength(); i++) {
                Node rhsAttribute = rhsAttributes.item(i);
                rhsAttributeMap.put(rhsAttribute.getNodeName(), rhsAttribute);
            }

            for (String nodeName : lhsAttributeMap.keySet()) {
                Node lhsNode = lhsAttributeMap.get(nodeName);
                Node rhsNode = rhsAttributeMap.get(nodeName);

                if (rhsNode == null) {
                    Log.e(TAG, "  " + path + "." + lhs.getNodeName() + "." + nodeName + ": missing attribute on converted obj.");
                    return false;
                }

                String lhsValue = lhsNode.getNodeValue();
                String rhsValue = rhsNode.getNodeValue();
                if (!lhsValue.equals(rhsValue)) {
                    Log.e(TAG, "  " + path + "." + lhs.getNodeName() + "." + nodeName + ": values differ: " + lhsValue + ", rhs: " + rhsValue);
                    return false;
                }
            }
        }

        // Compare children
        NodeList lhsNodeList = lhs.getChildNodes();
        Map<String, Node> lhsNodesMap = new HashMap<>();
        for (int i = 0 ; i < lhsNodeList.getLength() ; i++) {
            Node lhsNode = lhsNodeList.item(i);
            lhsNodesMap.put(lhsNode.getNodeName(), lhsNode);
        }

        NodeList rhsNodeList = rhs.getChildNodes();
        Map<String, Node> rhsNodesMap = new HashMap<>();
        for (int i = 0 ; i < rhsNodeList.getLength() ; i++) {
            Node rhsNode = rhsNodeList.item(i);
            rhsNodesMap.put(rhsNode.getNodeName(), rhsNode);
        }

        path += (path.length() > 0 ? "." : "");
        path += lhs.getNodeName();
        for (String nodeName : lhsNodesMap.keySet()) {
            Node lhsNode = lhsNodesMap.get(nodeName);
            Node rhsNode = rhsNodesMap.get(nodeName);

            if (rhsNode == null) {
                Log.e(TAG, "  " + path + "." + nodeName + ": missing child node on converted obj.");
                return false;
            }

            if(!match(lhsNode, rhsNode, path)) {
                return false;
            }
        }

        return true;
    }
}

