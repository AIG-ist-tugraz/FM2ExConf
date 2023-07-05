/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf;

import at.tugraz.ist.ase.fm2exconf.ui.MainWindowController;
import at.tugraz.ist.ase.fm2exconf.ui.RootLayoutController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

import static at.tugraz.ist.ase.fm2exconf.core.Utilities.showAlert;

/**
 * MainApp is the entry point of the application.
 * This class loads and takes care for the main screen of the application.
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class MainApp extends Application {

    private String defaultTitle = "FM2Conf";

    private Stage primaryStage;
    private BorderPane rootLayout;
    private AnchorPane mainWindow;

    private MainWindowController mainWindowController;

    /**
     * Returns the main stage of the application.
     * @return the main stage
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * The entry point of the application.
     *
     * @param primaryStage
     */
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        updateTitle(null);

        // TODO: app icon.
        // Set the application icon.
//        this.primaryStage.getIcons().add(new Image("/images/address_book_32.png"));

        // TODO: rootlayout
//        initRootLayout();

        showMainWindow();
    }

    /**
     * Initializes the root layout and tries to load the last opened
     * person file.
     */
    public void initRootLayout() {
        try {
            // Load root layout from fxml file.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class
                    .getResource("/RootLayout.fxml"));
            rootLayout = (BorderPane) loader.load();

//            MenuToolkit tk = MenuToolkit.toolkit(Locale.getDefault());
//            tk.setApplicationMenu(tk.createDefaultApplicationMenu("FM2ExConf"));
//
//            // check Mac OS X
//            final String os = System.getProperty("os.name");
//            if (os != null && os.startsWith("Mac"))
//                ((MenuBar)rootLayout.getTop()).useSystemMenuBarProperty().set(true);

            // Show the scene containing the root layout.
            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.setMinHeight(550);
            primaryStage.setMinWidth(650);
            primaryStage.setResizable(false);

            // Give the controller access to the main app.
            RootLayoutController controller = loader.getController();
            controller.setMainApp(this);

            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Try to load last opened person file.
//        File file = getPersonFilePath();
//        if (file != null) {
//            loadPersonDataFromFile(file);
//        }
    }

    /**
     * Shows the main window inside the root layout.
     */
    public void showMainWindow() {
        try {
            // Load main window
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/MainWindow.fxml"));
            mainWindow = (AnchorPane) loader.load();

            // Show the scene containing the root layout.
            Scene scene = new Scene(mainWindow);
            primaryStage.setScene(scene);

            // Set the main window into the center of root layout.
//            rootLayout.setCenter(mainWindow);

            // Give the controller access to the main app.
            mainWindowController = loader.getController();
            mainWindowController.setMainApp(this);

            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR,
                    this.getPrimaryStage(),
                    "Error",
                    e.getMessage());
        }
    }

    /**
     * Update the stage title with a file path of the currently loaded file.
     * If the file path is not provided, the default title (e.g., "FM2ExConf") will be used.
     *
     * @param filePath - the string of a file path of the currently loaded file
     */
    public void updateTitle(String filePath) {
        if (filePath == null) {
            primaryStage.setTitle(defaultTitle);
        } else {
            primaryStage.setTitle(defaultTitle + " - " + filePath);
        }
    }
}
