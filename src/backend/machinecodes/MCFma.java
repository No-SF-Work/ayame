package backend.machinecodes;

import backend.reg.VirtualReg;
import backend.reg.MachineOperand;

/**
 * Fma
 */
public class MCFma extends MachineCode{

    private MachineOperand dst;

    private MachineOperand lhs;

    private MachineOperand rhs;

    private MachineOperand acc;

    private ArmAddition.CondType cond;

    @Override
    public ArmAddition.CondType getCond() {
        return cond;
    }

    public MCFma( MachineBlock mb){
        super(TAG.FMA,mb);
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

    public MachineOperand getAcc() {
        return acc;
    }

    public void setDst(MachineOperand dst) {
        this.dst = dst;
    }

    public void setLhs(MachineOperand lhs) {
        this.lhs = lhs;
    }

    public void setRhs(MachineOperand rhs) {
        this.rhs = rhs;
    }

    public void setAcc(MachineOperand acc) {
        this.acc = acc;
    }
}
