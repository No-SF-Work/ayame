package backend.reg;

import backend.LiveInterval;

public class VirtualReg extends Reg {

    //是否是由SSA指令定义的
    private boolean isSSA=false;

    boolean isSSA(){
        return isSSA;
    }


}
