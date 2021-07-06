package backend.machinecodes;

import backend.reg.VirtualReg;

public class MCCompare extends MachineCode{

    private VirtualReg lhs;

    private VirtualReg rhs;

    MCCompare(MachineBlock mb){
        super(TAG.Compare,mb);
    }
}
