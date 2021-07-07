package backend.machinecodes;

public class ArmAddition{

    public static ArmAddition getAddition(){
        return addition;
    }
    private ArmAddition(){}

    private static ArmAddition addition=new ArmAddition();

    public Shift getShiftInstance(){
        return new Shift();
    }

    public Shift getShiftInstance(ShiftType t, int imm){
        return new Shift(t,imm);
    }

    enum ShiftType{
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
    };

    enum CondType{
        Any,
        Eq,
        Ne,
        Ge,
        Gt,
        Le,
        Lt
    };


    public class Shift {


        private int imm=0;

        private ShiftType t= ShiftType.None;

        private boolean isNone=true;

        public boolean isNone(){
            return isNone;
        }

        public ShiftType getType(){return t;}

        public int getImm(){
            return imm;
        }

        public void setType(ShiftType t, int imm){
            this.imm=imm;
            this.t=t;
        }

        public Shift(){ this.t=ShiftType.None;}

        public Shift(ShiftType t, int imm){
            this.t=t;
            this.imm=imm;
        }
    }
}


