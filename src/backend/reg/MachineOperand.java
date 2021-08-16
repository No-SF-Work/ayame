package backend.reg;

import java.util.Objects;

public class MachineOperand implements Comparable<MachineOperand> {
    public static MachineOperand zeroImm = new MachineOperand(0);

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



    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof MachineOperand)){
            return false;
        }
        if(((MachineOperand) obj).getState()!=this.getState()){
            return false;
        }
        if(this.getState()==state.imm){
            return this.imme== ((MachineOperand) obj).getImm();
        }else {
            return this.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(imme, s);
    }

    @Override
    public int compareTo(MachineOperand rhs) {
        int st1 = -1;
        if (this.isPrecolored()) st1 = 0;
        else if (this.isAllocated()) st1 = 1;
        else if (this.s == state.virtual) st1 = 2;
        else if (this.s == state.imm) st1 = 3;

        int st2 = -1;
        if (rhs.isPrecolored()) st2 = 0;
        else if (rhs.isAllocated()) st2 = 1;
        else if (rhs.s == state.virtual) st2 = 2;
        else if (rhs.s == state.imm) st2 = 3;
        assert st1 != -1 && st2 != -1;

        if (st1 == st2) {
            if (st1 == 0 || st1 == 1) return ((PhyReg) this).getIdx() - ((PhyReg) rhs).getIdx();
            else if (st1 == 2) {
                var name1 = this.getName();
                var name2 = rhs.getName();
                return name1.compareTo(name2);
            } else return imme - rhs.imme;
        } else return st1 - st2;
    }

}
