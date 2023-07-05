/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2020-2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.ui;

import at.tugraz.ist.ase.fm2exconf.MainApp;
import at.tugraz.ist.ase.fm2exconf.analysis.AnalysisOperator;
import at.tugraz.ist.ase.fm2exconf.core.Feature;
import at.tugraz.ist.ase.fm2exconf.core.FeatureModel;
import at.tugraz.ist.ase.fm2exconf.core.Relationship;
import at.tugraz.ist.ase.fm2exconf.generator.ConvertException;
import at.tugraz.ist.ase.fm2exconf.generator.FM2ExConfConverter;
import at.tugraz.ist.ase.fm2exconf.parser.Parser;
import at.tugraz.ist.ase.fm2exconf.parser.ParserException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static at.tugraz.ist.ase.fm2exconf.core.Utilities.*;


/**
 * This class is the controller for the main window.
 * This class holds the loaded feature model too.
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class MainWindowController {

    // states of the application
    // use to set enable for buttons
    private enum AppState {
        INIT, // after launching the app
        OPENED, // after opening a feature model
        CONSISTENT, // after check the consistency
        INCONSISTENT // after check the consistency
    }

    @FXML
    private Button buttonRunAnalysis;
    @FXML
    private Button buttonConvert;
    @FXML
    private Button buttonExportFM4Conf;
    @FXML
    private Label fmName;
    @FXML
    private TextFlow textAreaDetails;
    @FXML
    private TextFlow txtAreaMetrics;
    @FXML
    private TextFlow textAreaResults;

    @FXML
    private TitledPane titledPaneDetails;
    @FXML
    private Accordion accordion;

    // Reference to the main application
    private MainApp mainApp;

//    private ConvertToExcelController convertController;
    private ProgressDialogController progressController;
    private Thread convertThread;

    // Feature model
    private FeatureModel featureModel;

    /**
     * Is called by the main application to give a reference back to itself.
     *
     */
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        featureModel = null;

        accordion.setExpandedPane(this.titledPaneDetails); // open the Details pane
        setButtonsEnable(AppState.INIT); // set disable for 3 buttons

        showFeatureModelDetails(null); //
    }

    /**
     * Opens a FileChooser to let the user select an feature model file to load.
     */
    @FXML
    private void handleOpen() {
        FeatureModel oldFM = featureModel;

        try {
            FileChooser fileChooser = new FileChooser();

            // get the file path reference
            File file = getFilePathInPreferences();
            if (file != null && file.exists() && file.isDirectory()) {
                // set the file path
                fileChooser.setInitialDirectory(file);
            }

            // get the filter reference
            String descriptionOfFilter = getDescriptionOfFilterInPreferences();
            // Set extension filters and the selected filter
            addFiltersToFileChooser(fileChooser, descriptionOfFilter);

            // Show open file dialog
            file = fileChooser.showOpenDialog(mainApp.getPrimaryStage());

            if (file != null) {
                Parser parser = new Parser();

                featureModel = parser.parse(file);
                featureModel.buildDepthFirstFeatures();

                // convert to ChocoSolver model
                // run the consistent checking
//                ChocoModel model = new ChocoModel(featureModel);
//                if (!model.getSolver().solve()) {
//                    throw new ParserException("There exists errors in the feature model file!");
//                }

                setButtonsEnable(AppState.OPENED); // set enable for buttons

                // Save the file path, and the selected filter to the registry.
                setPreferences(file, fileChooser.getSelectedExtensionFilter());
                // Update title of the application.
                mainApp.updateTitle(file.getName());
            }
        } catch (ParserException | NullPointerException | IllegalArgumentException e) {
            featureModel = oldFM;
            showAlert(Alert.AlertType.ERROR,
                    mainApp.getPrimaryStage(),
                    "Error",
                    e.getMessage());
        } finally {
            showFeatureModelDetails(featureModel); // update the details of feature model
        }
    }

    /**
     * Adding the extension filters to the file chooser.
     *
     * @param fileChooser - an object of FileChooser class
     * @param descriptionOfFilter - a string represented the last chosen filter
     */
    private void addFiltersToFileChooser(FileChooser fileChooser, String descriptionOfFilter) {
        if (fileChooser == null) return;

        // create 4 extension filters for 4 types of format
        // xml format - SPLOT and FeatureIDE
        FileChooser.ExtensionFilter extFilter1 = new FileChooser.ExtensionFilter(
                "XML files (*.xml)", "*.xml");
        // xmi format - v.control format
        FileChooser.ExtensionFilter extFilter2 = new FileChooser.ExtensionFilter(
                "XMI files (*.xmi)", "*.xmi");
        // json format - glencoe
        FileChooser.ExtensionFilter extFilter3 = new FileChooser.ExtensionFilter(
                "JSON files (*.json)", "*.json");
        // descriptive format
        FileChooser.ExtensionFilter extFilter4 = new FileChooser.ExtensionFilter(
                "FM4Conf files (*.fm4conf)", "*.fm4conf");
        // all files
        FileChooser.ExtensionFilter extFilter5 = new FileChooser.ExtensionFilter(
                "All files", "*.*");
        // adding the extension filter to the file chooser
        fileChooser.getExtensionFilters().addAll(
                extFilter1,
                extFilter2,
                extFilter3,
                extFilter4,
                extFilter5);

        // on the basis of the last chosen filter,
        // set the corresponding filter to the selected filter
        if (descriptionOfFilter != null) {
            switch (descriptionOfFilter) {
                case "XML files (*.xml)":
                    fileChooser.setSelectedExtensionFilter(extFilter1);
                    break;
                case "XMI files (*.xmi)":
                    fileChooser.setSelectedExtensionFilter(extFilter2);
                    break;
                case "JSON files (*.json)":
                    fileChooser.setSelectedExtensionFilter(extFilter3);
                    break;
                case "FM4Conf files (*.fm4conf)":
                    fileChooser.setSelectedExtensionFilter(extFilter4);
                    break;
                case "All files":
                    fileChooser.setSelectedExtensionFilter(extFilter5);
                    break;
            }
        }
    }

    /**
     * Convert the feature model to a Configurator in Excel.
     */
    @FXML
    private void handleConvert() {
        // TODO: adding a Dialog box with Progress bar to indicate the progress of convert

        ButtonType result = showConvertDialog();

        if (result == ButtonType.OK) {
//            showProgressBar();

//            new Thread() {
//                @Override
//                public void run() {
                    try {
                        // convert
//                        if (featureModel.isConsistency()) {
                            FM2ExConfConverter converter = new FM2ExConfConverter(progressController);
                            String filepath = converter.convert(featureModel);

                            Desktop.getDesktop().open(new File(filepath));
//                        }
                    } catch (IOException | ConvertException e) {
                        // TODO: ConvertException
                        showAlert(Alert.AlertType.ERROR,
                                mainApp.getPrimaryStage(),
                                "Error",
                                e.getMessage());
                    }
//                }
//            }.start();
        }
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("FM2ExConf");

        StringBuilder st = new StringBuilder("A Generator for Excel-based configurators\n");

        st.append("\n").append("FM2ExConf is a tool enabling translate feature models into configurators in Excel worksheet.\n");

        st.append("\n").append("Copyright (C) 2020  AIG team");
        st.append("\n").append("Institute for Software Technology,");
        st.append("\n").append("Graz University of Technology, Austria\n");

        st.append("\n").append("Author: Viet-Man Le");
        st.append("\n").append("Email: vietman.le@ist.tugraz.at");
        st.append("\n").append("Website: http://ase.ist.tugraz.at/ASE/");

        alert.setContentText(st.toString());

        alert.showAndWait();
    }

    private void showProgressBar() {
        try {
            // Load ConvertToExcel dialog
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(mainApp.getClass().getResource("/ProgressDialog.fxml"));
            Parent progressDialog = loader.load();

            progressController = loader.getController();
            progressController.start();
//            convertController.setFeatureModel(featureModel);

            Scene scene = new Scene(progressDialog);

            Stage stage = new Stage();
            // now that we want to open dialog, we must use this line:
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            // TODO: ConvertException
            showAlert(Alert.AlertType.ERROR,
                    mainApp.getPrimaryStage(),
                    "Error",
                    e.getMessage());
        }
    }

    private ButtonType showConvertDialog() {
        try {
            // Load ConvertToExcel dialog
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(mainApp.getClass().getResource("/ConvertToExcel.fxml"));
            Parent convertDialog = loader.load();

            ConvertToExcelController convertController = loader.getController();
            convertController.setFeatureModel(featureModel);

            Scene scene = new Scene(convertDialog);

            Stage stage = new Stage();
            // now that we want to open dialog, we must use this line:
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);
            stage.showAndWait();

            return convertController.result;
        } catch (IOException e) {
            // TODO: ConvertException
            showAlert(Alert.AlertType.ERROR,
                    mainApp.getPrimaryStage(),
                    "Error",
                    e.getMessage());
        }

        return ButtonType.CANCEL;
    }

    /**
     * Perform the analysis operations
     */
    @FXML
    private void handleRunAnalysis() {
        try {
            AnalysisOperator operator = new AnalysisOperator(featureModel, this);
            boolean consistent = operator.run();

            if (consistent) {
                setButtonsEnable(AppState.CONSISTENT);
            } else {
                setButtonsEnable(AppState.INCONSISTENT);
            }
        } catch (Exception e) {
            // TODO: Analysis Exception
            showAlert(Alert.AlertType.ERROR,
                    mainApp.getPrimaryStage(),
                    "Error",
                    e.getMessage());
        }
    }

    /**
     * Save as the feature model in the Descriptive format
     */
    @FXML
    private void handleExportFM4Conf() {
        try {
            FileChooser fileChooser = new FileChooser();

            //Set extension filter for text files
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("FM4Conf files (*.fm4conf)", "*.fm4conf");
            fileChooser.getExtensionFilters().add(extFilter);

            //Show save file dialog
            File file = fileChooser.showSaveDialog(mainApp.getPrimaryStage());

            if (file != null) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(featureModel.getFM4ConfFormat());
                } catch (IOException e) {
                    showAlert(Alert.AlertType.ERROR,
                            mainApp.getPrimaryStage(),
                            "Error",
                            e.getMessage());
                }
            }
        } catch (NullPointerException | IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR,
                    mainApp.getPrimaryStage(),
                    "Error",
                    e.getMessage());
        }
    }

    /**
     * Set enable for buttons on the basis of {@link AppState}
     *
     * If state is INIT: Convert, Run Analysis and Export Descriptive format are disable
     * If state is OPENED (after opening a file):
     *          Run Analysis is enable,
     *          Convert is still disable,
     *          Export to Descriptive format is enable when the format of the opened file is not Descriptive format
     * If state is CONSISTENT: Convert button is enable
     * If state is INCONSISTENT: Convert button is disable
     *
     * @param state - an enum type {@link AppState}
     */
    private void setButtonsEnable(AppState state) {
        switch (state) {
            case INIT:
                this.buttonConvert.setDisable(true);
                this.buttonRunAnalysis.setDisable(true);
//                this.buttonExportFM4Conf.setDisable(true);
                break;
            case OPENED:
                this.buttonConvert.setDisable(false);
//                if (featureModel != null) {
//                    this.buttonExportFM4Conf.setDisable(featureModel.isFM2EXCONFFormat());
//                }
                this.buttonRunAnalysis.setDisable(false);
                break;
            case CONSISTENT:
                this.buttonConvert.setDisable(false);
                break;
            case INCONSISTENT:
                this.buttonConvert.setDisable(true);
                break;
        }
    }

    /**
     * Update information of the feature model to the interface of the app.
     *
     */
    private void showFeatureModelDetails(FeatureModel fm) {
        if (fm == null) {
            fmName.setText("");

            clearContent(textAreaDetails);
            addInformation(textAreaDetails, "Features and their interrelationships will be shown here...",
                    Color.LIGHTGRAY);
            addMetrics(null);
            clearContent(textAreaResults);
            addResult("Feature Model Analysis Results will be shown here...",
                    Color.LIGHTGRAY);
        } else {
            fmName.setText(fm.getName().toUpperCase());

            addDetails(fm);
            addMetrics(fm);
        }
    }

    /**
     * Clear the content of the textArea
     * which shows the results of analysis operations.
     */
    public void clearResults() {
        clearContent(textAreaResults);
    }

    /**
     * Add a string with color to textAreaResults.
     *
     * @param line - a string
     * @param color - a {@link Color}
     */
    public void addResult(String line, Color color) {

        // TODO: multithread
        Platform.runLater(() -> addInformation(textAreaResults, line, color));

        // TODO: kiem tra xem ham nay co can khong
//        refreshAreaResults();
//        textAreaResults.requestFocus();
    }

    /**
     * Show the feature model in the text-based representation
     *
     */
    private void addDetails(FeatureModel fm) {
        if (fm == null) return;

        clearContent(textAreaDetails);

        addInformation(textAreaDetails,"+ FEATURES:", Color.BLUE);
        for (Feature f: fm.getFeatures(FeatureModel.FEATURE_ORDER.BF)) {
            addInformation(textAreaDetails,"\t" + f.toString(), Color.BLACK);
        }

        addInformation(textAreaDetails,"+ RELATIONSHIPS:", Color.BLUE);
        for (Relationship r: fm.getRelationships()) {
            addInformation(textAreaDetails,"\t" + r.getConfRule(), Color.BLACK);
        }

        addInformation(textAreaDetails,"+ CONSTRAINTS:", Color.BLUE);
        for (Relationship c: fm.getConstraints()) {
            addInformation(textAreaDetails,"\t" + c.getConfRule(), Color.BLACK);
        }
    }

    /**
     * Show the metrics of the loaded feature model
     * @param fm - a feature model
     */
    private void addMetrics(FeatureModel fm) {
//        if (fm == null) return;

        clearContent(txtAreaMetrics);

        addInformation(txtAreaMetrics,
                "#Features: \t\t\t" + (fm == null ? " 0" : fm.getNumOfFeatures()),
                Color.BLACK);
        addInformation(txtAreaMetrics,
                "#Relationships: \t" + (fm == null ? " 0" : fm.getNumOfRelationships()),
                Color.BLACK);
        addInformation(txtAreaMetrics,
                "\t#Mandatory: \t" + (fm == null ? " 0" : fm.getNumOfRelationships(Relationship.RelationshipType.MANDATORY)),
                Color.BLACK);
        addInformation(txtAreaMetrics,
                "\t#Optional: \t\t" + (fm == null ? " 0" : fm.getNumOfRelationships(Relationship.RelationshipType.OPTIONAL)),
                Color.BLACK);
        addInformation(txtAreaMetrics,
                "\t#Or: \t\t\t" + (fm == null ? " 0" : fm.getNumOfRelationships(Relationship.RelationshipType.OR)),
                Color.BLACK);
        addInformation(txtAreaMetrics,
                "\t#Alternative: \t" + (fm == null ? " 0" : fm.getNumOfRelationships(Relationship.RelationshipType.ALTERNATIVE)),
                Color.BLACK);
        addInformation(txtAreaMetrics,
                "#Constraint: \t\t" + (fm == null ? " 0" : fm.getNumOfConstraints()),
                Color.BLACK);
        addInformation(txtAreaMetrics,
                "\t#Requires: \t\t" + (fm == null ? " 0" : fm.getNumOfRelationships(Relationship.RelationshipType.REQUIRES)),
                Color.BLACK);
        addInformation(txtAreaMetrics,
                "\t#Excludes: \t\t" + (fm == null ? " 0" : fm.getNumOfRelationships(Relationship.RelationshipType.EXCLUDES)),
                Color.BLACK);
    }

    /**
     * Add a string line to the textflow, with the given color
     */
    private void addInformation(TextFlow textFlow, String line, Color color) {
        if (textFlow == null) return;

        if (!textFlow.getChildren().isEmpty())
            line = "\n" + line;

        Text text = new Text(line);
        text.setFont(new Font(14));
        text.setFill(color);
        textFlow.getChildren().add(text);
    }

    /**
     * Clear the content of the given textflow
     */
    private void clearContent(TextFlow textFlow) {
        if (textFlow != null) {
            textFlow.getChildren().clear();
        }
    }
}
