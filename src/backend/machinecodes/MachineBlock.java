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

    private IList<MachineCode,MachineBlock> list;

    //phi machinecode
    private HashMap<VirtualReg, MachineCode> phiMap;

    //phi sets
    private ArrayList<HashSet<VirtualReg>> phis;

    //successor and predecessor
    ArrayList<MachineBlock> succ;

    ArrayList<MachineBlock> pred;

    MachineCode entry;// MC list entry


}
