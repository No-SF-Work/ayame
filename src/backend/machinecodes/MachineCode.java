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

    private ArrayList<Reg> regDef = new ArrayList<>();

    private ArrayList<Reg> regUse = new ArrayList<>();

    //使用的phyreg
    private ArrayList<PhyReg> phyUses = new ArrayList<>();

    //定义的phyreg
    private ArrayList<PhyReg> phyDef = new ArrayList<>();

    public TAG getTag(){return tag;}

    //所有的MC都有一个shift对象，初始值为无偏移。可以通过setShift设置Shift
    private ArmAddition.Shift shift = ArmAddition.getAddition().getNewShiftInstance();

    public Shift getShift(){return shift;}

    public void setShift(ArmAddition.ShiftType t,int i) { this.shift.setType(t,i); }

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

    public void addUse(MachineOperand r){
        if(r.getState()== MachineOperand.state.virtual)
            virtualUses.add((VirtualReg) r);
        if(r.getState()== MachineOperand.state.phy)
            phyUses.add((PhyReg) r);
        if(r instanceof VirtualReg||r instanceof PhyReg){
            regUse.add((Reg) r);
        }
    }

    public void addDef(MachineOperand r){
        if(r.getState()== MachineOperand.state.virtual)
            virtualDef.add((VirtualReg)r);
        if(r.getState()== MachineOperand.state.phy)
            phyDef.add((PhyReg) r);
        if(r instanceof VirtualReg||r instanceof PhyReg){
            regDef.add((Reg) r);
        }
    }

    public ArrayList<Reg> getUse(){
        return regUse;
    }

    public ArrayList<Reg> getDef(){
        return regDef;
    }


//    public void removeReg(VirtualReg vr){
//        if(virtualUses.contains(vr)){
//            virtualUses.remove(vr);
//        }
//        if(virtualDef.contains(vr)){
//            virtualDef.remove(vr);
//        }
//        if(regUse.contains(vr)){
//            regUse.remove(vr);
//        }
//        if(regDef.contains(vr)){
//            regDef.remove(vr);
//        }
//    }

    public void removeReg(Reg r){
        if(regUse.contains(r)){
            regUse.remove(r);
        }
        if(regDef.contains(r)){
            regDef.remove(r);
        }
    }


//    public void removeReg(PhyReg pr){
//        if(phyDef.contains(pr)){
//            phyDef.remove(pr);
//        }
//        if(phyUses.contains(pr)){
//            phyUses.remove(pr);
//        }
//        if(regUse.contains(pr)){
//            regUse.remove(pr);
//        }
//        if(regDef.contains(pr)){
//            regDef.remove(pr);
//        }
//    }


    public boolean isAllocated(){
        return phyReg != null;
    }


    public MachineCode(TAG tag) {
        this.tag = tag;
    }

    public MachineCode(TAG tag, MachineBlock mb) {
        this.tag = tag;
        this.mb = mb;
        mb.addAtEndMC(this);
    }

    public MachineCode(TAG tag, MachineBlock mb,int num) {
        this.tag = tag;
        this.mb = mb;
        mb.addAtEntryMC(this);
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

    public int getSlotIndex() {
        return slotIndex;
    }
}



