package ds1_project.Requests;

import java.io.Serializable;

public class CrashedNodeWarning extends Request {

    /**
     Crashed Node Warning : message sent by a node detecting 
     a crashed node in the network (typically via timeout).

     Takes the crashed node ID as argument

     */
    private static final long serialVersionUID = 1L;
    private int crashedNode;

    public CrashedNodeWarning(int node){
        super() ;
        this.crashedNode = node ;
    }

    public int getNode(){
        return this.crashedNode ;
    }
    
}