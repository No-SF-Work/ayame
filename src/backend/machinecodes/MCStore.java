package backend.machinecodes;

import backend.reg.MachineOperand;
import backend.reg.PhyReg;
import backend.reg.VirtualReg;

public class MCStore extends MachineCode{

    public MachineOperand getAddr() {
        return addr;
    }

    public void setAddr(MachineOperand addr) {
        super.insteadReg(this.addr);
        this.addr = addr;
    }

    public MachineOperand getOffset() {
        return offset;
    }

    public void setOffset(MachineOperand offset) {
        super.insteadReg(this.offset);
        this.offset = offset;
    }

    public MachineOperand getData() {
        return data;
    }

    public void setData(MachineOperand data) {
        super.insteadReg(this.data);
        this.data = data;
    }

    private MachineOperand addr;

    private MachineOperand offset;

    private MachineOperand data;

    public MCStore(MachineBlock mb){
        super(TAG.Store ,mb);
    }

}
