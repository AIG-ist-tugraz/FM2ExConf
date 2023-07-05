/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.generator.styles;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.HashMap;
import java.util.Map;

/**
 * Cell styles used by the {@link at.tugraz.ist.ase.fm2exconf.generator.FM2ExConfConverter}.
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class Styles {
    /**
     * A list of styles
     */
    public enum StyleName {
        DEFAULT,
        HEADER,
        STATE_HEADER, // header with center alginment
        FEATURE,
        STATE,
        RELATIONSHIP,
        CONSTRAINT_HEADER,
        CONSTRAINT,
        TOTAL_PRICE,
        PRICE
    }

    private XSSFWorkbook workbook;
    private Map<StyleName, XSSFCellStyle> styles;
    private Colors colors;
    private Fonts fonts;

    public Styles(XSSFWorkbook workbook) {
        this.workbook = workbook;
        colors = new Colors(workbook);
        fonts = new Fonts(workbook, colors);
        styles = createStyles();
    }

    /**
     * Return a {@link XSSFCellStyle} style based on a style name.
     *
     * @param name - a style name
     * @return a corresponding {@link XSSFCellStyle} style
     */
    public XSSFCellStyle get(StyleName name) {
        return styles.get(name);
    }

    /**
     * Return a {@link XSSFColor} color that is corresponding to a given color name
     * using the color palette of {@link Colors}.
     *
     * @param name - a color name
     * @return a {@link XSSFColor} color
     */
    public XSSFColor getColor(Colors.ColorName name) {
        return colors.get(name);
    }

    /**
     * Create a list of styles
     *
     * @return a map of pairs between style names and {@link XSSFCellStyle} styles
     */
    private Map<StyleName, XSSFCellStyle> createStyles(){
        styles = new HashMap<>();
        XSSFCellStyle style;

        // Create a CellStyle for DEFAULT cells
        style = makeStyle(fonts.get(Fonts.FontName.DEFAULT),
                null,
                false,
                HorizontalAlignment.LEFT,
                false);
        styles.put(StyleName.DEFAULT, style);

        // Create a CellStyle for HEADER cells
        style = makeStyle(fonts.get(Fonts.FontName.BOLD_WHITE),
                colors.get(Colors.ColorName.HEADER_BACKGROUND),
                false,
                HorizontalAlignment.LEFT,
                false);
        styles.put(StyleName.HEADER, style);

        // Create a CellStyle for STATE HEADER cell
        style = makeStyle(fonts.get(Fonts.FontName.BOLD_WHITE),
                colors.get(Colors.ColorName.HEADER_BACKGROUND),
                true,
                HorizontalAlignment.CENTER, // center alignmnet
                false);
        styles.put(StyleName.STATE_HEADER, style);

        // Create a CellStyle for FEATURE cells
        style = makeStyle(fonts.get(Fonts.FontName.BOLD),
                null,
                false,
                HorizontalAlignment.LEFT,
                true);
        styles.put(StyleName.FEATURE, style);

        // Create a CellStyle for STATE cells
        style = makeStyle(fonts.get(Fonts.FontName.BOLD_ORANGE),
                colors.get(Colors.ColorName.STATE_BACKGROUND),
                false,
                HorizontalAlignment.CENTER,
                true);
        styles.put(StyleName.STATE, style);

        // Create a CellStyle for RELATIONSHIP cells
        style = makeStyle(fonts.get(Fonts.FontName.DEFAULT),
                null,
                false,
                HorizontalAlignment.LEFT,
                true);
        styles.put(StyleName.RELATIONSHIP, style);

        // Create a CellStyle for Constraint Header cells
        style = makeStyle(fonts.get(Fonts.FontName.BOLD_WHITE),
                colors.get(Colors.ColorName.CONSTRAINT_BACKGROUND),
                false,
                HorizontalAlignment.LEFT,
                true);
        styles.put(StyleName.CONSTRAINT_HEADER, style);

        // Create a CellStyle for Constraint cells
        style = makeStyle(fonts.get(Fonts.FontName.WHITE),
                colors.get(Colors.ColorName.CONSTRAINT_BACKGROUND),
                false,
                HorizontalAlignment.LEFT,
                true);
        styles.put(StyleName.CONSTRAINT, style);

        // Create a CellStyle for Total Price cells
        DataFormat format = workbook.createDataFormat();
        style = makeStyle(fonts.get(Fonts.FontName.BOLD_ORANGE),
                null,
                false,
                HorizontalAlignment.RIGHT,
                true);
        style.setDataFormat(format.getFormat("#,##0.00"));
        styles.put(StyleName.TOTAL_PRICE, style);

        // Create a CellStyle for Price cells
        style = makeStyle(fonts.get(Fonts.FontName.DEFAULT),
                null,
                false,
                HorizontalAlignment.RIGHT,
                true);
        style.setDataFormat(format.getFormat("#,##0.00"));
        styles.put(StyleName.PRICE, style);

        return styles;
    }

    /**
     * Create a style based on a given font, a given color, and several settings
     *
     * @param font - a {@link XSSFFont} font
     * @param color - a {@link XSSFColor} color
     * @param isWrap - true - wrap text, false otherwise
     * @param alignment - a {@link HorizontalAlignment}
     * @param isBorders - true - make borders
     * @return a {@link XSSFCellStyle} style
     */
    private XSSFCellStyle makeStyle(XSSFFont font,
                                    XSSFColor color,
                                    boolean isWrap,
                                    HorizontalAlignment alignment,
                                    boolean isBorders) {
        XSSFCellStyle style = workbook.createCellStyle();

        if (font != null) {
            style.setFont(font);
        }

        if (color != null) {
            style.setFillForegroundColor(color);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        if (isWrap) {
            style.setWrapText(true);
        }

        style.setAlignment(alignment);

        if (isBorders) {
            makeBorders(style);
        }

        return style;
    }

    /**
     * Set the border style and the border color to a {@link XSSFCellStyle}
     *
     * @param style - a {@link XSSFCellStyle}
     */
    private void makeBorders(XSSFCellStyle style) {
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
    }
}
