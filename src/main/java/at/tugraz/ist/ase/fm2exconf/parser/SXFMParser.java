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
import constraints.BooleanVariable;
import constraints.PropositionalFormula;
import fm.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A parser for the SPLOT format
 *
 * Using "fmapi" library of Generative Software Development Lab (http://gsd.uwaterloo.ca/)
 * University of Waterloo
 * Waterloo, Ontario, Canada
 *
 * For further details of this library, we refer to http://52.32.1.180:8080/SPLOT/sxfm.html
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class SXFMParser implements BaseParser {

    /**
     * Check whether the format of the given file is SPLOT format
     *
     * @param filePath - a {@link File}
     * @@return true - if the format of the given file is SPLOT format
     *          false - otherwise
     */
    @Override
    public boolean checkFormat(File filePath) {
        return isSXFM(filePath);
    }

    /**
     * @return the {@link FMFormat} of the parser
     */
    @Override
    public FMFormat getFormat() {
        return FMFormat.SXFM;
    }

    /**
     * Check whether the format of the given file is SPLOT format
     *
     * @param filePath - a {@link File}
     * @return true - if the format of the given file is SPLOT format
     *         false - otherwise
     */
    private boolean isSXFM(File filePath) {
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

            // if it has three tag "feature_model", "feature_tree" and "constraints"
            if (rootEle.getTagName().equals("feature_model") &&
                    rootEle.getElementsByTagName("feature_tree").getLength() > 0 &&
                    rootEle.getElementsByTagName("constraints").getLength() > 0) {
                return true;
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return false; // if it raise an exception, it's not SPLOT format
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
        if (!isSXFM(filePath)) throw new ParserException("The format of file is not SXFM format or there exists errors in the file!");

        FeatureModel featureModel;
        try {
            fm.FeatureModel sxfm = new XMLFeatureModel(filePath.toString(), XMLFeatureModel.USE_VARIABLE_NAME_AS_ID);

            // Load the XML file and create
            sxfm.loadModel();

            // create the feature model
            featureModel = new FeatureModel(getFormat(), filePath);
            // convert features
            ArrayList<String> features = convertFeatures(sxfm);
            featureModel.addFeatures(features.toArray(new String[0]));

            if (featureModel.getNumOfFeatures() == 0) {
                throw new ParserException("Couldn't parse any features in the feature model file!");
            }

            // convert relationships
            convertRelationships(sxfm, featureModel);

            // convert constraints
            convertConstraints(sxfm, featureModel);
        } catch (FeatureModelException | ParserException | at.tugraz.ist.ase.fm2exconf.core.FeatureModelException ex) {
            throw new ParserException(ex.getMessage());
        }

        return featureModel;
    }

    /**
     * Iterate nodes to take features.
     *
     * @param sxfm - a {@link FeatureModel}
     * @return an array of feature names
     */
    private ArrayList<String> convertFeatures(fm.FeatureModel sxfm) throws ParserException {
        if (sxfm == null) return null;

        ArrayList<String> features = new ArrayList<>();
        Queue<FeatureTreeNode> queue = new LinkedList<>();
        queue.add(sxfm.getRoot());

        FeatureTreeNode node = queue.remove();
        while (node != null) {
            if ((node instanceof RootNode)
                    || (node instanceof SolitaireFeature)
                    || (node instanceof GroupedFeature)) {
                String name = node.getName();

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

                features.add(node.getName());
            }

            for( int i = 0 ; i < node.getChildCount() ; i++ ) {
                FeatureTreeNode child = (FeatureTreeNode) node.getChildAt(i);
                queue.add(child);
            }

            if (queue.isEmpty()) {
                break;
            } else {
                node = queue.remove();
            }
        }

        return features;
    }

    /**
     * Iterate nodes to take the relationships between features.
     *
     * @param sxfm - a {@link FeatureModel}
     * @param featureModel - a {@link FeatureModel}
     * @throws ParserException
     */
    private void convertRelationships(fm.FeatureModel sxfm, FeatureModel featureModel) throws ParserException {
        if (sxfm == null) return;

        try {
            Queue<FeatureTreeNode> queue = new LinkedList<>();
            queue.add(sxfm.getRoot());

            FeatureTreeNode node = queue.remove();
            while (node != null) {
                String leftSide;
                ArrayList<String> rightSide;

                if (node instanceof SolitaireFeature) {
                    if (((SolitaireFeature) node).isOptional()) { // OPTIONAL
                        leftSide = node.getName();
                        rightSide = new ArrayList<>();
                        rightSide.add(((FeatureTreeNode) node.getParent()).getName());

                        featureModel.addRelationship(Relationship.RelationshipType.OPTIONAL,
                                leftSide,
                                rightSide.toArray(new String[0]));
                    } else { // MANDATORY
                        leftSide = ((FeatureTreeNode) node.getParent()).getName();
                        rightSide = new ArrayList<>();
                        rightSide.add(node.getName());

                        featureModel.addRelationship(Relationship.RelationshipType.MANDATORY,
                                leftSide,
                                rightSide.toArray(new String[0]));
                    }
                } else if (node instanceof FeatureGroup) {
                    if (((FeatureGroup) node).getMax() == 1) { // ALTERNATIVE
                        leftSide = ((FeatureTreeNode) node.getParent()).getName();
                        rightSide = getChildrenName(node);

                        featureModel.addRelationship(Relationship.RelationshipType.ALTERNATIVE,
                                leftSide,
                                rightSide.toArray(new String[0]));
                    } else { // OR
                        leftSide = ((FeatureTreeNode) node.getParent()).getName();
                        rightSide = getChildrenName(node);

                        featureModel.addRelationship(Relationship.RelationshipType.OR,
                                leftSide,
                                rightSide.toArray(new String[0]));
                    }
                }

                for (int i = 0; i < node.getChildCount(); i++) {
                    FeatureTreeNode child = (FeatureTreeNode) node.getChildAt(i);
                    queue.add(child);
                }

                if (queue.isEmpty()) {
                    break;
                } else {
                    node = queue.remove();
                }
            }
        } catch (Exception e) {
            throw new ParserException("There exists errors in the feature model file!");
        }
    }

    /**
     * Convert constraints on the file into constraints in {@link FeatureModel}
     *
     * @param sxfm - a {@link FeatureModel}
     * @param featureModel - a {@link FeatureModel}
     * @throws ParserException
     */
    private void convertConstraints(fm.FeatureModel sxfm, FeatureModel featureModel) throws ParserException {
        if (sxfm == null) return;

        try {
            for (PropositionalFormula formula : sxfm.getConstraints()) {
                BooleanVariable[] variables = formula.getVariables().toArray(new BooleanVariable[0]);

                if (variables.length != 2) {
                    throw new ParserException(formula.toString() + " is not supported constraints!");
                }

                BooleanVariable leftSide = variables[0];
                BooleanVariable rightSide = variables[1];

                // take type
                Relationship.RelationshipType type = Relationship.RelationshipType.REQUIRES;
                if ((leftSide.isPositive() && !rightSide.isPositive())
                        || ((!leftSide.isPositive() && rightSide.isPositive()))) { // REQUIRES
                    type = Relationship.RelationshipType.REQUIRES;
                } else if (!leftSide.isPositive() && !rightSide.isPositive()) { // EXCLUDES
                    type = Relationship.RelationshipType.EXCLUDES;
                } else {
                    throw new ParserException(formula.toString() + " is not supported constraints!");
                }

                String left;
                ArrayList<String> rightSideList = new ArrayList<>();
                if (!rightSide.isPositive()) {
                    // take rightSide
                    left = sxfm.getNodeByID(rightSide.getID()).getName();
                    rightSideList.add(sxfm.getNodeByID(leftSide.getID()).getName());
                } else {
                    // take leftSide
                    left = sxfm.getNodeByID(leftSide.getID()).getName();
                    // take rightSide
                    rightSideList.add(sxfm.getNodeByID(rightSide.getID()).getName());
                }

                featureModel.addConstraint(type,
                        left,
                        rightSideList.toArray(new String[0]));
            }
        } catch (ParserException e) {
            throw new ParserException(e.getMessage());
        } catch (Exception e) {
            throw new ParserException("There exists errors in the feature model file!");
        }
    }

    /**
     * Get an array of names of child features.
     *
     * @param node - a node {@link FeatureTreeNode}
     * @return an array of names of child features.
     */
    private ArrayList<String> getChildrenName(FeatureTreeNode node) throws ParserException {
        if (node == null) return null;

        ArrayList<String> names = new ArrayList<>();
        for( int i = 0 ; i < node.getChildCount() ; i++ ) {
            FeatureTreeNode child = (FeatureTreeNode )node.getChildAt(i);

            String name = child.getName();
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
        return names;
    }
}
