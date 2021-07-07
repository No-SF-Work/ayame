package backend.machinecodes;

import backend.reg.*;
import ir.types.Type;
import backend.machinecodes.ArmAddition.Shift;

import java.util.ArrayList;

public class MachineCode {

    private static int ID = 0;

    int slotIndex;
    
    int id;

    public enum TAG {
        Add,
        Sub,
        Rsb,
        Mul,
        Div,
        Mod,
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

    private TAG tag;

    private MachineBlock mb;

    private MachineFunction mf;

    //例如add v a, b 那距离本指令最近的a和b的def就是本指令的dominator
    private ArrayList<MachineCode> dominators = new ArrayList<>();

    //返回本MC定义的virtualreg
    public ArrayList<VirtualReg> getDef(){
        ArrayList<VirtualReg> def=new ArrayList<>();
        def.add(virtualReg);
        return def;
    }

    public TAG getTag(){return tag;}

    private ArmAddition.Shift shift = ArmAddition.getAddition().getShiftInstance();

    public Shift getShift(){return shift;}

    public void setShift(Shift s){this.shift=s;}

    public ArmAddition.CondType getCond(){return ArmAddition.CondType.Any;}

    //返回本MC使用的virtualreg
    public ArrayList<VirtualReg> getUse(){
        return new ArrayList<>();
    }


    public MachineCode(TAG tag) {
        this.tag = tag;
    }

    public MachineCode(TAG tag, MachineBlock mb) {
        this.tag = tag;
        this.mb = mb;
    }

    public MachineCode(TAG tag, MachineFunction mf) {
        this.tag = tag;
        this.mf = mf;
    }

    public void setMb(MachineBlock mb) {
        this.mb = mb;
    }

    public void setMf(MachineFunction mf) {
        this.mf = mf;
    }

}



