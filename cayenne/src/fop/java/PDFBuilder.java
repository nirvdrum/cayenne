/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.avalon.framework.logger.NullLogger;
import org.apache.fop.apps.Driver;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A tool that renders Cayenne XML documentation to PDF.
 * 
 * @author Anton Sakalouski
 */
public class PDFBuilder {

    public static final String FO_XSL = "xdocs/stylesheets/fop/genfo.xsl";
    public static final String PANEL_ICON = "xdocs/stylesheets/fop/panel.gif";

    private ByteArrayOutputStream foResult = new ByteArrayOutputStream();
    private String initialPath;
    private DocumentBuilder docBuilder;
    private Document optDoc;
    private ArrayList sections;
    private String title;

    /**
     * Main method of PDFBuilder tool. Takes three arguments - base directory where source
     * XML files are located, the title of the generated PDF and the output PDF filename.
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            throw new RuntimeException("PDFBuilder expects exactly 3 arguments, got "
                    + args.length);
        }

        String baseDir = args[0];
        String title = args[1];
        String outputPDF = args[2];

        try {
            PDFBuilder pdfBuilder = new PDFBuilder(baseDir, title);
            pdfBuilder.buildPDF(new FileOutputStream(outputPDF));
        }
        catch (Exception e) {
            throw new RuntimeException("Error gennerating PDF", e);
        }
    }

    PDFBuilder(String initialPath, String title) throws ParserConfigurationException {
        this.initialPath = initialPath;
        this.title = title;
        this.docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        this.sections = new ArrayList();
    }

    /**
     * Main worker that creates a PDF document, writing it to provided output stream.
     */
    void buildPDF(OutputStream resultStream) throws Exception {
        try {
            createSectionsList(initialPath);
            Collections.sort(sections, new CompareSections());
            createOptimizedDocument();
            buildFO();
            Driver driver = new Driver(new InputSource(new ByteArrayInputStream(foResult
                    .toByteArray())), resultStream);
            driver.setLogger(new NullLogger());
            driver.setRenderer(Driver.RENDER_PDF);
            driver.run();
        }
        finally {
            resultStream.close();
        }
    }

    private void createSectionsList(String initialPath) throws SAXException, IOException {
        File contextDir = new File(initialPath);
        String[] innerName = contextDir.list();

        for (int i = 0; i < innerName.length; i++) {
            File innerContext = new File(initialPath + innerName[i]);
            if (innerContext.isDirectory()) {
                createSectionsList(initialPath + innerName[i] + "/");
            }

            if (innerContext.isFile() && (innerName[i].indexOf(".xml") != -1)) {
                Document document = docBuilder.parse(innerContext);
                NodeList imgNodeList = document.getElementsByTagName("img");
                for (int j = 0; j < imgNodeList.getLength(); j++) {
                    Node imgNode = imgNodeList.item(j);
                    NamedNodeMap imgAttr = imgNode.getAttributes();
                    String relativePath = imgAttr
                            .getNamedItem("src")
                            .getNodeValue()
                            .substring(1);
                    int index = relativePath.indexOf("images");
                    relativePath = relativePath.substring(index);
                    imgAttr.getNamedItem("src").setNodeValue(
                            this.initialPath + "../" + relativePath);
                }

                NodeList panelNodes = document.getElementsByTagName("panel");
                for (int m = 0; m < panelNodes.getLength(); m++) {
                    Element panelNode = (Element) panelNodes.item(m);
                    Attr icon = document.createAttribute("icon");
                    icon.setNodeValue(PANEL_ICON);
                    panelNode.setAttributeNode(icon);
                }

                NodeList nodeList = document.getElementsByTagName("section");
                for (int k = 0; k < nodeList.getLength(); k++)
                    sections.add(nodeList.item(k));
            }
        }
    }

