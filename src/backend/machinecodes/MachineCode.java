package backend.machinecodes;

import backend.reg.*;
import ir.types.Type;
import backend.reg.MachineOperand;
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

    //使用的virtualreg
    private ArrayList<VirtualReg> virtualUses = new ArrayList<>();

    //定义的virtualreg
    private ArrayList<VirtualReg> virtualDef = new ArrayList<>();

    //使用的phyreg
    private ArrayList<PhyReg> phyUses = new ArrayList<>();

    //定义的phyreg
    private ArrayList<PhyReg> phyDef = new ArrayList<>();

    public TAG getTag(){return tag;}

    private ArmAddition.Shift shift = ArmAddition.getAddition().getShiftInstance();

    public Shift getShift(){return shift;}

    public void setShift(Shift s){this.shift=s;}

    public ArmAddition.CondType getCond(){return ArmAddition.CondType.Any;}

    //返回本MC定义的virtualreg
    public ArrayList<VirtualReg> getVirtualDef(){
        return virtualDef;
    }

    //返回本MC使用的virtualreg
    public ArrayList<VirtualReg> getVirtualUses(){
        return virtualUses;
    }

    public ArrayList<PhyReg> getPhyUses(){
        return phyUses;
    }

    public ArrayList<PhyReg> getPhyDef(){
        return phyDef;
    }

    public void addVirtualUse(MachineOperand vr){
        if(vr.getState()== MachineOperand.state.virtual)
            virtualUses.add((VirtualReg) vr);
    }

    public void addVirtualDef(MachineOperand vr){
        if(vr.getState()== MachineOperand.state.virtual)
            virtualDef.add((VirtualReg)vr);
    }

    public void addPhyUse(MachineOperand pr){
        if(pr.getState()== MachineOperand.state.phy)
            phyUses.add((PhyReg) pr);
    }

    public void addPhyDef(MachineOperand pr){
        if(pr.getState()== MachineOperand.state.phy)
            phyDef.add((PhyReg) pr);
    }

    public boolean isAllocated(){
        return phyReg != null;
    }


    public MachineCode(TAG tag) {
        this.tag = tag;
    }

    public MachineCode(TAG tag, MachineBlock mb) {
        this.tag = tag;
        this.mb = mb;
        mb.addMC(this);
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



