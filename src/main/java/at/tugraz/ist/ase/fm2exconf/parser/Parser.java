/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.parser;

import at.tugraz.ist.ase.fm2exconf.core.FeatureModel;

import java.io.File;

/**
 * The parser manages all types of parsers.
 * The program use this class to parse all feature model files.
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class Parser {

    // TODO: xu ly try catch
    public FeatureModel parse(File filePath) throws ParserException {
        if (filePath == null) throw new ParserException("filePath cannot be empty");

        FeatureModel fm = null;
        BaseParser parser = getParser(filePath);

        if (parser == null) {
            throw new ParserException("The format of the chosen file is not supported or there exist errors in the file!");
        } else {
            fm = parser.parse(filePath);
        }

        return fm;
    }

    /**
     * @return a corresponding parser for the feature model file
     * or null if the program don't support the format of the feature model file
     */
    private BaseParser getParser(File filePath) {
        BaseParser[] parsers = {
            new SXFMParser(),
            new FeatureIDEParser(),
            new XMIParser(),
            new GLENCOEParser(),
            new DescriptiveFormatParser()
        };

        for (BaseParser parser : parsers) {
            if (parser.checkFormat(filePath)) {
                return parser;
            }
        }

        return null;
    }
}
