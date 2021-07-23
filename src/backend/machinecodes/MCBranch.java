package backend.machinecodes;

import backend.CodeGenManager;

/**
 * Branch
 */
public class MCBranch extends MachineCode{

    private MachineBlock target;

    private ArmAddition.CondType cond = ArmAddition.CondType.Any
            ;

    public void setCond(ArmAddition.CondType cond){
        this.cond=cond;
    }

    public void setTarget(MachineBlock target){
        this.target=target;
    }

    @Override
    public String toString(){
        String res="\tb"+ condString(cond)+"\t"+target.getName()+"\n";
        CodeGenManager.getInstance().addOffset(1,res.length());
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