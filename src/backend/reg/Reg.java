package backend.reg;

import backend.LiveInterval;
import backend.machinecodes.MachineCode;

import java.util.ArrayList;
import java.util.Objects;

public class Reg extends MachineOperand{
    //interval in;
    private LiveInterval liveInterval =new LiveInterval();

    public LiveInterval getLiveInterval(){
        return liveInterval;
    }

    public void setLiveInterval(LiveInterval liveInterval){
        this.liveInterval=liveInterval;
    }

    ArrayList<MachineCode> MClist=new ArrayList<>();

    public String getName(){return "";}

    Reg(state s){super(s);}
}
