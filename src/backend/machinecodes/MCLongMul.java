package backend.machinecodes;

import backend.CodeGenManager;
import backend.reg.MachineOperand;
import backend.reg.PhyReg;
import backend.reg.VirtualReg;

/**
 * LongMul
 * Rd := (Rm * Rs)[63:32]
 */
public class MCLongMul extends MachineCode {

    private MachineOperand dst = null;

    private MachineOperand lhs = null;

    private MachineOperand rhs = null;

    public MCLongMul(MachineBlock mb) {
        super(TAG.LongMul, mb);
    }

    public String toString() {
        String res = "\tsmmul\t" + dst.getName() + ",\t" + lhs.getName() + ",\t" + rhs.getName() + "\n";
        CodeGenManager.getInstance().addOffset(1, res.length());
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
        dealReg(this.dst, dst, false);
        this.dst = dst;
    }

    public void calcCost() {
        assert (this.dst != null);
        assert (this.lhs != null);
        assert (this.rhs != null);
        if(dst instanceof PhyReg){
            return;
        }
        int cost = 0;
        if (lhs instanceof PhyReg) {
            ((VirtualReg) dst).setUnMoveable();
        } else {
            cost += ((VirtualReg) lhs).getCost();
        }
        if (rhs instanceof PhyReg) {
            ((VirtualReg) dst).setUnMoveable();
        } else if (lhs != rhs) {
            cost += ((VirtualReg) rhs).getCost();
        }
        if (!getShift().isNone()) {
            cost += 1;
        }
        ((VirtualReg) dst).setDef(this, cost + 3);
    }

    public void setCond(ArmAddition.CondType cond) {
        this.cond = cond;
    }

    private ArmAddition.CondType cond = ArmAddition.CondType.Any;

    public void setLhs(MachineOperand lhs) {
        dealReg(this.lhs, lhs, true);
        this.lhs = lhs;
    }

    public void setRhs(MachineOperand rhs) {
        dealReg(this.rhs, rhs, true);
        this.rhs = rhs;
    }
}
