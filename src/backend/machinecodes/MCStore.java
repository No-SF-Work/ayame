package backend.machinecodes;

import backend.CodeGenManager;
import backend.reg.MachineOperand;

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

    private ArmAddition.CondType cond= ArmAddition.CondType.Any;

    private MachineOperand addr;

    private MachineOperand offset;

    private MachineOperand data;

    public MCStore(MachineBlock mb){
        super(TAG.Store ,mb);
    }

    public MCStore(){
        super(TAG.Store);
    }

    @Override
    public String toString(){
        String res="\tstr"+ condString(cond)+"\t"+data.getName()+",\t["+addr.getName();
        res+=",\t"+offset.getName()+getShift().toString()+"]\n";
        CodeGenManager.getInstance().addOffset(1,res.length());
        return res;
    }



}
