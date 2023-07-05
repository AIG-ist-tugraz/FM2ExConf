/*
 * Feature Model to Excel-based Configurator Support Tool
 *
 * Copyright (c) 2023 AIG team, Institute for Software Technology, Graz University of Technology, Austria
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 */
package at.tugraz.ist.ase.fm2exconf.analysis;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.SetUtils;
import org.chocosolver.solver.constraints.Constraint;

import java.util.*;

/**
 * Implementation for a MSS-based FastDiag algorithm.
 * MSS - Maximal Satisfiable Set
 *
 * // FastDiag Algorithm
 * //--------------------
 * // B: correctConstraints (background knowledge)
 * // C: possiblyFaultyConstraints
 * //--------------------
 * // Func FastDiag(C, B) : Δ
 * // if isEmpty(C) or consistent(B U C) return Φ
 * // else return C - FD(C, B, Φ)
 *
 * // Func FD(C = {c1..cn}, B, Δ) : MSS
 * // if Δ != Φ and consistent(B U C) return C;
 * // if singleton(C) return Φ;
 * // k = n/2;
 * // C1 = {c1..ck}; C2 = {ck+1..cn};
 * // Δ2 = FD(C1, B, C2);
 * // Δ1 = FD(C2, B U Δ2, C1 - Δ2);
 * // return Δ1 ∪ Δ2;
 *
 * @author Viet-Man Le (vietman.le@ist.tugraz.at)
 */
public class FastDiagV3 {

    private ConsistencyChecker checker;
    private ChocoModel model;

    public FastDiagV3(ChocoModel model) {
        this.model = model;

        checker = new ConsistencyChecker(model);
    }

    /**
     *  // Func FastDiag(C, B) : Δ
     *  // if isEmpty(C) or consistent(B U C) return Φ
     *  // else return C - FD(C, B, Φ)
     *
     * @param C
     * @return
     */
    public Set<Constraint> findDiagnosis(Set<Constraint> C, Set<Constraint> B)
    {
        Set<Constraint> BwithC = SetUtils.union(B, C); //incrementCounter(COUNTER_UNION_OPERATOR);

        // if isEmpty(C) or consistent(B U C) return Φ
        if (C.isEmpty()
                || checker.isConsistent(BwithC)) {
            return Collections.emptySet();
        }
        else{ // else return C \ FD(C, B, Φ)
            return SetUtils.difference(C, fd(C, B, Collections.emptySet()));
        }
    }

    private Set<Constraint> findDiagnosis(Set<Constraint> C, Set<Constraint> B, Set<Constraint> Δ)
    {
        Set<Constraint> BwithΔ = SetUtils.union(B, Δ);
        Set<Constraint> BΔwithC = SetUtils.union(BwithΔ, C);// incrementCounter(COUNTER_UNION_OPERATOR);

        // if isEmpty(C) or consistent(B U C) return Φ
        if (C.size() <= 1
                || !checker.isConsistent(BwithΔ)
                || checker.isConsistent(BΔwithC)) {
            return Collections.emptySet();
        }
        else{ // else return C - FD(C, B, Φ)
            Set<Constraint> diag = SetUtils.difference(C, fd(C, BwithΔ, Collections.emptySet()));

            Set<Constraint> BwithDiag = SetUtils.union(B, diag);
            if (checker.isConsistent(BwithDiag)) {
                return diag;
            }
            else {
                return Collections.emptySet();
            }
        }
    }

