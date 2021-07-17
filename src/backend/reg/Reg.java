package backend.reg;

import backend.machinecodes.MachineCode;

import java.util.ArrayList;

public class Reg extends MachineOperand{
    ArrayList<MachineCode> MClist=new ArrayList<>();

    public String getName(){return "";}

    Reg(state s){super(s);}
}
