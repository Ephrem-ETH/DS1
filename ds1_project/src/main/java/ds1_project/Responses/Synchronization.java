package ds1_project.Responses;

import java.io.Serializable;

public class Synchronization implements Serializable {
    
    private Update update ;
    private int coordinator_id ;


    public Synchronization (Update update){
        this.update = update ;
    }

    public Update getUpdate() {
        return update;
    }
}