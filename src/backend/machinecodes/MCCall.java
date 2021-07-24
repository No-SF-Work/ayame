package backend.machinecodes;

import backend.CodeGenManager;

public class MCCall extends MachineCode{

    public MachineFunction getFunc() {
        return func;
    }

    public void setFunc(MachineFunction func) {
        this.func = func;
    }

    private MachineFunction func;

    private static  int chi=0;

    @Override
    public String toString(){
        if(func.getName()=="getch"){
            chi++;
            String res="deal_special_ch"+chi+":\n";
            res+="\tbl getch\n";
            res+="\tcmp\tr0,\t#13\n";
            res+="\tbeq\tdeal_special_ch"+chi+"\n";
            CodeGenManager.getInstance().addOffset(3,res.length());
            return res;
        }
        String res="\tbl\t"+func.getName()+"\n";
        CodeGenManager.getInstance().addOffset(1,res.length());
        return res;
    }

    public MCCall(MachineBlock mb){
        super(TAG.Call,mb);
    }

    public MCCall(){
        super(TAG.Call);
    }
}
