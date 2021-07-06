package backend.machinecodes;

import backend.reg.VirtualReg;

/**
 * Add,Sub,Rsb, Mul, Div, Mod, Lt, Le, Ge, Gt, Eq, Ne, And, Or
 */
public class MCMove extends MachineCode{

    private VirtualReg dst;

    private VirtualReg rhs;

    private ArmAddition.CondType cond;

    private ArmAddition.Shift shift = ArmAddition.getAddition().getShiftInstance();

    public MCMove(MachineBlock mb){
        super(TAG.Mv,mb);
    }

}
