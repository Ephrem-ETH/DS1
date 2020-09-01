package ds1_project.Actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import ds1_project.Responses.*;

public class externalClient extends AbstractActor {

	/**
	 * externalClient: an Actor used for sending requests to the system. This
	 * is the main way of testing the system, simulating a practical use where
	 * the system should remain completely hidden from the standard user.
	 * 
	 */

	public externalClient() {
	}

	static public Props props() {
		return Props.create(externalClient.class, () -> new externalClient());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(ReadResponse.class, this::onReadResponse).build();
	}

	public void onReadResponse(ReadResponse msg) {
		System.out.println("Client : Read value " + msg.getValue());
	}
}
