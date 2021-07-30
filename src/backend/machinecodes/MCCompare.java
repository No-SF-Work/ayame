package backend.machinecodes;

import backend.CodeGenManager;
import backend.reg.MachineOperand;
import backend.reg.VirtualReg;

public class MCCompare extends MachineCode{

    private MachineOperand lhs;
    private boolean isCmn = false;

    @Override
    public ArmAddition.CondType getCond() {
        return cond;
    }

    public void setCmn() {
        this.isCmn = true;
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
        String res;
        if(isCmn){
            res="\tcmn\t"+lhs.getName()+",\t"+rhs.getName()+getShift().toString()+"\n";
        }else{
            res="\tcmp\t"+lhs.getName()+",\t"+rhs.getName()+getShift().toString()+"\n";
        }
        CodeGenManager.getInstance().addOffset(1,res.length());
        return res;
    }

    public MCCompare(MachineBlock mb){
        super(TAG.Compare,mb);
    }
}
