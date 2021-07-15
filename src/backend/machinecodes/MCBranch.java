package backend.machinecodes;

import backend.reg.VirtualReg;

/**
 * Branch
 */
public class MCBranch extends MachineCode{

    private MachineBlock target;

    private ArmAddition.CondType cond;

    public void setCond(ArmAddition.CondType cond){
        this.cond=cond;
    }

    public void setTarget(MachineBlock target){
        this.target=target;
    }

    @Override
    public String toString(){
        String res="\tb"+contString(cond)+"\t"+target.getName()+"\n";
        return res;
    }

    @Override
    public ArmAddition.CondType getCond() {
        return cond;
    }

    public MCBranch( MachineBlock mb){
        super(TAG.Branch,mb);
    }

    public MachineBlock getTarget() {
        return target;
    }
}