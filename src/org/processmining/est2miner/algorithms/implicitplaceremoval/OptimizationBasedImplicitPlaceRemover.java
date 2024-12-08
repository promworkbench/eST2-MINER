package org.processmining.est2miner.algorithms.implicitplaceremoval;

import org.apache.commons.math3.optim.linear.*;
import org.processmining.est2miner.models.coreobjects.ESTLog;
import org.processmining.est2miner.models.coreobjects.ESTPlace;
import org.processmining.est2miner.models.coreobjects.ESTProcessModel;

import java.util.ArrayList;
import java.util.Collection;

public class OptimizationBasedImplicitPlaceRemover extends AbstractImplicitPlacesRemover {


    public OptimizationBasedImplicitPlaceRemover(String[] transitions, ESTLog log) {
        super(transitions, log);
    }

    //removes all structurally implicit places in the givn pM (the relevant Trace variants are ignored)
    public ESTProcessModel removeAllIPs(ESTProcessModel pM) {
        pM.updateStatus(log); //just to be safe
        //compute all the stuff needed for the LPP
        ArrayList<ESTPlace> places = (ArrayList<ESTPlace>) pM.getPlaces().clone();
        final ArrayList<ESTPlace> finalPlaces = new ArrayList<ESTPlace>();
        final String[] transitions = pM.getTransitions();
        //compute pre-incidence, post-incidence and incidence matrix of pM: for each place there is a row, for each transition a column
        final ArrayList<Integer[]> preIncMatrix = new ArrayList<Integer[]>();
        final ArrayList<Integer[]> postIncMatrix = new ArrayList<Integer[]>();
        final ArrayList<Integer[]> incMatrix = new ArrayList<Integer[]>();
        for (int p = 0; p < places.size(); p++) {
            preIncMatrix.add(new Integer[transitions.length]);
            postIncMatrix.add(new Integer[transitions.length]);
            incMatrix.add(new Integer[transitions.length]);
            ESTPlace place = places.get(p);
            int pIn = place.getInputTrKey();
            int pOut = place.getOutputTrKey();
            for (int t = 0; t < transitions.length; t++) {
                if ((pIn & getMask(t, transitions)) > 0) {//t'th transition is contained in input of p
                    preIncMatrix.get(p)[t] = 1;
                } else {
                    preIncMatrix.get(p)[t] = 0;
                }
                if ((pOut & getMask(t, transitions)) > 0) {//t'th transition is contained in output of p
                    postIncMatrix.get(p)[t] = 1;
                } else {
                    postIncMatrix.get(p)[t] = 0;
                }
                incMatrix.get(p)[t] = preIncMatrix.get(p)[t] - postIncMatrix.get(p)[t];
            }
        }
        //do the LPP magic to check implicitness for each place
        //increase speed by removing implicit places for the next iteration
        ArrayList<ESTPlace> tempPlaces = (ArrayList<ESTPlace>) places.clone();
        while (!tempPlaces.isEmpty()) {
            int placePos = places.indexOf(tempPlaces.get(0));
            if (isImplicitByLPP(placePos, places, transitions, preIncMatrix, incMatrix)) {
                //this place is implicit and can be deleted
                places.remove(placePos);
                preIncMatrix.remove(placePos);
                incMatrix.remove(placePos);
                tempPlaces.remove(0);
            } else {
                tempPlaces.remove(0);
                finalPlaces.add(places.get(placePos));
            }
        }
        pM.setPlaces(finalPlaces);
        return pM;
    }


    //check for structural implicitness of the specified place within the PM defined by the given places set (ignore relevant traces)
    public ArrayList<ESTPlace> implicitRelatedToPlace(ESTPlace currentP, ArrayList<ESTPlace> places) {
        ArrayList<ESTPlace> result = new ArrayList<>();
        //compute all the stuff needed for the LPP
        places.add(currentP);
        //compute pre-incidence, post-incidence and incidence matrix of pM: for each place there is a row, for each transition a column
        final ArrayList<Integer[]> preIncMatrix = new ArrayList<Integer[]>();
        final ArrayList<Integer[]> postIncMatrix = new ArrayList<Integer[]>();
        final ArrayList<Integer[]> incMatrix = new ArrayList<Integer[]>();
        for (int p = 0; p < places.size(); p++) {
            preIncMatrix.add(new Integer[transitions.length]);
            postIncMatrix.add(new Integer[transitions.length]);
            incMatrix.add(new Integer[transitions.length]);
            ESTPlace place = places.get(p);
            int pIn = place.getInputTrKey();
            int pOut = place.getOutputTrKey();
            for (int t = 0; t < transitions.length; t++) {
                if ((pIn & getMask(t, transitions)) > 0) {//t'th transition is contained in input of p
                    preIncMatrix.get(p)[t] = 1;
                } else {
                    preIncMatrix.get(p)[t] = 0;
                }
                if ((pOut & getMask(t, transitions)) > 0) {//t'th transition is contained in output of p
                    postIncMatrix.get(p)[t] = 1;
                } else {
                    postIncMatrix.get(p)[t] = 0;
                }
                incMatrix.get(p)[t] = preIncMatrix.get(p)[t] - postIncMatrix.get(p)[t];
            }
        }
        //do the LPP magic to check implicitness for each place
        //increase speed by removing implicit places for the next iteration
        int placePos = places.indexOf(currentP);
        if (isImplicitByLPP(placePos, places, transitions, preIncMatrix, incMatrix)) {
            //this place is implicit
            result.add(currentP);
        } else {
            //currentP is not implciit, do not add
        }
        return result;
    }


