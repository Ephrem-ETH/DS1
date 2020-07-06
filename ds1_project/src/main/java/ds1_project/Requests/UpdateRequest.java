package ds1_project;

public class UpdateRequest extends Request {
	/**
     *
     */
    private static final long serialVersionUID = 1L;
    public int value;

	public UpdateRequest(final int node_id, final int value){
		super(node_id);
		this.value = value ;
	}
}