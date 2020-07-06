package ds1_project;

import java.io.Serializable;

// Requests
public class Request implements Serializable {
	/**
     *
     */
    private static final long serialVersionUID = 1L;
    public int node_id;

	public Request(final int id) {
		this.node_id = id ;
    }
}