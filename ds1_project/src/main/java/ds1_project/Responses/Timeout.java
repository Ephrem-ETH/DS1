package ds1_project.Responses;

import java.io.Serializable;

import ds1_project.Actors.Node.toMessages;

public class Timeout implements Serializable {

	/**
	 * Timeout : Message sent by a node to itself after a scheduled duration.
	 * It takes as arguments a toMessages type as well as an int representing
	 * the id of the watched node.
	 * 
	 * Upon reception, depending on the type of message and the watching node,
	 * relevant actions are taken. (See onTimeout method).
	 */

	private static final long serialVersionUID = 1L;
	public toMessages toMess;
	private int watchingNode;

	public Timeout(toMessages toMess,int node) {
		super();
		this.toMess = toMess;
		watchingNode = node ;
	}

	public int getWatchingNode() {
		return watchingNode;
	}

}
