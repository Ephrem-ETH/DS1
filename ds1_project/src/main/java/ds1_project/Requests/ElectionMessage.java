package ds1_project.Requests;

import java.util.List;
import java.io.Serializable;
import java.util.ArrayList;


public class ElectionMessage implements Serializable {
    
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    
    private String electionID; // Epoch number + (concatenation) emmiter ID
    private List<Integer> candidatesID ;
    private List<Integer> lastUpdates ;

    public ElectionMessage(String id){
        electionID = id ;
        candidatesID = new ArrayList<Integer>() ;
        lastUpdates = new ArrayList<Integer>();
    }

    public void addCandidate (int participant, int lastUpdate){
        candidatesID.add(participant) ;
        lastUpdates.add(lastUpdate) ;
    }

    public String getID(){
        return this.electionID ;
    }

    public List<Integer> getCandidatesID() {
        return candidatesID;
    }

    public List<Integer> getLastUpdates() {
        return lastUpdates;
    }

}