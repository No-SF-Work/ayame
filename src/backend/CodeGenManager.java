package backend;
import backend.LiveInterval;
import backend.machinecodes.MCMove;
import backend.machinecodes.MachineBlock;
import backend.machinecodes.MachineCode;
import backend.machinecodes.MachineFunction;
import backend.reg.VirtualReg;
import ir.MyModule;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.GlobalVariable;
import ir.values.Value;
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
    private ArrayList<MachineFunction> machineFunctions;

    //global virtualregs
    private ArrayList<VirtualReg> globalVirtualRegs;

    //ir moudle
    private static MyModule myModule;

    private static final CodeGenManager codeGenManager = new CodeGenManager(myModule);



    //ir->machinecode
    public static CodeGenManager getInstance(MyModule myModule){
        return codeGenManager;
    }

    public ArrayList<MachineFunction> getMachineFunctions(){return machineFunctions;}

    private void MachineCodeGeneration(MyModule myModule){
        ArrayList<GlobalVariable> gVs= myModule.__globalVariables;
        Iterator<GlobalVariable> itgVs=gVs.iterator();
        while(itgVs.hasNext()){
            GlobalVariable gV = itgVs.next();
            globalVirtualRegs.add(new VirtualReg(gV.getName(),true));
        }
        IList<Function,MyModule> fList=myModule.__functions;
        Iterator<INode<Function,MyModule>> fIt=fList.iterator();
        while (fIt.hasNext()){
            INode<Function, MyModule> fNode=fIt.next();
            MachineFunction mf=new MachineFunction(this);
            machineFunctions.add(mf);
            HashMap<BasicBlock, MachineBlock> bMap=new HashMap<>();
            IList<BasicBlock,Function> bList=fNode.getVal().getList_();
            Iterator<INode<BasicBlock,Function>> bIt=bList.iterator();
            while(bIt.hasNext()){
                INode<BasicBlock,Function> bNode= bIt.next();
                bMap.put(bNode.getVal(),new MachineBlock(mf));
            }
            bIt=bList.iterator();
            while(bIt.hasNext()){
                BasicBlock b=bIt.next().getVal();
                MachineBlock mb=bMap.get(b);
                Iterator<BasicBlock> bbIt=b.getPredecessor_().iterator();
                while(bbIt.hasNext()){
                    mb.addPred(bMap.get(bbIt.next()));
                }
                //TODO
                //翻译br指令的时候再指定后继基本块。有些情况下某个后继基本块必须要放在本基本块的下一个，跳转指令
//                bbIt=b.getSuccessor_().iterator();
//                while(bbIt.hasNext()){
//                    mb.addSucc(bMap.get(bbIt.next()));
//                }
            }
            for(int i=0;i<fNode.getVal().getNumArgs();i++) {
                Value v = fNode.getVal().getArgList().get(i);
                VirtualReg vr;
                if (mf.getRegMap().get(v.getName()) == null) {
                    vr=new VirtualReg(v.getName());
                    mf.addVirtualReg(vr);
                }else {
                    vr=mf.getRegMap().get(v.getName());
                }
                if (i < 4) {
                    MachineCode mc=new MCMove(bMap.get(fNode.getVal()));
                    ((MCMove)mc).setDst(vr);
                    ((MCMove)mc).setRhs(mf.getPhyReg(i));
                }
            }
        }
    }

    private CodeGenManager(MyModule myModule){
        MachineCodeGeneration(myModule);
    }
}
