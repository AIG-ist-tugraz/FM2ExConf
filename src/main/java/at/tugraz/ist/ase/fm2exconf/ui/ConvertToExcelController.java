/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.ui;

import at.tugraz.ist.ase.fm2exconf.core.FeatureModel;
import at.tugraz.ist.ase.fm2exconf.generator.ConvertException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ConvertToExcelController {
    @FXML
    ToggleGroup featuresOrder;
    @FXML
    RadioButton rbtnBF;
    @FXML
    RadioButton rbtnDF;
    @FXML
    ToggleGroup stateOfFeature;
    @FXML
    RadioButton rbtnBinary;
    @FXML
    RadioButton rbtnLogic;
    @FXML
    CheckBox chkPricing;
    @FXML
    ScrollPane holder;

    private FeatureModel featureModel;

    public ButtonType result;
    private PricePane pricePane;

    public void setFeatureModel(FeatureModel fm) {
        featureModel = fm;

        if (featureModel.getFeatureOrder() == FeatureModel.FEATURE_ORDER.BF)
            featuresOrder.selectToggle(rbtnBF);
        else
            featuresOrder.selectToggle(rbtnDF);

        if (featureModel.getFeatureStateType() == FeatureModel.FEATURE_STATE_TYPE.BINARY)
            stateOfFeature.selectToggle(rbtnBinary);
        else
            stateOfFeature.selectToggle(rbtnLogic);

        // add PricePane
        pricePane = new PricePane(featureModel);
        chkPricing.setSelected(featureModel.isPricingSupport());
        holder.setContent(pricePane);
        holder.setDisable(!featureModel.isPricingSupport());
    }

    @FXML
    public void handleConvert(ActionEvent event) throws ConvertException {
        if (featureModel == null) return;

        try {
            RadioButton order = (RadioButton) featuresOrder.getSelectedToggle();
            if (order.getText().equals("Depth-first order")) {
                featureModel.setFeatureOrder(FeatureModel.FEATURE_ORDER.DF);
            } else {
                featureModel.setFeatureOrder(FeatureModel.FEATURE_ORDER.BF);
            }

            RadioButton state = (RadioButton) stateOfFeature.getSelectedToggle();
            if (state.getText().equals("Logical values (TRUE/FALSE)")) {
                featureModel.setFeatureStateType(FeatureModel.FEATURE_STATE_TYPE.LOGIC);
            } else {
                featureModel.setFeatureStateType(FeatureModel.FEATURE_STATE_TYPE.BINARY);
            }

            // pricing
            if (chkPricing.isSelected()) {
                pricePane.savePrice();
                featureModel.setPricingSupport(chkPricing.isSelected());
            }

            result = ButtonType.OK;
        } catch (Exception e) {
            throw new ConvertException("Error occurs in converting!");
        } finally {
            closeStage(event);
        }
    }

    private void closeStage(ActionEvent event) {
        Node source = (Node)  event.getSource();
        Stage stage  = (Stage) source.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void handlePricingChanged(ActionEvent event) {
        if (chkPricing.isSelected()) {
            holder.setDisable(false);
        } else {
            holder.setDisable(true);
        }
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        result = ButtonType.CANCEL;
        closeStage(event);
    }
}
