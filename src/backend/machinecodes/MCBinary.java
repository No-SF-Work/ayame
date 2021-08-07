package backend.machinecodes;

import backend.CodeGenManager;
import backend.reg.PhyReg;
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

    public void calcCost(){
        assert(dst!=null);
        assert(this.lhs!=null);
        assert(this.rhs!=null);
        int cost = 0;
        if(dst instanceof PhyReg){
            return;
        }
        if(lhs instanceof PhyReg){
            if(((PhyReg)lhs).getName().equals("sp")){

            }else{
                ((VirtualReg)dst).setUnMoveable();
            }
        }else {
            cost+=((VirtualReg)lhs).getCost();
        }
        if(rhs.getState()== MachineOperand.state.imm){

        }else if(rhs instanceof PhyReg){
            ((VirtualReg)dst).setUnMoveable();
        }
        else if(lhs !=rhs){
            cost+=((VirtualReg)rhs).getCost();
        }
        if(!getShift().isNone()){
            cost+=1;
        }
        ((VirtualReg)dst).setDef(this,cost+1);
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
        }else if(getTag()==TAG.Bic){
            op="bic";
        }
        else{
            assert(false);
        }
        String res="\t"+op+"\t"+dst.getName()+",\t"+lhs.getName()+",\t"+rhs.getName()+getShift().toString()+"\n";
        CodeGenManager.getInstance().addOffset(1,res.length());
        return res;
    }

    public MachineOperand getRhs() {
        return rhs;
    }

    public void setRhs(MachineOperand rhs) {
        super.dealReg(this.rhs,rhs,true);
        this.rhs = rhs;
    }

    private MachineOperand lhs = null;

    private MachineOperand rhs = null;

    private ArmAddition.CondType cond = ArmAddition.CondType.Any;

    public void setCond(ArmAddition.CondType cond) {
        this.cond = cond;
    }

    @Override
    public ArmAddition.CondType getCond() {
        return cond;
    }

    public MCBinary(TAG tag, MachineBlock mb){
        super(tag,mb);
    }

    public MCBinary(TAG tag){
        super(tag);
    }

}
