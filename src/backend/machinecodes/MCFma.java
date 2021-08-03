package backend.machinecodes;

import backend.CodeGenManager;
import backend.reg.MachineOperand;

/**
 * Fma
 * smmla:Rn + (Rm * Rs)[63:32] or smmls:Rd := Rn – (Rm * Rs)[63:32]
 * mla:Rn + (Rm * Rs)[31:0] or mls:Rd := Rn – (Rm * Rs)[31:0]
 * dst = acc +(-) lhs * rhs
 */
public class MCFma extends MachineCode {

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

    private ArmAddition.CondType cond= ArmAddition.CondType.Any;

    public String toString() {
        String res = sign ? "\tsm" : "\t";
        res += add ? "mla" : "mls";
        res += condString(cond) + "\t" + dst.getName() + ",\t" + lhs.getName() + ",\t" + rhs.getName() + ",\t" + acc.getName() + "\n";
        CodeGenManager.getInstance().addOffset(1,res.length());
        return res;
    }

    public void setCond(ArmAddition.CondType cond) {
        this.cond = cond;
    }

    @Override
    public ArmAddition.CondType getCond() {
        return cond;
    }

    public MCFma(MachineBlock mb) {
        super(TAG.FMA, mb);
    }

    public MCFma(){
        super(TAG.FMA);
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
        dealReg(this.dst, dst, false);
        this.dst = dst;
    }

    public void setLhs(MachineOperand lhs) {
        dealReg(this.lhs, lhs, true);
        this.lhs = lhs;
    }

    public void setRhs(MachineOperand rhs) {
        dealReg(this.rhs, rhs, true);
        this.rhs = rhs;
    }

    public void setAcc(MachineOperand acc) {
        dealReg(this.acc, acc, true);
        this.acc = acc;
    }
}
