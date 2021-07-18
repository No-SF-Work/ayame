package backend.machinecodes;

import backend.reg.MachineOperand;
import backend.reg.VirtualReg;


public class MCComment extends MachineCode{

    private MachineOperand dst;

    private String str;

    public MCComment(String str, MachineBlock mb){
        super(TAG.Comment,mb);
        this.str=str;
    }

    @Override
    public String toString(){
        String res="@"+str+"\n";
        return res;
    }
}
