/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.core;

import java.util.ArrayList;

import static at.tugraz.ist.ase.fm2exconf.core.Utilities.createStringFromArrayWithSeparator;
import static at.tugraz.ist.ase.fm2exconf.core.Utilities.replaceSpecialCharactersByUnderscore;

/**
 *
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class Relationship {
    public enum RelationshipType {
        MANDATORY,
        OPTIONAL,
        ALTERNATIVE,
        OR,
        REQUIRES,
        EXCLUDES
    }

    private RelationshipType type;
    private String leftSide;
    private ArrayList<String> rightSide;

    private String confRule;
    private ArrayList<String> textBasedRules;
    private ArrayList<String> excelFormulae;
    private ArrayList<String> excelFormulaeWithTrueFalse;

    private ArrayList<String> constraints;

    public Relationship(RelationshipType type, String leftSide, ArrayList<String> rightSide) {
//        id = idCount++;
        this.type = type;
        this.leftSide = leftSide;
        this.rightSide = rightSide;

        convertToConfRule();
        convertToTextBasedRules();
        convertToExcelFormulae();

        constraints = new ArrayList<>();
    }

    public RelationshipType getType() {
        return type;
    }

    public boolean isOptional() {
        if (type == RelationshipType.OPTIONAL || type == RelationshipType.OR) {
            return true;
        }
        return false;
    }

    public boolean isType(RelationshipType type) {
        return this.type == type;
    }

    public String getLeftSide() {
        return leftSide;
    }

    public boolean belongsToLeftSide(Feature feature) {
        if (leftSide.equals(feature.toString()))
            return true;
        return false;
    }

    public boolean belongsToRightSide(Feature feature) {
        for (String right: rightSide) {
            if (right.equals(feature.toString())) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<String> getRightSide() {
        return rightSide;
    }

    public String getConfRule() {
        return confRule;
    }

    public ArrayList<String> getTextBasedRules() {
        return textBasedRules;
    }

    public ArrayList<String> getExcelFormulae(FeatureModel.FEATURE_STATE_TYPE featureStateType) {
        if (featureStateType == FeatureModel.FEATURE_STATE_TYPE.LOGIC)
            return excelFormulaeWithTrueFalse;
        return excelFormulae;
    }

    public void setConstraint(String constraint) {
        constraints.add(constraint);
    }

    public ArrayList<String> getConstraints() {
        return constraints;
    }

    public boolean isExist(String constraint) {
        for (String cstr: constraints) {
            if (cstr.equals(constraint))
                return true;
        }
        return false;
    }

    private void convertToConfRule() {
        switch (type) {
            case MANDATORY:
                confRule = String.format("mandatory(%s, %s)", leftSide, rightSide.get(0));
                break;
            case OPTIONAL:
                confRule = String.format("optional(%s, %s)", leftSide, rightSide.get(0));
                break;
            case REQUIRES:
                confRule = String.format("requires(%s, %s)", leftSide, rightSide.get(0));
                break;
            case ALTERNATIVE:
                confRule = String.format("alternative(%s, %s)", leftSide, createStringFromArrayWithSeparator(rightSide,","));
                break;
            case OR:
                confRule = String.format("or(%s, %s)", leftSide, createStringFromArrayWithSeparator(rightSide,","));
                break;
            case EXCLUDES:
                confRule = String.format("excludes(%s, %s)", leftSide, rightSide.get(0));
                break;
        }
    }

    private void convertToTextBasedRules() {
        textBasedRules = new ArrayList<>();
        switch (type) {
            case MANDATORY:
                textBasedRules.add(String.format("%s <-> %s", leftSide, rightSide.get(0)));
                break;
            case OPTIONAL:
            case REQUIRES:
                textBasedRules.add(String.format("%s -> %s", leftSide, rightSide.get(0)));
                break;
            case ALTERNATIVE:
                textBasedRules.add(String.format("%s -> XOR(%s)", leftSide, createStringFromArrayWithSeparator(rightSide,",")));
                for (String right: rightSide) {
                    textBasedRules.add(String.format("%s -> %s", right, leftSide));
                }
                break;
            case OR:
                textBasedRules.add(String.format("%s -> OR(%s)", leftSide, createStringFromArrayWithSeparator(rightSide,",")));
                for (String right: rightSide) {
                    textBasedRules.add(String.format("%s -> %s", right, leftSide));
                }
                break;
            case EXCLUDES:
                textBasedRules.add(String.format("not(%s) or not(%s)", leftSide, rightSide.get(0)));
                break;
            default:
                textBasedRules = null;
                break;
        }
    }

    private void convertToExcelFormulae() {
        excelFormulae = new ArrayList<>();
        excelFormulaeWithTrueFalse = new ArrayList<>();
        String sumRightSide;
        String sumRightSideWithTrueFalse;
        String strRightSide;
        String left;
        String right;

        switch (type) {
            case MANDATORY:
                left = replaceSpecialCharactersByUnderscore(leftSide);
                right = replaceSpecialCharactersByUnderscore(rightSide.get(0));
                excelFormulae.add(String.format("IF(%s=0,IF(%s=1,\"*include %s*\",\"ok\"),IF(%s=0,\"*include %s*\",\"ok\"))",
                        left, right, leftSide, right, rightSide.get(0)));
                excelFormulaeWithTrueFalse.add(String.format("IF(%s*1=0,IF(%s*1=1,\"*include %s*\",\"ok\"),IF(%s*1=0,\"*include %s*\",\"ok\"))",
                        left, right, leftSide, right, rightSide.get(0)));
                break;
            case OPTIONAL:
            case REQUIRES:
                left = replaceSpecialCharactersByUnderscore(leftSide);
                right = replaceSpecialCharactersByUnderscore(rightSide.get(0));
                excelFormulae.add(String.format("IF(%s=1,IF(%s=0,\"*exclude %s or include %s*\",\"ok\"),\"ok\")",
                        left, right, leftSide, rightSide.get(0)));
                excelFormulaeWithTrueFalse.add(String.format("IF(%s*1=1,IF(%s*1=0,\"*exclude %s or include %s*\",\"ok\"),\"ok\")",
                        left, right, leftSide, rightSide.get(0)));
                break;
            case ALTERNATIVE:
                left = replaceSpecialCharactersByUnderscore(leftSide);
                sumRightSide = createStringFromArrayWithSeparator(rightSide,"+");
                sumRightSideWithTrueFalse = createStringFromArrayWithSeparator(rightSide,"*1+");
                strRightSide = createStringFromArrayWithSeparator(rightSide,",");

                ArrayList<String> all = new ArrayList<>();
                all.add(leftSide);
                all.addAll(rightSide);
                String sumAll = createStringFromArrayWithSeparator(all, "+");
                String sumAllWithTrueFalse = createStringFromArrayWithSeparator(all, "*1+");

                excelFormulae.add(String.format("IF(%s=1,IF(%s=0,\"*include %s*\",\"ok\"),IF(%s=0,\"ok\",\"*include 1 out of %s*\"))",
                        sumRightSide, left, leftSide, sumAll, strRightSide));
                excelFormulaeWithTrueFalse.add(String.format("IF(%s=1,IF(%s*1=0,\"*include %s*\",\"ok\"),IF(%s=0,\"ok\",\"*include 1 out of %s*\"))",
                        sumRightSideWithTrueFalse, left, leftSide, sumAllWithTrueFalse, strRightSide));
                for (String rightside: rightSide) {
                    right = replaceSpecialCharactersByUnderscore(rightside);
                    excelFormulae.add(String.format("IF(%s=1,IF(%s=0,\"*exclude %s or include %s*\",\"ok\"),\"ok\")",
                            right, left, rightside, leftSide));
                    excelFormulaeWithTrueFalse.add(String.format("IF(%s*1=1,IF(%s*1=0,\"*exclude %s or include %s*\",\"ok\"),\"ok\")",
                            right, left, rightside, leftSide));
                }
                break;
            case OR:
                left = replaceSpecialCharactersByUnderscore(leftSide);
                sumRightSide = createStringFromArrayWithSeparator(rightSide,"+");
                sumRightSideWithTrueFalse = createStringFromArrayWithSeparator(rightSide,"*1+");
                strRightSide = createStringFromArrayWithSeparator(rightSide," or ");
                excelFormulae.add(String.format("IF(%s=0,IF(%s=1,\"*include %s*\",\"ok\"),IF(%s=0,\"*include %s or exclude %s's subfeatures*\",\"ok\"))",
                        sumRightSide, left, strRightSide, left, leftSide, leftSide));
                excelFormulaeWithTrueFalse.add(String.format("IF(%s=0,IF(%s*1=1,\"*include %s*\",\"ok\"),IF(%s*1=0,\"*include %s or exclude %s's subfeatures*\",\"ok\"))",
                        sumRightSideWithTrueFalse, left, strRightSide, left, leftSide, leftSide));
                for (String rightside: rightSide) {
                    right = replaceSpecialCharactersByUnderscore(rightside);
                    excelFormulae.add(String.format("IF(%s=1,IF(%s=0,\"*exclude %s or include %s*\",\"ok\"),\"ok\")",
                            right, left, rightside, leftSide));
                    excelFormulaeWithTrueFalse.add(String.format("IF(%s*1=1,IF(%s*1=0,\"*exclude %s or include %s*\",\"ok\"),\"ok\")",
                            right, left, rightside, leftSide));
                }
                break;
            case EXCLUDES:
                left = replaceSpecialCharactersByUnderscore(leftSide);
                right = replaceSpecialCharactersByUnderscore(rightSide.get(0));
                excelFormulae.add(String.format("IF(%s=1,IF(%s=1,\"*exclude %s or %s*\",\"ok\"),\"ok\")",
                        left, right, leftSide, rightSide.get(0)));
                excelFormulaeWithTrueFalse.add(String.format("IF(%s*1=1,IF(%s*1=1,\"*exclude %s or %s*\",\"ok\"),\"ok\")",
                        left, right, leftSide, rightSide.get(0)));
                break;
            default:
                excelFormulae = null;
                break;
        }
    }
}
