package backend.machinecodes;

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

    @Override
    public ArmAddition.CondType getCond() {
        return cond;
    }

    public void setCond(ArmAddition.CondType cond) {
        this.cond = cond;
    }

    @Override
    public String toString(){
        String res="\tstr"+contString(cond)+"\t"+dst.getName()+",\t["+addr.getName();
        res+=",\t"+offset.getName()+getShift().toString()+"]\n";
//        if(offset.getState()== MachineOperand.state.imm){
//            if(offset.getImm()==0){
//                res+="]\n";
//            }else{
//                res+=",\t#"+offset.getImm()+"]\n";
//            }
//        }else{
//            res+=",\t"+offset.getName()+getShift().toString()+"]\n";
//        }
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
