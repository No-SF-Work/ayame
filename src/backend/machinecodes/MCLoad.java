package backend.machinecodes;

import backend.reg.MachineOperand;
import backend.reg.VirtualReg;

public class MCLoad extends MachineCode{

    private MachineOperand addr;

    private MachineOperand offset;

    private MachineOperand data;

    MCLoad(MachineBlock mb){
        super(MachineCode.TAG.Branch,mb);
    }
}
