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

    public String getName(){return "#"+((Integer)imme).toString();}

    state s;
    MachineOperand(state s){
        this.s=s;
    }

    public state getState(){return s;}

    public boolean isPrecolored() {
        return this instanceof PhyReg && !((PhyReg) this).isAllocated;
    }

    public boolean isAllocated() {
        return this instanceof PhyReg && ((PhyReg) this).isAllocated;
    }

    public boolean isVirtual() { return this.s.equals(state.virtual); }

    public boolean needsColor() {
        return isPrecolored() || isVirtual();
    }

    public MachineOperand(int imme){
        this.s=state.imm;
        this.imme=imme;
    }
}