    private void createOptimizedDocument() throws Exception {
        optDoc = docBuilder.newDocument();
        Element root = optDoc.createElement("document");
        Attr titleAttr = optDoc.createAttribute("title");
        titleAttr.setValue(title);
        root.setAttributeNode(titleAttr);
        Element body = optDoc.createElement("body");

        String currentSection = ((Node) sections.get(0)).getNodeName();
        Node currentNode = optDoc.importNode((Node) sections.get(0), true);

        for (int i = 1; i < sections.size(); i++) {
            Node node = optDoc.importNode((Node) sections.get(i), true);
            if (currentSection.equals(node
                    .getAttributes()
                    .getNamedItem("name")
                    .getNodeValue())) {
                NodeList chNodes = node.getChildNodes();
                for (int j = 0; j < chNodes.getLength(); j++) {
                    Node chNode = optDoc.importNode(chNodes.item(j), true);
                    currentNode.appendChild(chNode);
                }
            }
            else {
                body.appendChild(currentNode);
                currentSection = node.getAttributes().getNamedItem("name").getNodeValue();
                currentNode = node;
            }
        }

        root.appendChild(body);
        optDoc.appendChild(root);
    }

    private void buildFO() throws Exception {
        // Andrus: looks like a noop:
        // docBuilder.parse(initialPath + "index.xml");

        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer(new StreamSource(FO_XSL));
        transformer.transform(new DOMSource(optDoc), new StreamResult(foResult));
    }

    private class CompareSections implements Comparator {

        public int compare(Object a, Object b) {
            Node node1 = (Node) a;
            Node node2 = (Node) b;
            NamedNodeMap mainAttr1 = node1.getAttributes();
            NamedNodeMap mainAttr2 = node2.getAttributes();
            String mainName1 = mainAttr1.getNamedItem("name").getNodeValue();
            String mainName2 = mainAttr2.getNamedItem("name").getNodeValue();
            int pos1 = mainName1.trim().indexOf(" ");
            int pos2 = mainName2.trim().indexOf(" ");
            double val1 = 0, val2 = 0;
            try {
                val1 = Double.parseDouble(mainName1.substring(0, pos1));
                val2 = Double.parseDouble(mainName2.substring(0, pos2));
            }
            catch (Exception e) {
            }

            if (!mainName1.equals(mainName2)) {
                if ((val1 - val2) > 0)
                    return 1;
                else
                    return -1;
            }
            NodeList subNodes1 = node1.getChildNodes();
            int i = 0, j = 0;
            for (i = 0; i < subNodes1.getLength(); i++) {
                Node node = subNodes1.item(i);
                if (node.getNodeName().equals("subsection"))
                    break;
            }
            NodeList subNodes2 = node2.getChildNodes();
            for (j = 0; j < subNodes2.getLength(); j++) {
                Node node = subNodes2.item(j);
                if (node.getNodeName().equals("subsection"))
                    break;
            }
            if (i == subNodes1.getLength())
                return 1;
            if (j == subNodes2.getLength())
                return -1;

            NamedNodeMap attr1 = subNodes1.item(i).getAttributes();
            NamedNodeMap attr2 = subNodes2.item(j).getAttributes();
            String name1 = attr1.getNamedItem("name").getNodeValue();
            String name2 = attr2.getNamedItem("name").getNodeValue();

            int spos1 = name1.trim().indexOf(" ");
            int spos2 = name2.trim().indexOf(" ");
            StringTokenizer st1 = new StringTokenizer(name1.substring(0, spos1), ".");
            StringTokenizer st2 = new StringTokenizer(name2.substring(0, spos2), ".");
            int val11 = 0, val22 = 0;
            while (st1.hasMoreTokens() && st2.hasMoreTokens()) {
                try {
                    val11 = Integer.parseInt(st1.nextToken());
                }
                catch (Exception e) {
                    return -1;
                }
                try {
                    val22 = Integer.parseInt(st2.nextToken());
                }
                catch (Exception e) {
                    return 1;
                }
                if (val11 == val22)
                    continue;
                return val11 - val22;
            }
            return 0;
        }
    }
}