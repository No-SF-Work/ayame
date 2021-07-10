package backend.machinecodes;

public class MCCall extends MachineCode{

    private MachineFunction func;


    MCCall(MachineBlock mb){
        super(TAG.Call,mb);
    }
}
