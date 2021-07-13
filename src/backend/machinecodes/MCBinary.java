package backend.machinecodes;

import backend.reg.VirtualReg;
import backend.reg.MachineOperand;

/**
 * Add,Sub,Rsb, Mul, Div, Mod, Lt, Le, Ge, Gt, Eq, Ne, And, Or
 */
public class MCBinary extends MachineCode{

    private MachineOperand dst;

    public MachineOperand getDst() {
        return dst;
    }

    public void setDst(MachineOperand dst) {
        super.insteadReg(this.dst);
        this.dst = dst;
    }

    public MachineOperand getLhs() {
        return lhs;
    }

    public void setLhs(MachineOperand lhs) {
        super.insteadReg(this.lhs);
        this.lhs = lhs;
    }

    public MachineOperand getRhs() {
        return rhs;
    }

    public void setRhs(MachineOperand rhs) {
        super.insteadReg(this.rhs);
        this.rhs = rhs;
    }

    private MachineOperand lhs;

    private MachineOperand rhs;

    private ArmAddition.CondType cond;

    @Override
    public ArmAddition.CondType getCond() {
        return cond;
    }

    public MCBinary(TAG tag, MachineBlock mb){
        super(tag,mb);
    }

}
