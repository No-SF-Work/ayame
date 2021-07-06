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

    public MCFma( MachineBlock mb){
        super(TAG.FMA,mb);
    }

}
