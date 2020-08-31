package ds1_project.Requests;

import java.io.Serializable;

public class CrashRequest extends Request {

    /**
     Debug message : sent to a node to make it crash
     */
    private static final long serialVersionUID = 1L;

    public CrashRequest(){
        super() ;
    }
    
}