package ds1_project;

public class Key {
	public static int[] keyparams;

	// epoch and sequence numbers
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