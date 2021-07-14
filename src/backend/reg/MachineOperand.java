package backend.reg;

public class MachineOperand {
    public enum state{
        virtual,
        phy,
        imm
    }

    //if MO is imm, the value is imme
    int imme;

    public int getImm(){return imme;}

    state s;
    MachineOperand(state s){
        this.s=s;
    }

    public state getState(){return s;}

    public boolean isPrecolored() {
        return this.s.equals(state.phy);
    }

    public MachineOperand(int imme){
        this.s=state.imm;
        this.imme=imme;
    }
}
