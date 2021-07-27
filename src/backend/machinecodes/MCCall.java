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
