package ds1_project.Responses;

import java.io.Serializable;

public class ReadResponse implements Serializable {

	/**
	 * ReadResponse : Message replied by a node to a client
	 * requesting to read its value. It takes as argument the
	 * node value.
	 */
	private static final long serialVersionUID = 1L;

	private final int value;

	public int getValue() {
		return this.value;
	}

	public ReadResponse(final int value2) {
		this.value = value2;
	}

}