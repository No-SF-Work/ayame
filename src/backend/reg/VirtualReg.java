package backend.reg;

import backend.LiveInterval;
import backend.machinecodes.MachineCode;
import backend.reg.MachineOperand;

import java.util.HashMap;

public class VirtualReg extends Reg {

    //是否是由SSA指令定义的
    private boolean isSSA=false;

    private boolean isGlobal=false;

    private String name;

    public void setSSA(){
        this.isSSA=true;
    }


    boolean isSSA(){
        return isSSA;
    }

    public String getName() {
        return name;
    }

    //该virtualreg在某条指令中被使用的时候被分配的物理寄存器，如果没被分配则usemap中不存在该key
    private HashMap<MachineCode,state> useMap=new HashMap<>();

    public VirtualReg(String name){
        super(state.virtual);
        this.name=name;
    }

    public VirtualReg(String name, boolean isGlobal){
        super(state.virtual);
        this.name=name;
        this.isGlobal=isGlobal;
    }


}
