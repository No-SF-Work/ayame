package backend.machinecodes;

/**
 * Branch
 */
public class MCJump extends MachineCode{

    private MachineBlock target;

    public MCJump( MachineBlock mb){
        super(TAG.Branch,mb);
    }

}
