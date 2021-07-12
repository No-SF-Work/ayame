package backend;

import backend.LiveInterval;
import backend.machinecodes.*;
import backend.reg.MachineOperand;
import backend.reg.VirtualReg;
import ir.MyModule;
import ir.values.*;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.TerminatorInst;
import util.IList;
import util.IList.INode;
import ir.values.instructions.MemInst.Phi;
import util.Pair;
import backend.machinecodes.ArmAddition.CondType;
import backend.machinecodes.ArmAddition.Shift;

import javax.crypto.Mac;
import java.util.*;

/**
 * 后端的顶层模块，管理整个后端的流程，
 */
public class CodeGenManager {

    // all functions
    private ArrayList<MachineFunction> machineFunctions;

    //global virtualregs
    private ArrayList<VirtualReg> globalVirtualRegs = new ArrayList<>();

    //map value in ir to vr in mc
    private HashMap<Value, VirtualReg> vMap = new HashMap<>();

    //key为phi指令目标vr，value为phi指令的参数vr和目标vr组成的集合
    private HashMap<VirtualReg, HashSet<VirtualReg>> phiSets;


    //key为phi指令的目标vr，value为一串01序列，长度等同于phi指令所在基本块的前驱块，如果有环该位为0
    private HashMap<VirtualReg,ArrayList<Boolean>> phiRows=new HashMap<>();

    //ir moudle
    private static MyModule myModule;

    private CodeGenManager(MyModule myModule) {
        this.myModule = myModule;
    }

    private static CodeGenManager codeGenManager;

    //ir->machinecode
    public CodeGenManager getInstance(MyModule myModule) {
        if (this.codeGenManager == null) {
            this.codeGenManager = new CodeGenManager(myModule);
        }
        return this.codeGenManager;
    }

    public ArrayList<MachineFunction> getMachineFunctions() {
        return machineFunctions;
    }

    interface HandlePhi {
        void handlephi();
    }

    interface AnalyzeValue{
        MachineOperand analyzeValue(Value v);
    }

