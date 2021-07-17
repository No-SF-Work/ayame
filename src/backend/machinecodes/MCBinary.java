package backend.machinecodes;

import backend.reg.VirtualReg;
import backend.reg.MachineOperand;

/**
 * Add,Sub,Rsb, Mul, Div, Mod, Lt, Le, Ge, Gt, Eq, Ne, And, Or
 */
public class MCBinary extends MachineCode{

    private MachineOperand dst;

    public MachineOperand getDst() {
        return dst;
    }

    public void setDst(MachineOperand dst) {
        super.dealReg(this.dst,dst,false);
        this.dst = dst;
    }

    public MachineOperand getLhs() {
        return lhs;
    }

    public void setLhs(MachineOperand lhs) {
        super.dealReg(this.lhs,lhs,true);
        this.lhs = lhs;
    }

    @Override
    public String toString(){
        String op="";
        if(getTag()==TAG.Add){
            op="add";
        }else if(getTag()==TAG.Sub){
            op="sub";
        }else if(getTag()==TAG.Rsb){
            op="rsb";
        }else if(getTag()==TAG.Div){
            op="sdiv";
        }else if(getTag()==TAG.Mul){
            op="mul";
        }else{
            assert(false);
        }
        String res="\t"+op+"\t"+dst.getName()+",\t"+lhs.getName()+",\t"+rhs.getName()+getShift().toString()+"\n";
        return res;
    }

    public MachineOperand getRhs() {
        return rhs;
    }

    public void setRhs(MachineOperand rhs) {
        super.dealReg(this.rhs,rhs,true);
        this.rhs = rhs;
    }

    private MachineOperand lhs;

    private MachineOperand rhs;

    private ArmAddition.CondType cond = ArmAddition.CondType.Any;

    @Override
    public ArmAddition.CondType getCond() {
        return cond;
    }

    public MCBinary(TAG tag, MachineBlock mb){
        super(tag,mb);
    }

}
