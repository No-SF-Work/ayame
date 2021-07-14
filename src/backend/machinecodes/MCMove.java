package backend.machinecodes;

import backend.reg.VirtualReg;
import backend.reg.MachineOperand;



public class MCMove extends MachineCode{

    public MachineOperand getDst() {
        return dst;
    }

    public void setDst(MachineOperand dst) {
        dealReg(this.dst,dst,false);
        this.dst = dst;
    }

    public MachineOperand getRhs() {
        return rhs;
    }

    public void setRhs(MachineOperand rhs) {
        dealReg(this.rhs,rhs,true);
        this.rhs = rhs;
    }

    private MachineOperand dst;

    private MachineOperand rhs;

    @Override
    public ArmAddition.CondType getCond() {
        return cond;
    }

    public void setCond(ArmAddition.CondType cond) {
        this.cond = cond;
    }

    private ArmAddition.CondType cond;

    public MCMove(MachineBlock mb){
        super(TAG.Mv,mb);
    }

    public MCMove(){
        super(TAG.Mv);
    }

    public MCMove(MachineBlock mb,int num){
        super(TAG.Mv,mb,num);
    }
}
