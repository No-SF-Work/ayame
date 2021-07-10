package backend.machinecodes;

import backend.reg.MachineOperand;
import backend.reg.VirtualReg;

public class MCCompare extends MachineCode{

    private MachineOperand lhs;

    private MachineOperand rhs;

    MCCompare(MachineBlock mb){
        super(TAG.Compare,mb);
    }
}
