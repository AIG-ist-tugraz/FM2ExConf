/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2020-2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
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
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class PricePane extends GridPane {

    private final Map<TextField, Feature> map = new HashMap<>();
    private final NumberFormat format = new DecimalFormat("#0.00");

    public PricePane(FeatureModel fm) {

        int row = 0;
        for (Feature feature: fm.getFeatures(FeatureModel.FEATURE_ORDER.BF)) {
            row = row + 1;

            Label label = new Label(feature.getName());

            String price = format.format(feature.getPrice());

            TextField textField = new TextField(price);

            map.put(textField, feature);

            textField.focusedProperty().addListener((arg0, oldValue, newValue) -> {
                if (!newValue) { //when focus lost
                    if(!textField.getText().matches("\\d{0,7}([\\.]\\d{0,4})?")){
                        textField.setText(format.format(0.0));
                    } else {
                        Double newPrice = Double.parseDouble(textField.getText());

                        map.get(textField).setPrice(newPrice);

                        textField.setText(format.format(newPrice));
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
            double newPrice = 0.0;
            if (!textField.getText().isEmpty()) {
                try {
                    newPrice = format.parse(textField.getText()).doubleValue();
                } catch (NumberFormatException | ParseException e) {
                    newPrice = 0.0;
                }
            }

            feature.setPrice(newPrice);
        });
    }
}
