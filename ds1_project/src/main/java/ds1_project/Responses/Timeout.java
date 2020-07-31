package ds1_project.Responses;

import java.io.Serializable;

import ds1_project.Actors.Node.toMessages;

public class Timeout implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	public toMessages toMess;

	public Timeout(toMessages toMess) {
		super();
		this.toMess = toMess;
	}

}
