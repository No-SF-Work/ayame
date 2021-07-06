package backend.machinecodes;

import backend.reg.VirtualReg;

public class MCGlobal extends MachineCode{

    private VirtualReg dst;


    MCGlobal(MachineBlock mb){
        super(TAG.Global,mb);
    }
}
