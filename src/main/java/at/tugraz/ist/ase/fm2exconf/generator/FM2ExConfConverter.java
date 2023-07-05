/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2020-2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.generator;

import at.tugraz.ist.ase.fm2exconf.core.FeatureModel;
import at.tugraz.ist.ase.fm2exconf.ui.ProgressDialogController;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class FM2ExConfConverter {

    private FM2ExConfWorkbook workbook;
    private ProgressDialogController progressController;

    public FM2ExConfConverter(ProgressDialogController progressController) {
        // Create a Workbook
        workbook = new FM2ExConfWorkbook(progressController);

        /* CreationHelper helps us create instances for various things like DataFormat,
           Hyperlink, RichTextString etc in a format (HSSF, XSSF) independent way */
//        CreationHelper createHelper = workbook.getCreationHelper();

        // TODO: PROGRESSCONTROLLER
//        this.progressController = progressController;
    }

    @Override
    protected void finalize() throws IOException {
        workbook.close();
    }

    public String convert(FeatureModel fm) throws ConvertException, IOException {
        workbook.addFeatureModel(fm);

        // Write the output to a file
        // TODO: PROGRESSCONTROLLER
//        progressController.setStatus("Saving to file...", 6/7);

        String filename = String.format("%s-configurator.xlsx", fm.getName());
        FileOutputStream fileOut = new FileOutputStream(filename);
        workbook.write(fileOut);
        fileOut.close();

        // TODO: PROGRESSCONTROLLER
//        progressController.closeStage();

        return filename;
    }
}
