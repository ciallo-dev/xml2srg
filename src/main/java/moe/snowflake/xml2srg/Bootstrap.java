package moe.snowflake.xml2srg;


import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;

public class Bootstrap {
    static StringBuilder sb = new StringBuilder();

    static String[] arr = new String[]{"Field", "Method", "Return", "Desc", "Param"};

    public static void main(String[] args) throws Exception {
        readXMLFile(new File("Mapping.xml"));
        writeSRGFile(new File("Mapping.txt"));
    }


    private static void writeSRGFile(File file) throws Exception {
        FileUtils.writeByteArrayToFile(file, sb.toString().getBytes());
    }

    public static void readXMLFile(File file) throws Exception {
        String xmlContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8).trim();

        // xml 初始化
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new InputSource(new StringReader(xmlContent)));
        doc.getDocumentElement().normalize();

        System.out.println("convert started");

        NodeList nList = doc.getElementsByTagName("*");

        // package loop
        for (int i = 0; i < nList.getLength(); i++) {
            Node nNode = nList.item(i);

            if (nNode.getNodeType() != Node.ELEMENT_NODE) continue;

            Element packageChildsElements = (Element) nNode;

            // check package node
            if (!packageChildsElements.getTagName().contains(".")) continue;

            if (packageChildsElements.getTagName().equals("ASMShooterMappingData")) continue;

            NodeList clazzList = packageChildsElements.getElementsByTagName("*");

            // handle class
            for (int j = 0; j < clazzList.getLength(); j++) {
                Node clazzNode = clazzList.item(j);

                if (!isClassNode(clazzNode.getNodeName())) continue;

                String fullName = clazzNode.getAttributes().getNamedItem("FullName").getNodeValue();
                String obfName = clazzNode.getAttributes().getNamedItem("Obscured").getNodeValue();

                // add classes mapping
                sb.append("CL: ").append(obfName).append(" ").append(fullName).append("\n");

                NodeList otherNodes = ((Element) clazzNode).getElementsByTagName("*");

                for (int k = 0; k < otherNodes.getLength(); k++) {
                    Node otherNode = otherNodes.item(k);

                    String nodeName = otherNode.getNodeName();

                    if (nodeName.equals("Return") || nodeName.equals("Param") || nodeName.equals("Desc")) continue;

                    String obscured = otherNode.getAttributes().getNamedItem("Obscured").getNodeValue();
                    String unObscured = otherNode.getAttributes().getNamedItem("Unobscured").getNodeValue();

                    // convert fileds
                    if (nodeName.equals("Field")) {
                        // FD: obfClassName/fieldObfName unObfName/fieldUnObf
                        sb.append("FD: " + obfName).append("/").append(obscured).append(" ").append(fullName).append("/").append(unObscured).append("\n");
                    }

                    if (nodeName.equals("Method")) {
                        // MD: obfClassName/methodObfName desc unObfName/methodUnObf desc
                        String desc = ((Element)otherNode).getElementsByTagName("Desc").item(0).getTextContent();


                        sb.append("MD: " + obfName).append("/").append(obscured)
                                .append(" ")
                                .append(desc)
                                .append(" ")
                                .append(fullName).append("/").append(unObscured)
                                .append(" ")
                                .append(desc)
                                .append("\n");
                    }

                }

            }
        }
    }


    public static boolean isClassNode(String tagName) {
        for (String str : arr)
            if (tagName.equals(str)) return false;

        return true;
    }


}