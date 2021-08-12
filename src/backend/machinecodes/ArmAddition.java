package backend.machinecodes;

import backend.reg.MachineOperand;
import backend.reg.PhyReg;
import backend.reg.Reg;

import java.util.Objects;

public class ArmAddition {

    public static ArmAddition getAddition() {
        return addition;
    }

    private ArmAddition() {
    }

    private static ArmAddition addition = new ArmAddition();

    public Shift getNewShiftInstance() {
        return new Shift();
    }

    public Shift getNewShiftInstance(ShiftType t, int imm) {
        return new Shift(t, imm);
    }

    public enum ShiftType {
        //no shift
        None,
        //arithmetic right
        Asr,
        //logic left
        Lsl,
        //logic right,
        Lsr,
        //rotate right,
        Ror,
        //rotate right one bit with extend
        Rrx
    }

    ;

    public enum CondType {
        Any,
        Eq,
        Ne,
        Ge,
        Gt,
        Le,
        Lt
    }

    ;


    public class Shift {


        private int imm = 0;

        public Reg getReg() {
            return reg;
        }

        private Reg reg=null;

        private ShiftType t = ShiftType.None;

        private boolean isNone = true;

        public boolean isNone() {
            return t == ShiftType.None || (imm == 0 && reg == null);
        }

        public boolean isReg = false;

        public ShiftType getType() {
            return t;
        }

        public void setReg(Reg reg){
            this.reg = reg;
            isReg = true;
        }

        public int getImm() {
            return imm;
        }

        public void setType(ShiftType t, int imm) {
            this.imm = imm;
            this.t = t;
        }

        public void setType(ShiftType t, Reg reg) {
            this.reg = reg;
            isReg = true;
            this.t = t;
        }

        @Override
        public String toString() {
            if (t == ShiftType.None || (imm == 0 && reg == null)) {
                return "";
            }
            String op = ", ";
            if (t == ShiftType.Asr) {
                op += "asr";
            } else if (t == ShiftType.Lsl) {
                op += "lsl";
            } else if (t == ShiftType.Lsr) {
                op += "lsr";
            } else {
                op += "";
                assert (false);
            }
            if(reg == null){
                op += " #" + ((Integer) imm).toString();
            }
            else{
                op += " "+reg.getName();
            }
            return op;
        }

        public Shift() {
            this.t = ShiftType.None;
        }

        public Shift(ShiftType t, int imm) {
            this.t = t;
            this.imm = imm;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Shift shift = (Shift) o;
            if (imm == shift.imm && t == shift.t) {
                return true;
            } else {
                return imm == 0 && shift.imm == 0;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(imm, t, isNone);
        }
    }
}


