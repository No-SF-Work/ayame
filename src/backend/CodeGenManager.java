package backend;
import backend.LiveInterval;
import backend.machinecodes.MachineBlock;
import backend.machinecodes.MachineCode;
import backend.machinecodes.MachineFunction;
import backend.reg.VirtualReg;
import ir.MyModule;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.GlobalVariable;
import util.IList;
import util.IList.INode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * 后端的顶层模块，管理整个后端的流程，
 */
public class CodeGenManager {

    // all functions
    private ArrayList<MachineFunction> machineFunctionsfs;

    //global virtualregs
    private ArrayList<VirtualReg> globalVirtualRegs;

    //ir moudle
    private static MyModule myModule;

    private static final CodeGenManager codeGenManager = new CodeGenManager(myModule);

    //ir->machinecode
    public static CodeGenManager getInstance(MyModule myModule){
        return codeGenManager;
    }

    private void MachineCodeGeneration(MyModule myModule){
        ArrayList<GlobalVariable> gVs= myModule.__globalVariables;
        Iterator<GlobalVariable> itgVs=gVs.iterator();
        while(itgVs.hasNext()){
            GlobalVariable gV = itgVs.next();
            globalVirtualRegs.add(new VirtualReg(gV.getName()));
        }
        IList<Function,MyModule> fList=myModule.__functions;
        Iterator<INode<Function,MyModule>> fIt=fList.iterator();
        while (fIt.hasNext()){
            INode<Function, MyModule> fNode=fIt.next();
            MachineFunction mf=new MachineFunction();
            HashMap<BasicBlock, MachineBlock> bMap=new HashMap<>();
            IList<BasicBlock,Function> bList=fNode.getVal().getList_();
            Iterator<INode<BasicBlock,Function>> bIt=bList.iterator();
            while(bIt.hasNext()){
                INode<BasicBlock,Function> bNode= bIt.next();
                bMap.put(bNode.getVal(),new MachineBlock(mf));
            }

        }
    }

    private CodeGenManager(MyModule myModule){
        MachineCodeGeneration(myModule);
    }
}