    private void MachineCodeGeneration() {
        ArrayList<GlobalVariable> gVs = myModule.__globalVariables;
        Iterator<GlobalVariable> itgVs = gVs.iterator();
        while (itgVs.hasNext()) {
            GlobalVariable gV = itgVs.next();
            VirtualReg gVr = new VirtualReg(gV.getName(), true);
            vMap.put(gV, gVr);
            globalVirtualRegs.add(gVr);
        }
        IList<Function, MyModule> fList = myModule.__functions;
        Iterator<INode<Function, MyModule>> fIt = fList.iterator();
        while (fIt.hasNext()) {
            INode<Function, MyModule> fNode = fIt.next();
            MachineFunction mf = new MachineFunction(this);
            machineFunctions.add(mf);
            HashMap<BasicBlock, MachineBlock> bMap = new HashMap<>();
            IList<BasicBlock, Function> bList = fNode.getVal().getList_();
            Iterator<INode<BasicBlock, Function>> bIt = bList.iterator();
            while (bIt.hasNext()) {
                INode<BasicBlock, Function> bNode = bIt.next();
                bMap.put(bNode.getVal(), new MachineBlock(mf));
            }
            bIt = bList.iterator();
            while (bIt.hasNext()) {
                BasicBlock b = bIt.next().getVal();
                MachineBlock mb = bMap.get(b);
                Iterator<BasicBlock> bbIt = b.getPredecessor_().iterator();
                while (bbIt.hasNext()) {
                    mb.addPred(bMap.get(bbIt.next()));
                }
                //TODO
                //翻译br指令的时候再指定后继基本块。有些情况下某个后继基本块必须要放在本基本块的下一个，跳转指令
//                bbIt=b.getSuccessor_().iterator();
//                while(bbIt.hasNext()){
//                    mb.addSucc(bMap.get(bbIt.next()));
//                }

            }

            AnalyzeValue aV=(Value v)->{
                if (v instanceof Function.Arg) {
                    VirtualReg vr;
                    if (mf.getVRegMap().get(v.getName()) == null) {
                        vr = new VirtualReg(v.getName());
                        mf.addVirtualReg(vr);
                        for (int i = 0; i < fNode.getVal().getNumArgs(); i++) {
                            if (i < 4) {
                                MachineCode mc = new MCMove(bMap.get(fNode.getVal().getList_().getEntry()), 0);
                                ((MCMove) mc).setDst(vr);
                                ((MCMove) mc).setRhs(mf.getPhyReg(i));
                            } else {
                                MachineCode mcLD = new MCLoad(bMap.get(fNode.getVal().getList_().getEntry()), 0);
                                ((MCLoad) mcLD).setAddr(mf.getPhyReg("sp"));
                                ((MCLoad) mcLD).setOffset(new MachineOperand((i - 4) * 4));
                                ((MCLoad) mcLD).setDst(vr);
                            }
                            break;
                        }
                        return vr;
                    } else {
                        return mf.getVRegMap().get(v.getName());
                    }
                } else if (myModule.__globalVariables.contains(v)) {
                    assert (vMap.containsKey(v));
                    return vMap.get(v);
                } else if (v instanceof Constants.ConstantInt) {
                    return new MachineOperand(((Constants.ConstantInt) v).getVal());
                } else {
                    if (mf.getVRegMap().get(v.getName()) == null) {
                        VirtualReg vr = new VirtualReg(v.getName());
                        mf.addVirtualReg(vr);
                        return vr;
                    } else {
                        return mf.getVRegMap().get(v.getName());
                    }
                }
            };

            HandlePhi handlePhi = () -> {
                Iterator<INode<BasicBlock, Function>> bItt = bList.iterator();
                while (bItt.hasNext()) {
                    BasicBlock bb = bItt.next().getVal();
                    MachineBlock mbb = bMap.get(bb);
                    Iterator<MachineBlock> mbItt=mbb.getPred().iterator();
                    while(mbItt.hasNext()){
                        MachineBlock predM=mbItt.next();
                        //prd,succ
                        HashMap<MachineBlock,ArrayList<MachineCode>> map=new HashMap<>();
                        map.put(mbb,new ArrayList<>());
                        //create waiting
                        waiting.put(predM,map);

                    }
                    IList<Instruction, BasicBlock> irList = bb.getList();
                    Iterator<INode<Instruction, BasicBlock>> irIt = irList.iterator();
                    //构造phiTarget到phiSet的映射
                    while (irIt.hasNext()) {
                        Instruction ir = irIt.next().getVal();
                        if (ir.tag == Instruction.TAG_.Phi) {
                            MachineOperand phiTarget = aV.analyzeValue(ir);
                            assert (phiTarget instanceof VirtualReg);
                            Iterator<Value> vIt = ((Phi) ir).getIncomingVals().iterator();
                            HashSet<VirtualReg> phiSet = new HashSet<>();
                            phiSet.add((VirtualReg) phiTarget);
                            while (vIt.hasNext()) {
                                MachineOperand phiArg = aV.analyzeValue(vIt.next());
                                if (phiArg.getState() == MachineOperand.state.imm) {
                                    continue;
                                }
                                phiSet.add((VirtualReg) phiArg);
                            }
                            phiRows.put((VirtualReg) phiTarget,new ArrayList<>());
                        } else {
                            break;
                        }
                    }
                    //大风车吱呀吱哟哟地转 见SSA Elimination after Register Allocation
                    //key是每个前驱块，value的map为phiTarget->phiParam
//                    HashMap<MachineBlock,HashMap<VirtualReg,VirtualReg>> phiGraph=new HashMap<>();
                    int predNum=bb.getPredecessor_().size();
                    //遍历该块的所有pred块
                    for(int i=0;i<predNum;i++){
                        IList<Instruction,BasicBlock> irrList=bb.getList();
                        Iterator<INode<Instruction,BasicBlock>> irrIt=irrList.iterator();
                        HashMap<VirtualReg,VirtualReg> edges=new HashMap<>();
                        while(irrIt.hasNext()){
                            Instruction ir=irIt.next().getVal();
                            if(ir.tag==Instruction.TAG_.Phi){
                                MachineOperand phiTarget= aV.analyzeValue(ir);
                                assert(phiTarget instanceof VirtualReg);
                                MachineOperand phiParam=aV.analyzeValue(((Phi)ir).getIncomingVals().get(i));
                                edges.put((VirtualReg) phiTarget,(VirtualReg) phiParam);
                            }else{
                                break;
                            }
                        }
                        ArrayList<ArrayList<VirtualReg>> circles=calcCircle(edges,i);
                        if(!circles.isEmpty()){
                            Iterator<ArrayList<VirtualReg>> it1=circles.iterator();
                            while(it1.hasNext()){
                                ArrayList<VirtualReg> circle=it1.next();
                                Iterator<VirtualReg> it2=circle.iterator();
                                assert(!circle.isEmpty());
                                VirtualReg temp=new VirtualReg();
                                mf.addVirtualReg(temp);
                                MachineCode mc=new MCMove();
                                ((MCMove)mc).setDst(temp);
                                while(it2.hasNext()){
                                    VirtualReg vr=it2.next();
                                    ((MCMove)mc).setRhs(vr);
                                    waiting.get(bMap.get(bb.getPredecessor_().get(i))).get(mbb).add(mc);
                                    mc=new MCMove();
                                    ((MCMove)mc).setDst(vr);
                                }
                                ((MCMove)mc).setRhs(temp);
                                waiting.get(bMap.get(bb.getPredecessor_().get(i))).get(mbb).add(mc);
                            }
                        }
                        Iterator<INode<Instruction,BasicBlock>> irItt=irList.iterator();
                        //对于没有环的正常插入copy：phiParam->phiTarget
                        while(irItt.hasNext()){
                            Instruction ir=irItt.next().getVal();
                            if(ir.tag==Instruction.TAG_.Phi){
                                MachineOperand phiTarget= aV.analyzeValue(ir);
                                assert(phiTarget instanceof VirtualReg);
                                assert(phiRows.containsKey(phiTarget));
                                if(phiRows.get(phiTarget).get(i)){
                                    MachineOperand phiParam=aV.analyzeValue(((Phi)ir).getIncomingVals().get(i));
                                    MachineCode mv=new MCMove();
                                    ((MCMove)mv).setRhs(phiParam);
                                    ((MCMove)mv).setDst(phiTarget);
                                    waiting.get(bMap.get(bb.getPredecessor_().get(i))).get(mbb).add(mv);
                                }
                            }else{
                                break;
                            }
                        }

                    }
                }
            };
            //处理phi指令
            handlePhi.handlephi();
            HashMap<Instruction,Pair<MachineCode,ArmAddition.CondType>>condMap=new HashMap<>();
            for(bIt=bList.iterator();bIt.hasNext();){
                BasicBlock bb=bIt.next().getVal();
                MachineBlock mb=bMap.get(bb);
                for(Iterator<INode<Instruction,BasicBlock>>iIt=bb.getList().iterator();iIt.hasNext();){
                    Instruction ir=iIt.next().getVal();
                    if(ir.tag== Instruction.TAG_.Phi){
                        continue;
                    }else if(ir instanceof BinaryInst){

                    }else if(ir .tag== Instruction.TAG_.Br){
                        if(ir.getNumOP()==3){
                            CondType cond=getCond((BinaryInst) ir.getOperands().get(0));
                            MachineCode br=new MCBranch(mb);
                            ((MCBranch)br).setCond(cond);
                            //set trueblock to branch target
                            ((MCBranch)br).setTarget(bMap.get(ir.getOperands().get(1)));
                            mb.setFalseSucc(bMap.get(ir.getOperands().get(2)));
                            mb.setTrueSucc(bMap.get(ir.getOperands().get(1)));
                        }else{
                            assert(ir.getNumOP()==1);
                            //如果只有一个后继块，那么此跳转指令就是废的
                            if(bb.getPredecessor_().size()==1){
                                mb.setFalseSucc(bMap.get(ir.getOperands().get(0)));
                                continue;
                            }
                            MachineCode j=new MCJump(mb);
                            ((MCJump)j).setTarget(bMap.get(ir.getOperands().get(0)));
                            mb.setTrueSucc(bMap.get(ir.getOperands().get(0)));
                            if(ir.getOperands().get(0)==bb.getPredecessor_().get(0)){
                                mb.setFalseSucc(bMap.get(bb.getPredecessor_().get(1)));
                            }else{
                                assert(ir.getOperands().get(0)==bb.getPredecessor_().get(1));
                                mb.setFalseSucc(bMap.get(bb.getPredecessor_().get(0)));
                            }
                        }

                    }else if(ir.tag==Instruction.TAG_.Call){

                    }else if(ir.tag==Instruction.TAG_.Ret){

                    }else if(ir.tag==Instruction.TAG_.Alloca){

                    }else if(ir.tag==Instruction.TAG_.Load){

                    }else if(ir.tag==Instruction.TAG_.Store){

                    }else if(ir.tag==Instruction.TAG_.GEP){

                    }else if(ir.tag==Instruction.TAG_.Zext){

                    }
                }
            }


        }
    }

