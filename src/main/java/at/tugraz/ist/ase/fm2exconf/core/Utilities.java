/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.core;

import at.tugraz.ist.ase.fm2exconf.MainApp;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * This class holds utility methods
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class Utilities {
    /**
     * Replace special characters (e.g. space, -) by underscore characters.
     *
     * @param st - a string needed to replace
     * @return a new string in which the special characters are replaced
     */
    public static String replaceSpecialCharactersByUnderscore(String st) {
        return st.replaceAll("[^a-zA-Z0-9]", "_");
    }

    /**
     * Convert an array to a string with a given separator.
     *
     * @param array - an array of strings
     * @param separator - a separator
     * @return a new string includes all strings in the array which is separated by the given separator.
     */
    public static String createStringFromArrayWithSeparator(ArrayList<String> array, String separator) {
        StringBuilder str = new StringBuilder();
        for (String s: array) {
            if (separator.equals("+") || separator.equals("*1+") || separator.equals(" or ")) {
                s = replaceSpecialCharactersByUnderscore(s);
                str.append(String.format("%s%s", s, separator));
            } else if (separator.equals("\n")) {
                str.append(String.format("\t[%s]%s", s, separator));
            } else {
                str.append(String.format("%s%s ", s, separator));
            }
        }
        // delete several residual words in the postfix of the string
        String s;
        switch (separator) {
            case "+":
                s = str.substring(0, str.length() - 1);
                break;
            case "*1+":
                s = str.substring(0, str.length() - 3);
                break;
            case " or ":
                s = str.substring(0, str.length() - 4);
                break;
            case "\n":
                s = str.substring(0, str.length() - 1);
                break;
            default:
                s = str.substring(0, str.length() - 2);
                break;
        }
        return s;
    }

    /**
     * Show a dialog to inform something
     *
     * @param alertType - the type of alert (e.g. Error, Information,...)
     * @param owner - an object of Window
     * @param title - the title of the dialog
     * @param message - the message needed to show
     */
    public static void showAlert(Alert.AlertType alertType, Window owner, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(owner);
        alert.show();
        //            alert.showAndWait();
    }

    /**
     * Check whether if a string exists in an array of strings.
     *
     * @param list - an array of strings
     * @param st - a string will be checked
     * @return true if the string exists in the array, false otherwise
     */
    public static boolean isExistInArrayList(ArrayList<String> list, String st) {
        for (String s: list) {
            if (s.equals(st))
                return true;
        }
        return false;
    }

    /**
     * Returns the file path preference, i.e. the file path that was last opened.
     * The preference is read from the OS specific registry. If no such
     * preference can be found, null is returned.
     *
     * @return a file path that was last opened - or null
     *
     */
    public static File getFilePathInPreferences() throws NullPointerException {
        Preferences prefs = Preferences.userNodeForPackage(MainApp.class);
        String filePath = prefs.get("filePath", null);
        if (filePath != null) {
            return new File(filePath);
        } else {
            return null;
        }
    }

    /**
     * Returns the filter preference, i.e. the description of a filter
     * that was last chosen. The preference is read from the OS specific registry.
     * If no such preference can be found, null is returned.
     *
     * @return a string - description of the filter that was last chosen - or null
     *
     */
    public static String getDescriptionOfFilterInPreferences() throws NullPointerException {
        Preferences prefs = Preferences.userNodeForPackage(MainApp.class);
        return prefs.get("extensionFilter", null);
    }

    /**
     * Sets the file path of the currently loaded file.
     * The path is persisted in the OS specific registry.
     *
     * @param file - the file or null to remove the path
     * @param filter - the extension filter
     *
     */
    public static void setPreferences(File file, FileChooser.ExtensionFilter filter) throws NullPointerException {
        Preferences prefs = Preferences.userNodeForPackage(MainApp.class);
        if (file != null && filter != null) {
            prefs.put("filePath", file.getPath());
            prefs.put("extensionFilter", filter.getDescription());
        } else {
            prefs.remove("filePath");
            prefs.remove("extensionFilter");
        }
    }

    /**
     * Print relationships and constraints of a feature model to the console
     *
     * @param fm - a feature model
     */
    public static void printAllConstraints(FeatureModel fm) {
        // print out constraints in relationship
        for (Relationship r : fm.getRelationships()) {
            System.out.println(r.getTextBasedRules());
            for (String s : r.getConstraints()) {
                System.out.println(s);
            }
            System.out.println();
        }

        for (Relationship r : fm.getConstraints()) {
            System.out.println(r.getTextBasedRules());
            for (String s : r.getConstraints()) {
                System.out.println(s);
            }
            System.out.println();
        }
    }

    /**
     * Print diagnoses to the console
     *
     * @param allDiag - a list of list of constraints
     */
    public static void printAllDiagnoses(List<List<Constraint>> allDiag) {
        int count = 0;
        for (List<Constraint> diag : allDiag) {
            count++;
            System.out.println("Diagnosis " + count + ":");
            diag.forEach(System.out::println);
        }
    }

    /**
     * Print all constraints of a Choco model to the console
     *
     * @param model - a Choco model
     */
    public static void printConstraints(Model model) {
        List<Constraint> ac = Arrays.asList(model.getCstrs());
        ac.forEach(System.out::println);
    }
}
