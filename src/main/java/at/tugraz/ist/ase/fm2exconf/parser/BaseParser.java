/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2020-2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.parser;

import at.tugraz.ist.ase.fm2exconf.core.FeatureModel;

import java.io.File;

/**
 * An interface for all parsers
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public interface BaseParser {
    /**
     * Check the format of a feature model file.
     *
     * @param filePath - a {@link File}
     * @return true - if the feature model file has the same format with the parser
     *         false - otherwise
     */
    public boolean checkFormat(File filePath);

    /**
     * Return the {@link FMFormat} of the parser
     *
     * @return the {@link FMFormat} of the parser
     */
    public FMFormat getFormat();

    /**
     * Parse the feature model file into a {@link FeatureModel}.
     *
     * @param filePath - a {@link File}
     * @return a {@link FeatureModel}
     * @throws ParserException
     */
    public FeatureModel parse(File filePath) throws ParserException;
}
