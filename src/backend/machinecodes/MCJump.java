package backend.machinecodes;

import backend.CodeGenManager;

/**
 * Branch
 */
public class MCJump extends MachineCode{

    public MachineBlock getTarget() {
        return target;
    }

    public void setTarget(MachineBlock target) {
        this.target = target;
    }

    private MachineBlock target;

    @Override
    public String toString(){
        String res="\tb\t"+target.getName()+"\n";
        CodeGenManager.getInstance().addOffset(1,res.length());
        return res;
    }

    public MCJump( MachineBlock mb){
        super(TAG.Jump,mb);
    }

}
