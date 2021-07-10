package backend.machinecodes;

import backend.reg.MachineOperand;
import backend.reg.VirtualReg;

/**
 * LongMul
 */
public class MCLongMul extends MachineCode{

    private MachineOperand dst;

    private MachineOperand lhs;

    private MachineOperand rhs;

    public MCLongMul( MachineBlock mb){
        super(TAG.LongMul,mb);
    }

}
