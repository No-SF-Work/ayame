package backend.machinecodes;

import backend.reg.*;
import ir.types.Type;
import backend.reg.MachineOperand;
import backend.machinecodes.ArmAddition.Shift;
import util.IList;

import java.util.ArrayList;
import java.util.HashMap;

public class MachineCode implements Cloneable {

    private static int ID = 0;

    int slotIndex;

    int id;

    public Object clone(){
        MachineCode mc=null;
        try{
            mc=(MachineCode)super.clone();
        }catch (Exception e){

        }
        return mc;
    }

    public enum TAG {
        Add,
        Sub,
        Rsb,
        Mul,
        Div,
        And,
        Or,
        Bic,
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
    private VirtualReg virtualReg;

    //allocated phyreg
    private PhyReg phyReg = null;

    private TAG tag;

    public MachineBlock mb;

    public MachineFunction mf;


    private ArrayList<Reg> regDef = new ArrayList<>();

    private ArrayList<Reg> regUse = new ArrayList<>();

    public TAG getTag() {
        return tag;
    }

    //所有的MC都有一个shift对象，初始值为无偏移。可以通过setShift设置Shift
    private ArmAddition.Shift shift = ArmAddition.getAddition().getNewShiftInstance();

    //处理旧reg，添加新reg 布尔值用来代表是使用还是定义
    public void dealReg(MachineOperand oldmo, MachineOperand newmo, boolean isUse) {
        if (oldmo == null) {
            if (isUse) {
                addUse(newmo);
            } else {
                addDef(newmo);
            }
            return;
        }
        if (oldmo.getState() == MachineOperand.state.imm) {
        } else if (oldmo instanceof Reg) {
            if (isUse) {
                regUse.remove(oldmo);
            } else {
                regDef.remove(oldmo);
            }
        }
        if (isUse) {
            addUse(newmo);
        } else {
            addDef(newmo);
        }
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(ArmAddition.ShiftType t, int i) {
        this.shift.setType(t, i);
    }

    public ArmAddition.CondType getCond() {
        return ArmAddition.CondType.Any;
    }

    public void setCond(ArmAddition.CondType cond) {

    }

    public void addUse(MachineOperand r) {
        if (r instanceof VirtualReg || r instanceof PhyReg) {
            regUse.add((Reg) r);
        }
    }

    public void addDef(MachineOperand r) {
        if (r instanceof VirtualReg || r instanceof PhyReg) {
            regDef.add((Reg) r);
        }
    }

    public ArrayList<Reg> getUse() {
        return regUse;
    }

    public ArrayList<Reg> getDef() {
        return regDef;
    }

    public ArrayList<Reg> getMCDef() {
        var defs = this.getDef();
        var cond = new PhyReg(16);
        if (this instanceof MCCompare || this instanceof MCCall) {
            defs.add(cond);
        }
        return defs;
    }

    public ArrayList<Reg> getMCUse() {
        var uses = this.getUse();
        var cond = new PhyReg(16);
        if (this.getCond() != ArmAddition.CondType.Any) {
            uses.add(cond);
        }
        if (this instanceof MCCall) {
            uses.add(new PhyReg(13));
        }
        return uses;
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

//    public void removeReg(Reg r){
//        if(regUse.contains(r)){
//            regUse.remove(r);
//        }
//        if(regDef.contains(r)){
//            regDef.remove(r);
//        }
//    }


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


    public boolean isAllocated() {
        return phyReg != null;
    }

    private IList.INode node;

    public MachineCode(TAG tag) {
        this.tag = tag;
        node = new IList.INode(this);
    }

    public MachineCode(TAG tag, MachineBlock mb) {
        this.tag = tag;
        this.mb = mb;
        node = new IList.INode(this);
        node.setParent(mb.getmclist());
        mb.addAtEndMC(node);
    }

    public void setNode(IList.INode node) {
        this.node = node;
    }

    public void genNewNode(){
        this.node=new IList.INode(this);
    }

    public String condString(ArmAddition.CondType t) {
        if (t == ArmAddition.CondType.Gt) {
            return "gt";
        } else if (t == ArmAddition.CondType.Ge) {
            return "ge";
        } else if (t == ArmAddition.CondType.Eq) {
            return "eq";
        } else if (t == ArmAddition.CondType.Ne) {
            return "ne";
        } else if (t == ArmAddition.CondType.Le) {
            return "le";
        } else if (t == ArmAddition.CondType.Lt) {
            return "lt";
        } else {
            return "";
        }
    }

    public void insertBeforeNode(MachineCode mc) {
        node.setParent(mc.node.getParent());
        this.node.insertBefore(mc.node);
        this.mb = mc.getMb();
    }

    public void insertAfterNode(MachineCode mc) {
        node.setParent(mc.node.getParent());
        this.node.insertAfter(mc.node);
        this.mb = mc.getMb();
    }

    public MachineCode(TAG tag, MachineBlock mb, int num) {
        this.tag = tag;
        this.mb = mb;
        node = new IList.INode(this);
        node.setParent(mb.getmclist());
        mb.addAtEntryMC(node);
    }


//    public MachineCode(TAG tag, MachineFunction mf) {
//        this.tag = tag;
//        this.mf = mf;
//        node=new IList.INode(this);
//        node.setParent(mb.getmclist());
//    }

    public IList.INode getNode() {
        return node;
    }

    public void setMb(MachineBlock mb) {
        this.mb = mb;
        node.setParent(mb.getmclist());
        mb.addAtEndMC(node);
    }

    public MachineBlock getMb() {
        return this.mb;
    }

    public void setMf(MachineFunction mf) {
        this.mf = mf;
    }

    public int getSlotIndex() {
        return slotIndex;
    }
}



