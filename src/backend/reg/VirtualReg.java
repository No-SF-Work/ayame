package backend.reg;

import backend.LiveInterval;

public class VirtualReg extends Reg {

    private LiveInterval liveInterval =new LiveInterval();

    public LiveInterval getLiveInterval(){
        return liveInterval;
    }

    public void setLiveInterval(LiveInterval liveInterval){
        this.liveInterval=liveInterval;
    }
}
