package backend.machinecodes;

import backend.reg.VirtualReg;
import backend.reg.MachineOperand;


/**
 * Add,Sub,Rsb, Mul, Div, Mod, Lt, Le, Ge, Gt, Eq, Ne, And, Or
 */
public class MCMove extends MachineCode{

    public MachineOperand getDst() {
        return dst;
    }

    public void setDst(MachineOperand dst) {

        this.dst = dst;
        addDef(dst);
    }

    public MachineOperand getRhs() {
        return rhs;
    }

    public void setRhs(MachineOperand rhs) {
        this.rhs = rhs;
        addUse(rhs);
    }

    private MachineOperand dst;

    private MachineOperand rhs;

    private ArmAddition.CondType cond;

    public MCMove(MachineBlock mb){
        super(TAG.Mv,mb);
    }

}
