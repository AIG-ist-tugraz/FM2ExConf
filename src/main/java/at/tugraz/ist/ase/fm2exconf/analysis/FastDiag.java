/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */

package at.tugraz.ist.ase.fm2exconf.analysis;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//FastDiag Algorithm
//--------------------
//func FastDiag(C ⊆ AC, AC = {c1..ct}) :  Δ
//if isEmpty(C) or inconsistent(AC - C) return Φ
//else return FD(Φ, C, AC)

//func FD(D, C = {c1..cq}, AC) : diagnosis  Δ
//if D != Φ and consistent(AC) return Φ;
//if singleton(C) return C;
//k = q/2;
//C1 = {c1..ck}; C2 = {ck+1..cq};
//D1 = FD(C1, C2, AC - C1);
//D2 = FD(D1, C1, AC - D1);
//return(D1 ∪ D2);

/**
 * The class implements the FastDiag algorithm.
 *
 * @author Muslum Atas (muesluem.atas@ist.tugraz.at)
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class FastDiag {

    public static List<Constraint> fastDiag(List<Constraint> c, List<Constraint> ac, Model model)
    {
        List<Constraint> acOriginal = new ArrayList<Constraint>(); acOriginal.addAll(ac);
        //if isEmpty(C) or inconsistent(AC - C) return Φ
        if (c.isEmpty()
            || (ac.containsAll(c) && isConsistent(ac, model))
            || (!isConsistent(subConstrsSets(acOriginal,c), model))) {
            return Collections.<Constraint>emptyList();
        } else{ //else return FD(Φ, C, AC)
            List<Constraint> emptyList = new ArrayList<Constraint>();
            return fd(emptyList,c, ac, model);}
    }
    // func FD(D, C = {c1..cq}, AC) : diagnosis  Δ
    private static List<Constraint> fd(List<Constraint> d, List<Constraint> c, List<Constraint> ac, Model model){
        List<Constraint> diagnosis = new ArrayList<Constraint>();
        int cSize=c.size();
        // if D != Φ and consistent(AC) return Φ;
        if( !d.isEmpty() && isConsistent(ac, model))
            return Collections.<Constraint>emptyList();
        // if singleton(C) return C;
        if(cSize==1)
            return c;
        int k = cSize/2;  // k = q/2;
        // C1 = {c1..ck}; C2 = {ck+1..cq};
        List<Constraint> c1 = new ArrayList<Constraint>();
        c1.addAll(c.subList(0, k));
        List<Constraint> c2 = new ArrayList<Constraint>();
        c2.addAll(c.subList(k, cSize));
        //Saving AC of the parent node
        List<Constraint> prevAC=new ArrayList<Constraint>();
        prevAC.addAll(ac);
        // D1 = FD(C1, C2, AC - C1);
        List<Constraint> acTemp = new ArrayList<Constraint>();
        acTemp.addAll(ac);

        List<Constraint> d1 = fd(c1, c2, subConstrsSets(acTemp,c1),model);
        diagnosis.addAll(d1);

        // D2 = FD(D1, C1, AC - D1);
        List<Constraint> d2 = fd(d1, c1, subConstrsSets(prevAC,d1),model);
        for (int i=0; i<d2.size(); i++)
            if (!diagnosis.contains(d2.get(i)))
                diagnosis.add(d2.get(i));
        return diagnosis;
    }
    // Check if set of constraint is consistent
    public static boolean isConsistent(List<Constraint> constrs, Model model)
    {
        model.getSolver().reset();
        model.unpost(model.getCstrs());
        for (int i=0; i<constrs.size(); i++)
        {
            model.post(constrs.get(i));
        }
        return model.getSolver().solve();
    }

    //Calculate c1-c2
    public static List<Constraint> subConstrsSets(List<Constraint> c1, List<Constraint> c2)
    {
        c1.removeAll(c2);
        return c1;
    }

    //Calculate diagnoses from a node depending on FastDiag (returns children (diagnoses) of a node)
    public static List<List<Constraint>> nodeDiagnoses(List<Constraint> node, List<Constraint> c,List<Constraint> ac, Model model, List<List<Constraint>> nodeDiag, List<List<Constraint>> allDiag, List<List<Constraint>> childC)
    {
        for (int i=0; i<node.size();i++)
        {
            List<Constraint> acOriginal=new ArrayList<Constraint>();  acOriginal.addAll(ac); // after calling fastDiag, ac changes so we need to restore its original value.
            List<Constraint> cOriginal=new ArrayList<Constraint>();  cOriginal.addAll(c); // after calling fastDiag, c changes so we need to restore its original value.
            Constraint constr=node.get(i);
            cOriginal.remove(constr);
            List<Constraint> diag=new ArrayList<Constraint>();
            diag=fastDiag(cOriginal, acOriginal,model);
            if (!diag.isEmpty() && isMinimal(diag,allDiag) && !allDiag.containsAll(diag))
            {
                nodeDiag.add(diag);
                allDiag.add(diag);
                childC.add(cOriginal); // saving the constraints set (c) of each child (diagnosis) to be used on the next call.
            }
        }
        return nodeDiag;
    }

    //calculate all diagnosis starting from the first diagnosis using FastDiag
    public static List<List<Constraint>> calculateAllDiagnoses(List<Constraint> firstDiag, List<Constraint> c, List<Constraint> ac, Model model, List<List<Constraint>> allDiag)
    {
        allDiag.add(firstDiag);
        List<List<Constraint>> nodeDiagnoses= new ArrayList<List<Constraint>>();
        List<List<Constraint>> childCnstnts= new ArrayList<List<Constraint>>();

        nodeDiagnoses(firstDiag, c, ac, model, nodeDiagnoses, allDiag, childCnstnts);

        while (!nodeDiagnoses.isEmpty())
        {
            List<List<Constraint>> childDiagnoses= new ArrayList<List<Constraint>>();
            List<List<Constraint>> childConstraints= new ArrayList<List<Constraint>>();
            for (int j=0; j<nodeDiagnoses.size(); j++)
            {
                nodeDiagnoses(nodeDiagnoses.get(j), childCnstnts.get(j), ac, model, childDiagnoses, allDiag, childConstraints);
            }
            nodeDiagnoses=childDiagnoses;
            childCnstnts=childConstraints;
        }
        return allDiag;
    }

    public static boolean isMinimal(List<Constraint> diag, List<List<Constraint>> allDiag)
    {
        boolean minimal=true;
        for (int i=0; i<allDiag.size() && minimal; i++)
        {
            if (diag.containsAll(allDiag.get(i)))
                minimal=false;
            else
                minimal=true;
        }
        return minimal;
    }

    //Calculate all diagnoses based on resolving the conflict sets of QuickXplain (HSDAG)
//    public static List<List<Constraint>> nodeConflictSets(List<Constraint> node, List<Constraint> b, List<Constraint> c, List<List<Constraint>> nodeConflicts, Model model, List<List<Constraint>> childC, List<Constraint> parentDiag,List<List<Constraint>> parentsDiag, List<List<Constraint>> allDiagnoses)
//    {
//        long startTime = System.currentTimeMillis();
//
//        List<Constraint> conflictSet=new ArrayList<Constraint>();
//        for (int i=0; i<node.size();i++)
//        {
//            Constraint constr=node.get(i);
//            List<Constraint> bOriginal=new ArrayList<Constraint>(); bOriginal.addAll(b);
//            List<Constraint> cOriginal=new ArrayList<Constraint>(); cOriginal.addAll(c);
//            cOriginal.remove(constr);
//            List<Constraint> constrList= new ArrayList<Constraint>(); constrList.add(constr);
//            QuickXplain.constrsUnion(constrList,parentDiag); //keeping a track of removed constraints to construct diagnoses
//            if (!isConsistent(QuickXplain.constrsUnion(bOriginal,cOriginal),model))
//            {
//                bOriginal=new ArrayList<Constraint>(); bOriginal.addAll(b);
//                conflictSet=QuickXplain.quickXPlain(bOriginal,cOriginal,model);
//                if (!conflictSet.isEmpty())// && !FinacialServices.contain(allConflictSets, conflictSet)) // if it's not already been calculated
//                {
//                    nodeConflicts.add(conflictSet);
//                    childC.add(cOriginal);  // saving the constraints set (c) of each child (diagnosis) to be used on the next call.
//                }
//                int index=parentsDiag.indexOf(parentDiag);
//                //constructing diagnoses
//                if (index==-1)
//                    parentsDiag.add(constrList);
//                else
//                    parentsDiag.set(index, constrList); // replacing the diagnoses track with the new one
//            }
//            else if(!FinacialServices.contain(allDiagnoses,constrList) && isMinimal(constrList,allDiagnoses)) //we have a minimal diagnosis
//            {allDiagnoses.add(constrList);
//                long endTime = System.currentTimeMillis();
//                System.out.println("first minimal diagnosis using hiting set is:"+(endTime-startTime) + "ms\n");
//            }
//        }
//        return nodeConflicts;
//    }
//    public static List<List<Constraint>> hsdag(List<Constraint> firstConflictSet, List<Constraint> b, List<Constraint> c, Model model)
//    {
//        List<List<Constraint>> allDiagnoses= new ArrayList<List<Constraint>>();
//        List<List<Constraint>> parentsDiag= new ArrayList<List<Constraint>>();
//        List<Constraint> parentDiag= new ArrayList<Constraint>();
//        List<List<Constraint>> nodeConflicts= new ArrayList<List<Constraint>>();
//        List<List<Constraint>> childCnstnts= new ArrayList<List<Constraint>>();
//        nodeConflictSets(firstConflictSet,b, c, nodeConflicts,model, childCnstnts,parentDiag,parentsDiag, allDiagnoses);
//        while (!nodeConflicts.isEmpty())
//        {
//            List<List<Constraint>> childConflicts= new ArrayList<List<Constraint>>();
//            List<List<Constraint>> childConstraints= new ArrayList<List<Constraint>>();
//            for (int j=0; j<nodeConflicts.size(); j++)
//            {
//                nodeConflictSets(nodeConflicts.get(j), b, childCnstnts.get(j), childConflicts, model, childConstraints,parentsDiag.get(j),parentsDiag, allDiagnoses);
//            }
//            nodeConflicts=childConflicts;
//            childCnstnts=childConstraints;
//        }
//        return allDiagnoses;
//    }
}