    /**
     * // Func FD(C = {c1..cn}, B, Δ) : MSS
     * // if Δ != Φ and consistent(B U C) return C;
     * // if singleton(C) return Φ;
     * // k = n/2;
     * // C1 = {c1..ck}; C2 = {ck+1..cn};
     * // Δ2 = FD(C1, B, C2);
     * // Δ1 = FD(C2, B U Δ2, C1 - Δ2);
     * // return Δ1 ∪ Δ2;
     *
     * @return
     */
    private Set<Constraint> fd(Set<Constraint> C, Set<Constraint> B, Set<Constraint> Δ){
        // if Δ != Φ and consistent(B U C) return C;
        if( !Δ.isEmpty()) {
            Set<Constraint> BwithC = SetUtils.union(B, C); //incrementCounter(COUNTER_UNION_OPERATOR);
            if (checker.isConsistent(BwithC)) {
                return C;
            }
        }

        // if singleton(C) return Φ;
        int n = C.size();
        if (n == 1) {
            return Collections.emptySet();
        }

        int k = n / 2;  // k = n/2;
        // C1 = {c1..ck}; C2 = {ck+1..cn};
        List<Constraint> firstSubList = new ArrayList<>(C).subList(0, k);
        List<Constraint> secondSubList = new ArrayList<>(C).subList(k, n);
        Set<Constraint> C1 = new LinkedHashSet<>(firstSubList);
        Set<Constraint> C2 = new LinkedHashSet<>(secondSubList);
//        incrementCounter(COUNTER_SPLIT_SET);

        // Δ2 = FD(C1, B, C2);
//        incrementCounter(COUNTER_LEFT_BRANCH_CALLS);
        Set<Constraint> Δ2 = fd(C1, B, C2);

        // Δ1 = FD(C2, B U Δ2, C1 - Δ2);
        Set<Constraint> BwithΔ2 = SetUtils.union(Δ2, B); //incrementCounter(COUNTER_UNION_OPERATOR);
        Set<Constraint> C1withoutΔ2 = SetUtils.difference(C1, Δ2); //incrementCounter(COUNTER_DIFFERENT_OPERATOR);
//        incrementCounter(COUNTER_RIGHT_BRANCH_CALLS);
        Set<Constraint> Δ1 = fd(C2, BwithΔ2, C1withoutΔ2);

//        incrementCounter(COUNTER_UNION_OPERATOR);
        return SetUtils.union(Δ1, Δ2);
    }

    //calculate all diagnosis starting from the first diagnosis using FastDiag
    public List<Set<Constraint>> findAllDiagnoses(Set<Constraint> firstDiag, Set<Constraint> C, Set<Constraint> B)
    {
        List<Set<Constraint>> allDiag = new ArrayList<>();
        allDiag.add(firstDiag); //incrementCounter(COUNTER_ADD_OPERATOR);

        diagnoses = new LinkedList<>();
        considerations = new LinkedList<>();
        background = new LinkedList<>();

        pushNode(firstDiag, C, Collections.emptySet());

        while (!diagnoses.isEmpty()) {
            exploreNode(allDiag, B);
        }

        diagnoses = null;
        considerations = null;
        background = null;

        return allDiag;
    }

    Queue<Set<Constraint>> diagnoses;
    Queue<Set<Constraint>> considerations;
    Queue<Set<Constraint>> background;

    private void popNode(Set<Constraint> node, Set<Constraint> C, Set<Constraint> Δ) {
        node.addAll(diagnoses.remove());
        C.addAll(considerations.remove());
        Δ.addAll(background.remove());
    }

    private void pushNode(Set<Constraint> node, Set<Constraint> C, Set<Constraint> Δ) {
        diagnoses.add(node);
        considerations.add(C);
        background.add(Δ);
    }

    //Calculate diagnoses from a node depending on FastDiag (returns children (diagnoses) of a node)
    public void exploreNode(List<Set<Constraint>> allDiag, Set<Constraint> B)
    {
        Set<Constraint> node = new LinkedHashSet<>();
        Set<Constraint> C = new LinkedHashSet<>();
        Set<Constraint> Δ = new LinkedHashSet<>();
        popNode(node, C, Δ);

        Iterator itr = IteratorUtils.getIterator(node);
        while (itr.hasNext()) {
            Constraint constraint = (Constraint) itr.next();

            Set<Constraint> AConstraint = new LinkedHashSet<>();
            AConstraint.add(constraint); //incrementCounter(COUNTER_ADD_OPERATOR);

            Set<Constraint> CwithoutAConstraint = SetUtils.difference(C, AConstraint); //incrementCounter(COUNTER_DIFFERENT_OPERATOR);
            Set<Constraint> ΔwithAConstraint = SetUtils.union(Δ, AConstraint); //incrementCounter(COUNTER_UNION_OPERATOR);

            Set<Constraint> diag = findDiagnosis(CwithoutAConstraint, B, ΔwithAConstraint);

            if (!diag.isEmpty() && isMinimal(diag, allDiag) && !allDiag.containsAll(diag))
            {
                allDiag.add(diag); //incrementCounter(COUNTER_ADD_OPERATOR);
                pushNode(diag, CwithoutAConstraint, ΔwithAConstraint);
            }
        }
    }

    private boolean isMinimal(Set<Constraint> diag, List<Set<Constraint>> allDiag)
    {
        for (int i = 0; i < allDiag.size(); i++)
        {
            if (diag.containsAll(allDiag.get(i))) {
                return false;
            }
        }

        return true;
    }
}
