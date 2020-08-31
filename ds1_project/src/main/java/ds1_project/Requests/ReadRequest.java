package ds1_project.Requests;

public class ReadRequest extends Request {
	/**
	 * Read request : Message sent by a client to a node.
	 * The node replies by snending a ReadResponse message, containing
	 * the current value.
	 */
	private static final long serialVersionUID = 1L;

	public ReadRequest() {
		super();
	}
}