package backend.machinecodes;

import backend.CodeGenManager;
import backend.reg.PhyReg;
import backend.reg.VirtualReg;
import util.IList;
import util.IList.INode;

import java.util.ArrayList;
import java.util.HashMap;

public class MachineFunction {

    public void insertBlock(MachineBlock mb){
        INode<MachineBlock, MachineFunction> mbNode = new INode<>(mb);
        if(mbList.getNumNode()==0) {
            mbNode.insertAtEntry(mbList);
        } else {
            mbNode.insertAfter(mbList.getLast());
        }
    }

    //使基本块线性化，存储在mbList中
    public void serializeBlocks(){}

    //get prev and next
    private INode<MachineFunction, CodeGenManager> node;

    //basic block list
    private IList<MachineBlock,MachineFunction> mbList = new IList<>(this);

    public IList<MachineBlock,MachineFunction> getmbList(){return mbList;}

    private ArrayList<PhyReg> phyRegs=new ArrayList<>();

    //all the virtual regs in this function
    private HashMap<String, VirtualReg> regMap=new HashMap<>();

    public void addVirtualReg(VirtualReg vr){
        regMap.put(vr.getName(),vr);
        stackSize+=4;
    }

    public HashMap<String,VirtualReg> getVRegMap(){return regMap;}

    //PhyReg nums
    private int regNums=0;

    //size of stack allocated for virtual register
    private int stackSize = 0;

    private CodeGenManager cgm;

    public PhyReg getPhyReg(String name){
        return phyRegs.get(regNameMap.get(name));
    }

    public PhyReg getPhyReg(int n){
        return phyRegs.get(n);
    }

    private static HashMap<String,Integer>regNameMap=new HashMap<>();
    static {
        regNameMap.put("r0",0);
        regNameMap.put("r1",1);
        regNameMap.put("r2",2);
        regNameMap.put("r3",3);
        regNameMap.put("r4",4);
        regNameMap.put("r5",5);
        regNameMap.put("r6",6);
        regNameMap.put("r7",7);
        regNameMap.put("r8",8);
        regNameMap.put("r9",9);
        regNameMap.put("r10",10);
        regNameMap.put("r11",11);
        regNameMap.put("r12",12);
        regNameMap.put("r13",13);
        regNameMap.put("r14",14);
        regNameMap.put("r15",15);
        regNameMap.put("fp",11);
        regNameMap.put("ip",12);
        regNameMap.put("sp",13);
        regNameMap.put("lr",14);
        regNameMap.put("pc",15);
    }

    public MachineFunction(CodeGenManager cgm){
        this.cgm=cgm;
        for(int i=0;i<=15;i++){
            phyRegs.add(new PhyReg(i));
        }
    }

}
