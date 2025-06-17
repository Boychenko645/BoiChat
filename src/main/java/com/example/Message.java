package com.example;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;
import java.io.StringReader;
import java.io.StringWriter;

import org.xml.sax.InputSource;

public class Message {
    public String sender;
    public String receiver;
    public String text;

    public Message() {
    }

    public Message(String sender, String receiver, String text) {
        this.sender = sender;
        this.receiver = receiver;
        this.text = text;
    }

    public String toXML() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element root = doc.createElement("message");
            doc.appendChild(root);

            Element senderEl = doc.createElement("sender");
            senderEl.appendChild(doc.createTextNode(sender));
            root.appendChild(senderEl);

            Element receiverEl = doc.createElement("receiver");
            receiverEl.appendChild(doc.createTextNode(receiver));
            root.appendChild(receiverEl);

            Element textEl = doc.createElement("text");
            textEl.appendChild(doc.createTextNode(text));
            root.appendChild(textEl);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.getBuffer().toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Message fromXML(String xml) {
        try {
            if (xml == null || xml.trim().isEmpty())
                return null;

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            InputSource is = new InputSource(new StringReader(xml.trim()));
            Document doc = builder.parse(is);

            Node messageNode = doc.getElementsByTagName("message").item(0);
            if (messageNode == null)
                return null;

            String sender = getTextContent(messageNode, "sender");
            String receiver = getTextContent(messageNode, "receiver");
            String text = getTextContent(messageNode, "text");

            return new Message(sender, receiver, text);

        } catch (Exception e) {
            System.err.println("Не вдалося розпарсити XML: " + xml);
            e.printStackTrace();
            return null;
        }
    }

    private static String getTextContent(Node node, String tag) {
        NodeList list = ((Element) node).getElementsByTagName(tag);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent();
        }
        return "";
    }
}
