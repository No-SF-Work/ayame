package backend.machinecodes;

import backend.CodeGenManager;
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

    private ArmAddition.CondType cond= ArmAddition.CondType.Any;

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

    @Override
    public String toString(){
        String res="\tcmp\t"+lhs.getName()+",\t"+rhs.getName()+"\n";
        CodeGenManager.getInstance().addOffset(1,res.length());
        return res;
    }

    public MCCompare(MachineBlock mb){
        super(TAG.Compare,mb);
    }
}
