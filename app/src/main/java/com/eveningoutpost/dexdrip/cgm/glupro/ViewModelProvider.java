package com.eveningoutpost.dexdrip.cgm.glupro;


/**
 * JamOrHam
 * <p>
 * Glucose Profile shared ViewModel singleton provider
 */
public class ViewModelProvider {

    private static volatile ViewModel viewModel;

    public static synchronized ViewModel get(){
        if (viewModel == null) {
            viewModel = new ViewModel();
        }
        return viewModel;
    }

}
