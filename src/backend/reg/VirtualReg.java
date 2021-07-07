package backend.reg;

import backend.LiveInterval;

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

    public VirtualReg(String name){
        super();
        this.name=name;
    }

    public VirtualReg(String name, boolean isGlobal){
        super();
        this.name=name;
        this.isGlobal=isGlobal;
    }


}
