package ds1_project.Requests;

public class UpdateRequest extends Request {
	/**
	Update Request : Takes desired value by the the client
	When received by a regular participant, it is forwarded to the coordinator
	that will process it.
	The coordinator manages the ordering and the redistribution of the updates
	No response is sent to the client emitting the request.
	 */
	private static final long serialVersionUID = 1L;
	private int value;

	public UpdateRequest(final int value) {
		super();
		this.value = value;
	}

	public UpdateRequest() {
		super();
	}

	public int getValue() {
		return this.value;
	}
}