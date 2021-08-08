package backend.machinecodes;

import backend.CodeGenManager;
import backend.reg.MachineOperand;
import backend.reg.PhyReg;
import backend.reg.VirtualReg;

public class MCLoad extends MachineCode {

    public void setAddr(MachineOperand addr) {
        super.dealReg(this.addr, addr, true);
        this.addr = addr;
    }

    public void setOffset(MachineOperand offset) {
        super.dealReg(this.offset, offset, true);
        this.offset = offset;
    }

    public void setDst(MachineOperand dst) {
        dealReg(this.dst, dst, false);
        this.dst = dst;
    }

    public void calcCost() {
        assert (this.dst != null);
        assert (this.addr != null);
        assert (this.offset != null);
        if (dst instanceof PhyReg) {
            return;
        }
        if (addr instanceof VirtualReg && ((VirtualReg) addr).isGlobal()) {
            ((VirtualReg) dst).setDef(this, 1);
        } else {
            int cost = 0;
            if (addr instanceof PhyReg) {
                ((VirtualReg) dst).setUnMoveable();
            } else {
                cost += ((VirtualReg) addr).getCost();
            }
            if (offset.getState() == MachineOperand.state.imm) {
            } else if (offset instanceof PhyReg) {
                ((VirtualReg) dst).setUnMoveable();
            } else {
                cost += ((VirtualReg) offset).getCost();
            }
            ((VirtualReg) dst).setDef(this, cost + 4);
        }
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

    private MachineOperand addr = null;

    private MachineOperand offset = null;

    private MachineOperand dst = null;

    private ArmAddition.CondType cond = ArmAddition.CondType.Any;

    @Override
    public ArmAddition.CondType getCond() {
        return cond;
    }

    public void setCond(ArmAddition.CondType cond) {
        this.cond = cond;
    }

    @Override
    public String toString() {
        if (addr instanceof VirtualReg && ((VirtualReg) addr).isGlobal()) {
            //gcc，狗都不用
//            CodeGenManager.getInstance().setGlobalInfo(this);
            String res = "\tmovw" + condString(cond) + "\t" + dst.getName() + ",\t:lower16:" + addr.getName() + "\n";
            res += "\tmovt" + condString(cond) + "\t" + dst.getName() + ",\t:upper16:" + addr.getName() + "\n";
            return res;
//            return "";
//            "\tldr\t"+dst.getName()+",\t="+addr.getName()+"\n"
        }
        String res = "";
        res = "\tldr" + condString(cond) + "\t" + dst.getName() + ",\t[" + addr.getName();
        res += ",\t" + offset.getName() + getShift().toString() + "]\n";
        CodeGenManager.getInstance().addOffset(1, res.length());
        return res;
    }

    public MCLoad(MachineBlock mb) {
        super(TAG.Load, mb);
    }

    public MCLoad() {
        super(TAG.Load);
    }

    public MCLoad(MachineBlock mb, int num) {
        super(TAG.Load, mb, num);
    }
}
