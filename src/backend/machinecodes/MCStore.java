package backend.machinecodes;

import backend.reg.MachineOperand;
import backend.reg.VirtualReg;

public class MCStore extends MachineCode{

    private MachineOperand addr;

    private MachineOperand offset;

    private MachineOperand data;

    MCStore(MachineBlock mb){
        super(MachineCode.TAG.Branch,mb);
    }
}
