/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2020-2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.generator;

import at.tugraz.ist.ase.fm2exconf.core.Feature;
import at.tugraz.ist.ase.fm2exconf.core.FeatureModel;
import at.tugraz.ist.ase.fm2exconf.core.Relationship;
import at.tugraz.ist.ase.fm2exconf.generator.styles.Colors;
import at.tugraz.ist.ase.fm2exconf.generator.styles.Styles;
import at.tugraz.ist.ase.fm2exconf.ui.ProgressDialogController;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.util.ArrayList;

import static at.tugraz.ist.ase.fm2exconf.core.Utilities.createStringFromArrayWithSeparator;
import static at.tugraz.ist.ase.fm2exconf.core.Utilities.replaceSpecialCharactersByUnderscore;

/**
 *
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class FM2ExConfWorkbook extends XSSFWorkbook {
    // structure of file
    // TODO: add Price
    private String[] columnTitles = {"FEATURE", "INCLUDED\n(1 yes, 0 no)", "RELATIONSHIP/CONSTRAINT", "OK"};
    private String secondColumnTitleWithTrueFalse = "INCLUDED\n(TRUE yes, FALSE no)";
    private String priceTitle = "PRICE";

    private XSSFSheet sheet;
    private Styles styles;
    private FeatureModel fm;

    private int numRowForFeatures;

    private ProgressDialogController progressController;

    public FM2ExConfWorkbook(ProgressDialogController progressController) {
        super();

        // TODO: PROGRESSCONTROLLER
        this.progressController = progressController;

        // Create a Sheet
        sheet = createSheet("Configurator");
        // Create styles for styling the worksheet
        styles = new Styles(this);
        // set the default style
        for(int i = 0; i < columnTitles.length; i++) sheet.setDefaultColumnStyle(i, styles.get(Styles.StyleName.DEFAULT));
    }

    public void addFeatureModel(FeatureModel fm) throws ConvertException {
        try {
            this.fm = fm;

            // create header cells
            // TODO: PROGRESSCONTROLLER
//            progressController.setStatus("Creating header cells...", 0.0);

            createHeader();
//            Thread.sleep(4000);

//            progressController.setStatus("Creating columns 1 and 2...", 1 / 7);

            translateFeatures(); // create Column 1 (feature names) and 2 (states)
//            Thread.sleep(4000);
//            progressController.setStatus("Creating columns 3 and 4...", 2 / 7);

            translateRelationships(); // create Column 3 (text-based rules) and Column 4 (Excel formulae)
//            Thread.sleep(4000);
//            progressController.setStatus("Creating rows for the cross-tree constraints...", 3 / 7);

            translateConstraints(); // create the Cross-tree constraints
//            Thread.sleep(4000);

//            progressController.setStatus("Applying the conditional formatting for column 4...", 4 / 7);

            createConditionalFormatting(); // apply the conditional formatting for Column 4
//            Thread.sleep(4000);

            if (fm.isPricingSupport()) {
//                progressController.setStatus("Creating pricing column...", 5 / 7);
                // TODO: PROGRESSCONTROLLER
                createPriceCells();
//                Thread.sleep(4000);
            }

            autoSizeColumn();
            sheet.setActiveCell(new CellAddress("B3"));
            sheet.setZoom(150);
        } catch (ConvertException e) {
            throw (e);
        }
    }

    private void createHeader() throws ConvertException {
        try {
            // Create the header Row
            Row headerRow = sheet.createRow(0);
            //increase row height to accommodate two lines of text
            headerRow.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());

            // Creating cells
            for (int i = 0; i < columnTitles.length; i++) {
                Cell cell = headerRow.createCell(i);
                if (i == 1) {
                    if (fm.getFeatureStateType() == FeatureModel.FEATURE_STATE_TYPE.LOGIC)
                        cell.setCellValue(secondColumnTitleWithTrueFalse);
                    else
                        cell.setCellValue(columnTitles[i]);
                    cell.setCellStyle(styles.get(Styles.StyleName.STATE_HEADER));
                } else {
                    cell.setCellValue(columnTitles[i]);
                    cell.setCellStyle(styles.get(Styles.StyleName.HEADER));
                }
            }

            // Price support
            if (fm.isPricingSupport()) {
                Cell cell = headerRow.createCell(columnTitles.length);
                cell.setCellValue(priceTitle);
                cell.setCellStyle(styles.get(Styles.StyleName.HEADER));
            }
        } catch (Exception e) {
            throw (new ConvertException(e.getMessage()));
        }
    }

    private void translateFeatures() throws ConvertException {
        int rowNum = 1;
        Row row;
        XSSFName name;
        String ref;

        try {
            for (Feature feature : fm.getFeatures(fm.getFeatureOrder())) {
                row = sheet.createRow(rowNum++);

                if (rowNum == 2) {
                    createFeatureAndStateCells(row, feature.getName(), 1, false);
                } else {
                    createFeatureAndStateCells(row, feature.getName(), 0, false);
                }

                // create XSSFName for state of feature
                name = createName();
                String nameStr = replaceSpecialCharactersByUnderscore(feature.getName());
                name.setNameName(nameStr);
                ref = "'Configurator'!$B$" + rowNum;
                name.setRefersToFormula(ref);
                // create XSSFName for price of feature
                name = createName();
                String priceStr = replaceSpecialCharactersByUnderscore(feature.getName()) + "_price";
                name.setNameName(priceStr);
                ref = "'Configurator'!$E$" + rowNum;
                name.setRefersToFormula(ref);
            }
        } catch (IllegalArgumentException e) {
            StringBuilder st = new StringBuilder("");
            st.append("The first character of a feature name must be a letter, or an underscore character (_).");
            st.append(" Remaining characters in the feature name can be letters, numbers, periods, and underscore characters.");
            st.append("\n").append("Names cannot be the same as a cell reference, such as ABC100 or Z$100 or R1C1.");
            st.append("\n").append("A name can contain up to 255 characters.");
            st.append("\n\n").append(e.getMessage());

            throw new ConvertException(st.toString());
        } catch (Exception e) {
            throw new ConvertException(e.getMessage());
        }
    }

    private void translateRelationships() throws ConvertException {
        Row row;
        String feature;
        int index;

        try {
            for (Relationship relationship : fm.getRelationships()) {
                switch (relationship.getType()) {
                    case MANDATORY:
                        // takes Right side
                        feature = relationship.getRightSide().get(0);
                        row = findRow(feature);
                        if (row == null) throw new ConvertException(feature + " is a non-existent feature name");

                        createRuleAndFormulaCells(row, relationship, 0, Styles.StyleName.RELATIONSHIP);
                        break;
                    case OPTIONAL:
                        // takes Left side
                        feature = relationship.getLeftSide();
                        row = findRow(feature);
                        if (row == null) throw new ConvertException(feature + " is a non-existent feature name");

                        createRuleAndFormulaCells(row, relationship, 0, Styles.StyleName.RELATIONSHIP);
                        break;
                    case ALTERNATIVE:
                    case OR:
                        // find index to shifts 1 row
                        index = findIndexToInsert(relationship.getRightSide());
//                        if (index == -1) throw new ConvertException(relationship.getRightSide().toString() + " are non-existent feature names");
                        // insert 1 row
                        sheet.shiftRows(index, sheet.getLastRowNum(), 1);

                        row = sheet.createRow(index);
                        createFeatureAndStateCells(row, relationship.getLeftSide(), 0, true);
                        createRuleAndFormulaCells(row, relationship, 0, Styles.StyleName.RELATIONSHIP);

                        index = -1;
                        for (String right : relationship.getRightSide()) {
                            index++;
                            row = findRow(right);
                            if (row == null) throw new ConvertException(right + " is a non-existent feature name");

                            createRuleAndFormulaCells(row, relationship, index + 1, Styles.StyleName.RELATIONSHIP);
                        }
                        break;
                }
            }

            // Empty rule and formula for the root feature
            row = sheet.getRow(1);
            createRuleAndFormulaCells(row, null, 0, Styles.StyleName.RELATIONSHIP);

            numRowForFeatures = sheet.getLastRowNum() + 1;
        } catch (Exception e) {
            throw new ConvertException(e.getMessage());
        }
    }

    private void translateConstraints() throws ConvertException {
        int index = 0;
        Row row;

        try {
            for (Relationship constraint : fm.getConstraints()) {
                row = sheet.createRow(sheet.getLastRowNum() + 1);
                if (index == 0) {
                    createFeatureAndStateCells(row, "Cross-Tree Constraints", -1, true);
                } else {
                    createFeatureAndStateCells(row, "", -1, true);
                }

                createRuleAndFormulaCells(row, constraint, 0, Styles.StyleName.CONSTRAINT);
                index++;
            }
        } catch (Exception e) {
            throw new ConvertException(e.getMessage());
        }
    }

    private void createConditionalFormatting() throws ConvertException {
        try {
            XSSFSheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();
            // Condition: Cell Value Is   'ok'   (Light Red Fill)
            ConditionalFormattingRule rule = sheetCF.createConditionalFormattingRule(ComparisonOperator.NOT_EQUAL, "\"ok\"");
            XSSFPatternFormatting fill = (XSSFPatternFormatting) rule.createPatternFormatting();
            fill.setFillBackgroundColor(styles.getColor(Colors.ColorName.CONDITIONAL_BACKGROUND));
            fill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

            int num = sheet.getLastRowNum() + 1;
            String range = "D3:D" + num;
            CellRangeAddress[] regions = {
                    CellRangeAddress.valueOf(range)
            };

            sheetCF.addConditionalFormatting(regions, rule);
        } catch (Exception e) {
            throw new ConvertException(e.getMessage());
        }
    }

    private void createFeatureAndStateCells(Row row, String feature, int state, boolean isConstraintHeader) {
        // FEATURE column
        Cell cell = row.createCell(0);
        cell.setCellValue(feature);
        if (isConstraintHeader) {
            cell.setCellStyle(styles.get(Styles.StyleName.CONSTRAINT_HEADER));
        } else {
            cell.setCellStyle(styles.get(Styles.StyleName.FEATURE));
        }

        // STATE column
        cell = row.createCell(1);
        if (isConstraintHeader) {
            cell.setCellStyle(styles.get(Styles.StyleName.CONSTRAINT));
        } else {
            if (state != -1) {
                if (fm.getFeatureStateType() == FeatureModel.FEATURE_STATE_TYPE.BINARY)
                    cell.setCellValue(state);
                else
                    cell.setCellValue(state == 1 ? true : false);
            }
            cell.setCellStyle(styles.get(Styles.StyleName.STATE));
        }
    }

    private void createRuleAndFormulaCells(Row row, Relationship relationship, int index, Styles.StyleName style) {
        Cell cell;
        if (relationship != null) {
            cell = row.createCell(2);
            cell.setCellValue(relationship.getTextBasedRules().get(index));
            cell.setCellStyle(styles.get(style));

            cell = row.createCell(3);
            cell.setCellFormula(relationship.getExcelFormulae(fm.getFeatureStateType()).get(index));
            cell.setCellStyle(styles.get(style));
        } else {
            cell = row.createCell(2);
            cell.setCellStyle(styles.get(style));

            cell = row.createCell(3);
            cell.setCellStyle(styles.get(style));
        }
    }

    private ArrayList<String> getChildFeatures(ArrayList<Feature> features) {
        ArrayList<String> names = new ArrayList<>();
        for (int i = 1; i < features.size(); i++) {
            names.add(features.get(i).toString() + "_price");
        }
        return names;
    }

    private void createPriceCells() {
        for (Feature feature: fm.getFeatures(fm.getFeatureOrder())) {
            Row row = findRow(feature.getName());
            if (row == null) continue;

            Cell cell = row.createCell(4);

            if (feature.getName().equals(fm.getName())) {
                ArrayList<String> names = getChildFeatures(fm.getFeatures(fm.getFeatureOrder()));
                String formula = createStringFromArrayWithSeparator(names,"+");
                cell.setCellFormula(formula);
                cell.setCellStyle(styles.get(Styles.StyleName.TOTAL_PRICE));
            } else {
                cell.setCellFormula(feature.getExcelFormula());
            }
        }

        // duyet qua tat ca cac o de set cell style
        for (int i = 2; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            Cell cell;
            if (i < numRowForFeatures) {
                cell = row.getCell(4);
                if (cell == null) cell = row.createCell(4);

                cell.setCellStyle(styles.get(Styles.StyleName.PRICE));
            } else {
                cell = row.createCell(4);
                cell.setCellStyle(styles.get(Styles.StyleName.CONSTRAINT));
            }
        }
    }

    private Row findRow(String featureName) {
        for (Row row: sheet) {
            Cell cell = row.getCell(0);
            if (cell.getStringCellValue().equals(featureName))
                return row;
        }
        return null;
    }

    private int findIndexToInsert(ArrayList<String> features) throws ConvertException {
        int index = sheet.getLastRowNum();
        for (String feature: features) {
            Row row = findRow(feature);
            if (row == null) throw new ConvertException(feature + " is a non-existent feature name");
            int rowNum = row.getRowNum();
            if (rowNum < index) {
                index = rowNum;
            }
        }
        return index == sheet.getLastRowNum() ? -1 : index;
    }

    private void autoSizeColumn() {
        // Resize all columns to fit the content size
        for(int i = 0; i < columnTitles.length; i++) sheet.autoSizeColumn(i);
    }
}
