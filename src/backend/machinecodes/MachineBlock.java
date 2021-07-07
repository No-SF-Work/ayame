package backend.machinecodes;

import backend.reg.VirtualReg;
import util.IList;
import util.IList.INode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class MachineBlock {

    //basic struct
    private INode<MachineBlock,MachineFunction> node;

    private IList<MachineCode,MachineBlock> mclist;

    //phi machinecode
    private HashMap<VirtualReg, MachineCode> phiMap;

    //phi sets
    private ArrayList<HashSet<VirtualReg>> phis;

    public IList<MachineCode, MachineBlock> getmclist() {
        return mclist;
    }

    //successor and predecessor
    ArrayList<MachineBlock> succ;

    ArrayList<MachineBlock> pred;

    MachineCode entry;// MC list entry

    public MachineBlock(MachineFunction mf){
        mf.insertBlock(this);
    }

    public void addSucc(MachineBlock mb){
        succ.add(mb);
    }

    public void addPred(MachineBlock mb){
        pred.add(mb);
    }


}
