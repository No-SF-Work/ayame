package backend;
import backend.LiveInterval;
import backend.machinecodes.*;
import backend.reg.MachineOperand;
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
    private ArrayList<VirtualReg> globalVirtualRegs = new ArrayList<>();

    //map value in ir to vr in mc
    private HashMap<Value,VirtualReg> vMap=new HashMap<>();

    //ir moudle
    private static MyModule myModule;

    private CodeGenManager(MyModule myModule) {
        this.myModule=myModule;
    }

    private static CodeGenManager codeGenManager;

    //ir->machinecode
    public CodeGenManager getInstance(MyModule myModule){
        if(this.codeGenManager==null){
            this.codeGenManager=new CodeGenManager(myModule);
        }
        return this.codeGenManager;
    }

    public ArrayList<MachineFunction> getMachineFunctions(){return machineFunctions;}

    private void MachineCodeGeneration(){
        ArrayList<GlobalVariable> gVs= myModule.__globalVariables;
        Iterator<GlobalVariable> itgVs=gVs.iterator();
        while(itgVs.hasNext()){
            GlobalVariable gV = itgVs.next();
            VirtualReg gVr=new VirtualReg(gV.getName(),true);
            vMap.put(gV,gVr);
            globalVirtualRegs.add(gVr);
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


        }
    }

    private VirtualReg analyzeValue(Value v,MachineFunction mf, Function f,HashMap<BasicBlock, MachineBlock> bMap){
        if(v instanceof Function.Arg){
            VirtualReg vr;
            if (mf.getVRegMap().get(v.getName()) == null) {
                vr=new VirtualReg(v.getName());
                mf.addVirtualReg(vr);
                for(int i=0;i<f.getNumArgs();i++) {
                    if (i < 4) {
                        MachineCode mc=new MCMove(bMap.get(f.getList_().getEntry()),0);
                        ((MCMove)mc).setDst(vr);
                        ((MCMove)mc).setRhs(mf.getPhyReg(i));
                    }else{
                        MachineCode mcLD=new MCLoad(bMap.get(f.getList_().getEntry()),0);
                        ((MCLoad)mcLD).setAddr(mf.getPhyReg("sp"));
                        ((MCLoad)mcLD).setOffset(new MachineOperand((i-4)*4));
                        ((MCLoad)mcLD).setDst(vr);
                    }
                    break;
                }
                return vr;
            }else {
                return mf.getVRegMap().get(v.getName());
            }
        } else if(myModule.__globalVariables.contains(v)){
            assert(vMap.containsKey(v));
            return vMap.get(v);
        }else{
            if (mf.getVRegMap().get(v.getName()) == null){
                VirtualReg vr = new VirtualReg(v.getName());
                return vr;
            }else{
                return mf.getVRegMap().get(v.getName());
            }
        }
    }

}
