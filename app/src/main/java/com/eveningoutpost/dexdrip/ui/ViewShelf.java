package com.eveningoutpost.dexdrip.ui;

/**
 * Created by jamorham on 20/10/2017.
 *
 * Common interface for Shelf implementations
 */

public interface ViewShelf {

    public boolean get(String id) ;
    public void set(String id, boolean value) ;
    public void pset(String id, boolean value) ;

    public void ptoggle(String id);
    //public void populate();

}
