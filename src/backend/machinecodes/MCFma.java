package backend.machinecodes;

import backend.reg.VirtualReg;
import backend.reg.MachineOperand;

/**
 * Fma
 */
public class MCFma extends MachineCode{

    private MachineOperand dst;

    private MachineOperand lhs;

    private MachineOperand rhs;

    private MachineOperand acc;

    public boolean isAdd() {
        return add;
    }

    public void setAdd(boolean add) {
        this.add = add;
    }

    public boolean isSign() {
        return sign;
    }

    public void setSign(boolean sign) {
        this.sign = sign;
    }

    boolean add;

    boolean sign;

    private ArmAddition.CondType cond;

    public String toString(){
        String res=add?"\tsmmla":"\tsmmls";
        res+=contString(cond)+"\t"+dst.getName()+",\t"+lhs.getName()+",\t"+rhs.getName()+",\t"+acc.getName()+"\n";
        return res;
    }

    public void setCond(ArmAddition.CondType cond){
        this.cond=cond;
    }

    @Override
    public ArmAddition.CondType getCond() {
        return cond;
    }

    public MCFma( MachineBlock mb){
        super(TAG.FMA,mb);
    }

    public MachineOperand getDst() {
        return dst;
    }

    public MachineOperand getLhs() {
        return lhs;
    }

    public MachineOperand getRhs() {
        return rhs;
    }

    public MachineOperand getAcc() {
        return acc;
    }

    public void setDst(MachineOperand dst) {
        dealReg(this.dst,dst,false);
        this.dst = dst;
    }

    public void setLhs(MachineOperand lhs) {
        dealReg(this.lhs,lhs,true);
        this.lhs = lhs;
    }

    public void setRhs(MachineOperand rhs) {
        dealReg(this.rhs,rhs,true);
        this.rhs = rhs;
    }

    public void setAcc(MachineOperand acc) {
        dealReg(this.acc,acc,true);
        this.acc = acc;
    }
}
