package backend.machinecodes;

import backend.reg.MachineOperand;
import backend.reg.VirtualReg;

public class MCCompare extends MachineCode{

    private MachineOperand lhs;

    @Override
    public ArmAddition.CondType getCond() {
        return cond;
    }

    public void setCond(ArmAddition.CondType cond) {
        this.cond = cond;
    }

    private ArmAddition.CondType cond;

    public MachineOperand getLhs() {
        return lhs;
    }

    public void setLhs(MachineOperand lhs) {
        dealReg(this.lhs,lhs,true);
        this.lhs = lhs;
    }

    public MachineOperand getRhs() {
        return rhs;
    }

    public void setRhs(MachineOperand rhs) {
        dealReg(this.rhs,rhs,true);
        this.rhs = rhs;
    }

    private MachineOperand rhs;

    public MCCompare(MachineBlock mb){
        super(TAG.Compare,mb);
    }
}
