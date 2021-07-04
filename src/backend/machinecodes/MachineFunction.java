package backend.machinecodes;

import backend.CodeGenManager;
import util.IList;
import util.IList.INode;

public class MachineFunction {

    //get prev and next
    private INode<MachineFunction, CodeGenManager> node;

    private IList<MachineBlock,MachineFunction> list;

    //PhyReg nums
    private int regNums=0;

    //size of stack allocated for virtual register
    private int stackSize;

}
