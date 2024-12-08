package org.processmining.est2miner.dialogs;

import com.fluxicon.slickerbox.components.NiceDoubleSlider;
import com.fluxicon.slickerbox.components.NiceIntegerSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.factory.SlickerFactory;
import org.processmining.est2miner.parameters.Parameters;
import org.processmining.framework.util.ui.widgets.ProMPropertiesPanel;
import org.processmining.framework.util.ui.wizard.ProMWizardStep;

import javax.swing.*;

//this class provides the userinterface, takes parameter values and stores them in the parameters

public class UIDialog extends ProMPropertiesPanel implements ProMWizardStep<Parameters> {
    public static String TITLE = "Configure eST-Miner parameters:";
    NiceDoubleSlider threshold_tau;
    NiceDoubleSlider threshold_delta;
    JCheckBox RemoveImplicitPlacesBox;
    NiceIntegerSlider treeDepth;

    public UIDialog(String title) {
        super(title);
        // TODO Auto-generated constructor stub
    }

    //method that actually takes the parameters
    public UIDialog() {
        super(TITLE);

        //configure threshold tau
        this.threshold_tau = SlickerFactory.instance().createNiceDoubleSlider("Threshold Tau ", 0, 1, 1, Orientation.HORIZONTAL);
        this.add(this.threshold_tau);

        //configure threshold delta
        this.threshold_delta = SlickerFactory.instance().createNiceDoubleSlider("Threshold Delta ", 0, 1, 0.05, Orientation.HORIZONTAL);
        this.add(this.threshold_delta);

        //configure postprocessing
        RemoveImplicitPlacesBox = this.addCheckBox("Remove Implicit Places?", true); //IP removal?

        //configure tree traversal depth
        this.treeDepth = SlickerFactory.instance().createNiceIntegerSlider("Max Number of Transitions: ", 1, 20, 8, Orientation.HORIZONTAL);
        this.add(this.treeDepth);
    }

    //store input in parameters
    public Parameters apply(Parameters model, JComponent component) {
        double threshold_tau_result = this.threshold_tau.getValue();

        double threshold_delta_result = this.threshold_delta.getValue();

        int treeDepth = this.treeDepth.getValue();

        boolean removeImplicitPlaces = RemoveImplicitPlacesBox.isSelected();

        Parameters parameters = new Parameters(treeDepth, threshold_tau_result, threshold_delta_result, removeImplicitPlaces);

        return parameters;
    }

    public boolean canApply(Parameters model, JComponent component) {
        // TODO Auto-generated method stub
        return true;
    }

    public JComponent getComponent(Parameters model) {
        // TODO Auto-generated method stub
        return this;
    }

    public String getTitle() {
        // TODO Auto-generated method stub
        return TITLE;
    }

}