package backend.machinecodes;

import backend.reg.VirtualReg;

public class MCStore extends MachineCode{

    private VirtualReg addr;

    private VirtualReg offset;

    private VirtualReg data;

    ArmAddition.Shift shift=ArmAddition.getAddition().getShiftInstance();

    MCStore(MachineBlock mb){
        super(MachineCode.TAG.Branch,mb);
    }
}
