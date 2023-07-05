/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.generator.styles;

import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.HashMap;
import java.util.Map;

/**
 * This class provides a list of fonts used by the {@link at.tugraz.ist.ase.fm2exconf.generator.FM2ExConfConverter}
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class Fonts {
    /**
     * A list of names of fonts
     */
    public enum FontName {
        DEFAULT,
        BOLD_WHITE, // font for HEADER, CONSTRAINT HEADER
        BOLD, // font for styling FEATURE cells
        WHITE, // font for styling CONSTRAINT cells
        BOLD_ORANGE // font for styling STATE cells
    }

    private XSSFWorkbook workbook;
    private Colors colors;
    private Map<FontName, XSSFFont> fonts;

    public Fonts(XSSFWorkbook workbook, Colors colors) {
        this.workbook = workbook;
        this.colors = colors;
        fonts = createFonts();
    }

    /**
     * Return a {@link XSSFFont} on the basis of a font name.
     *
     * @param name - a font name
     * @return a {@link XSSFFont}
     */
    public XSSFFont get(FontName name) {
        return fonts.get(name);
    }

    /**
     * Create a list of fonts
     *
     * @return a map of pairs between font names and {@link XSSFFont}
     */
    private Map<FontName, XSSFFont> createFonts() {
        fonts = new HashMap<>();

        // Create a Font for styling DEFAULT cells
        XSSFFont def = workbook.createFont();
        def.setFontHeightInPoints((short) 12);
        def.setColor(IndexedColors.BLACK.getIndex());
        fonts.put(FontName.DEFAULT, def);

        // Create a Font for styling HEADER, CONSTRAINT HEADER cells
        XSSFFont bold_white = workbook.createFont();
        bold_white.setBold(true);
        bold_white.setFontHeightInPoints((short) 12);
        bold_white.setColor(IndexedColors.WHITE.getIndex());
        fonts.put(FontName.BOLD_WHITE, bold_white);

        // Create a Font for styling FEATURE, STATE cells
        XSSFFont bold = workbook.createFont();
        bold.setBold(true);
        bold.setFontHeightInPoints((short) 12);
        fonts.put(FontName.BOLD, bold);

        // Create a Font for styling Constraint cell
        XSSFFont white = workbook.createFont();
        white.setFontHeightInPoints((short) 12);
        white.setColor(IndexedColors.WHITE.getIndex());
        fonts.put(FontName.WHITE, white);

        // Create a Font for styling STATE cells
        XSSFFont bold_orange = workbook.createFont();
        bold_orange.setBold(true);
        bold_orange.setFontHeightInPoints((short) 12);
        bold_orange.setColor(colors.get(Colors.ColorName.STATE_FONT));
        fonts.put(FontName.BOLD_ORANGE, bold_orange);

        return fonts;
    }
}
