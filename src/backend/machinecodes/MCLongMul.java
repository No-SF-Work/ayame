package backend.machinecodes;

import backend.reg.MachineOperand;
import backend.reg.VirtualReg;

/**
 * LongMul
 */
public class MCLongMul extends MachineCode{

    private MachineOperand dst;

    private MachineOperand lhs;

    private MachineOperand rhs;

    public MCLongMul( MachineBlock mb){
        super(TAG.LongMul,mb);
    }

    public MachineOperand getDst() {
        return dst;
    }

    public MachineOperand getLhs() {
        return lhs;
    }

    public MachineOperand getRhs() {
        return rhs;
    }

    public void setDst(MachineOperand dst) {
        dealReg(this.dst,dst,false);
        this.dst = dst;
    }

    public void setLhs(MachineOperand lhs) {
        dealReg(this.lhs,lhs,true);
        this.lhs = lhs;
    }

    public void setRhs(MachineOperand rhs) {
        dealReg(this.rhs,rhs,false);
        this.rhs = rhs;
    }
}
