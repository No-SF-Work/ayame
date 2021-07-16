package backend.machinecodes;

public class MCCall extends MachineCode{

    public MachineFunction getFunc() {
        return func;
    }

    public void setFunc(MachineFunction func) {
        this.func = func;
    }

    private MachineFunction func;

    @Override
    public String toString(){
        String res="\tbl\t"+func.getName()+"\n";
        return res;
    }

    public MCCall(MachineBlock mb){
        super(TAG.Call,mb);
    }
}
