package backend.machinecodes;

import backend.reg.VirtualReg;

/**
 * Add,Sub,Rsb, Mul, Div, Mod, Lt, Le, Ge, Gt, Eq, Ne, And, Or
 */
public class MCBinary extends MachineCode{

    private VirtualReg dst;

    private VirtualReg lhs;

    private VirtualReg rhs;

    public MCBinary(TAG_ tag, MachineBlock mb){
        super(tag,2,mb);
    }

}
