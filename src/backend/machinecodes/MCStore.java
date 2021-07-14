package backend.machinecodes;

import backend.reg.MachineOperand;
import backend.reg.PhyReg;
import backend.reg.VirtualReg;

public class MCStore extends MachineCode{

    public MachineOperand getAddr() {
        return addr;
    }

    public void setAddr(MachineOperand addr) {
        super.dealReg(this.addr,addr,true);
        this.addr = addr;
    }

    public MachineOperand getOffset() {
        return offset;
    }

    public void setOffset(MachineOperand offset) {
        super.dealReg(this.offset,offset,true);
        this.offset = offset;
    }

    public MachineOperand getData() {
        return data;
    }

    public void setData(MachineOperand data) {
        super.dealReg(this.data,data,true);
        this.data = data;
    }

    @Override
    public ArmAddition.CondType getCond() {
        return cond;
    }

    public void setCond(ArmAddition.CondType cond) {
        this.cond = cond;
    }

    private ArmAddition.CondType cond;

    private MachineOperand addr;

    private MachineOperand offset;

    private MachineOperand data;

    public MCStore(MachineBlock mb){
        super(TAG.Store ,mb);
    }

    public MCStore(){
        super(TAG.Store);
    }

}
