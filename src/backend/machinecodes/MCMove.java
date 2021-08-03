package backend.machinecodes;

import backend.CodeGenManager;
import backend.reg.MachineOperand;
import backend.reg.PhyReg;
import backend.reg.VirtualReg;

public class MCMove extends MachineCode {

    public MachineOperand getDst() {
        return dst;
    }

    public void setDst(MachineOperand dst) {
        dealReg(this.dst, dst, false);
        this.dst = dst;
        if(dst instanceof VirtualReg){
            assert(rhs!=null);
            if(rhs instanceof PhyReg){
                ((VirtualReg)dst).setUnMoveable();
            }else if(rhs.getState()== MachineOperand.state.imm){
                ((VirtualReg)dst).setDef(this,1);
            }else if(!getShift().isNone()){
                ((VirtualReg)dst).setDef(this,((VirtualReg)rhs).getCost()+2);
            }else{
                ((VirtualReg)dst).setDef(this,((VirtualReg)rhs).getCost()+1);
            }
        }
    }

    public MachineOperand getRhs() {
        return rhs;
    }

    public void setRhs(MachineOperand rhs) {
        dealReg(this.rhs, rhs, true);
        this.rhs = rhs;
    }

    private MachineOperand dst;

    private MachineOperand rhs=null;

    @Override
    public ArmAddition.CondType getCond() {
        return cond;
    }

    @Override
    public String toString() {
        String res = "\t";
        if (rhs.getState() == MachineOperand.state.imm && CodeGenManager.canEncodeImm(~(rhs.getImm()))) {
            int imm = ~rhs.getImm();
            res += "mvn" + condString(cond) + "\t" + dst.getName() + ",\t#" + imm + "\n";
            CodeGenManager.getInstance().addOffset(1, res.length());
        } else if (rhs.getState() == MachineOperand.state.imm && !CodeGenManager.canEncodeImm(rhs.getImm())) {
            int imm = rhs.getImm();
            int immH = imm >>> 16;
            int immL = (imm << 16) >>> 16;
            int offnum = 1;
            res += "movw" + condString(cond) + "\t" + dst.getName() + ",\t#" + immL + "\n";
            if (immH != 0) {
                res += "\tmovt" + condString(cond) + "\t" + dst.getName() + ",\t#" + immH + "\n";
                offnum++;
            }
            CodeGenManager.getInstance().addOffset(offnum, res.length());
        } else {
            res += "mov" + condString(cond) + "\t" + dst.getName() + ",\t" + rhs.getName() + getShift().toString() + "\n";
            CodeGenManager.getInstance().addOffset(1, res.length());
        }
        return res;
    }

    public void setCond(ArmAddition.CondType cond) {
        this.cond = cond;
    }

    private ArmAddition.CondType cond = ArmAddition.CondType.Any;

    public MCMove(MachineBlock mb) {
        super(TAG.Mv, mb);
    }

    public MCMove() {
        super(TAG.Mv);
    }

    public MCMove(MachineBlock mb, int num) {
        super(TAG.Mv, mb, num);
    }
}
