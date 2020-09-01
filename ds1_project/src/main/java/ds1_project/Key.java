package ds1_project;

public class Key {

	/** 
	 * Key : class used for coordinate positionning of Updates (epoch,
	 * sequence number). Using objects for such a simple positionning
	 * instead of primitive types is cumbersome and was hence almost
	 * abandoned for a more straigthforward solution.
	*/ 

	// epoch and sequence numbers
	public static int[] keyparams;

	public Key(final int e, final int s) {
		int [] params = {e,s} ;
		keyparams = params ;
	}

	public int getE(){
		return keyparams[0] ;
	}

	public int getS(){
		return keyparams[1] ;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof Key) && (this.getE()==((Key)obj).getE()) && (((Key)obj).getS()==this.getS());
	}
}