    //use an LPP solver on the given parameters to test the place defined by placePos for implicitness
    //variables: y | z | k | x --> places.size()+places.size()+1+1
    private boolean isImplicitByLPP(int currP, ArrayList<ESTPlace> places, String[] transitions, ArrayList<Integer[]> preIncMatrix, ArrayList<Integer[]> incMatrix) {
        //For initial marking 0, variables k, x and reference sets Y, Z the objective functions is
        //0*y1+0*y2+ ... 0*yn + 0*z1+0*z2+ ... 0*zn + 1*k + 0*x + 0
        //This simplifies to 1*k
        double[] coefficientsLinearObjectiveFunction = new double[(places.size() * 2) + 2]; //there are 2*|places| coefficients for Y, Z and 2 coefficient for k, x
        for (int i = 0; i < (places.size() * 2); i++) {
            coefficientsLinearObjectiveFunction[i] = 0;
        }
        coefficientsLinearObjectiveFunction[places.size() * 2] = 1; //k*1
        LinearObjectiveFunction objectiveFunction = new LinearObjectiveFunction(coefficientsLinearObjectiveFunction, 0);

        //Add the linear constraints w.r.t. the current place currP, using k, x, Y, Z, incMatrix, preIncMatrix
        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        //Type 0: ensure currP is not in Y, Z, that is currP=0
        double[] removeCurrPY = new double[(places.size() * 2) + 2];//coefficients are all 0, except for currP in Y
        double[] removeCurrPZ = new double[(places.size() * 2) + 2];//coefficients are all 0, except for currP in Z
        for (int i = 0; i < ((places.size() * 2) + 2); i++) {
            removeCurrPY[i] = 0;
            removeCurrPZ[i] = 0;
        }
        removeCurrPY[currP] = 1;
        removeCurrPZ[currP + places.size()] = 1;
        constraints.add(new LinearConstraint(removeCurrPY, Relationship.EQ, 0));
        constraints.add(new LinearConstraint(removeCurrPZ, Relationship.EQ, 0));
        //Type 1: Y>=Z>=0, k>=0, x=0, x<k--> x-k<=-0
        //Y>=Z>=0 --> Z>=0 AND Y-Z>=0
        for (int p = 0; p < places.size(); p++) {
            double[] nonNegZ = new double[(places.size() * 2) + 2];//coefficients are all 0, except for the current z=1
            double[] YGegZ = new double[(places.size() * 2) + 2];//coefficients are all 0, except for the current y=1 and z=-1
            for (int i = 0; i < ((places.size() * 2) + 2); i++) {//put everything to 0 by default
                nonNegZ[i] = 0;
                YGegZ[i] = 0;
            }
            nonNegZ[places.size() + p] = 1; //z=1
            YGegZ[p] = 1; //y=1
            YGegZ[places.size() + p] = -1; //z=-1
            constraints.add(new LinearConstraint(nonNegZ, Relationship.GEQ, 0));//1*z>=0
            constraints.add(new LinearConstraint(YGegZ, Relationship.GEQ, 0));//1*y-1*z>=0
        }
        //k>=0, x=0, x-k<=-1
        double[] nonNegk = new double[(places.size() * 2) + 2];//coefficients are all 0, except for k=1
        double[] zerox = new double[(places.size() * 2) + 2];//coefficients are all 0, except for x=1
        double[] xSmallerk = new double[(places.size() * 2) + 2];//coefficients are all 0, except for x=1, k=-1
        for (int i = 0; i < ((places.size() * 2) + 2); i++) {
            nonNegk[i] = 0;
            zerox[i] = 0;
        }
        nonNegk[places.size() * 2] = 1;
        zerox[(places.size() * 2) + 1] = 1;
        xSmallerk[places.size() * 2] = -1; //k=-1
        xSmallerk[(places.size() * 2) + 1] = 1; //x=1
        constraints.add(new LinearConstraint(xSmallerk, Relationship.LEQ, -1));//1*x-1*k <=-1
        constraints.add(new LinearConstraint(nonNegk, Relationship.GEQ, 0));//1*k>=0
        constraints.add(new LinearConstraint(zerox, Relationship.EQ, 0));//1*x=0

        //Type 2: Y*incMatrix<=k*inc(currP) ---> Y*incMatrix - k*inc(currP) <=0;
        for (int t = 0; t < transitions.length; t++) {//add one constraint for each transition
            double[] coefficients = new double[(places.size() * 2) + 2];//coefficients are based on incMatrix
            for (int p = 0; p < places.size(); p++) {
                coefficients[p] = incMatrix.get(p)[t]; //find the coefficient for Y each pair (p,t)
                coefficients[p + places.size()] = 0; //Z is put to 0
            }
            //coefficient for k is -incMatrix[currP, t]
            coefficients[places.size() * 2] = incMatrix.get(currP)[t] * (-1);
            //coefficient for x is 0;
            coefficients[(places.size() * 2) + 1] = 0;
            //the sum should be <= inc(currP,t)
            constraints.add(new LinearConstraint(coefficients, Relationship.LEQ, 0));
        }

        //Type 3: forall t with currP in pre(t): Z*pre(q, t) + x >= k *pre(currP, t), for q in P/{currP}
        // --> Z*pre(q, t) + x - k* pre(currP, t) >=0 	//TODO in contrast to paper (imp places in net systems, garcia&colom, proposition 13) seems to work fine for k=1. explain this result theoretically?!
        for (int t = 0; t < transitions.length; t++) {
            if (preIncMatrix.get(currP)[t] == 1) {//for all t with currP in pre(t)
                double[] coefficients = new double[(places.size() * 2) + 2];
                for (int p = 0; p < places.size(); p++) {
                    coefficients[p] = 0; //coefficients of Y=0
                    coefficients[p + places.size()] = preIncMatrix.get(p)[t]; //coefficients of Z = pre(p,t)
                }
                //coefficients[places.size()*2] = (-1)*preIncMatrix.get(currP)[t];//coefficient of k=-pre(currP, t)
                coefficients[(places.size() * 2) + 1] = 1;//coefficient of x=1
                constraints.add(new LinearConstraint(coefficients, Relationship.GEQ, preIncMatrix.get(currP)[t]));
            }
        }

        LinearOptimizer solver = new SimplexSolver();
        try {
            solver.optimize(objectiveFunction, new LinearConstraintSet(constraints));
        } catch (NoFeasibleSolutionException NFSE) {
            //				System.out.println("LPP Solver found no feasable solution for reference set of place "+placeToNamedString(places[currP], transitions));
            return false;
        }
        /*
         * //for debuggin print out the found solution double[] solutionSupport
         * = solution.getPoint(); String refSetStringY = "Y: "; String
         * refSetStringZ = "Z: "; String k = "k: "; String x = "x: "; for (int
         * pos = 0; pos < solutionSupport.length-2; pos++) { if
         * (solutionSupport[pos] > 0 && pos < places.size()) { refSetStringY =
         * refSetStringY + placeToNamedString(places.get(pos), transitions); }
         * else if(solutionSupport[pos] > 0 && pos < places.size()*2){
         * refSetStringZ = refSetStringZ +
         * placeToNamedString(places.get(pos-places.size()), transitions); } } k
         * = k+solutionSupport[places.size()*2]; x =
         * x+solutionSupport[places.size()*2+1];
         * System.out.println("Place "+placeToNamedString(places.get(currP),
         * transitions)+" is implicit with solution ");
         * System.out.println(refSetStringY); System.out.println(refSetStringZ);
         * System.out.println(k); System.out.println(x);
         */
        return true;
    }