    private CondType getCond(BinaryInst bI){
        if (bI.isLt()){
            return CondType.Lt;
        }else if(bI.isLe()){
            return CondType.Le;
        }else if(bI.isGe()){
            return CondType.Ge;
        }else if(bI.isGt()){
            return CondType.Gt;
        }else if(bI.isEq()){
            return CondType.Eq;
        }else if(bI.isNe()){
            return CondType.Ne;
        }else {
            assert(false);
            return CondType.Ge;
        }
    }

    //计算来自某一前驱块的一堆phiTarget中哪些在环中，共有几个环。
    private ArrayList<ArrayList<VirtualReg>> calcCircle(HashMap<VirtualReg, VirtualReg> graph,int i){
        ArrayList<ArrayList<VirtualReg>> result=new ArrayList<>();
        while(!graph.isEmpty()){
            //从剩余图中获得一个节点
            Iterator<Map.Entry<VirtualReg,VirtualReg>> ite=graph.entrySet().iterator();
            VirtualReg now=ite.next().getKey();
            Stack<VirtualReg> stack=new Stack<>();
            //深度优先搜索
            while (true){
                //如果一个节点没有出度，退出循环
                if(!graph.containsKey(now)){
                    break;
                }else if(stack.contains(now)){
                    break;
                }else{
                    stack.push(now);
                    now=graph.get(now);
                }
            }
            //如果以该点出发没有环路，那么从graph中删去把栈内所有点
            if(!graph.containsKey(now)){
                while(!stack.isEmpty()){
                    VirtualReg r=stack.pop();
                    assert(graph.containsKey(r));
                    assert(phiRows.get(r).size()==i);
                    phiRows.get(r).add(true);
                    graph.remove(r);
                }
            }else{
                ArrayList<VirtualReg> circle=new ArrayList<>();
                assert(stack.contains(now));
                while(stack.contains(now)){
                    VirtualReg r=stack.pop();
                    circle.add(r);
                    assert(graph.containsKey(r));
                    assert(phiRows.get(r).size()==i);
                    phiRows.get(r).add(false);
                    graph.remove(r);
                }
                while(!stack.isEmpty()){
                    VirtualReg r=stack.pop();
                    assert(graph.containsKey(r));
                    assert(phiRows.get(r).size()==i);
                    phiRows.get(r).add(true);
                    graph.remove(r);
                }
                result.add(circle);
            }
        }
        return result;
    }

    //pred->(succ->MCs)
    private HashMap<MachineBlock,HashMap<MachineBlock, ArrayList<MachineCode>>> waiting = new HashMap<>();
//
//    private MachineOperand analyzeValue(Value v, MachineFunction mf, Function f, HashMap<BasicBlock, MachineBlock> bMap) {
//
//    }

}
