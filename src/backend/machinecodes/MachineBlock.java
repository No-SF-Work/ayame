package backend.machinecodes;

import backend.reg.VirtualReg;
import util.IList;
import util.IList.INode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class MachineBlock {

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
    ArrayList<MachineBlock> pred;

    MachineCode entry;// MC list entry

    public void addAtEndMC(MachineCode mc){
        INode<MachineCode,MachineBlock> mcNode=new INode<>(mc);
        mcNode.insertAtEnd(mclist);
    }

    public void addAtEntryMC(MachineCode mc){
        INode<MachineCode,MachineBlock> mcNode=new INode<>(mc);
        mcNode.insertAtEntry(mclist);
    }

    public MachineBlock(MachineFunction mf) {
        mf.insertBlock(this);
    }

    public MachineCode getControlTransferInst() {
        return controlTransferInst;
    }

    public void setControlTransferInst(MachineCode controlTransferInst) {
        this.controlTransferInst = controlTransferInst;
    }

    private MachineCode controlTransferInst;


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