    //Utility Methods_______________________________________________________________________________________________

    private String placeToNamedString(final ESTPlace p, final String[] transitions) {
        return getTransitionNames(p.getInputTrKey(), transitions).toString() + "|" + getTransitionNames(p.getOutputTrKey(), transitions).toString() + ",";
    }

    //returns a collection containing all transitions names from the given transitions array
    private Collection<String> getTransitionNames(final int key, final String[] transitions) {
        Collection<String> result = new ArrayList<String>();
        if (key > (Math.pow(2, transitions.length))) {
            return null;
        }
        for (int i = 0; i < transitions.length; i++) {
            if ((key & getMask(i, transitions)) > 0) { //test key for ones
                result.add(transitions[i]);
            }
        }

        return result;
    }

    //return the transitions corresponding to the given key
    private Collection<String> getTransitions(final int key, final ESTProcessModel pM) {
        Collection<String> result = new ArrayList<String>();
        if (key > Math.pow(2, pM.getTransitions().length)) {
            return null;
        }
        for (int i = 0; i < pM.getTransitions().length; i++) {
            if ((key & getMask(i, pM.getTransitions())) > 0) { //test key for ones
                result.add(pM.getTransitions()[i]);
            }
        }
        return result;
    }

    //return bitmask corresponding to position in the transition array
    protected int getMask(final int position, final String[] transitions) {
        return (1 << (transitions.length - 1 - position));
    }

    public ESTProcessModel removeAllIPsAndRepair(ESTProcessModel inputPM, ArrayList<ArrayList<Integer>> relevantTraceVariants) {
        System.out.println("ERROR: IP Removal with repair not imlemented!");
        // TODO Auto-generated method stub
        return null;
    }


}
