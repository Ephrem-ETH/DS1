package ds1_project.Responses;

import java.io.Serializable;

public class UpdateResponse implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public boolean WRITEOK = false;

	public UpdateResponse(final boolean WRITEOK) {
		this.WRITEOK = WRITEOK;
	}
}