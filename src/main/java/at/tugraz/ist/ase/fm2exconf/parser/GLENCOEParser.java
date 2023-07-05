/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2020-2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.parser;

import at.tugraz.ist.ase.fm2exconf.core.FeatureModel;
import at.tugraz.ist.ase.fm2exconf.core.Relationship;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A parser for the Glencoe format
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class GLENCOEParser implements BaseParser {

    /**
     * Check whether the format of the given file is Glencoe format
     *
     * @param filePath - a {@link File}
     * @@return true - if the format of the given file is Glencoe format
     *          false - otherwise
     */
    @Override
    public boolean checkFormat(File filePath) {
        return isGLENCOE(filePath);
    }

    /**
     * @return the {@link FMFormat} of the parser
     */
    @Override
    public FMFormat getFormat() {
        return FMFormat.GLENCOE;
    }

    /**
     * Check whether the format of the given file is Glencoe format
     *
     * @param filePath - a {@link File}
     * @return true - if the format of the given file is Glencoe format
     *         false - otherwise
     */
    private boolean isGLENCOE(File filePath) {
        if (filePath == null) return false;
        // first, check the extension of file
        if (!filePath.getName().endsWith(".json")) {
            return false;
        }
        // second, check the structure of file
        try {
            // read the file
            InputStream is = FileUtils.openInputStream(filePath);

            JSONTokener tokener = new JSONTokener(is);
            JSONObject object = new JSONObject(tokener);

            // if it has three object "features", "tree" and "constraints"
            JSONObject features = object.getJSONObject("features");
            JSONObject tree = object.getJSONObject("tree");
            JSONObject constraints = object.getJSONObject("constraints");

            if (features != null && tree != null && constraints != null) {
                return true; // it is Glencoe format
            }
        } catch (Exception e) {
            return false; // if it raise an exception, it's not Glencoe format
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
        if (!isGLENCOE(filePath)) throw new ParserException("The format of file is not Glencoe format or there exists errors in the file!");

        FeatureModel featureModel;
        try {
            InputStream is = FileUtils.openInputStream(filePath);
            if (is == null) {
                throw new NullPointerException("Cannot find resource file " + filePath);
            }

            JSONTokener tokener = new JSONTokener(is);
            JSONObject object = new JSONObject(tokener);

            JSONObject features = object.getJSONObject("features");
            JSONObject tree = object.getJSONObject("tree");
            JSONObject constraints = object.getJSONObject("constraints");

            // create the feature model
            featureModel = new FeatureModel(getFormat(), filePath);

            convertTree(tree, features, featureModel);

            if (featureModel.getNumOfFeatures() == 0) {
                throw new ParserException("Couldn't parse any features in the feature model file!");
            }

            convertConstraints(constraints, features, featureModel);
        } catch (IOException | NullPointerException | ParserException ex) {
            throw new ParserException(ex.getMessage());
        }

        return featureModel;
    }

    /**
     * Find a feature based on its id.
     *
     * @param id - an id
     * @param features - features in the form of {@link JSONObject}
     * @return a {@link JSONObject} of the found feature or null
     */
    private JSONObject getFeature(String id, JSONObject features) {
        for (Iterator<String> it = features.keys(); it.hasNext(); ) {
            String key = it.next();
            if (key.equals(id)) {
                return features.getJSONObject(key);
            }
        }
        return null;
    }

    /**
     * Iterate objects in the {@link JSONObject} of the key "tree" to
     * take the feature names and relationships between features.
     *
     * @param tree - a {@link JSONObject} of the key "tree"
     * @param features - a {@link JSONObject} of the key "features"
     * @param fm - a {@link FeatureModel}
     * @throws ParserException
     */
    private void convertTree(JSONObject tree, JSONObject features, FeatureModel fm) throws ParserException {
        if (tree == null) return;

        try {
            String id;
            try {
                id = tree.getString("id");
            } catch (Exception e) {
                throw new ParserException("There doesn't exist \"id\" key in \"tree\" object.");
            }

            JSONObject rootfeature = getFeature(id, features);

            if (rootfeature == null) {
                throw new ParserException("There doesn't exist an id \"" + id + "\" in \"features\" object.");
            }

            String name;
            try {
                name = rootfeature.getString("name");
            } catch (Exception e) {
                throw new ParserException("There doesn't exist \"name\" key in " + id + " object.");
            }

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

            fm.addFeature(name);

            examineANode(tree, features, fm);
        } catch (ParserException e) {
            throw new ParserException(e.getMessage());
        } catch (Exception e) {
            throw new ParserException("There exists errors in the feature model file!");
        }
    }

    /**
     * Examine a node to convert child nodes into features,
     * and relationships of a {@link FeatureModel}.
     *
     * @param node - a {@link JSONObject}
     * @param features - a {@link JSONObject} of the key "features"
     * @param fm - a {@link FeatureModel}
     * @throws ParserException
     */
    private void examineANode(JSONObject node, JSONObject features, FeatureModel fm) throws ParserException {
        try {
            JSONObject parentFeature = getFeature(node.getString("id"), features);
            String parentName = parentFeature.getString("name");

            if (node.has("children")) {
                // takes children name
                JSONArray children = node.getJSONArray("children");

                ArrayList<String> childrenName = getChildrenName(node, features);
                fm.addFeatures(childrenName.toArray(new String[0]));

                // convert relationships
                if (parentFeature.has("type")) {
                    String leftSide;
                    ArrayList<String> rightSide;

                    String type = parentFeature.getString("type");
                    switch (type) {
                        case "FEATURE":
                            for (int i = 0; i < children.length(); i++) {
                                JSONObject child = (JSONObject) children.get(i);
                                JSONObject childFeature = getFeature(child.getString("id"), features);
                                String childName = childFeature.getString("name");

                                // takes optional
                                if (childFeature.has("optional")) {

                                    boolean opt;
                                    try {
                                        opt = childFeature.getBoolean("optional");
                                    } catch (Exception e) {
                                        throw new ParserException("The value of \"optional\" key of \"" + childFeature + "\" must be \"true\" or \"false\".");
                                    }

                                    if (!opt) {
                                        // MANDATORY
                                        leftSide = parentName;
                                        rightSide = new ArrayList<>();
                                        rightSide.add(childName);

                                        fm.addRelationship(Relationship.RelationshipType.MANDATORY,
                                                leftSide,
                                                rightSide.toArray(new String[0]));
                                    } else { // OPTIONAL
                                        leftSide = childName;
                                        rightSide = new ArrayList<>();
                                        rightSide.add(parentName);

                                        fm.addRelationship(Relationship.RelationshipType.OPTIONAL,
                                                leftSide,
                                                rightSide.toArray(new String[0]));
                                    }
                                } else {
                                    throw new ParserException("There doesn't exist \"optional\" key in \"" + childFeature + "\" object.");
                                }
                            }
                            break;
                        case "XOR":
                            leftSide = parentName;
                            rightSide = getChildrenName(node, features);

                            fm.addRelationship(Relationship.RelationshipType.ALTERNATIVE,
                                    leftSide,
                                    rightSide.toArray(new String[0]));
                            break;
                        case "OR":
                            leftSide = parentName;
                            rightSide = getChildrenName(node, features);

                            fm.addRelationship(Relationship.RelationshipType.OR,
                                    leftSide,
                                    rightSide.toArray(new String[0]));
                            break;
                        default:
                            throw new ParserException("\"" + type + "\" is not supported type.");
                    }
                } else {
                    throw new ParserException("There doesn't exist \"type\" key in \"" + parentFeature + "\" object.");
                }

                // examine sub-nodes
                for (int i = 0; i < children.length(); i++) {
                    JSONObject child = (JSONObject) children.get(i);
                    examineANode(child, features, fm);
                }
            }
        } catch (ParserException e) {
            throw new ParserException(e.getMessage());
        } catch (Exception e) {
            throw new ParserException("There exists errors in the feature model file!");
        }
    }

    /**
     * Iterate objects in a {@link JSONObject} of the key "constraints" to
     * take constraints for a {@link FeatureModel}.
     *
     * @param constraints - a {@link JSONObject} of the key "constraints"
     * @param features - a {@link JSONObject} of the key "features"
     * @param fm - a {@link FeatureModel}
     */
    private void convertConstraints(JSONObject constraints, JSONObject features, FeatureModel fm) throws ParserException {
        if (constraints == null) return;

        for (Iterator<String> it = constraints.keys(); it.hasNext(); ) {
            String key = it.next();

            examineAConstraintNode(constraints.getJSONObject(key), features, fm);
        }
    }

    /**
     * Examine a constraint that belongs to the value of the key "constraints"
     * to convert it into a constraint in the {@link FeatureModel}.
     *
     * @param constraint - a constraint of the key "constraints"
     * @param features - a {@link JSONObject} of the key "features"
     * @param fm - a {@link FeatureModel}
     * @throws ParserException
     */
    private void examineAConstraintNode(JSONObject constraint, JSONObject features, FeatureModel fm) throws ParserException {
        try {
            if (constraint.has("type")) {
                JSONArray operands;
                try {
                    operands = constraint.getJSONArray("operands");
                } catch (Exception e) {
                    throw new ParserException("There doesn't exist \"operands\" key in \"" + constraint + "\" object.");
                }

                if (operands.length() != 2) {
                    throw new ParserException("\"" + constraint + "\" is not supported constraint.");
                }

                String leftId;
                String rightId;
                try {
                    leftId = ((JSONArray) ((JSONObject) operands.get(0)).getJSONArray("operands")).get(0).toString();
                    rightId = ((JSONArray) ((JSONObject) operands.get(1)).getJSONArray("operands")).get(0).toString();
                } catch (Exception e) {
                    throw new ParserException("There doesn't exist \"operands\" key in \"" + constraint + "\" object.");
                }

                JSONObject leftFeature = getFeature(leftId, features);

                if (leftFeature == null) {
                    throw new ParserException("There doesn't exist an id \"" + leftId + "\" in \"features\" object.");
                }

                JSONObject rightFeature = getFeature(rightId, features);

                if (rightFeature == null) {
                    throw new ParserException("There doesn't exist an id \"" + rightId + "\" in \"features\" object.");
                }

                String left;
                try {
                    left = leftFeature.getString("name");
                } catch (Exception e) {
                    throw new ParserException("There doesn't exist \"name\" key in " + leftId + " object.");
                }

                ArrayList<String> rightSideList = new ArrayList<>();
                String right;
                try {
                    right = rightFeature.getString("name");
                } catch (Exception e) {
                    throw new ParserException("There doesn't exist \"name\" key in " + rightId + " object.");
                }

                rightSideList.add(right);

                String type = constraint.getString("type");
                switch (type) {
                    case "ExcludesTerm":
                        fm.addConstraint(Relationship.RelationshipType.EXCLUDES,
                                left,
                                rightSideList.toArray(new String[0]));
                        break;
                    case "ImpliesTerm":
                        fm.addConstraint(Relationship.RelationshipType.REQUIRES,
                                left,
                                rightSideList.toArray(new String[0]));
                        break;
                    default:
                        throw new ParserException("\"" + type + "\" is not supported constraint.");
                }
            } else {
                throw new ParserException("There doesn't exist \"type\" key in \"" + constraint + "\" object.");
            }
        } catch (ParserException e) {
            throw new ParserException(e.getMessage());
        } catch (Exception e) {
            throw new ParserException("There exists errors in the feature model file!");
        }
    }

    /**
     * Take names of child features of a {@link JSONObject} node on the
     * basic of {@link JSONObject} objects of the key "features".
     *
     * @param node - a {@link JSONObject}
     * @param features - a {@link JSONObject} of the key "features"
     * @return an array of names of child features.
     */
    private ArrayList<String> getChildrenName(JSONObject node, JSONObject features) throws ParserException {
        ArrayList<String> names = new ArrayList<>();

        JSONArray children;
        try {
            children = node.getJSONArray("children");
        } catch (Exception e) {
            throw new ParserException("There doesn't exist \"children\" key in \"" + node + "\" object.");
        }

        for (int i = 0; i < children.length(); i++) {
            JSONObject child = (JSONObject) children.get(i);

            String id;
            try {
                id = child.getString("id");
            } catch (Exception e) {
                throw new ParserException("There doesn't exist \"id\" key in \"" + child + "\" object.");
            }

            JSONObject childFeature = getFeature(id, features);

            if (childFeature == null) {
                throw new ParserException("There doesn't exist an id \"" + id + "\" in \"features\" object.");
            }

            String childName;
            try {
                childName = childFeature.getString("name");
            } catch (Exception e) {
                throw new ParserException("There doesn't exist \"name\" key in " + id + " object.");
            }

            if (childName.isEmpty())
            {
                throw new ParserException("The feature name could not be blank!");
            }
            if (!childName.matches("[a-zA-Z_][0-9a-zA-Z_\\s\\-]*")
                    || childName.matches("[a-zA-Z]{1,3}\\$?[0-9]{1,7}(\\:?\\$?[a-zA-Z]{1,3}\\$?[0-9]{1,7})?")
                    || childName.length() > 255) {
                StringBuilder st = new StringBuilder("\"" + childName + "\" is a wrong name!");
                st.append("\n").append("The first character of a feature name must be a letter, or an underscore character (_).");
                st.append("\n").append("Remaining characters in the feature name can be letters, numbers, periods and underscore characters.");
                st.append("\n").append("Names cannot be the same as a cell reference, such as ABC100 or Z$100 or R1C1.");
                st.append("\n").append("A name can contain up to 255 characters.");

                st.append("\n\n").append("Invalid name: " + childName);

                throw new ParserException(st.toString());
            }

            names.add(childName);
        }
        return names;
    }
}
