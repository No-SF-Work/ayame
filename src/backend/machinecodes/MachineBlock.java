package backend.machinecodes;

import backend.reg.VirtualReg;
import util.IList;
import util.IList.INode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class MachineBlock {

    public INode<MachineBlock, MachineFunction> getNode() {
        return node;
    }

    private static int index=0;

    private String name;

    //basic struct
    private INode<MachineBlock, MachineFunction> node;

    private IList<MachineCode, MachineBlock> mclist =new IList<>(this);

    //phi machinecode
    private HashMap<VirtualReg, MachineCode> phiMap;

    //phi sets
    private ArrayList<HashSet<VirtualReg>> phis;

    public IList<MachineCode, MachineBlock> getmclist() {
        return mclist;
    }

    //如果最后一条指令是有条件跳转指令，那falseSucc就是直接后继块。false指条件跳转中不满足条件下继续执行的基本块
    MachineBlock falseSucc=null;

    //一个基本块最多两个后继块，如果基本块只有一个后继，那么falseSucc是null，trueSucc不是null
    MachineBlock trueSucc=null;

    //predecessor block
    ArrayList<MachineBlock> pred=new ArrayList<>();

    MachineCode entry;// MC list entry

    public void addAtEndMC(INode<MachineCode,MachineBlock> node){
        node.insertAtEnd(mclist);
    }

    public void addAtEntryMC(INode<MachineCode,MachineBlock> node){
        node.insertAtEntry(mclist);
    }

    private MachineFunction mf;

    public MachineFunction getMF(){return mf;}

    public MachineBlock(MachineFunction mf) {
//        mf.insertBlock(this);
        this.mf=mf;
        node=new INode<>(this);
        node.setParent(mf.getmbList());
        this.name=".__BB__"+((Integer)index).toString();
        index++;
    }

    public String getName(){return name;}

    public MachineCode getControlTransferInst() {
        return controlTransferInst;
    }

    public void setControlTransferInst(MachineCode controlTransferInst) {
        this.controlTransferInst = controlTransferInst;
    }

    private MachineCode controlTransferInst;

    public void removePred(MachineBlock mb){
        this.pred.remove(mb);
    }


    public void setFalseSucc(MachineBlock mb) {
        falseSucc = mb;
    }

    public void setTrueSucc(MachineBlock mb) {
        trueSucc = mb;
    }

    public void addPred(MachineBlock mb) {
        pred.add(mb);
    }

    public MachineBlock getTrueSucc() {
        return trueSucc;
    }

    public ArrayList<MachineBlock> getPred(){return pred;}

    public MachineBlock getFalseSucc() {
        return falseSucc;
    }
}
