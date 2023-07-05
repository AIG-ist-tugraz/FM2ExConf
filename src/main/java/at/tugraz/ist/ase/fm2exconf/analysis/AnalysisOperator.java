/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2020-2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.analysis;

import at.tugraz.ist.ase.fm2exconf.core.Feature;
import at.tugraz.ist.ase.fm2exconf.core.FeatureModel;
import at.tugraz.ist.ase.fm2exconf.core.FeatureModelException;
import at.tugraz.ist.ase.fm2exconf.core.Relationship;
import at.tugraz.ist.ase.fm2exconf.ui.MainWindowController;
import javafx.scene.paint.Color;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;

import java.util.*;

import static at.tugraz.ist.ase.fm2exconf.core.Feature.AnomalyType.*;
import static at.tugraz.ist.ase.fm2exconf.core.Utilities.*;

/**
 *
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class AnalysisOperator {
    private ChocoModel model;
    private FeatureModel featureModel;

    private MainWindowController controller;

    public AnalysisOperator(FeatureModel fm, MainWindowController controller) {
        this.featureModel = fm;
        this.controller = controller;

        model = new ChocoModel(fm);
    }

    public boolean run() throws FeatureModelException {
        controller.clearResults();
        boolean consistent;

        // check the consistency
        // check the void feature model
        consistent = checkConsistency();

        if (consistent) {
            // check dead features
            checkDeadFeatures();

            // check conditionally dead
            checkConditionallyDead();

            // check full mandatory
            checkFullMandatory();

            // check false optional
            checkFalseOptional();

            // check redundancies
            checkRedundancies();
        }

        return consistent;
    }

    // CHECK THE CONSISTENCY
    private boolean checkConsistency() {
        System.out.println("Check consistency----------------");
        System.out.println(model.getNbCstrs());
        printConstraints(model);

        boolean consistent = true;

        model.getSolver().reset();
        if (model.getSolver().solve()) {
            this.controller.addResult("\u2713 Consistency: ok", Color.BLUE);

            // TODO: xu cu nay - Java heap
//            model.getSolver().findAllSolutions();
//            this.controller.addResult(String.format("\tValid configurations: %s", model.getSolver().getSolutionCount()), Color.BLACK);

            featureModel.setConsistency(true);
        } else {
            this.controller.addResult("X Void feature model", Color.RED);

            featureModel.setConsistency(false);
            consistent = false;

//            System.out.println("Before calculating all diagnoses----------------");
//            System.out.println(model.getNbCstrs());
//            printConstraints(model);

            List<List<Constraint>> allDiag = calculateAllDiagnoses();

//            System.out.println("After calculating all diagnoses----------------");
//            System.out.println(model.getNbCstrs());
//            printConstraints(model);

//            System.out.println("All diagnoses:---------------------------");
//            printAllDiagnoses(allDiag);

            Map<String, ArrayList<String>> explanations = new LinkedHashMap<>();
            String anomaly = "void feature model";
            explanations.put(anomaly, new ArrayList<>());
            createExplanations(allDiag, anomaly, explanations);

//            addCountingForExplanations(explanations);

            showExplanations(explanations);

            model.resetCstrs();
        }

//        System.out.println("After checking the consistency----------------");
//        System.out.println(model.getNbCstrs());
//        printConstraints(model);

        return consistent;
    }

    // CHECK DEAD FEATURES
    private void checkDeadFeatures() {
        // Find all dead features
        ArrayList<String> deadfeatures = new ArrayList<>();
        Map<String, ArrayList<String>> explanations = new LinkedHashMap<>();

        findDeadFeatures(deadfeatures, explanations);

        // show results
        showAnalysis("Dead feature", deadfeatures);
        showExplanations(explanations);
    }

    // Helper functions for checkDeadFeatures
    private void findDeadFeatures(ArrayList<String> deadfeatures,
                                  Map<String, ArrayList<String>> explanations) {
        for (int i = 1; i < featureModel.getNumOfFeatures(); i++) {
            Feature feature = featureModel.getFeature(i);
            BoolVar v = model.getVarWithName(feature.getName()); // TODO: need try catch

            // add {fi = true}
            model.addClauseTrue(v);

//            System.out.println("Before check the dead features----------------");
//            System.out.println(model.getNbCstrs());
//            printConstraints(model);

            model.getSolver().reset();
            if (!model.getSolver().solve()) {
                deadfeatures.add(feature.getName());
                feature.setAnomalyType(DEAD);

                List<List<Constraint>> allDiag = calculateAllDiagnoses();

                System.out.println("All diagnoses:---------------------------");
                printAllDiagnoses(allDiag);

                explanations.put(feature.getName(), new ArrayList<>());
                createExplanations(allDiag, feature.getName(), explanations);
            } else {
                System.out.println("--------------> Consistent");
            }

            model.resetCstrs();

//            System.out.println("After check the dead features----------------");
//            System.out.println(model.getNbCstrs());
//            printConstraints(model);
        }
    }

    // CHECK FALSE OPTIONAL
    private void checkFalseOptional() throws FeatureModelException {
        // Find all false optional features
        ArrayList<String> falseoptionals = new ArrayList<>();
        Map<String, ArrayList<String>> explanations = new LinkedHashMap<>();

        findFalseOptionals2(falseoptionals, explanations);

        // show results
        showAnalysis("False optional feature", falseoptionals);
        showExplanations(explanations);
    }

    private void findFalseOptionals(ArrayList<String> falseoptionals,
                                    Map<String, ArrayList<String>> explanations) {
        for (int i = 1; i < featureModel.getNumOfFeatures(); i++) {
            Feature feature = featureModel.getFeature(i);

            // Feature nay phai la optional - not mandatory
            if (featureModel.isOptionalFeature(feature)) {

//                if (feature.isAnomalyType(FULLMANDATORY)) {
//                    falseoptionals.add(feature.getName());
//                    feature.setAnomalyType(FALSEOPTIONAL);
//
//                    model.getSolver().reset();
//                    List<List<Constraint>> allDiag = calculateAllDiagnoses();
//
////                                System.out.println("All diagnoses:---------------------------");
////                                printAllDiagnoses(allDiag);
//
//                    explanations.put(feature.getName(), new ArrayList<>());
//                    createExplanations(allDiag, feature.getName(), explanations);
//
//                    model.resetCstrs();
//                    continue;
//                }

                ArrayList<Feature> parents = featureModel.getLeftSideOfRequiresConstraint(feature);

                if (parents.size() > 0) { // co tham gia vao ve phai cua a requires constraint

                    BoolVar v = model.getVarWithName(feature.getName()); // TODO: need try catch

                    // add {f_opt = false}
                    model.addClauseFalse(v);

                    for (Feature parent : parents) {

                        if (featureModel.isMandatoryFeature(parent)) { // trong mot so truong hop no co the bat cau

                            BoolVar p = model.getVarWithName(parent.getName());
                            // add {f_p = true}
                            model.addClauseTrue(p);

//                            System.out.println("Before check false optional----------------");
//                            System.out.println(model.getNbCstrs());
//                            printConstraints(model);

                            model.getSolver().reset();
                            if (!model.getSolver().solve()) {
                                falseoptionals.add(feature.getName());
                                feature.setAnomalyType(FALSEOPTIONAL);

                                List<List<Constraint>> allDiag = calculateAllDiagnoses();

//                                System.out.println("All diagnoses:---------------------------");
//                                printAllDiagnoses(allDiag);

                                explanations.put(feature.getName(), new ArrayList<>());
                                createExplanations(allDiag, feature.getName(), explanations);
                            } else {
                                System.out.println("--------------> Consistent");
                            }

                            model.resetCstrs();
                            // readd {f_opt = false}
                            model.addClauseFalse(v);

//                            System.out.println("After check false optional----------------");
//                            System.out.println(model.getNbCstrs());
//                            printConstraints(model);
                        }
                    }
                    model.resetCstrs();
                }
            }
        }

//        addCountingForExplanations(explanations);
    }

    private void findFalseOptionals1(ArrayList<String> falseoptionals,
                                    Map<String, ArrayList<String>> explanations) throws FeatureModelException {
        for (int i = 1; i < featureModel.getNumOfFeatures(); i++) {
            Feature feature = featureModel.getFeature(i);

            // Feature nay phai la optional - not mandatory
            if (featureModel.isOptionalFeature(feature)) {

                ArrayList<Feature> parents = featureModel.getMandatoryParents(feature);

                if (parents.size() > 0) { // co tham gia vao ve phai cua a requires constraint

                    BoolVar v = model.getVarWithName(feature.getName()); // TODO: need try catch

                    // add {f_opt = false}
                    model.addClauseFalse(v);

                    for (Feature parent : parents) {

                        if (featureModel.isMandatoryFeature(parent)) { // trong mot so truong hop no co the bat cau

                            BoolVar p = model.getVarWithName(parent.getName());
                            // add {f_p = true}
                            model.addClauseTrue(p);

//                            System.out.println("Before check false optional----------------");
//                            System.out.println(model.getNbCstrs());
//                            printConstraints(model);

                            model.getSolver().reset();
                            if (!model.getSolver().solve()) {
                                if (!isExistInArrayList(falseoptionals, feature.getName())) {
                                    falseoptionals.add(feature.getName());
                                    feature.setAnomalyType(FALSEOPTIONAL);
                                }

                                List<List<Constraint>> allDiag = calculateAllDiagnoses();

//                                System.out.println("All diagnoses:---------------------------");
//                                printAllDiagnoses(allDiag);

                                if (!explanations.containsKey(feature.getName())) {
                                    explanations.put(feature.getName(), new ArrayList<>());
                                }

                                createExplanations(allDiag, feature.getName(), explanations);
//                                addCountingForExplanations(explanations);
                            } else {
                                System.out.println("--------------> Consistent");
                            }

                            model.resetCstrs();
                            // readd {f_opt = false}
                            model.addClauseFalse(v);

//                            System.out.println("After check false optional----------------");
//                            System.out.println(model.getNbCstrs());
//                            printConstraints(model);
                        }
                    }
                    model.resetCstrs();
                }
            }
        }

//        addCountingForExplanations(explanations);
    }

    private void findFalseOptionals2(ArrayList<String> falseoptionals,
                                     Map<String, ArrayList<String>> explanations) throws FeatureModelException {
        for (int i = 1; i < featureModel.getNumOfFeatures(); i++) {
            Feature feature = featureModel.getFeature(i);

            // Feature nay phai la optional - not mandatory
            if (featureModel.isOptionalFeature(feature)) {

                ArrayList<Feature> parents = featureModel.getMandatoryParents(feature);

                if (parents.size() > 0) { // co tham gia vao ve phai cua a requires constraint

                    BoolVar v = model.getVarWithName(feature.getName()); // TODO: need try catch

                    // add {f_opt = false}
                    model.addClauseFalse(v);

                    boolean haveMandatory = false;
                    for (Feature parent : parents) {

                        if (featureModel.isMandatoryFeature(parent)) { // trong mot so truong hop no co the bat cau

                            BoolVar p = model.getVarWithName(parent.getName());
                            // add {f_p = true}
                            model.addClauseTrue(p);
                            haveMandatory = true;
                            break;
                        }
                    }

//                            System.out.println("Before check false optional----------------");
//                            System.out.println(model.getNbCstrs());
//                            printConstraints(model);

                    if (haveMandatory) {
                        model.getSolver().reset();
                        if (!model.getSolver().solve()) {
                            if (!isExistInArrayList(falseoptionals, feature.getName())) {
                                falseoptionals.add(feature.getName());
                                feature.setAnomalyType(FALSEOPTIONAL);
                            }

                            List<List<Constraint>> allDiag = calculateAllDiagnoses();

//                                System.out.println("All diagnoses:---------------------------");
//                                printAllDiagnoses(allDiag);

                            if (!explanations.containsKey(feature.getName())) {
                                explanations.put(feature.getName(), new ArrayList<>());
                            }

                            createExplanations(allDiag, feature.getName(), explanations);
                        } else {
                            System.out.println("--------------> Consistent");
                        }

                        model.resetCstrs();
                        // readd {f_opt = false}
                        model.addClauseFalse(v);

//                            System.out.println("After check false optional----------------");
//                            System.out.println(model.getNbCstrs());
//                            printConstraints(model);
//                        }
                    }
                    model.resetCstrs();
                }
            }
        }
    }

    // CHECK CONDITIONALLY DEAD
    private void checkConditionallyDead() {
        // Find all conditionally deads
        ArrayList<String> conditionallydeads = new ArrayList<>();
        Map<String, ArrayList<String>> explanations = new LinkedHashMap<>();

        findConditionallyDead(conditionallydeads, explanations);

        // show results
        showAnalysis("Conditionally dead feature", conditionallydeads);
        showExplanations(explanations);
    }

    private void findConditionallyDead(ArrayList<String> conditionallydeads,
                                       Map<String, ArrayList<String>> explanations) {
        for (int i = 1; i < featureModel.getNumOfFeatures(); i++) {
            Feature fi = featureModel.getFeature(i);

            // a feature is not DEAD and have to be optional
            if (!fi.isAnomalyType(DEAD) && featureModel.isOptionalFeature(fi)) {

                for (int j = 1; j < featureModel.getNumOfFeatures(); j++) {
                    if (j != i) {
                        BoolVar vi = model.getVarWithName(fi.getName());

                        Feature fj = featureModel.getFeature(j);
                        if (fj.isAnomalyType(DEAD)) continue;

                        BoolVar vj = model.getVarWithName(fj.getName());

                        // add {fi = true}
                        model.addClauseTrue(vi);
                        // add {fj = true}
                        model.addClauseTrue(vj);

//                        System.out.println("Before the checking----------------");
//                        System.out.println(model.getNbCstrs());
//                        printConstraints(model);

                        model.getSolver().reset();
                        if (!model.getSolver().solve()) {
                            System.out.println("------------> inConsistent: " + fi);

                            if (!isExistInArrayList(conditionallydeads, fi.toString())) { // neu chua moi them vao
                                conditionallydeads.add(fi.toString());
                                fi.setAnomalyType(CONDITIONALLYDEAD);

                                List<List<Constraint>> allDiag = calculateAllDiagnoses();

//                                System.out.println("All diagnoses:---------------------------");
//                                printAllDiagnoses(allDiag);

                                explanations.put(fi.getName(), new ArrayList<>());
                                createExplanations(allDiag, fi.getName(), explanations);
                            }
                        } else {
                            System.out.println("------------> Consistent");
                        }

                        model.resetCstrs();

//                        System.out.println("Before the checking----------------");
//                        System.out.println(model.getNbCstrs());
//                        printConstraints(model);
                    }
                }
            }
        }
    }

    // CHECK FULL MANDATORY
    private void checkFullMandatory() {
        // Find all full mandatory
        ArrayList<String> fullmandatorys = new ArrayList<>();
        Map<String, ArrayList<String>> explanations = new LinkedHashMap<>();

        findFullMandatory(fullmandatorys, explanations);

        // show results
        showAnalysis("Full mandatory feature", fullmandatorys);
        showExplanations(explanations);
    }

    private void findFullMandatory(ArrayList<String> fullmandatorys,
                                   Map<String, ArrayList<String>> explanations) {
        for (int i = 1; i < featureModel.getNumOfFeatures(); i++) {
            Feature feature = featureModel.getFeature(i);
            BoolVar v = model.getVarWithName(feature.getName()); // TODO: need try catch

            // add {fi = false}
            model.addClauseFalse(v);

//            System.out.println("Before check the dead features----------------");
//            System.out.println(model.getNbCstrs());
//            printConstraints(model);

            model.getSolver().reset();
            if (!model.getSolver().solve()) {
                fullmandatorys.add(feature.getName());
                feature.setAnomalyType(FULLMANDATORY);

                List<List<Constraint>> allDiag = calculateAllDiagnoses();

//                System.out.println("All diagnoses:---------------------------");
//                printAllDiagnoses(allDiag);

                explanations.put(feature.getName(), new ArrayList<>());
                createExplanations(allDiag, feature.getName(), explanations);
            } else {
                System.out.println("--------------> Consistent");
            }

            model.resetCstrs();

//            System.out.println("After check the dead features----------------");
//            System.out.println(model.getNbCstrs());
//            printConstraints(model);
        }
    }

    // CHECK REDUNDANCIES
    private void checkRedundancies() {
        System.out.println("Check Redundancies");
        ArrayList<String> redundancies = new ArrayList<>();
        // chay FMCORE de tim ra minimal core
        FMCore(redundancies);

//        System.out.println("After FMCore----------------");
//        System.out.println(model.getNbCstrs());
//        printConstraints(model);

        // show results
        showAnalysis("Redundant constraint", redundancies);
    }

    private void FMCore(ArrayList<String> redundancies) {
//        System.out.println("Original constraints----------------");
//        System.out.println(model.getNbCstrs());
//        printConstraints(model);

        // duyet qua tung constraint
        ArrayList<Relationship> constraints = featureModel.getConstraints();
        for (Relationship constraint : constraints) {
            String cstr = constraint.getConstraints().get(0);

            System.out.println("CHECK " + cstr.toUpperCase());

            Constraint choco_cstr = model.getCstr(cstr);

            System.out.println("-------------------------------------------");
            System.out.println(choco_cstr);
            System.out.println("-------------------------------------------");

            if (choco_cstr != null) {
                // loai ra khoi CF
                model.unpost(choco_cstr);

                List<Constraint> not_cstr = model.postNotConstraint(constraint);
                System.out.println("NOT NOT CONSTRAINT-------------------------------------------");
                System.out.println(not_cstr);
                System.out.println("-------------------------------------------");

//                System.out.println("After post NOT constraint----------------");
//                System.out.println(model.getNbCstrs());
//                printConstraints(model);

                // if consistent(CF - {ci} U {not ci})
                model.getSolver().reset();
                if (model.getSolver().solve()) {
                    // bo vao lai
                    System.out.println("----------------------- Consistent");
                    model.post(choco_cstr);
                } else {
                    redundancies.add(constraint.getConfRule());
                    System.out.println("----------------------- inConsistent");
                }

                for (Constraint c: not_cstr) {
                    model.unpost(c);
                }

                System.out.println("After repost the constraint----------------");
                System.out.println(model.getNbCstrs());
                printConstraints(model);
            }
        }
    }

    // HELPER FUNCTIONS FOR ALL ANALYSES
    private void showAnalysis(String title, ArrayList<String> features) {
        if (features.size() > 0) {

            String st = "X " + title;
            if (features.size() > 1) {
                st += "s";
            }
            st += " (" + features.size() + "): ";

            if (title.equals("Redundant constraint")) {
                controller.addResult(st, Color.RED);

                for (String s: features) {
                    controller.addResult("\t[" + s + "]" , Color.BLACK);
                }
            } else {
                st = st + createStringFromArrayWithSeparator(features, ",");
                controller.addResult(st, Color.RED);
            }
        } else {
            controller.addResult("\u2713 " + title + ": 0", Color.BLUE);
        }
    }

    private void showExplanations(Map<String, ArrayList<String>> explanations) {

        explanations.forEach((key, value) -> {
            if (value.size() > 0) {
                StringBuilder st = new StringBuilder("\tExplanation(s) for " + key + ":\n");
                for (String e : value) {
                    st.append("\t\t").append(e).append("\n");
                }
                String line = st.substring(0, st.length() - 1);
                controller.addResult(line, Color.BLACK);
            }
        });
    }

    private void createExplanations(List<List<Constraint>> allDiag,
                                    String anomaly,
                                    Map<String, ArrayList<String>> explanations) {
        ArrayList<String> ex = explanations.get(anomaly);
        for (List<Constraint> diag : allDiag) {
            String diagnosis = createDiagnosis(diag);

            // kiem tra xem co trong explanations da co hay chua
            if (!isExistInArrayList(ex, diagnosis)) { // neu chua moi them vao
                ex.add(diagnosis);
            }
        }

        addCountingForExplanations(ex);
    }

    private String createDiagnosis(List<Constraint> diag) {
        StringBuilder s = new StringBuilder("[");
        for (Constraint cstr: diag) {
            Relationship r = featureModel.getRelationship(cstr.toString());
            if (r != null) {
                s.append(r.getConfRule()).append(",");
            }
        }
        s.deleteCharAt(s.length() - 1);
        s.append("]");

        return s.toString();
    }

    private void addCountingForExplanations(ArrayList<String> explanations) {
        ArrayList<String> temp = new ArrayList<>();
        int count = 0;
        for (String st : explanations) {
            count++;
            String newSt = "Diagnosis " + count + ": " +  st;
            temp.add(newSt);
        }

        explanations.clear();
        explanations.addAll(temp);
    }

    private List<List<Constraint>> calculateAllDiagnoses() {
        List<Constraint> c = new ArrayList<>(model.getCF());
        // reverse order before call FastDiag
        Collections.reverse(c);

//        System.out.println("After getting CF----------------");
//        System.out.println(model.getNbCstrs());
//        printConstraints(model);

        List<Constraint> ac = Arrays.asList(model.getCstrs());
        // reverse order before call FastDiag
        Collections.reverse(ac);

//        System.out.println("After get AC----------------");
//        System.out.println(model.getNbCstrs());
//        printConstraints(model);

        // run the fastDiag to find diagnoses
        List<List<Constraint>> allDiag = new ArrayList<>();

        List<Constraint> diag = FastDiag.fastDiag(c, ac, model);

//        System.out.println("After first fastDiag----------------");
//        System.out.println(model.getNbCstrs());
//        printConstraints(model);

        FastDiag.calculateAllDiagnoses(diag, c, ac, model, allDiag);

//        System.out.println("After calculating all diagnoses----------------");
//        System.out.println(model.getNbCstrs());
//        printConstraints(model);

        return allDiag;
    }
}
