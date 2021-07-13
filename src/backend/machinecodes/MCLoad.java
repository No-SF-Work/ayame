package backend.machinecodes;

import backend.reg.MachineOperand;
import backend.reg.VirtualReg;

public class MCLoad extends MachineCode{

    public void setAddr(MachineOperand addr) {
        this.addr = addr;
        addUse(addr);
    }

    public void setOffset(MachineOperand offset) {
        this.offset = offset;
        addUse(offset);
    }

    public void setDst(MachineOperand dst) {
        this.dst = dst;
        addDef(dst);
    }

    public MachineOperand getAddr() {
        return addr;
    }

    public MachineOperand getOffset() {
        return offset;
    }

    public MachineOperand getDst() {
        return dst;
    }

    private MachineOperand addr;

    private MachineOperand offset;

    private MachineOperand dst;

    private ArmAddition.CondType cond;

    public MCLoad(MachineBlock mb){
        super(TAG.Load,mb);
    }

    public MCLoad(MachineBlock mb,int num){
        super(TAG.Load,mb,num);
    }
}
