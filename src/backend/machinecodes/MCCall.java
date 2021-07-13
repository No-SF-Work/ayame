package backend.machinecodes;

public class MCCall extends MachineCode{

    public MachineFunction getFunc() {
        return func;
    }

    public void setFunc(MachineFunction func) {
        this.func = func;
    }

    private MachineFunction func;


    public MCCall(MachineBlock mb){
        super(TAG.Call,mb);
    }
}
