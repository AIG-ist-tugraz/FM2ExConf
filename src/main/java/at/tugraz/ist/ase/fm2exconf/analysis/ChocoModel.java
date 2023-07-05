/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2020-2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.analysis;

import at.tugraz.ist.ase.fm2exconf.core.FeatureModel;
import at.tugraz.ist.ase.fm2exconf.core.Relationship;
import org.apache.commons.collections4.ListUtils;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.nary.cnf.LogOp;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An extension class of {@link Model} of ChocoSolver library.
 * It performs the consistency checking and the configuration determination
 * on the basic of a {@link FeatureModel}.
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class ChocoModel extends Model {

    private FeatureModel featureModel;
    // a set of feature model constraints,
    // without the constraint for the root feature
    // {f0 = true}
    private List<Constraint> cf;
    // a set of feature model constraints
    // with the constraint for the root feature
    // {f0 = true}
    private List<Constraint> ac;

    /**
     * A constructor
     * On the basic of a given {@link FeatureModel}, it creates
     * corresponding variables and constraints for the model.
     *
     * @param fm - a {@link FeatureModel}
     *
     * @throws NullPointerException when the input is null
     * @throws IllegalArgumentException when couldn't find the corresponding variable on the model
     */
    public ChocoModel(FeatureModel fm) throws NullPointerException, IllegalArgumentException {
        if (fm == null) throw new NullPointerException("The feature model have to be not null!");

        this.featureModel = fm;
        setName(fm.getName());

        createVariables();
        createConstraints();

        cf = new ArrayList<Constraint>();
        cf.addAll(Arrays.asList(this.getCstrs()));

        // {f0 = true}
        addClauseTrue(getVarWithName(featureModel.getName()));

        ac = new ArrayList<Constraint>();
        ac.addAll(Arrays.asList(this.getCstrs()));
    }

    /**
     * Return the set of feature model constraints.
     * This set don't have the constraint for the root feature {f0 = true}
     *
     * @return the set of feature model constraints in the form of List<Constraint>
     */
    public List<Constraint> getCF() {
        return cf;
    }

    /**
     * Return the set of background knowledge, including corrected constraints
     *
     * @return the set of background knowledge in the form of List<Constraint>
     */
    public List<Constraint> getB() {
        List<Constraint> AC = Arrays.asList(this.getCstrs());

        return ListUtils.subtract(AC, cf);
    }

    /**
     * Reset the constraints of the feature model
     * into the initial set which includes the constraint {f0 = true}
     */
    public void resetCstrs() {
        getSolver().reset(); // reset the solver
        unpost(getCstrs()); // remove all actual constraints
        for (int j=0; j< ac.size(); j++) // reset the initial set of constraints
        {
            post(ac.get(j));
        }
    }

    /**
     * On the basic of a given {@link FeatureModel}, this function creates
     * corresponding variables for the model.
     */
    private void createVariables() {
        BoolVar[] x = new BoolVar[featureModel.getNumOfFeatures()];
        for (int i = 0; i < featureModel.getNumOfFeatures(); i++)
        {
            x[i] = this.boolVar(featureModel.getFeature(i).getName());
        }
    }

    /**
     * On the basic of a given {@link FeatureModel}, this function creates
     * corresponding constraints for the model.
     */
    private void createConstraints() throws IllegalArgumentException {
        // first convert relationships into constraints
        for (Relationship relationship: featureModel.getRelationships()) {
            BoolVar leftVar = getVarWithName(relationship.getLeftSide());
            BoolVar rightVar;

            int oldNumCstrs = this.getNbCstrs();
            int newNumCstrs = oldNumCstrs;
            switch (relationship.getType())
            {
                case MANDATORY:
                    rightVar = getVarWithName(relationship.getRightSide().get(0));
                    // leftVar <=> rightVar
                    addClauses(LogOp.ifOnlyIf(leftVar, rightVar));

                    newNumCstrs = this.getNbCstrs();
                    break;
                case OPTIONAL:
                    rightVar = getVarWithName(relationship.getRightSide().get(0));
                    // leftVar => rightVar
                    this.addClauses(LogOp.implies(leftVar, rightVar));

                    newNumCstrs = this.getNbCstrs();
                    break;
                case OR:
                    // LogOp of rule {A \/ B \/ ... \/ C}
                    LogOp rightLogOp = getRightSideOfOrRelationship(relationship.getRightSide());
                    // leftVar <=> rightLogOp
                    this.addClauses(LogOp.ifOnlyIf(leftVar, rightLogOp));

                    newNumCstrs = this.getNbCstrs();
                    break;
                case ALTERNATIVE:
                    // LogOp of an ALTERNATIVE relationship
                    LogOp op = getLogOpOfAlternativeRelationship(relationship);
                    this.addClauses(op);

                    newNumCstrs = this.getNbCstrs();
                    break;
            }

            setConstraintsToRelationship(oldNumCstrs, newNumCstrs, relationship);
        }

        // second convert constraints of {@link FeatureModel} into ChocoSolver constraints
        for (Relationship relationship: featureModel.getConstraints()) {
            BoolVar leftVar = getVarWithName(relationship.getLeftSide());
            BoolVar rightVar = getVarWithName(relationship.getRightSide().get(0));

            int oldNumCstrs = this.getNbCstrs();
            int newNumCstrs = oldNumCstrs;
            switch (relationship.getType())
            {
                case REQUIRES:
                    this.addClauses(LogOp.implies(leftVar, rightVar));
                    newNumCstrs = this.getNbCstrs();
                    break;
                case EXCLUDES:
                    this.addClauses(LogOp.or(LogOp.nor(leftVar), LogOp.nor(rightVar)));
                    newNumCstrs = this.getNbCstrs();
                    break;
            }

            setConstraintsToRelationship(oldNumCstrs, newNumCstrs, relationship);
        }
    }

    /**
     * Given a constraint of {@link FeatureModel}, this function convert it into
     * a negative constraint and post the new constraint to the model.
     *
     * ChocoSolver could convert one input constraint into multiple rules
     * in the form that ChocoSolver could process. Thus, the output of this function
     * have to be a list of {@link Constraint}.
     *
     * @param constraint - a constraint of {@link FeatureModel}
     * @return a list of ChocoSolver constraints that represent the given constraint
     * @throws IllegalArgumentException when couldn't find the corresponding variable in the model
     */
    public List<Constraint> postNotConstraint(Relationship constraint) throws IllegalArgumentException {
        int old_NbCstrs = this.getNbCstrs();

        BoolVar leftVar = getVarWithName(constraint.getLeftSide());
        BoolVar rightVar = getVarWithName(constraint.getRightSide().get(0));

        // create a negative constraint on the basic of the type of the given constraint
        // and add to the model
        switch (constraint.getType())
        {
            case REQUIRES:
                this.addClauses(LogOp.and(leftVar, LogOp.nor(rightVar)));
                break;
            case EXCLUDES:
                this.addClauses(LogOp.and(leftVar, rightVar));
                break;
        }

        // take the created constraints
        List<Constraint> constraints = new ArrayList<>();
        int num = this.getNbCstrs() - old_NbCstrs;
        for (int i = 0; i < num; i++)
            constraints.add(this.getCstrs()[old_NbCstrs + i]);

        // return the created constraints
        return constraints;
    }

    /**
     * Add back the created constraints to the {@link Relationship}.
     * It means that the {@link Relationship} holds references to the constraint in the ChocoSolver model.
     * This allows us to reuse the constraints without recreating.
     *
     * @param oldNumCstrs - the number of old constraints
     * @param newNumCstrs - the number of all constraints
     * @param relationship - a {@link Relationship}
     */
    private void setConstraintsToRelationship(int oldNumCstrs, int newNumCstrs, Relationship relationship) {
        Constraint[] constraints = this.getCstrs();
        for (int i = 0; i < constraints.length; i++) {
            if (oldNumCstrs != newNumCstrs && i >= oldNumCstrs && i < newNumCstrs) {
                relationship.setConstraint(constraints[i].toString());
            }
        }
    }

    /**
     * Create a {@link LogOp} that represent to an ALTERNATIVE relationship.
     * The form of rule is {C1 <=> (not C2 /\ ... /\ not Cn /\ P) /\
     *                      C2 <=> (not C1 /\ ... /\ not Cn /\ P) /\
     *                      ... /\
     *                      Cn <=> (not C1 /\ ... /\ not Cn-1 /\ P)
     *
     * @param relationship - a {@link Relationship} of {@link FeatureModel}
     * @return A {@link LogOp} that represent to an ALTERNATIVE relationship
     * @throws IllegalArgumentException when couldn't find the corresponding variable in the model
     */
    private LogOp getLogOpOfAlternativeRelationship(Relationship relationship) throws IllegalArgumentException {
        LogOp op = LogOp.and(); // an LogOp of AND operators
        for (int i = 0; i < relationship.getRightSide().size(); i++) {
            BoolVar rightVar = getVarWithName(relationship.getRightSide().get(i));
            // (not C2 /\ ... /\ not Cn /\ P)
            LogOp rightSide = getRightSideOfAlternativeRelationship(relationship.getLeftSide(), relationship.getRightSide(), i);
            // {C1 <=> (not C2 /\ ... /\ not Cn /\ P)}
            LogOp part = LogOp.ifOnlyIf(rightVar, rightSide);
            op.addChild(part);
        }
        return op;
    }

    /**
     * Create a {@link LogOp} that represent the rule {(not C2 /\ ... /\ not Cn /\ P)}.
     * This is the right side of the rule {C1 <=> (not C2 /\ ... /\ not Cn /\ P)}
     *
     * @param leftSide - the name of the parent feature
     * @param rightSide - names of the child features
     * @param removedIndex - the index of the child feature that is the left side of the rule
     * @return a {@link LogOp} that represent the rule {(not C2 /\ ... /\ not Cn /\ P)}.
     * @throws IllegalArgumentException when couldn't find the variable in the model
     */
    private LogOp getRightSideOfAlternativeRelationship(String leftSide, ArrayList<String> rightSide, int removedIndex) throws IllegalArgumentException {
        BoolVar leftVar = getVarWithName(leftSide);
        LogOp op = LogOp.and(leftVar);
        for (int i = 0; i < rightSide.size(); i++) {
            if (i != removedIndex) {
                op.addChild(LogOp.nor(getVarWithName(rightSide.get(i))));
            }
        }
        return op;
    }

    /**
     * Create a {@link LogOp} for the right side of an OR relationship.
     * The form of rule is {A \/ B \/ ... \/ C}.
     *
     * @param rightSide - an array of feature names which belong to the right side of an OR relationship
     * @return a {@link LogOp} or null if the rightSide is empty
     * @throws IllegalArgumentException when couldn't find a variable which corresponds to the given feature name
     */
    private LogOp getRightSideOfOrRelationship(ArrayList<String> rightSide) throws IllegalArgumentException {
        if (rightSide.size() == 0) return null;
        LogOp op = LogOp.or(); // create a LogOp of OR operators
        for (int i = 0; i < rightSide.size(); i++) {
            BoolVar var = getVarWithName(rightSide.get(i));
            op.addChild(var);
        }
        return op;
    }

    /**
     * On the basic of a feature name, this function return
     * the corresponding ChocoSolver variable in the model.
     *
     * @param name - a feature name
     * @return the corresponding ChocoSolver variable in the model or null
     * @throws IllegalArgumentException when couldn't find the variable in the model
     */
    public BoolVar getVarWithName(String name) throws IllegalArgumentException {
        Variable var = null;
        for (Variable v : this.getVars()) {
            if (v.getName().equals(name)) {
                var = v;
                break;
            }
        }
        if (var == null)
            throw new IllegalArgumentException("The feature " + name + " is not exist in the feature model!");
        return (BoolVar) var;
    }

    /**
     * Return a {@link Constraint} in the model based on the text-based rule of that constraint.
     *
     * @param cstr - the text-based rule of a constraint
     * @return a {@link Constraint} or null
     * @throws IllegalArgumentException when couldn't find the constraint in the model
     */
    public Constraint getCstr(String cstr) throws IllegalArgumentException {
        Constraint[] constraints = this.getCstrs();
        for (int i = 0; i < constraints.length; i++) {
            if (constraints[i].toString().equals(cstr))
                return constraints[i];
        }
        throw new IllegalArgumentException("The constraint " + cstr + " is not exist in the feature model!");
    }
}
