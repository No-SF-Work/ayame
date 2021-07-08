package backend.machinecodes;

import backend.reg.MachineOperand;
import backend.reg.VirtualReg;


public class MCComment extends MachineCode{

    private MachineOperand dst;


    MCComment(String str, MachineBlock mb){
        super(TAG.Global,mb);
    }
}
