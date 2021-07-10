package backend.machinecodes;

import backend.reg.VirtualReg;

/**
 * Branch
 */
public class MCBranch extends MachineCode{

    private MachineBlock target;

    private ArmAddition.CondType cond;

    @Override
    public ArmAddition.CondType getCond() {
        return cond;
    }

    public MCBranch( MachineBlock mb){
        super(TAG.Branch,mb);
    }

}