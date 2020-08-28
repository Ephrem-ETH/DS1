package ds1_project.Responses;

import java.io.Serializable;

public class Update implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final int epochs;
	private final int sequencenum;
	private final int value;
	private boolean isValidated ;
	private int sender_id ;
	private boolean isEpochConsolidation = false ;

	public Update(final int epochs, final int sequencenum, final int value,int sender_id) {
		super();
		this.epochs = epochs;
		this.sequencenum = sequencenum;
		this.value = value;
		this.isValidated = false ;
		this.sender_id = sender_id ;
	}

	public int getEpoch() {
		return this.epochs;
	}

	public int getSequenceNumber() {
		return this.sequencenum;
	}

	public int getValue() {
		return this.value;
	}

	public void setValidity(boolean valid){
		this.isValidated = valid ;
	}

	public boolean isValidated() {
		return isValidated;
	}

	public int getSender_id() {
		return sender_id;
	}

	public void setEpochConsolidation(boolean isEpochConsolidation) {
		this.isEpochConsolidation = isEpochConsolidation;
	}

	public boolean isEpochConsolidation() {
		return isEpochConsolidation;
	}
}
