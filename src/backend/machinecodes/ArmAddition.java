package backend.machinecodes;

public class ArmAddition{

    public static ArmAddition getAddition(){
        return addition;
    }
    private ArmAddition(){}

    private static ArmAddition addition=new ArmAddition();

    public Shift getNewShiftInstance(){
        return new Shift();
    }

    public Shift getNewShiftInstance(ShiftType t, int imm){
        return new Shift(t,imm);
    }

    public enum ShiftType{
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

    public enum CondType{
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

        @Override
        public String toString(){
            if(t==ShiftType.None||imm==0){
                return "";
            }
            String op=", ";
            if(t==ShiftType.Asr){
                op+="asr";
            }else if(t==ShiftType.Lsl){
                op+="lsl";
            }else if(t==ShiftType.Lsr){
                op+="lsr";
            }else{
                op+="";
                assert(false);
            }
            op+=" #"+((Integer)imm).toString();
            return op;
        }

        public Shift(){ this.t=ShiftType.None;}

        public Shift(ShiftType t, int imm){
            this.t=t;
            this.imm=imm;
        }
    }
}


