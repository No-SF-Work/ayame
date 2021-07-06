package backend.machinecodes;

import backend.reg.VirtualReg;

/**
 * LongMul
 */
public class MCLongMul extends MachineCode{

    private VirtualReg dst;

    private VirtualReg lhs;

    private VirtualReg rhs;

    public MCLongMul( MachineBlock mb){
        super(TAG.LongMul,mb);
    }

}
