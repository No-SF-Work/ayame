package backend.machinecodes;

import backend.reg.VirtualReg;

/**
 * Fma
 */
public class MCFma extends MachineCode{

    private VirtualReg dst;

    private VirtualReg lhs;

    private VirtualReg rhs;

    private VirtualReg acc;

    private ArmAddition.CondType cond;

    @Override
    public ArmAddition.CondType getCond() {
        return cond;
    }

    public MCFma( MachineBlock mb){
        super(TAG.FMA,mb);
    }

}
