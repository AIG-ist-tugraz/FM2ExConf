/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2020-2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.core;

import java.util.ArrayList;

import static at.tugraz.ist.ase.fm2exconf.core.Utilities.replaceSpecialCharactersByUnderscore;

/**
 * Represent a feature of a feature model
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class Feature {
    public enum AnomalyType {
        DEAD,
        FALSEOPTIONAL,
        CONDITIONALLYDEAD,
        FULLMANDATORY
    }

    private String name;
    private ArrayList<AnomalyType> anomalyType;
    private Double price;

    /**
     * A constructor with a name of a feature
     *
     * @param name - a name of a feature
     */
    public Feature(String name) {
        this.name = name;
        anomalyType = new ArrayList<>();
        price = 0.0;
    }

    /**
     * @return name of the feature
     */
    public String getName() {
        return name;
    }

    /**
     * Set a new name to the feature
     *
     * @param name - a new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Check whether the feature belongs to an anomaly type
     *
     * @param type - an {@link AnomalyType} type
     * @return true if it is right, false otherwise
     */
    public boolean isAnomalyType(AnomalyType type) {
        return anomalyType.contains(type);
    }

    /**
     * Set an anomaly type to the feature.
     *
     * @param type - an {@link AnomalyType} type
     */
    public void setAnomalyType(AnomalyType type) {
        anomalyType.add(type);
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getExcelFormula() {
        return String.format("%s*%s",
                replaceSpecialCharactersByUnderscore(name),
                price);
    }

    /**
     * Return the name of the feature.
     *
     * @return name of the feature
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Check whether two objects are equals in terms of the same name
     *
     * @param obj - another feature
     * @return true if the same name, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!Feature.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        final Feature other = (Feature) obj;
        return this.name.equals(other.name);
    }
}
