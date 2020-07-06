package ds1_project.Actors;

import akka.actor.ActorRef;
import akka.actor.AbstractActor;
import ds1_project.TwoPhaseBroadcast.*;
import ds1_project.Responses.*;
import ds1_project.Requests.*;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

/*-- Common functionality for both Coordinator and Participants ------------*/

public abstract class Node extends AbstractActor {
    protected int id; // node ID
    protected List<ActorRef> participants; // list of participant nodes
    // protected Decision decision = null; // decision taken by this node
    protected boolean isUpdated = false;
    private boolean isCoordinator = false;
    private int value;
    private ActorRef sender;

    public Node(final int id) {
        super();
        this.id = id;
    }

	//Getters and Setters
    public int getId() {
        return this.id;
	}
	
	public int getValue(){
		return this.value ;
	}

	public void setCoordinator(boolean bool){
		if (bool) {
		this.isCoordinator = true ;
		} else {
			this.isCoordinator = false ;
		}
	}

	public boolean isCoordinator(){
		return this.isCoordinator ;
	}

	public ActorRef getNodeSender(){
		return this.sender ;
	}

	public void setSender(ActorRef sender){
		this.sender = sender ;
	}

    public void setGroup(final StartMessage sm) {
        participants = new ArrayList<>();
        for (final ActorRef b : sm.group) {
            if (!b.equals(getSelf())) {

                // copying all participant refs except for self
                this.participants.add(b);
            }
        }
        print("starting with " + sm.group.size() + " peer(s)");
    }

    public void multicast(final Serializable m) {
        for (final ActorRef p : participants)
            p.tell(m, getSelf());
    }

    // void sendWriteOk(boolean isUpdated) {
    // if (isUpdated) {
    // return;
    // }
    // }

    // a simple logging function
    public void print(final String s) {
        System.out.format("%2d: %s\n", id, s);
    }

    @Override
    public Receive createReceive() {

        // Empty mapping: we'll define it in the inherited classes
        return receiveBuilder().build();
    }

    public void OnReadRequest(final ReadRequest msg) {
        getSender().tell(new ReadResponse(value), self());
    }

}