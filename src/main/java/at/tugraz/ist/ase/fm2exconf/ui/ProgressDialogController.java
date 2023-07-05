/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2020-2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

public class ProgressDialogController {
    @FXML
    private Label lblStatus;
    @FXML
    private ProgressBar pgbCompletion;
    @FXML
    private Button btnCancel;

    // TODO: raise an event when btnCancel is clicked

    public void start() {
        lblStatus.setText("Starting...");
        pgbCompletion.setProgress(0.0);
    }

    public void setStatus(String status, double percent) {
        lblStatus.setText(status);
        pgbCompletion.setProgress(percent);
    }

    public void closeStage() {
        Stage stage = (Stage)lblStatus.getParent().getScene().getWindow();
        stage.close();
    }
}
