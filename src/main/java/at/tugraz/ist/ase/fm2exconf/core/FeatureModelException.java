/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2020-2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.core;

/**
 * An exception for errors which occur in parsing feature model files
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class FeatureModelException extends Exception {
    public FeatureModelException(String message) {
        super(message);
    }

    public FeatureModelException(String message, Throwable throwable) {
        super(message, throwable);
    }
}

