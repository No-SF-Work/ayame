package backend.machinecodes;

import backend.CodeGenManager;
import backend.reg.MachineOperand;
import backend.reg.VirtualReg;

/**
 * LongMul
 * Rd := (Rm * Rs)[63:32]
 */
public class MCLongMul extends MachineCode{

    private MachineOperand dst=null;

    private MachineOperand lhs=null;

    private MachineOperand rhs=null;

    public MCLongMul( MachineBlock mb){
        super(TAG.LongMul,mb);
    }

    public String toString(){
        String res="\tsmmul\t"+dst.getName()+",\t"+lhs.getName()+",\t"+rhs.getName()+"\n";
        CodeGenManager.getInstance().addOffset(1,res.length());
        return res;
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

    public void setDst(MachineOperand dst) {
        dealReg(this.dst,dst,false);
        this.dst = dst;
        if(dst instanceof VirtualReg){
            assert(this.lhs!=null);
            assert(this.rhs!=null);
            assert(this.lhs instanceof VirtualReg);
            assert(this.rhs instanceof VirtualReg);
            int cost=((VirtualReg)lhs).getCost()+((VirtualReg)rhs).getCost()+3;
            ((VirtualReg)dst).setDef(this,cost);
        }
    }

    public void setLhs(MachineOperand lhs) {
        dealReg(this.lhs,lhs,true);
        this.lhs = lhs;
    }

    public void setRhs(MachineOperand rhs) {
        dealReg(this.rhs,rhs,true);
        this.rhs = rhs;
    }
}
