package backend.machinecodes;

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

    public MCJump( MachineBlock mb){
        super(TAG.Jump,mb);
    }

}
