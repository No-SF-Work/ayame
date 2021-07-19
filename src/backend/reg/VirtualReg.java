package backend.reg;

import backend.machinecodes.MachineCode;

import java.util.HashMap;
import java.util.Objects;

public class VirtualReg extends Reg {

    //是否是由SSA指令定义的
    private boolean isSSA=false;

    private static int Name = 0;

    public boolean isGlobal() {
        return isGlobal;
    }

    private boolean isGlobal=false;

    private String name;

    public void setSSA(){
        this.isSSA=true;
    }


    boolean isSSA(){
        return isSSA;
    }

    @Override
    public String getName() {
        return name;
    }

    //该virtualreg在某条指令中被使用的时候被分配的物理寄存器，如果没被分配则usemap中不存在该key
    private HashMap<MachineCode,state> useMap=new HashMap<>();

    public VirtualReg(String name){
        super(state.virtual);
        if(name==""){
            this.name="%%"+Integer.toString(Name++);
        }else{
            this.name=name;
        }
    }

    public VirtualReg(){
        super(state.virtual);
        this.name="%%"+Integer.toString(Name++);
    }

    public VirtualReg(String name, boolean isGlobal){
        super(state.virtual);
        this.name=name;
//        this.idx = Integer.parseInt(name.substring(2));
        this.isGlobal=isGlobal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirtualReg that = (VirtualReg) o;
        return isSSA == that.isSSA && isGlobal == that.isGlobal && Objects.equals(name, that.name) && Objects.equals(useMap, that.useMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isSSA, isGlobal, name, useMap);
    }
}
