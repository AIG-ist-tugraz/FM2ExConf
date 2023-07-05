/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2020-2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.generator.styles;

import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.HashMap;
import java.util.Map;

/**
 * This class provides a color palette used by the {@link at.tugraz.ist.ase.fm2exconf.generator.FM2ExConfConverter}.
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class Colors {

    /**
     * A list of names of colors
     */
    public enum ColorName {
        HEADER_BACKGROUND,
        STATE_BACKGROUND,
        STATE_FONT,
        CONSTRAINT_BACKGROUND,
        CONDITIONAL_BACKGROUND
    }

    private XSSFWorkbook workbook;
    private Map<ColorName, XSSFColor> colors;

    public Colors(XSSFWorkbook workbook) {
        this.workbook = workbook;
        colors = createColors();
    }

    /**
     * Return a {@link XSSFColor} on the basis of a color name.
     *
     * @param name - a color name
     * @return a {@link XSSFColor}
     */
    public XSSFColor get(ColorName name) {
        return colors.get(name);
    }

    /**
     * Create a color palette.
     *
     * @return a map of pairs between color names and {@link XSSFColor}
     */
    private Map<ColorName, XSSFColor> createColors(){
        Map<ColorName, XSSFColor> colors = new HashMap<>();

        IndexedColorMap colorMap = workbook.getStylesSource().getIndexedColors();

        java.awt.Color headerBackgroundAWTColor = new java.awt.Color(38, 38, 38);
        XSSFColor headerBackgroundColor = new XSSFColor(headerBackgroundAWTColor, colorMap);
        colors.put(ColorName.HEADER_BACKGROUND, headerBackgroundColor);

        java.awt.Color stateBackgroundAWTColor = new java.awt.Color(255, 255, 204);
        XSSFColor stateBackgroundColor = new XSSFColor(stateBackgroundAWTColor, colorMap);
        colors.put(ColorName.STATE_BACKGROUND, stateBackgroundColor);

        java.awt.Color stateFontAWTColor = new java.awt.Color(156, 87, 0);
        XSSFColor stateFontColor = new XSSFColor(stateFontAWTColor, colorMap);
        colors.put(ColorName.STATE_FONT, stateFontColor);

        java.awt.Color constraintBackgroundAWTColor = new java.awt.Color(128, 128, 128);
        XSSFColor constraintBackgroundColor = new XSSFColor(constraintBackgroundAWTColor, colorMap);
        colors.put(ColorName.CONSTRAINT_BACKGROUND, constraintBackgroundColor);

        java.awt.Color conditionalBackgroundAWTColor = new java.awt.Color(255, 199, 206);
        XSSFColor conditionalBackgroundColor = new XSSFColor(conditionalBackgroundAWTColor, colorMap);
        colors.put(ColorName.CONDITIONAL_BACKGROUND, conditionalBackgroundColor);

        return colors;
    }
}
