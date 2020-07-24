package ds1_project.Actors;

import ds1_project.TwoPhaseBroadcast.*;
import akka.actor.AbstractActor;
import akka.actor.Props;
import ds1_project.Responses.*;

public class externalClient extends AbstractActor {

	public externalClient() {
	}

	static public Props props() {
		return Props.create(externalClient.class, () -> new externalClient());
	}

	@Override
	public Receive createReceive() {
		// TODO Auto-generated method stub
		return receiveBuilder().match(ReadResponse.class, this::onReadResponse).build();
	}

	public void onReadResponse(ReadResponse msg) {
		System.out.println("Client : Read value " + msg.getValue());
	}
}
