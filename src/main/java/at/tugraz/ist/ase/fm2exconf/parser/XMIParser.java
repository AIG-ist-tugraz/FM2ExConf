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
 * A parser for the XMI format (a format of v.control)
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class XMIParser implements BaseParser {

    Element rootEle = null;

    /**
     * Check whether the format of the given file is v.control format
     *
     * @param filePath - a {@link File}
     * @@return true - if the format of the given file is v.control format
     *          false - otherwise
     */
    @Override
    public boolean checkFormat(File filePath) {
        return isXMI(filePath);
    }

    /**
     * @return the {@link FMFormat} of the parser
     */
    @Override
    public FMFormat getFormat() {
        return FMFormat.XMI;
    }

    /**
     * Check whether the format of the given file is v.control format
     *
     * @param filePath - a {@link File}
     * @return true - if the format of the given file is v.control format
     *         false - otherwise
     */
    private boolean isXMI(File filePath) {
        if (filePath == null) return false;
        // first, check the extension of file
        if (!filePath.getName().endsWith(".xmi")) {
            return false;
        }
        // second, check the structure of file
        try {
            // read the file
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(filePath.toString());
            Element rootEle = doc.getDocumentElement();


            // if it has three tag "xmi:XMI", "models" and "constraints"
            if (rootEle.getTagName().equals("xmi:XMI") &&
                    rootEle.getElementsByTagName("models").getLength() > 0 &&
                    rootEle.getElementsByTagName("constraints").getLength() > 0) {
                return true;
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return false; // if it raise an exception, it's not v.control format
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
        if (!isXMI(filePath)) throw new ParserException("The format of file is not XMI format or there exists errors in the file!");

        FeatureModel featureModel;
        try {
            // read XMI file
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(filePath.toString());
            doc.getDocumentElement().normalize();
            rootEle = doc.getDocumentElement();

            // create the feature model
            featureModel = new FeatureModel(getFormat(), filePath);

            convertModelsNode(rootEle, featureModel);

            if (featureModel.getNumOfFeatures() == 0) {
                throw new ParserException("Couldn't parse any features in the feature model file!");
            }

            convertConstraintsNodes(rootEle, featureModel);

        } catch (ParserConfigurationException | ParserException | SAXException | IOException ex) {
            throw new ParserException(ex.getMessage());
        }

        return featureModel;
    }

    /**
     * Take the "models" node and convert its child nodes into features
     * and relationships in the {@link FeatureModel}.
     *
     * @param rootEle - a XML root element
     * @param fm - a {@link FeatureModel}
     */
    private void convertModelsNode(Element rootEle, FeatureModel fm) throws ParserException {
        if (rootEle == null) return;

        NodeList models = rootEle.getElementsByTagName("models");

        examineModelsNode(models.item(0), fm);
    }

    /**
     * Examine a XML node to convert child nodes into features, and relationships
     * of a {@link FeatureModel}.
     *
     * @param node - a XML node
     * @param fm - a {@link FeatureModel}
     * @throws ParserException
     */
    private void examineModelsNode(Node node, FeatureModel fm) throws ParserException {
        try {
            NodeList children = node.getChildNodes();
            Element parentElement = (Element) node;
            // take children names
            ArrayList<String> childrenName = getChildrenName(node);
            fm.addFeatures(childrenName.toArray(new String[0]));

            // convert relationships
            if (!node.getNodeName().equals("models")) {
                // relationships
                String leftSide;
                ArrayList<String> rightSide;

                switch (parentElement.getAttribute("xsi:type")) {
                    case "com.prostep.vcontrol.model.feature:Feature":
                        for (int i = 0; i < children.getLength(); i++) {
                            Node child = children.item(i);

                            if (isCorrectNode(child)) {
                                Element childElement = (Element) child;

                                if (childElement.getAttribute("optional").equals("false")) {
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
                    case "com.prostep.vcontrol.model.feature:FeatureGroup":
                        if (parentElement.getAttribute("max").isEmpty()) { // ALTERNATIVE
                            leftSide = parentElement.getAttribute("name");
                            rightSide = getChildrenName(node);

                            if (rightSide.size() > 0) {
                                fm.addRelationship(Relationship.RelationshipType.ALTERNATIVE,
                                        leftSide,
                                        rightSide.toArray(new String[0]));
                            }
                        } else { // OR
                            leftSide = parentElement.getAttribute("name");
                            rightSide = getChildrenName(node);

                            if (rightSide.size() > 0) {
                                fm.addRelationship(Relationship.RelationshipType.OR,
                                        leftSide,
                                        rightSide.toArray(new String[0]));
                            }
                        }
                        break;
                    default:
                        throw new ParserException("Missing or Not supported xsi:type.");
                }
            }

            // examine sub-nodes
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (isCorrectNode(child)) {
                    examineModelsNode(child, fm);
                }
            }
        } catch (ParserException e) {
            throw new ParserException(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(); // TODO: log
            throw new ParserException("There exists errors in the feature model file!");
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
            if (node.getNodeName().equals("constraints")) {
                return false;
            }
            if ((!node.getNodeName().equals("rootFeature")
                && !node.getNodeName().equals("children"))) {
                throw new ParserException("\"" + node.getNodeName() + "\" is not a supported tag!");
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Take "constraints" nodes and convert them into constraints in {@link FeatureModel}.
     *
     * @param rootEle - the root element
     * @param fm - a {@link FeatureModel}
     * @throws ParserException
     */
    private void convertConstraintsNodes(Element rootEle, FeatureModel fm) throws ParserException {
        if (rootEle == null) return;

        NodeList constraints = rootEle.getElementsByTagName("constraints");

        for (int i = 0; i < constraints.getLength(); i++) {
            examineAConstraintsNode(constraints.item(i), fm);
        }
    }

    /**
     * Examine a "rule" node to convert into a constraint
     *
     * @param node - an XML node
     * @param fm - a {@link FeatureModel}
     * @throws ParserException
     */
    private void examineAConstraintsNode(Node node, FeatureModel fm) throws ParserException {
        try {
            if (node.getChildNodes().getLength() < 3)
                throw new ParserException("Missing an \"rootTerm\" tag in a \"constraints\" tag!");
            if (node.getChildNodes().getLength() > 3)
                throw new ParserException("Excess \"rootTerm\" tags in a \"constraints\" tag!");
            if (!node.getChildNodes().item(1).getNodeName().equals("rootTerm"))
                throw new ParserException("Tag name \"" + node.getChildNodes().item(1).getNodeName() + "\" must be \"rootTerm\"!");

            Node n = node.getChildNodes().item(1);
            Element ele = (Element) n;

            String left, id, right;
            ArrayList<String> rightSideList = new ArrayList<>();

            Element leftOperand;
            Element rightOperand;

            String type = ele.getAttribute("xsi:type");
            switch (type) {
                case "com.prostep.vcontrol.model.terms:ImpliesTerm":
                    if (n.getChildNodes().getLength() < 4)
                        throw new ParserException("Missing \"operand\" tags in a \"constraints\" tag!");
                    if (n.getChildNodes().getLength() > 5)
                        throw new ParserException("Excess \"operand\" tags in a \"constraints\" tag!");
                    if (!n.getChildNodes().item(1).getNodeName().equals("leftOperand"))
                        throw new ParserException("Tag name \"" + n.getChildNodes().item(1).getNodeName() + "\" must be \"leftOperand\"!");
                    if (!n.getChildNodes().item(3).getNodeName().equals("rightOperand"))
                        throw new ParserException("Tag name \"" + n.getChildNodes().item(3).getNodeName() + "\" must be \"rightOperand\"!");

                    leftOperand = (Element) (n.getChildNodes().item(1));
                    rightOperand = (Element) (n.getChildNodes().item(3));

                    id = leftOperand.getAttribute("element");
                    left = getName(id);

                    if (left.isEmpty()) {
                        throw new ParserException("Missing the \"element\" property or the feature id:\"" + id + "\" doesn't exist.");
                    }

                    id = rightOperand.getAttribute("element");
                    right = getName(id);

                    if (right.isEmpty()) {
                        throw new ParserException("Missing the \"element\" property or the feature id:\"" + id + "\" doesn't exist.");
                    }

                    rightSideList.add(right);

                    fm.addConstraint(Relationship.RelationshipType.REQUIRES,
                            left,
                            rightSideList.toArray(new String[0]));
                    break;
                case "com.prostep.vcontrol.model.terms:ExcludesTerm":
                    if (n.getChildNodes().getLength() < 4)
                        throw new ParserException("Missing \"operand\" tags in a \"constraints\" tag!");
                    if (n.getChildNodes().getLength() > 5)
                        throw new ParserException("Excess \"operand\" tags in a \"constraints\" tag!");
                    if (!n.getChildNodes().item(1).getNodeName().equals("leftOperand"))
                        throw new ParserException("Tag name \"" + n.getChildNodes().item(1).getNodeName() + "\" must be \"leftOperand\"!");
                    if (!n.getChildNodes().item(3).getNodeName().equals("rightOperand"))
                        throw new ParserException("Tag name \"" + n.getChildNodes().item(3).getNodeName() + "\" must be \"rightOperand\"!");

                    leftOperand = (Element) (n.getChildNodes().item(1));
                    rightOperand = (Element) (n.getChildNodes().item(3));

                    id = leftOperand.getAttribute("element");
                    left = getName(id);

                    if (left.isEmpty()) {
                        throw new ParserException("Missing the \"element\" property or the feature id:\"" + id + "\" doesn't exist.");
                    }

                    id = rightOperand.getAttribute("element");
                    right = getName(id);

                    if (right.isEmpty()) {
                        throw new ParserException("Missing the \"element\" property or the feature id:\"" + id + "\" doesn't exist.");
                    }

                    rightSideList.add(right);

                    fm.addConstraint(Relationship.RelationshipType.EXCLUDES,
                            left,
                            rightSideList.toArray(new String[0]));
                    break;
                default:
                    throw new ParserException("\"" + type + "\" is an wrong type for constraints!");
            }
        } catch (ParserException e) {
            throw new ParserException(e.getMessage());
        } catch (Exception e) {
            throw new ParserException("There exists errors in the feature model file!");
        }
    }

    /**
     * Return the name of feature based on the given id.
     *
     * @param id - a given id
     * @return the name of feature or an empty string
     */
    private String getName(String id) {
        if (rootEle == null) return "";

        NodeList nodes = rootEle.getElementsByTagName("rootFeature");

        for (int i = 0; i < nodes.getLength(); i++) {
            if (((Element)nodes.item(i)).getAttribute("id").equals(id)) {
                return ((Element)nodes.item(i)).getAttribute("name");
            }
        }

        nodes = rootEle.getElementsByTagName("children");

        for (int i = 0; i < nodes.getLength(); i++) {
            if (((Element)nodes.item(i)).getAttribute("id").equals(id)) {
                return ((Element)nodes.item(i)).getAttribute("name");
            }
        }

        return "";
    }
}