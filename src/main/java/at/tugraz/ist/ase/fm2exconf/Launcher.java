/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2020-2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf;

import javafx.application.Application;

/**
 * This class is separated from MainApp class to allow the debugging.
 */
public class Launcher {
    /**
     * The entry point of the program
     *
     * @param args
     */
    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }
}
