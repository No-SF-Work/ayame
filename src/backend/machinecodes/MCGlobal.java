package backend.machinecodes;

import backend.reg.MachineOperand;
import backend.reg.VirtualReg;

public class MCGlobal extends MachineCode{

    private MachineOperand dst;


    MCGlobal(MachineBlock mb){
        super(TAG.Global,mb);
    }
}
