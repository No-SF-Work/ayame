package backend.machinecodes;

import backend.CodeGenManager;
import backend.reg.PhyReg;
import backend.reg.VirtualReg;
import util.IList;
import util.IList.INode;

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

    private HashMap<String, PhyReg> phyRegs;

    //all the virtual regs in this function
    private HashMap<String, VirtualReg> regMap=new HashMap<>();

    public void addVirtualReg(VirtualReg vr){
        regMap.put(vr.getName(),vr);
    }

    public HashMap<String,VirtualReg> getRegMap(){return regMap;}

    //PhyReg nums
    private int regNums=0;

    //size of stack allocated for virtual register
    private int stackSize;

    private CodeGenManager cgm;

    public MachineFunction(CodeGenManager cgm){
        this.cgm=cgm;
        phyRegs.put("r0",new PhyReg("r0"));
        phyRegs.put("r1",new PhyReg("r1"));
        phyRegs.put("r2",new PhyReg("r2"));
        phyRegs.put("r3",new PhyReg("r3"));
        phyRegs.put("r4",new PhyReg("r4"));
        phyRegs.put("r5",new PhyReg("r5"));
        phyRegs.put("r6",new PhyReg("r6"));
        phyRegs.put("r7",new PhyReg("r7"));
        phyRegs.put("r8",new PhyReg("r8"));
        phyRegs.put("r9",new PhyReg("r9"));
        phyRegs.put("r11",new PhyReg("r11"));
        phyRegs.put("r12",new PhyReg("r12"));
        phyRegs.put("r13",new PhyReg("r13"));
        phyRegs.put("r14",new PhyReg("r14"));
        phyRegs.put("r15",new PhyReg("r15"));
        phyRegs.put("fp",phyRegs.get("r11"));
        phyRegs.put("ip",phyRegs.get("r12"));
        phyRegs.put("sp",phyRegs.get("r13"));
        phyRegs.put("lr",phyRegs.get("r14"));
        phyRegs.put("pc",phyRegs.get("r15"));
    }

}
