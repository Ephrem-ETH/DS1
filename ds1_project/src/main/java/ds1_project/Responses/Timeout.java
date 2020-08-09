package ds1_project.Responses;

import java.io.Serializable;

import ds1_project.Actors.Node.toMessages;

public class Timeout implements Serializable {

	/**
	 *
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
