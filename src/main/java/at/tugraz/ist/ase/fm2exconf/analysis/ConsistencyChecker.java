/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */
package at.tugraz.ist.ase.fm2exconf.analysis;

import org.chocosolver.solver.constraints.Constraint;

import java.util.Collection;

/**
 * A consistency checker implementation using the Choco solver, version 4.10.2.
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class ConsistencyChecker {

    /**
     * An internal models
     */
    private ChocoModel model;

    public ConsistencyChecker(ChocoModel diagModel) {
        this.model = diagModel;
    }

    public boolean isConsistent(Collection<Constraint> constraints) { //, boolean reuseModel) {

        reset();
        for (Constraint c : constraints) {
            model.post(c);
        }

        // Call solve()
        try {
            // System.out.println("Start solve..");

            boolean isFeasible = model.getSolver().solve();
            // System.out.println("Solution: " + isFeasible);

            return isFeasible;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception here, " + e.getMessage());
            return false;
        }
    }

    public void reset() {
        model.getSolver().reset();
        model.unpost(model.getCstrs());
    }

    public void dispose() {
        this.model = null;
    }
}
