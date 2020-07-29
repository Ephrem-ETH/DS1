package ds1_project.Requests;

import java.util.List;
import java.util.ArrayList;


public class ElectionMessage {
    
    private int electionID ; //Epoch number + (concatenation) emmiter ID
    private List<Integer> candidatesID ;
    private List<Integer> lastUpdates ;

    public ElectionMessage(int id){
        electionID = id ;
        candidatesID = new ArrayList<Integer>() ;
        lastUpdates = new ArrayList<Integer>();
    }

    public void addCandidate (int participant, int lastUpdate){
        candidatesID.add(participant) ;
        lastUpdates.add(lastUpdate) ;
    }

    public int getID(){
        return this.electionID ;
    }

}