package ds1_project.Requests;

public class UpdateRequest extends Request {
	/**
     *
     */
    private static final long serialVersionUID = 1L;
    private int value;

	public UpdateRequest(final int value){
		super();
		this.value = value ;
	}

	public UpdateRequest() {
		super();
	}

	public int getValue(){
		return this.value ;
	}
}