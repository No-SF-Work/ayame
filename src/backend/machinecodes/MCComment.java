package backend.machinecodes;

import backend.reg.VirtualReg;

public class MCComment extends MachineCode{

    private VirtualReg dst;


    MCComment(String str, MachineBlock mb){
        super(TAG.Global,mb);
    }
}
