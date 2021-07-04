package backend.machinecodes;

import backend.reg.*;
import ir.types.Type;

import java.util.ArrayList;

public class MachineCode {

    private static int ID = 0;

    int slotIndex;
    
    int id;

    public enum TAG_ {
        Add,
        Sub,
        Rsb,
        Mul,
        Div,
        Mod,
        Lt,
        Le,
        Ge,
        Gt,
        Eq,
        Ne,
        And,
        Or,
        //binary
        LongMul,
        FMA,
        Mv,
        Branch,
        Jump,
        Return,
        Load,
        Store,
        Compare,
        Call,
        Global,
        Comment,
        Phi
    }

    //target virtualreg
    private VirtualReg virtualReg ;

    //allocated phyreg
    private PhyReg phyReg = null;

    private TAG_ tag;

    private int numOps;

    private MachineBlock mb;

    private MachineFunction mf;

    //例如add v a, b 那距离本指令最近的a和b的def就是本指令的dominator
    private ArrayList<MachineCode> dominators = new ArrayList<>();

    //返回
    public ArrayList<VirtualReg> getDef(){
        ArrayList<VirtualReg> def=new ArrayList<>();
        def.add(virtualReg);
        return def;
    }

    public ArrayList<VirtualReg> getUse(){
        return new ArrayList<>();
    }


    public MachineCode(TAG_ tag, int numOps) {
        this.tag = tag;
        this.numOps = numOps;
    }

    public MachineCode(TAG_ tag, int numOps, MachineBlock mb) {
        this.tag = tag;
        this.numOps = numOps;
        this.mb = mb;
    }

    public MachineCode(TAG_ tag, int numOps, MachineFunction mf) {
        this.tag = tag;
        this.numOps = numOps;
        this.mf = mf;
    }

    public void setMb(MachineBlock mb) {
        this.mb = mb;
    }

    public void setMf(MachineFunction mf) {
        this.mf = mf;
    }

}



