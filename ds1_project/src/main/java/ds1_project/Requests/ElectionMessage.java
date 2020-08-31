package ds1_project.Requests;

import java.util.List;
import java.io.Serializable;
import java.util.ArrayList;


public class ElectionMessage implements Serializable {
    
    /**
     * Election message: Sent by a node detecting a coordinator crash.
     * Created with the emitter ID and the epoch number.
     * The candidatesID list will be updated alongside the lastUpdate list
     * each time a new node in the ring topology (based on nodes ID)
     * receives this token.
     */

    private static final long serialVersionUID = 1L;
    
    private int electionEpoch; // Epoch number + (concatenation) emmiter ID
    private int emmiter_id ;
    private List<Integer> candidatesID ;
    private List<Integer> lastUpdates ;

    public ElectionMessage(int epoch, int id){
        emmiter_id = id ;
        electionEpoch = epoch ;
        candidatesID = new ArrayList<Integer>() ;
        lastUpdates = new ArrayList<Integer>();
    }

    public void addCandidate (int participant, int lastUpdate){
        candidatesID.add(participant) ;
        lastUpdates.add(lastUpdate) ;
    }

    public int getElectionEpoch() {
        return electionEpoch;
    }

    public int getEmmiter_id() {
        return emmiter_id;
    }

    public List<Integer> getCandidatesID() {
        return candidatesID;
    }

    public List<Integer> getLastUpdates() {
        return lastUpdates;
    }

}