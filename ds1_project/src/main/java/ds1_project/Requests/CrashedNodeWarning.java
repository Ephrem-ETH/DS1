package ds1_project.Requests;

import java.io.Serializable;

public class CrashedNodeWarning implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private int crashedNode;

    public CrashedNodeWarning(int node){
        this.crashedNode = node ;
    }

    public int getNode(){
        return this.crashedNode ;
    }
    
}