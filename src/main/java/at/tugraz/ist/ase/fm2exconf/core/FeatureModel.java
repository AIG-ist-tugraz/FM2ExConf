/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2020-2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.core;

import at.tugraz.ist.ase.fm2exconf.parser.FMFormat;

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;

import static at.tugraz.ist.ase.fm2exconf.core.Utilities.isExistInArrayList;

/**
 * Represents a feature model
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class FeatureModel {
    private String version = "1.0";

    private FMFormat sourceFormat;
    private File sourceFilePath;

    private ArrayList<Feature> bfFeatures; // breadth-first order
    private ArrayList<Feature> dfFeatures; // depth-first order
    private ArrayList<Relationship> relationships;
    private ArrayList<Relationship> constraints;

    private boolean consistency;

    // converting options
    public enum FEATURE_ORDER {BF, DF}
    private FEATURE_ORDER featureOrder;
    public enum FEATURE_STATE_TYPE {BINARY, LOGIC}
    private FEATURE_STATE_TYPE featureStateType;
    private boolean pricing;

    public FeatureModel() {
        bfFeatures = new ArrayList<Feature>();
        dfFeatures = new ArrayList<Feature>();
        relationships = new ArrayList<Relationship>();
        constraints = new ArrayList<Relationship>();
        consistency = false;
        sourceFormat = FMFormat.NONE;
        sourceFilePath = null;

        featureOrder = FEATURE_ORDER.BF;
        featureStateType = FEATURE_STATE_TYPE.BINARY;
        pricing = false;
    }

    public FeatureModel(FMFormat format, File filePath){
        bfFeatures = new ArrayList<Feature>();
        dfFeatures = new ArrayList<Feature>();
        relationships = new ArrayList<Relationship>();
        constraints = new ArrayList<Relationship>();
        consistency = false;
        sourceFormat = format;
        sourceFilePath = filePath;

        featureOrder = FEATURE_ORDER.BF;
        featureStateType = FEATURE_STATE_TYPE.BINARY;
        pricing = false;
    }

    public FeatureModel(ArrayList<Feature> bfFeatures,
                        ArrayList<Relationship> relationships,
                        ArrayList<Relationship> constraints,
                        FMFormat format,
                        File filePath) {
        this.bfFeatures = bfFeatures;
        this.relationships = relationships;
        this.constraints = constraints;
        consistency = false;
        sourceFormat = format;
        sourceFilePath = filePath;

        featureOrder = FEATURE_ORDER.BF;
        featureStateType = FEATURE_STATE_TYPE.BINARY;
        pricing = false;
    }

    public FEATURE_ORDER getFeatureOrder() {
        return featureOrder;
    }

    public void setFeatureOrder(FEATURE_ORDER featureOrder) {
        this.featureOrder = featureOrder;
    }

    public FEATURE_STATE_TYPE getFeatureStateType() {
        return featureStateType;
    }

    public void setFeatureStateType(FEATURE_STATE_TYPE type) {
        this.featureStateType = type;
    }

    public boolean isPricingSupport() {
        return pricing;
    }

    public void setPricingSupport(boolean isPricingSupport) {
        pricing = isPricingSupport;
    }

    public void setConsistency(boolean consistency) {
        this.consistency = consistency;
    }

    public boolean isConsistency() {
        return consistency;
    }

    public boolean isFM2EXCONFFormat() {
        return sourceFormat == FMFormat.DESCRIPTIVE;
    }

    public FMFormat getSourceFormat() {
        return sourceFormat;
    }

    public File getSourceFilePath() {
        return sourceFilePath;
    }

    public void setSourceFilePath(File sourceFilePath) {
        this.sourceFilePath = sourceFilePath;
    }

    public String getName() {
        return bfFeatures.get(0).getName();
    }

    private boolean isUniqueFeatureName(String name) {
        for (Feature f: bfFeatures) {
            if (f.getName().equals(name)) {
                return false;
            }
        }
        return true;
    }

    public void addFeature(String fname) throws FeatureModelException {
        // Check blank fname
        if (fname.isEmpty()) {
            throw new FeatureModelException("The feature name can't be blank.");
        }

        // Check the existence of fname in the Feature model
        if (!isUniqueFeatureName(fname)) {
            StringBuilder st = new StringBuilder("The feature name " + fname.toUpperCase() + " is used many times in the feature model.");
            st.append("\n\n").append("The feature name must be unique.");

            throw new FeatureModelException(st.toString());
        }

        Feature f = new Feature(fname);
        this.bfFeatures.add(f);
    }

    public void addFeatures(String[] fnames) throws FeatureModelException {
        for (String fname: fnames) {
            addFeature(fname);
        }
    }

    public ArrayList<Feature> getFeatures(FEATURE_ORDER featureOrder) {
        if (featureOrder == FEATURE_ORDER.DF)
            return dfFeatures;
        return bfFeatures;
    }

    public Feature getFeature(int index) {
        if (index >= 0 || index < bfFeatures.size()) {
            return bfFeatures.get(index);
        }
        return null;
    }

    public Feature getFeature(String name) {
        for (Feature f: bfFeatures) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        return null;
    }

    public int getNumOfFeatures() {
        return bfFeatures.size();
    }

    public boolean isMandatoryFeature(Feature feature) {
        for (Relationship r : relationships) {
            switch (r.getType()) {
                case MANDATORY:
                    if (r.belongsToRightSide(feature)) {
                        return true;
                    }
                    break;
            }
        }
        return false;
    }

    public boolean isOptionalFeature(Feature feature) {
        for (Relationship r : relationships) {
            switch (r.getType()) {
                case OPTIONAL:
                    if (r.belongsToLeftSide(feature)) {
                        return true;
                    }
                    break;
                case OR:
                case ALTERNATIVE:
                    if (r.belongsToRightSide(feature)) {
                        return true;
                    }
                    break;
            }
        }
        return false;
    }

    public ArrayList<Feature> getLeftSideOfRequiresConstraint(Feature rightSide) {
        ArrayList<Feature> parents = new ArrayList<>();
        for (Relationship r : constraints) {
            if (r.getType() == Relationship.RelationshipType.REQUIRES) {
                if (r.belongsToRightSide(rightSide)) {
                    String left = r.getLeftSide();
                    Feature parent = getFeature(left);
                    if (parent != null) {
                        parents.add(parent);
                    }
                }
            }
        }
        return parents;
    }

    public ArrayList<Feature> getRightSideOfRelationships(Feature leftSide) {
        ArrayList<Feature> children = new ArrayList<>();
        for (Relationship r : relationships) {
            if (r.getType() == Relationship.RelationshipType.OPTIONAL) {
                if (r.belongsToRightSide(leftSide)) {
                    String left = r.getLeftSide();
                    Feature parent = getFeature(left);
                    if (parent != null) {
                        children.add(parent);
                    }
                }
            } else {
                if (r.belongsToLeftSide(leftSide)) {
                    ArrayList<String> rightSide = r.getRightSide();
                    for (String right : rightSide) {
                        Feature child = getFeature(right);
                        if (child != null) {
                            children.add(child);
                        }
                    }
                }
            }
        }
        return children;
    }

    private ArrayList<Relationship> getRelationshipsWith(Feature feature) {
        ArrayList<Relationship> rs = new ArrayList<>();
        for (Relationship r : relationships) {
            if (r.belongsToRightSide(feature) || r.belongsToLeftSide(feature)) {
                rs.add(r);
            }
        }
        for (Relationship r : constraints) {
            if (r.belongsToRightSide(feature) || r.belongsToLeftSide(feature)) {
                rs.add(r);
            }
        }
        return rs;
    }

    public ArrayList<Feature> getMandatoryParents(Feature rightSide) throws FeatureModelException {
        ArrayList<Feature> parents = new ArrayList<>();

        ArrayList<Relationship> relationships = getRelationshipsWith(rightSide);
        for (Relationship r : relationships) {
            ArrayList<String> parentsqueue = new ArrayList<>();
            if (r.getType() == Relationship.RelationshipType.REQUIRES) {
                if (r.belongsToRightSide(rightSide)) {
                    parentsqueue.add(rightSide.toString());
                    getMandatoryParent(r, rightSide, parents, parentsqueue);
                }
            } else if (r.getType() == Relationship.RelationshipType.ALTERNATIVE
                    || r.getType() == Relationship.RelationshipType.OR) {
                if (r.belongsToRightSide(rightSide) || r.belongsToLeftSide(rightSide)) {
                    parentsqueue.add(rightSide.toString());
                    getMandatoryParent(r, rightSide, parents, parentsqueue);
                }
            }
        }

        return parents;
    }

    public void getMandatoryParent(Relationship r, Feature feature, ArrayList<Feature> parents, ArrayList<String> parentsqueue) throws FeatureModelException {
        if (feature.toString().equals(this.getName())) return;

        if (r.getType() == Relationship.RelationshipType.REQUIRES) {
            String left = r.getLeftSide();
            Feature parent = getFeature(left);

            if (parent.getName().equals(this.getName())) return;
            if (isExistInArrayList(parentsqueue,left)) return;

            if (this.isMandatoryFeature(parent)) {
                if (!parents.contains(parent)) {
                    parents.add(parent);
                }
            } else {

                ArrayList<Relationship> relationships = getRelationshipsWith(parent);
                for (Relationship r1 : relationships) {
                    if (r1.getType() == Relationship.RelationshipType.REQUIRES) {
                        if (r1.belongsToRightSide(parent)) {
                            parentsqueue.add(left);
                            getMandatoryParent(r1, parent, parents, parentsqueue);
                            parentsqueue.remove(parentsqueue.size() - 1);
                        }
                    } else if (r1.getType() == Relationship.RelationshipType.ALTERNATIVE
                            || r1.getType() == Relationship.RelationshipType.OR) {
                        if (r1.belongsToRightSide(parent) || r1.belongsToLeftSide(parent)) {
                            parentsqueue.add(left);
                            getMandatoryParent(r1, parent, parents, parentsqueue);
                            parentsqueue.remove(parentsqueue.size() - 1);
                        }
                    }
                }
            }
        } else if (r.getType() == Relationship.RelationshipType.ALTERNATIVE
                || r.getType() == Relationship.RelationshipType.OR) {
            if (r.belongsToRightSide(feature)) {
                String left = r.getLeftSide();
                Feature parent = getFeature(left);

                if (parent.getName().equals(this.getName())) return;
                if (isExistInArrayList(parentsqueue,left)) return;

                if (this.isMandatoryFeature(parent)) {
                    if (!parents.contains(parent)) {
                        parents.add(parent);
                    }
                } else {
                    ArrayList<Relationship> relationships = getRelationshipsWith(parent);
                    for (Relationship r1 : relationships) {
                        if (r1.getType() == Relationship.RelationshipType.REQUIRES) {
                            if (r1.belongsToRightSide(parent)) {
                                parentsqueue.add(left);
                                getMandatoryParent(r1, parent, parents, parentsqueue);
                                parentsqueue.remove(parentsqueue.size() - 1);
                            }
                        } else if (r1.getType() == Relationship.RelationshipType.ALTERNATIVE
                                || r1.getType() == Relationship.RelationshipType.OR) {
                            if (r1.belongsToRightSide(parent) || r1.belongsToLeftSide(parent)) {
                                parentsqueue.add(left);
                                getMandatoryParent(r1, parent, parents, parentsqueue);
                                parentsqueue.remove(parentsqueue.size() - 1);
                            }
                        }
                    }
                }
            } else if (r.belongsToLeftSide(feature)) {
                ArrayList<String> lefts = r.getRightSide();
                for (String left: lefts) {

                    if (isExistInArrayList(parentsqueue,left)) return;

                    Feature parent = getFeature(left);
                    if (parent.getName().equals(this.getName())) return;

                    if (this.isMandatoryFeature(parent)) {
                        if (!parents.contains(parent)) {
                            parents.add(parent);
                        }
                    } else {
                        ArrayList<Relationship> relationships = getRelationshipsWith(parent);
                        for (Relationship r1 : relationships) {
                            if (r1.getType() == Relationship.RelationshipType.REQUIRES) {
                                if (r1.belongsToRightSide(parent)) {
                                    parentsqueue.add(left);
                                    getMandatoryParent(r1, parent, parents, parentsqueue);
                                    parentsqueue.remove(parentsqueue.size() - 1);
                                }
                            } else if (r1.getType() == Relationship.RelationshipType.ALTERNATIVE
                                    || r1.getType() == Relationship.RelationshipType.OR) {
                                if (r1.belongsToRightSide(parent) || r1.belongsToLeftSide(parent)) {
                                    parentsqueue.add(left);
                                    getMandatoryParent(r1, parent, parents, parentsqueue);
                                    parentsqueue.remove(parentsqueue.size() - 1);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void addRelationship(Relationship.RelationshipType type, String leftSide, String[] rightSide) {
        ArrayList<String> rightSideAL = convertArray2ArrayList(rightSide);

        Relationship r = new Relationship(type, leftSide, rightSideAL);
        this.relationships.add(r);
    }

    public ArrayList<Relationship> getRelationships() {
        return relationships;
    }

    public Relationship getRelationship(String constraint) {
        for (Relationship relationship: relationships) {
            if (relationship.isExist(constraint))
                return relationship;
        }
        for (Relationship relationship: constraints) {
            if (relationship.isExist(constraint))
                return relationship;
        }
        return null;
    }

    public int getNumOfRelationships() {
        return relationships.size();
    }

    public int getNumOfRelationships(Relationship.RelationshipType type) {
        int count = 0;
        if (type == Relationship.RelationshipType.REQUIRES || type == Relationship.RelationshipType.EXCLUDES) {
            for (Relationship relationship : constraints) {
                if (relationship.isType(type)) {
                    count++;
                }
            }
        } else {
            for (Relationship relationship : relationships) {
                if (relationship.isType(type)) {
                    count++;
                }
            }
        }
        return count;
    }

    public void addConstraint(Relationship.RelationshipType type, String leftSide, String[] rightSide) {
        ArrayList<String> rightSideAL = convertArray2ArrayList(rightSide);

        Relationship r = new Relationship(type, leftSide, rightSideAL);
        this.constraints.add(r);
    }

    public ArrayList<Relationship> getConstraints() {
        return constraints;
    }

    public int getNumOfConstraints() {
        return constraints.size();
    }

    // duoc su dung de hien thi trong titledPaneDetails
    @Override
    public String toString() {
        if (bfFeatures.isEmpty()) return "";

        StringBuilder st = new StringBuilder();

        st.append("FEATURES:\n");
        for (Feature feature : bfFeatures) {
            st.append(String.format("\t%s\n", feature));
        }

        st.append("RELATIONSHIPS:\n");
        for (Relationship relationship: relationships) {
            st.append(String.format("\t%s\n", relationship.getConfRule()));
        }

        st.append("CONSTRAINTS:\n");
        for (Relationship constraint: constraints) {
            st.append(String.format("\t%s\n", constraint.getConfRule()));
        }

        return st.toString();
    }

    public String getFM4ConfFormat() {
        StringBuilder st = new StringBuilder("FM4Conf-v" + version + "\n");

        st.append("MODEL:\n");

        st.append(this.getName() + "\n");

        st.append("FEATURES:\n");
        for (Feature feature : bfFeatures) {
            st.append(String.format("%s,\n", feature));
        }
        st.delete(st.length() - 2, st.length() - 1);

        st.append("RELATIONSHIPS:\n");
        for (Relationship relationship: relationships) {
            st.append(String.format("%s,\n", relationship.getConfRule()));
        }
        st.delete(st.length() - 2, st.length() - 1);

        st.append("CONSTRAINTS:\n");
        for (Relationship constraint: constraints) {
            st.append(String.format("%s,\n", constraint.getConfRule()));
        }
        st.delete(st.length() - 2, st.length() - 1);

        return st.toString();
    }

    private ArrayList<String> convertArray2ArrayList(String[] arrStr) {
        ArrayList<String> arrList = new ArrayList<String>();
        for(String name: arrStr) {
            arrList.add(name);
        }
        return arrList;
    }

    public void buildDepthFirstFeatures() {
        // Root feature
        Feature rootfeature = getFeature(getName());

        // Use a stack to store features
        Stack stack = new Stack();

        // Push root feature to stack
        stack.push(rootfeature);

        while (!stack.empty())
        {
            Feature feature = (Feature) stack.pop();
            dfFeatures.add(feature);

            // get features on the right side of relationships in which the feature is in the left side
            ArrayList<Feature> rightSide = getRightSideOfRelationships(feature);

            for (int i = rightSide.size() - 1; i >= 0; i--) {
                stack.push(rightSide.get(i));
            }
        }
    }
}
