/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.ui;

import at.tugraz.ist.ase.fm2exconf.core.Feature;
import at.tugraz.ist.ase.fm2exconf.core.FeatureModel;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

public class PricePane extends GridPane {

    private FeatureModel featureModel;
    private Map<TextField, Feature> map = new HashMap<>();

    public PricePane(FeatureModel fm) {
        featureModel = fm;

        int row = 0;
        for (Feature feature: featureModel.getFeatures(FeatureModel.FEATURE_ORDER.BF)) {
            row = row + 1;

            Label label = new Label(feature.getName());

            String price = format(feature.getPrice());

            TextField textField = new TextField(price);

            map.put(textField, feature);

            textField.focusedProperty().addListener((arg0, oldValue, newValue) -> {
                if (!newValue) { //when focus lost
                    if(!textField.getText().matches("\\d{0,7}([\\.]\\d{0,4})?")){
                        textField.setText(format(0.0));
                    } else {
                        Double newPrice = Double.parseDouble(textField.getText());

                        map.get(textField).setPrice(newPrice);

                        textField.setText(format(newPrice));
                    }
                }

            });

            add(label, 0, row);
            add(textField, 1, row);
        }

        setHgap(10);
        setVgap(10);
    }

    public void savePrice() {
        map.forEach((textField,feature) -> {
            Double newPrice = 0.0;
            if (!textField.getText().isEmpty()) {
                try {
                    newPrice = Double.parseDouble(textField.getText());
                } catch (NumberFormatException e) {
                    newPrice = 0.0;
                }
            }

            feature.setPrice(newPrice);
        });
    }

    private String format(Double value) {
        NumberFormat format = new DecimalFormat("#0.00");
        String formatted = format.format(value);
        return formatted;
    }
}
