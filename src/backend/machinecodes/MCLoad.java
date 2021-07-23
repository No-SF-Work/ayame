package backend.machinecodes;

import backend.CodeGenManager;
import backend.reg.MachineOperand;
import backend.reg.VirtualReg;

public class MCLoad extends MachineCode{

    public void setAddr(MachineOperand addr) {
        super.dealReg(this.addr,addr,true);
        this.addr = addr;
    }

    public void setOffset(MachineOperand offset) {
        super.dealReg(this.offset,offset,true);
        this.offset = offset;
    }

    public void setDst(MachineOperand dst) {
        dealReg(this.dst,dst,false);
        this.dst=dst;
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

    private ArmAddition.CondType cond= ArmAddition.CondType.Any;

    @Override
    public ArmAddition.CondType getCond() {
        return cond;
    }

    public void setCond(ArmAddition.CondType cond) {
        this.cond = cond;
    }

    @Override
    public String toString(){
        if(addr instanceof VirtualReg && ((VirtualReg)addr).isGlobal()){
            CodeGenManager.getInstance().setGlobalInfo(this);
            return "";
//            "\tldr\t"+dst.getName()+",\t="+addr.getName()+"\n"
        }
        String res="\tldr"+ condString(cond)+"\t"+dst.getName()+",\t["+addr.getName();
        res+=",\t"+offset.getName()+getShift().toString()+"]\n";
        CodeGenManager.getInstance().addOffset(1,res.length());
        return res;
    }

    public MCLoad(MachineBlock mb){
        super(TAG.Load,mb);
    }

    public MCLoad(){
        super(TAG.Load);
    }

    public MCLoad(MachineBlock mb,int num){
        super(TAG.Load,mb,num);
    }
}
