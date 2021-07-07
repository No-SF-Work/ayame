package backend.machinecodes;

import backend.CodeGenManager;
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

    //get prev and next
    private INode<MachineFunction, CodeGenManager> node;

    //basic block list
    private IList<MachineBlock,MachineFunction> mbList;

    public IList<MachineBlock,MachineFunction> getmbList(){return mbList;}

    //all the virtual regs in this function
    private HashMap<String, VirtualReg> regMap=new HashMap<>();

    //PhyReg nums
    private int regNums=0;

    //size of stack allocated for virtual register
    private int stackSize;

    private CodeGenManager cgm;

    public MachineFunction(CodeGenManager cgm){
        this.cgm=cgm;
    }

}
