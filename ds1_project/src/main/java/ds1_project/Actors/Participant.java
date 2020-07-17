package ds1_project.Actors;

import ds1_project.TwoPhaseBroadcast.*;
import ds1_project.Requests.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import akka.actor.ActorRef;
import akka.actor.Props;
import ds1_project.Responses.*;


public class Participant extends Node {
    ActorRef coordinator;
    int myvalue;
    private HashMap<Key,Integer> waitingList = new HashMap<Key,Integer>();
    List<int[]> list = new ArrayList<int[]>(); //transform into hashmap : waiting list of updates - Maybe in Node class as the coordinator will also need it ?

    public Participant(final int id) {
        super(id);
    }

    static public Props props(final int id) {
        return Props.create(Participant.class, () -> new Participant(id));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(StartMessage.class, this::onStartMessage)
                .match(WriteOk.class, this::onWriteOK)
                .match(Update.class, this::onUpdate)
                .match(UpdateRequest.class, this::onUpdateRequest)
                .match(ReadRequest.class, this::OnReadRequest)
                // .match(Timeout.class, this::onTimeout)
                // .match(Recovery.class, this::onRecovery)
                .build();
    }

    public void onStartMessage(final StartMessage msg) {
        setGroup(msg);
    }

    public void onUpdateRequest(final UpdateRequest msg) {
        setSender(getSender()) ;
        coordinator.tell(msg, self());
    }

    public void onWriteOK(final WriteOk msg) {
        int epoch = msg.getRequest_id()[0];
        int seq_num = msg.getRequest_id()[1];
        Key req_num = new Key(epoch,seq_num) ;

        for (int[] request : list){
            if (request[0]==epoch && request[1]==seq_num){
                this.myvalue = waitingList.get(req_num) ;
                System.out.println("Updated value");
                waitingList.remove(req_num) ;
                System.out.println("Update removed from queue");
            }
        }
    }

	public void onUpdate(Update msg) {      // Update propagates from coordinator
        
        Key request_id = new Key(msg.getEpochs(),msg.getSequenceNum()) ;
        waitingList.put(request_id, msg.getValue()) ;
		
		this.coordinator.tell(new Acknowledgement(Acknowledge.ACK,msg.getKeepSequence()), getSelf());
	}
	
}