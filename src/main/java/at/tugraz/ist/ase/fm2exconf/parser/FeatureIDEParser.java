/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.parser;

import at.tugraz.ist.ase.fm2exconf.core.FeatureModel;
import at.tugraz.ist.ase.fm2exconf.core.Relationship;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A parser for the FeatureIDE format
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class FeatureIDEParser implements BaseParser {

    /**
     * Check whether the format of the given file is FeatureIDE format
     *
     * @param filePath - a {@link File}
     * @@return true - if the format of the given file is FeatureIDE format
     *          false - otherwise
     */
    @Override
    public boolean checkFormat(File filePath) {
        return isFeatureIDE(filePath);
    }

    /**
     * @return the {@link FMFormat} of the parser
     */
    @Override
    public FMFormat getFormat() {
        return FMFormat.FEATUREIDE;
    }

    /**
     * Check whether the format of the given file is FeatureIDE format
     *
     * @param filePath - a {@link File}
     * @return true - if the format of the given file is FeatureIDE format
     *         false - otherwise
     */
    private boolean isFeatureIDE(File filePath) {
        if (filePath == null) return false;
        // first, check the extension of file
        if (!filePath.getName().endsWith(".xml")) {
            return false;
        }
        // second, check the structure of file
        try {
            // read the file
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(filePath.toString());
            Element rootEle = doc.getDocumentElement();

            // if it has three tag "featureModel", "struct" and "constraints"
            if (rootEle.getTagName().equals("featureModel") &&
                    rootEle.getElementsByTagName("struct").getLength() > 0 &&
                    rootEle.getElementsByTagName("constraints").getLength() > 0) {
                return true;
            }
        } catch (SAXException | IOException | ParserConfigurationException e) {
            return false; // if it raise an exception, it's not FeatureIDE format
        }
        return false;
    }

    /**
     * This function parse the given {@link File} into a {@link FeatureModel}.
     *
     * @param filePath - a {@link File}
     * @return a {@link FeatureModel}
     * @throws ParserException when error occurs in parsing
     */
    @Override
    public FeatureModel parse(File filePath) throws ParserException {
        if (filePath == null) throw new ParserException("filePath cannot be empty!");
        if (!isFeatureIDE(filePath)) throw new ParserException("The format of file is not a FeatureIDE format or there exist errors in the file!");

        FeatureModel featureModel;
        try {
            // read XML file
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(filePath.toString());
            doc.getDocumentElement().normalize();
            Element rootEle = doc.getDocumentElement();

            // create the feature model
            featureModel = new FeatureModel(getFormat(), filePath);

            convertStructNodes(rootEle, featureModel);

            if (featureModel.getNumOfFeatures() == 0) {
                throw new ParserException("Couldn't parse any features in the feature model file!");
            }

            convertConstraintNodes(rootEle, featureModel);
        } catch (Exception ex) {
            throw new ParserException(ex.getMessage());
        }

        return featureModel;
    }

    /**
     * Take the "struct" node and convert its child nodes into features
     * and relationships in the {@link FeatureModel}.
     *
     * @param rootEle - a XML root element
     * @param fm - a {@link FeatureModel}
     */
    private void convertStructNodes(Element rootEle, FeatureModel fm) throws ParserException {
        if (rootEle == null) return;

        NodeList struct = rootEle.getElementsByTagName("struct");

        examineAStructNode(struct.item(0), fm);
    }

    /**
     * Examine a XML node to convert child nodes into features, and relationships
     * of a {@link FeatureModel}.
     *
     * @param node - a XML node
     * @param fm - a {@link FeatureModel}
     * @throws ParserException
     */
    private void examineAStructNode(Node node, FeatureModel fm) throws ParserException {
        try {
            NodeList children = node.getChildNodes();
            Element parentElement = (Element) node;
            // take children names
            ArrayList<String> childrenName = getChildrenName(node);
            fm.addFeatures(childrenName.toArray(new String[0]));

            // convert relationships
            if (!node.getNodeName().equals("struct")) {
                // relationships
                String leftSide;
                ArrayList<String> rightSide;

                switch (node.getNodeName()) {
                    case "and":
                        for (int i = 0; i < children.getLength(); i++) {
                            Node child = children.item(i);

                            if (isCorrectNode(child)) {
                                Element childElement = (Element) child;

                                if (childElement.getAttribute("mandatory").equals("true")) {
                                    // MANDATORY
                                    leftSide = parentElement.getAttribute("name");

                                    rightSide = new ArrayList<>();
                                    rightSide.add(childElement.getAttribute("name"));

                                    fm.addRelationship(Relationship.RelationshipType.MANDATORY,
                                            leftSide,
                                            rightSide.toArray(new String[0]));
                                } else {
                                    // OPTIONAL
                                    leftSide = childElement.getAttribute("name");
                                    rightSide = new ArrayList<>();
                                    rightSide.add(parentElement.getAttribute("name"));

                                    fm.addRelationship(Relationship.RelationshipType.OPTIONAL,
                                            leftSide,
                                            rightSide.toArray(new String[0]));
                                }
                            }
                        }
                        break;
                    case "or":
                        leftSide = parentElement.getAttribute("name");
                        rightSide = getChildrenName(node);

                        fm.addRelationship(Relationship.RelationshipType.OR,
                                leftSide,
                                rightSide.toArray(new String[0]));
                        break;
                    case "alt":
                        leftSide = parentElement.getAttribute("name");
                        rightSide = getChildrenName(node);

                        fm.addRelationship(Relationship.RelationshipType.ALTERNATIVE,
                                leftSide,
                                rightSide.toArray(new String[0]));
                        break;
                }
            }

            // examine sub-nodes
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (isCorrectNode(child)) {
                    examineAStructNode(child, fm);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // TODO: log
            throw new ParserException(e.getMessage());
        }
    }

    /**
     * Take the names of child nodes of a XML node.
     *
     * @param node - a XML node
     * @return an array of names of child nodes of a given XML node
     */
    private ArrayList<String> getChildrenName(Node node) throws ParserException {
        NodeList children = node.getChildNodes();
        ArrayList<String> names = new ArrayList<>();

        for (int i = 0; i < children.getLength(); i++)
        {
            Node child = children.item(i);
            if (isCorrectNode(child)) {
                Element childElement = (Element) child;

                String name = childElement.getAttribute("name");
                if (name.isEmpty())
                {
                    throw new ParserException("The feature name could not be blank!");
                }
                if (!name.matches("[a-zA-Z_][0-9a-zA-Z_\\s\\-]*")
                    || name.matches("[a-zA-Z]{1,3}\\$?[0-9]{1,7}(\\:?\\$?[a-zA-Z]{1,3}\\$?[0-9]{1,7})?")
                    || name.length() > 255) {
                    StringBuilder st = new StringBuilder("\"" + name + "\" is a wrong name!");
                    st.append("\n").append("The first character of a feature name must be a letter, or an underscore character (_).");
                    st.append("\n").append("Remaining characters in the feature name can be letters, numbers, periods and underscore characters.");
                    st.append("\n").append("Names cannot be the same as a cell reference, such as ABC100 or Z$100 or R1C1.");
                    st.append("\n").append("A name can contain up to 255 characters.");

                    st.append("\n\n").append("Invalid name: " + name);

                    throw new ParserException(st.toString());
                }

                names.add(name);
            }
        }

        return names;
    }

    /**
     * Check whether a {@link Node} is a Element node
     * and the node name is "and" or "or" or "alt" or "feature"
     *
     * @param node - a {@link Node}
     * @return true if it's correct, false otherwise
     */
    private boolean isCorrectNode(Node node) throws ParserException {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            if (node.getNodeName().equals("graphics")) {
                return false;
            }
            if (!node.getNodeName().equals("and")
                && !node.getNodeName().equals("or")
                && !node.getNodeName().equals("alt")
                && !node.getNodeName().equals("feature")) {
                throw new ParserException("\"" + node.getNodeName() + "\" is not a feature model's relationship!");
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Take "rule" nodes and convert them into constraints in {@link FeatureModel}.
     *
     * @param rootEle - the root element
     * @param fm - a {@link FeatureModel}
     */
    private void convertConstraintNodes(Element rootEle, FeatureModel fm) throws ParserException {
        if (rootEle == null) return;

        NodeList constraints = rootEle.getElementsByTagName("constraints");

        NodeList rules = constraints.item(0).getChildNodes();

        for (int i = 0; i < rules.getLength(); i++) {
            Node node = rules.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String nodename = node.getNodeName();
                if (nodename.equals("rule")) {
                    examineARuleNode(node, fm);
                } else {
                    throw new ParserException("Tag name \"" + nodename + "\" must be \"rule\"!");
                }
            }
        }
    }

    /**
     * Examine a "rule" node to convert into a constraint
     *
     * @param node - an XML node
     * @param fm - a {@link FeatureModel}
     * @throws ParserException
     */
    private void examineARuleNode(Node node, FeatureModel fm) throws ParserException {
        try {
            if (node.getChildNodes().getLength() < 3)
                throw new ParserException("Missing an \"imp\" or a \"disj\" tag in the \"rule\" constraint!");
            if (node.getChildNodes().getLength() > 3)
                throw new ParserException("Excess \"imp\" or \"disj\" tags in the \"rule\" constraint!");

            Node n = node.getChildNodes().item(1);

            String left;
            ArrayList<String> rightSideList = new ArrayList<>();

            switch (n.getNodeName()) {
                case "imp":
                    if (n.getChildNodes().getLength() < 4)
                        throw new ParserException("Missing \"var\" tags in the \"imp\" constraint!");
                    if (n.getChildNodes().getLength() > 5)
                        throw new ParserException("Excess \"var\" tags in the \"imp\" constraint!");
                    if (!n.getChildNodes().item(1).getNodeName().equals("var"))
                        throw new ParserException("Tag name \"" + n.getChildNodes().item(1).getNodeName() + "\" must be \"var\"!");
                    if (!n.getChildNodes().item(3).getNodeName().equals("var"))
                        throw new ParserException("Tag name \"" + n.getChildNodes().item(3).getNodeName() + "\" must be \"var\"!");

                    left = n.getChildNodes().item(1).getTextContent();
                    rightSideList.add(n.getChildNodes().item(3).getTextContent());

                    fm.addConstraint(Relationship.RelationshipType.REQUIRES,
                            left,
                            rightSideList.toArray(new String[0]));
                    break;
                case "disj":
                    NodeList n1 = n.getChildNodes();

                    if (n1.getLength() < 4)
                        throw new ParserException("Missing \"not\" tags in the \"disj\" constraint!");
                    if (n1.getLength() > 5)
                        throw new ParserException("Excess \"not\" tags in the \"disj\" constraint!");
                    if (!n1.item(1).getNodeName().equals("not"))
                        throw new ParserException("Tag name \"" + n.getChildNodes().item(1).getNodeName() + "\" must be \"not\"!");
                    if (!n1.item(3).getNodeName().equals("not"))
                        throw new ParserException("Tag name \"" + n.getChildNodes().item(3).getNodeName() + "\" must be \"not\"!");
                    if (n1.item(1).getChildNodes().getLength() < 3)
                        throw new ParserException("Missing \"var\" tags in the \"disj\" constraint!");
                    if (n1.item(1).getChildNodes().getLength() > 3)
                        throw new ParserException("Excess \"var\" tags in the \"disj\" constraint!");
                    if (n1.item(3).getChildNodes().getLength() < 3)
                        throw new ParserException("Missing \"var\" tags in the \"disj\" constraint!");
                    if (n1.item(3).getChildNodes().getLength() > 3)
                        throw new ParserException("Excess \"var\" tags in the \"disj\" constraint!");
                    if (!n1.item(1).getChildNodes().item(1).getNodeName().equals("var"))
                        throw new ParserException("Tag name \"" + n1.item(1).getChildNodes().item(1).getNodeName() + "\" must be \"var\"!");
                    if (!n1.item(3).getChildNodes().item(1).getNodeName().equals("var"))
                        throw new ParserException("Tag name \"" + n1.item(3).getChildNodes().item(1).getNodeName() + "\" must be \"var\"!");

                    left = n1.item(1).getChildNodes().item(1).getTextContent();
                    rightSideList.add(n1.item(3).getChildNodes().item(1).getTextContent());

                    fm.addConstraint(Relationship.RelationshipType.EXCLUDES,
                            left,
                            rightSideList.toArray(new String[0]));
                    break;
                default:
                    throw new ParserException("\"" + n.getNodeName() + "\" is an wrong name for constraints!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ParserException(e.getMessage());
        }
    }
}
