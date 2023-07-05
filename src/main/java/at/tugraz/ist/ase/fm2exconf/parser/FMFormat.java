/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2020-2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.parser;

/**
 * A enum of types of feature model formats.
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public enum FMFormat {
    NONE,
    SXFM, // SPLOT format
    FEATUREIDE, // FeatureIDE format
    XMI, // v.control format
    GLENCOE, // Glencoe format
    DESCRIPTIVE // my format
}
