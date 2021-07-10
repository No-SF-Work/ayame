package backend.machinecodes;

import backend.reg.VirtualReg;
import backend.reg.MachineOperand;



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

    public MCMove(MachineBlock mb,int num){
        super(TAG.Mv,mb,num);
    }
}
