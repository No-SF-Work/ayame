package backend;

import backend.LiveInterval;
import backend.machinecodes.*;
import backend.reg.MachineOperand;
import backend.reg.VirtualReg;
import ir.MyModule;
import ir.types.ArrayType;
import ir.types.IntegerType;
import ir.types.PointerType;
import ir.types.Type;
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
import java.lang.reflect.Array;
import java.rmi.registry.Registry;
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
    private HashMap<VirtualReg, ArrayList<Boolean>> phiRows = new HashMap<>();

    //ir moudle
    private static MyModule myModule;

    private CodeGenManager(MyModule myModule) {
        this.myModule = myModule;
    }

    private static CodeGenManager codeGenManager;

    private boolean canEncodeImm(int imm) {
        int n = imm;
        for (int ror = 0; ror < 32; ror += 2) {
            if ((n & ~0xFF) == 0) {
                return true;
            }
            n = (n << 2) | (n >>> 30);
        }
        return false;
    }

    private MachineOperand genImm(int imm, MachineBlock mb) {
        MachineOperand mo = new MachineOperand(imm);
        if (canEncodeImm(imm)) {
            return mo;
        } else {
            VirtualReg vr = new VirtualReg();
            mb.getMF().addVirtualReg(vr);
            MachineCode mc = new MCMove(mb);
            ((MCMove) mc).setDst(vr);
            ((MCMove) mc).setDst(mo);
            return vr;
        }
    }

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

    interface AnalyzeValue {
        MachineOperand analyzeValue(Value v);
    }

    interface AnalyzeNoImm {
        MachineOperand analyzeNoImm(Value v, MachineBlock mb);
    }

    interface DFSSerialize {
        void dfsSerialize();
    }

    public void dfsSerial(MachineBlock mb, MachineFunction mf,HashMap<MachineBlock,Boolean> isVisit){
        isVisit.put(mb, true);
        mf.insertBlock(mb);
        if (mb.getTrueSucc() == null && mb.getFalseSucc() == null) {
            return;
        }
        //只有一个后继
        if (mb.getFalseSucc() == null) {
            //当前基本块只有一个后继，且后继一定排在当前基本块之后，那么跳转指令就是废的
            MachineCode mcl = mb.getmclist().getLast().getVal();
            assert (mcl instanceof MCJump);
            mb.getmclist().getLast().removeSelf();
            //如果waiting中有copy，则插入当前块之后，即两个块中间
            if (waiting.containsKey(mb)) {
                if (waiting.get(mb).containsKey(mb.getTrueSucc())) {
                    Iterator<MachineCode> mcIte = waiting.get(mb).get(mb.getTrueSucc()).iterator();
                    while (mcIte.hasNext()) {
                        MachineCode mci = mcIte.next();
                        mci.setMb(mb);
                    }
                }
            }
            if (isVisit.containsKey(mb.getTrueSucc())) {
                mb.addAtEndMC(mcl.getNode());
            } else {
                dfsSerial(mb.getTrueSucc(),mf,isVisit);
            }
        } else {
            //有两个后继的情况下优先让false后继放到自己后面，处理False后继和只有一种后继的情况相同
            if (isVisit.containsKey(mb.getFalseSucc())) {
                if (waiting.containsKey(mb)) {
                    if (waiting.get(mb).containsKey(mb.getFalseSucc())) {
                        MachineBlock newMB = new MachineBlock(mf);
                        Iterator<MachineCode> mcIte = waiting.get(mb).get(mb.getFalseSucc()).iterator();
                        while (mcIte.hasNext()) {
                            MachineCode mci = mcIte.next();
                            mci.setMb(newMB);
                        }
                        MCJump jump = new MCJump(newMB);
                        jump.setTarget(mb.getFalseSucc());
                        assert (mb.getFalseSucc().getPred().contains(mb));
                        mb.getFalseSucc().removePred(mb);
                        mb.getFalseSucc().addPred(newMB);
                        newMB.setTrueSucc(mb.getFalseSucc());
                        newMB.addPred(mb);
                        mb.setFalseSucc(newMB);
                    }
                }
            } else {
                MachineCode mcl = mb.getmclist().getLast().getVal();
                assert (mcl instanceof MCBranch);
                mb.getmclist().getLast().removeSelf();
                //如果waiting中有copy，则插入当前块之后，即两个块中间
                if (waiting.containsKey(mb)) {
                    if (waiting.get(mb).containsKey(mb.getFalseSucc())) {
                        Iterator<MachineCode> mcIte = waiting.get(mb).get(mb.getFalseSucc()).iterator();
                        while (mcIte.hasNext()) {
                            MachineCode mci = mcIte.next();
                            mci.setMb(mb);
                        }
                    }
                }
                dfsSerial(mb.getFalseSucc(),mf,isVisit);
            }
            //处理True后继
            if (waiting.containsKey(mb)) {
                if (waiting.get(mb).containsKey(mb.getTrueSucc())) {
                    if(mb.getTrueSucc().getPred().size()==1){
                        Iterator<MachineCode> mcIte = waiting.get(mb).get(mb.getTrueSucc()).iterator();
                        while (mcIte.hasNext()) {
                            MachineCode mci = mcIte.next();
                            mci.insertBeforeNode(mb.getTrueSucc().getmclist().getEntry().getVal());
                        }
                    }else{
                        MachineBlock newMB = new MachineBlock(mf);
                        Iterator<MachineCode> mcIte = waiting.get(mb).get(mb.getTrueSucc()).iterator();
                        while (mcIte.hasNext()) {
                            MachineCode mci = mcIte.next();
                            mci.setMb(newMB);
                        }
                        MCJump jump = new MCJump(newMB);
                        jump.setTarget(mb.getTrueSucc());
                        assert (mb.getTrueSucc().getPred().contains(mb));
                        mb.getTrueSucc().removePred(mb);
                        mb.getTrueSucc().addPred(newMB);
                        newMB.setTrueSucc(mb.getTrueSucc());
                        newMB.addPred(mb);
                        mb.setFalseSucc(newMB);
                    }

                }
            }
            if(!isVisit.containsKey(mb.getTrueSucc())){
                dfsSerial(mb.getTrueSucc(),mf,isVisit);
            }
        }
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
        HashMap<Function, MachineFunction> fMap = new HashMap<>();
        while (fIt.hasNext()) {
            MachineFunction mf = new MachineFunction(this);
            fMap.put(fIt.next().getVal(), mf);
        }
        fIt = fList.iterator();
        while (fIt.hasNext()) {
            INode<Function, MyModule> fNode = fIt.next();
            Function f = fNode.getVal();
            MachineFunction mf = fMap.get(f);
            machineFunctions.add(mf);
            HashMap<Instruction, VirtualReg> irMap = new HashMap<>();
            HashMap<BasicBlock, MachineBlock> bMap = new HashMap<>();
            IList<BasicBlock, Function> bList = f.getList_();
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

            AnalyzeValue aV = (Value v) -> {
                if (v instanceof Function.Arg && f.getArgList().contains(v)) {
                    VirtualReg vr;
                    if (mf.getVRegMap().get(v.getName()) == null) {
                        vr = new VirtualReg(v.getName());
                        mf.addVirtualReg(vr);
                        for (int i = 0; i < f.getNumArgs(); i++) {
                            if (i < 4) {
                                MachineCode mc = new MCMove(bMap.get(f.getList_().getEntry()), 0);
                                ((MCMove) mc).setDst(vr);
                                ((MCMove) mc).setRhs(mf.getPhyReg(i));
                            } else {
                                MachineCode mcLD = new MCLoad(bMap.get(f.getList_().getEntry()), 0);
                                ((MCLoad) mcLD).setAddr(mf.getPhyReg("sp"));
                                ((MCLoad) mcLD).setOffset(genImm((i - 4) * 4, bMap.get(f.getList_().getEntry())));
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
                } else if (v instanceof BinaryInst && (((BinaryInst) v).isCond())) {
                    //br指令会检查此cond指令是否已经执行过，执行过irMap就会有记录
                    if (irMap.get(v) == null) {
                        VirtualReg vr = new VirtualReg(v.getName());
                        irMap.put(((BinaryInst) v), vr);
                        return vr;
                    } else {
                        return irMap.get(v);
                    }
                } else {
                    if (irMap.get(v) == null) {
                        VirtualReg vr = new VirtualReg(v.getName());
                        mf.addVirtualReg(vr);
                        irMap.put((Instruction) v, vr);
                        return vr;
                    } else {
                        return irMap.get(v);
                    }
                }
            };

            AnalyzeNoImm ani = (Value v, MachineBlock mb) -> {
                if (v instanceof Constants.ConstantInt) {
                    VirtualReg vr = new VirtualReg();
                    mb.getMF().addVirtualReg(vr);
                    MachineCode mv = new MCMove(mb);
                    ((MCMove) mv).setDst(vr);
                    ((MCMove) mv).setRhs(new MachineOperand(((Constants.ConstantInt) v).getVal()));
                    return vr;
                } else {
                    return aV.analyzeValue(v);
                }
            };

            HandlePhi handlePhi = () -> {
                Iterator<INode<BasicBlock, Function>> bItt = bList.iterator();
                while (bItt.hasNext()) {
                    BasicBlock bb = bItt.next().getVal();
                    MachineBlock mbb = bMap.get(bb);
                    Iterator<MachineBlock> mbItt = mbb.getPred().iterator();
                    while (mbItt.hasNext()) {
                        MachineBlock predM = mbItt.next();
                        //prd,succ
                        HashMap<MachineBlock, ArrayList<MachineCode>> map = new HashMap<>();
                        map.put(mbb, new ArrayList<>());
                        //create waiting
                        waiting.put(predM, map);

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
                            phiRows.put((VirtualReg) phiTarget, new ArrayList<>());
                        } else {
                            break;
                        }
                    }
                    //大风车吱呀吱哟哟地转 见SSA Elimination after Register Allocation
                    //key是每个前驱块，value的map为phiTarget->phiParam
//                    HashMap<MachineBlock,HashMap<VirtualReg,VirtualReg>> phiGraph=new HashMap<>();
                    int predNum = bb.getPredecessor_().size();
                    //遍历该块的所有pred块
                    for (int i = 0; i < predNum; i++) {
                        IList<Instruction, BasicBlock> irrList = bb.getList();
                        Iterator<INode<Instruction, BasicBlock>> irrIt = irrList.iterator();
                        HashMap<VirtualReg, VirtualReg> edges = new HashMap<>();
                        while (irrIt.hasNext()) {
                            Instruction ir = irIt.next().getVal();
                            if (ir.tag == Instruction.TAG_.Phi) {
//                                MachineOperand phiTarget = aV.analyzeValue(ir,bMap.get(f.getList_().getEntry()));
                                MachineOperand phiTarget = aV.analyzeValue(ir);
                                assert (phiTarget instanceof VirtualReg);
                                MachineOperand phiParam = aV.analyzeValue(((Phi) ir).getIncomingVals().get(i));
//                                MachineOperand phiParam = aV.analyzeValue(((Phi) ir).getIncomingVals().get(i),bMap.get(ir.getBB()));
                                edges.put((VirtualReg) phiTarget, (VirtualReg) phiParam);
                            } else {
                                break;
                            }
                        }
                        ArrayList<ArrayList<VirtualReg>> circles = calcCircle(edges, i);
                        if (!circles.isEmpty()) {
                            Iterator<ArrayList<VirtualReg>> it1 = circles.iterator();
                            while (it1.hasNext()) {
                                ArrayList<VirtualReg> circle = it1.next();
                                Iterator<VirtualReg> it2 = circle.iterator();
                                assert (!circle.isEmpty());
                                VirtualReg temp = new VirtualReg();
                                mf.addVirtualReg(temp);
                                MachineCode mc = new MCMove();
                                ((MCMove) mc).setDst(temp);
                                while (it2.hasNext()) {
                                    VirtualReg vr = it2.next();
                                    ((MCMove) mc).setRhs(vr);
                                    waiting.get(bMap.get(bb.getPredecessor_().get(i))).get(mbb).add(mc);
                                    mc = new MCMove();
                                    ((MCMove) mc).setDst(vr);
                                }
                                ((MCMove) mc).setRhs(temp);
                                waiting.get(bMap.get(bb.getPredecessor_().get(i))).get(mbb).add(mc);
                            }
                        }
                        Iterator<INode<Instruction, BasicBlock>> irItt = irList.iterator();
                        //对于没有环的正常插入copy：phiParam->phiTarget
                        while (irItt.hasNext()) {
                            Instruction ir = irItt.next().getVal();
                            if (ir.tag == Instruction.TAG_.Phi) {
                                MachineOperand phiTarget = aV.analyzeValue(ir);
                                assert (phiTarget instanceof VirtualReg);
                                assert (phiRows.containsKey(phiTarget));
                                if (phiRows.get(phiTarget).get(i)) {
                                    MachineOperand phiParam = aV.analyzeValue(((Phi) ir).getIncomingVals().get(i));
                                    MachineCode mv = new MCMove();
                                    ((MCMove) mv).setRhs(phiParam);
                                    ((MCMove) mv).setDst(phiTarget);
                                    waiting.get(bMap.get(bb.getPredecessor_().get(i))).get(mbb).add(mv);
                                }
                            } else {
                                break;
                            }
                        }

                    }
                }
            };
            //处理phi指令
            handlePhi.handlephi();
            HashMap<Instruction, Pair<MachineCode, ArmAddition.CondType>> condMap = new HashMap<>();
            for (bIt = bList.iterator(); bIt.hasNext(); ) {
                BasicBlock bb = bIt.next().getVal();
                MachineBlock mb = bMap.get(bb);
                for (Iterator<INode<Instruction, BasicBlock>> iIt = bb.getList().iterator(); iIt.hasNext(); ) {
                    Instruction ir = iIt.next().getVal();
                    if (ir.tag == Instruction.TAG_.Phi) {
                        continue;
                    } else if (ir instanceof BinaryInst) {
                        MachineOperand lhs = aV.analyzeValue(ir.getOperands().get(0));
                        MachineOperand rhs = aV.analyzeValue(ir.getOperands().get(1));
                        boolean rhsIsConst = ir.getOperands().get(1) instanceof Constants.ConstantInt;
                        boolean lhsIsConst = ir.getOperands().get(0) instanceof Constants.ConstantInt;
                        assert (!(rhsIsConst && lhsIsConst));
                        if (rhsIsConst) {
                            int imm = ((Constants.ConstantInt) ir.getOperands().get(1)).getVal();
                            int temp = imm;
                            if (((BinaryInst) ir).isDiv() && imm > 0) {
                                //除以2的幂
                                if ((imm & (imm - 1)) == 0) {
                                    assert (imm != 0);
                                    MCMove mv = new MCMove(mb);
                                    MachineOperand dst = aV.analyzeValue(ir);
                                    mv.setDst(dst);
                                    mv.setRhs(lhs);
                                    mv.setShift(ArmAddition.ShiftType.Lsr, calcCTZ(imm));
                                } else {
                                    long nc = (1 << 31) - ((1 << 31) % temp) - 1;
                                    long p = 32;
                                    while (((long) 1 << p) <= nc * (temp - ((long) 1 << p) % temp)) {
                                        p++;
                                    }
                                    int m = (int) ((((long) 1 << p) + (long) temp - ((long) 1 << p) % temp) / (long) temp);
                                    int shift = (int) (p - 32);
                                    MCMove mc0 = new MCMove(mb);
                                    VirtualReg v = new VirtualReg();
                                    mf.addVirtualReg(v);
                                    mc0.setDst(v);
                                    mc0.setRhs(new MachineOperand(m));
                                    VirtualReg v1 = new VirtualReg();
                                    mf.addVirtualReg(v1);
                                    if (m >= 0x80000000) {
                                        MCFma mc2 = new MCFma(mb);
                                        mc2.setAdd(true);
                                        mc2.setSign(true);
                                        mc2.setDst(v1);
                                        mc2.setLhs(lhs);
                                        mc2.setRhs(v);
                                        mc2.setAcc(lhs);
                                    } else {
                                        MCLongMul mc1 = new MCLongMul(mb);
                                        mc1.setDst(v1);
                                        mc1.setRhs(v);
                                        mc1.setLhs(lhs);
                                    }
                                    MCMove mc3 = new MCMove(mb);
                                    VirtualReg v3 = new VirtualReg();
                                    mf.addVirtualReg(v3);
                                    mc3.setDst(v3);
                                    mc3.setRhs(v1);
                                    mc3.setShift(ArmAddition.ShiftType.Asr, shift);
                                    MCBinary mc4 = new MCBinary(MachineCode.TAG.Add, mb);
                                    MachineOperand dst = aV.analyzeValue(ir);
                                    mc4.setDst(dst);
                                    mc4.setLhs(mc3.getDst());
                                    mc4.setRhs(lhs);
                                    mc4.setShift(ArmAddition.ShiftType.Lsr, 31);
                                }
                                continue;
                            }
                            if (((BinaryInst) ir).isMul()) {
                                int log = calcCTZ(imm);
                                if ((imm & (imm - 1)) == 0) {
                                    MachineOperand dst = aV.analyzeValue(ir);
                                    MCMove mc = new MCMove(mb);
                                    mc.setDst(dst);
                                    if (imm == 0) {
                                        mc.setRhs(new MachineOperand(0));
                                        continue;
                                    }
                                    mc.setRhs(lhs);
                                    if (log > 0) {
                                        mc.setShift(ArmAddition.ShiftType.Lsl, log);
                                    }
                                    continue;
                                }
                            }
                        }
                        if (lhsIsConst) {
                            int imm = ((Constants.ConstantInt) ir.getOperands().get(0)).getVal();
                            if (((BinaryInst) ir).isMul()) {
                                int log = calcCTZ(imm);
                                if ((imm & (imm - 1)) == 0) {
                                    MachineOperand dst = aV.analyzeValue(ir);
                                    MCMove mc = new MCMove(mb);
                                    mc.setDst(dst);
                                    if (imm == 0) {
                                        mc.setRhs(new MachineOperand(0));
                                        continue;
                                    }
                                    mc.setRhs(rhs);
                                    if (log > 0) {
                                        mc.setShift(ArmAddition.ShiftType.Lsl, log);
                                    }
                                    continue;
                                }
                            }
                        }
                        //可以使用立即数的场景
                        if (!((BinaryInst) ir).isMul() && !((BinaryInst) ir).isDiv()) {
                            if (rhsIsConst) {
                                int imm = ((Constants.ConstantInt) ir.getOperands().get(1)).getVal();
                                if (((BinaryInst) ir).isAdd() || ((BinaryInst) ir).isSub()) {
                                    if (!canEncodeImm(imm) && canEncodeImm(-imm)) {
                                        MCBinary in;
                                        in = ((BinaryInst) ir).isAdd() ? new MCBinary(MachineCode.TAG.Sub, mb) : new MCBinary(MachineCode.TAG.Add, mb);
                                        imm = -imm;
                                        MachineOperand dst = aV.analyzeValue(ir);
                                        in.setDst(dst);
                                        in.setRhs(genImm(imm, mb));
                                        in.setLhs(lhs);
                                        continue;
                                    }
                                }
                            } else if (lhsIsConst) {
                                int imm = ((Constants.ConstantInt) ir.getOperands().get(0)).getVal();
                                if (((BinaryInst) ir).isAdd() || ((BinaryInst) ir).isSub()) {
                                    if (!canEncodeImm(imm) && canEncodeImm(-imm)) {
                                        MCBinary in;
                                        in = ((BinaryInst) ir).isAdd() ? new MCBinary(MachineCode.TAG.Rsb, mb) : new MCBinary(MachineCode.TAG.Add, mb);
                                        imm = -imm;
                                        MachineOperand dst = aV.analyzeValue(ir);
                                        in.setDst(dst);
                                        in.setRhs(genImm(imm, mb));
                                        in.setLhs(rhs);
                                        continue;
                                    }
                                }
                            }
                        }
                        //之后的情况只能使用两个寄存器
                        //TODO，将乘法和加法合并成乘加，好像有问题，现在先不做
                        BinaryInst bi = ((BinaryInst) ir);
                        if (bi.isCond()) {
                            continue;
                        } else if (bi.isAnd() || bi.isOr()) {
                            assert (false);
                        } else {
                            MachineCode.TAG tag;
                            if (bi.isAdd()) {
                                tag = MachineCode.TAG.Add;
                            } else if (bi.isSub()) {
                                tag = MachineCode.TAG.Sub;
                            } else if (bi.isMul()) {
                                tag = MachineCode.TAG.Mul;
                            } else if (bi.isDiv()) {
                                tag = MachineCode.TAG.Div;
                            } else if (bi.isRsb()) {
                                tag = MachineCode.TAG.Rsb;
                            } else {
                                assert (false);
                                tag = MachineCode.TAG.Add;
                            }
                            MCBinary binary = new MCBinary(tag, mb);
                            binary.setLhs(lhs);
                            binary.setRhs(rhs);
                        }

                    } else if (ir.tag == Instruction.TAG_.Br) {
                        if (ir.getNumOP() == 3) {
                            CondType cond = getCond((BinaryInst) ir.getOperands().get(0));
                            if (!irMap.containsKey(ir.getOperands().get(0))) {
                                aV.analyzeValue(ir.getOperands().get(0));
                                assert (ir.getOperands().get(0) instanceof BinaryInst);
                                assert (((BinaryInst) ir.getOperands().get(0)).isCond());
                                Instruction condi = (BinaryInst) (ir.getOperands().get(0));
                                MCCompare compare = new MCCompare(mb);
                                compare.setRhs(aV.analyzeValue(condi.getOperands().get(0)));
                                compare.setLhs(aV.analyzeValue(condi.getOperands().get(1)));
                                compare.setCond(cond);
                            }
                            MachineCode br = new MCBranch(mb);
                            ((MCBranch) br).setCond(cond);
                            //set trueblock to branch target
                            ((MCBranch) br).setTarget(bMap.get(ir.getOperands().get(1)));
                            mb.setFalseSucc(bMap.get(ir.getOperands().get(2)));
                            mb.setTrueSucc(bMap.get(ir.getOperands().get(1)));
                        } else {
                            assert (ir.getNumOP() == 1);
                            //如果只有一个后继块，那么此跳转指令就是废的
//                            if (bb.getPredecessor_().size() == 1) {
//                                mb.setFalseSucc(bMap.get(ir.getOperands().get(0)));
//                                continue;
//                            }
                            MachineCode j = new MCJump(mb);
                            ((MCJump) j).setTarget(bMap.get(ir.getOperands().get(0)));
                            mb.setTrueSucc(bMap.get(ir.getOperands().get(0)));
                            if (ir.getOperands().get(0) == bb.getPredecessor_().get(0)) {
                                mb.setFalseSucc(bMap.get(bb.getPredecessor_().get(1)));
                            } else {
                                assert (ir.getOperands().get(0) == bb.getPredecessor_().get(1));
                                mb.setFalseSucc(bMap.get(bb.getPredecessor_().get(0)));
                            }
                        }



                    } else if (ir.tag == Instruction.TAG_.Call) {
                        //获取调用函数的参数数量
                        int argNum = ir.getOperands().size() - 1;
                        for (int i = 0; i < argNum; i++) {
                            if (i < 4) {
                                MachineOperand rhs = aV.analyzeValue(ir.getOperands().get(i + 1));
                                MachineCode mv = new MCMove(mb);
                                ((MCMove) mv).setRhs(rhs);
                                ((MCMove) mv).setDst(mf.getPhyReg(i));
                            } else {
                                VirtualReg vr = (VirtualReg) ani.analyzeNoImm(ir.getOperands().get(i + 1), mb);
                                MachineCode st = new MCStore(mb);
                                ((MCStore) st).setData(vr);
                                ((MCStore) st).setAddr(mf.getPhyReg("sp"));
                                ((MCStore) st).setOffset(new MachineOperand(-(argNum - i)));
                                st.setShift(ArmAddition.ShiftType.Lsl, 2);
                            }
                            if (argNum > 4) {
                                MachineCode sub = new MCBinary(MachineCode.TAG.Sub, mb);
                                ((MCBinary) sub).setDst(mf.getPhyReg("sp"));
                                ((MCBinary) sub).setLhs(mf.getPhyReg("sp"));
                                ((MCBinary) sub).setRhs(new MachineOperand(4 * (argNum - 4)));
                            }

                            MachineCode call = new MCCall(mb);
                            assert (ir.getOperands().get(0) instanceof Function);
                            ((MCCall) call).setFunc(fMap.get((Function) ir.getOperands().get(0)));
                            if (argNum > 4) {
                                MachineCode add = new MCBinary(MachineCode.TAG.Add, mb);
                                ((MCBinary) add).setDst(mf.getPhyReg("sp"));
                                ((MCBinary) add).setLhs(mf.getPhyReg("sp"));
                                ((MCBinary) add).setRhs(new MachineOperand(4 * (argNum - 4)));
                            }
                            if (!((Function) (ir.getOperands().get(0))).getType().getRetType().isVoidTy()) {
                                MCMove mv = new MCMove(mb);
                                mv.setDst(aV.analyzeValue(ir));
                                mv.setRhs(mf.getPhyReg("r0"));
                            }
                        }
                    } else if (ir.tag == Instruction.TAG_.Ret) {
                        //如果有返回值
                        if (((TerminatorInst.RetInst) ir).getNumOP() != 0) {
                            MachineOperand res = aV.analyzeValue(ir.getOperands().get(0));
                            MCMove mv = new MCMove(mb);
                            mv.setDst(mf.getPhyReg("r0"));
                            mv.setRhs(res);
                            MCReturn re = new MCReturn(mb);
                        } else {
                            MCReturn re = new MCReturn(mb);
                        }
                    } else if (ir.tag == Instruction.TAG_.Alloca) {
                        //TODO 如何获得到底是哪个指针
                        if (ir.getType() instanceof PointerType) {

                        } else {
                            //alloca整数已被mem2reg优化
                            assert (ir.getType() instanceof ArrayType);
                            int size = 4;
                            Iterator<Integer> dimList = ((ArrayType) ir.getType()).getDims().iterator();
                            while (dimList.hasNext()) {
                                size *= dimList.next();
                            }
                            MachineOperand dst = aV.analyzeValue(ir);
                            MachineOperand offset = genImm(mf.getStackSize(), mb);
                            MCBinary add = new MCBinary(MachineCode.TAG.Add, mb);
                            add.setDst(dst);
                            add.setLhs(mf.getPhyReg("sp"));
                            add.setRhs(offset);
                        }
                    } else if (ir.tag == Instruction.TAG_.Load) {
                        MachineOperand dst = aV.analyzeValue(ir);
                        MachineOperand addr = aV.analyzeValue(ir.getOperands().get(0));
                        MachineOperand offset = new MachineOperand(0);
                        MCLoad load = new MCLoad(mb);
                        load.setDst(dst);
                        load.setOffset(offset);
                        load.setAddr(addr);
                    } else if (ir.tag == Instruction.TAG_.Store) {
                        MachineOperand arr = aV.analyzeValue(ir.getOperands().get(1));
                        MachineOperand data = ani.analyzeNoImm(ir.getOperands().get(0), mb);
                        MachineOperand offset = new MachineOperand(0);

                    } else if (ir.tag == Instruction.TAG_.GEP) {
                        //最后一个gep应该被优化合并到load/store里
                        assert (!(ir.getType() instanceof IntegerType));

                        //基址为上一个gep
                        MachineOperand arr = aV.analyzeValue(ir.getOperands().get(0));
                        //获取偏移
                        MachineOperand off = aV.analyzeValue(ir.getOperands().get(2));
                        boolean isOffConst = off.getState() == MachineOperand.state.imm;
                        //获取下一维度长度，即偏移的单位
                        int mult = 4;
                        Iterator<Integer> dimList = ((ArrayType) ir.getType()).getDims().iterator();
                        //跳过当前维度
                        dimList.next();
                        while (dimList.hasNext()) {
                            mult *= dimList.next();
                        }
                        if (mult == 0 || (isOffConst && off.getImm() == 0)) {
                            MachineOperand dst = aV.analyzeValue(ir);
                            MCMove mv = new MCMove(mb);
                            mv.setDst(dst);
                            mv.setRhs(arr);
                        } else if (isOffConst) {
                            MachineOperand dst = aV.analyzeValue(ir);
                            int totalOff = mult * off.getImm();
                            MachineOperand imm = genImm(totalOff, mb);
                            MCBinary add = new MCBinary(MachineCode.TAG.Add, mb);
                            add.setDst(dst);
                            add.setLhs(arr);
                            add.setRhs(imm);
                        } else if ((mult & (mult - 1)) == 0) {
                            MachineOperand dst = aV.analyzeValue(ir);
                            MCBinary add = new MCBinary(MachineCode.TAG.Add, mb);
                            add.setDst(dst);
                            add.setLhs(arr);
                            add.setRhs(off);
                            add.setShift(ArmAddition.ShiftType.Lsl, calcCTZ(mult));
                        } else {
                            MCFma fma = new MCFma(mb);
                            fma.setAcc(arr);
                            MCMove mv = new MCMove(mb);
                            VirtualReg v = new VirtualReg();
                            mf.addVirtualReg(v);
                            mv.setDst(v);
                            mv.setRhs(new MachineOperand(mult));
                            fma.setRhs(mv.getDst());
                            fma.setLhs(off);
                            MCMove mv1 = new MCMove(mb);
                            VirtualReg v1 = new VirtualReg();
                            mf.addVirtualReg(v1);
                            mv1.setDst(v1);
                            mv1.setRhs(arr);
                            fma.setDst(v1);
                            fma.setAdd(true);
                            fma.setSign(false);
                        }
                    }
                }
            }


            DFSSerialize s = () -> {
                HashMap<MachineBlock, Boolean> isVisit = new HashMap<>();
                dfsSerial(bMap.get(f.getList_().getEntry()),mf,isVisit);
            };

            s.dfsSerialize();

        }
    }

    //计算最低位1的位数，如果输入0，返回0
    private int calcCTZ(int n) {
        int res = 0;
        n = n >>> 1;
        while (n != 0) {
            n = n >>> 1;
            res++;
        }
        return res;
    }

    private CondType getOppoCond(CondType t) {
        if (t == CondType.Lt) {
            return CondType.Ge;
        } else if (t == CondType.Le) {
            return CondType.Gt;
        } else if (t == CondType.Ge) {
            return CondType.Lt;
        } else if (t == CondType.Gt) {
            return CondType.Le;
        } else if (t == CondType.Eq) {
            return CondType.Ne;
        } else if (t == CondType.Ne) {
            return CondType.Eq;
        } else {
            assert (false);
            return CondType.Eq;
        }
    }

    private CondType getCond(BinaryInst bI) {
        if (bI.isLt()) {
            return CondType.Lt;
        } else if (bI.isLe()) {
            return CondType.Le;
        } else if (bI.isGe()) {
            return CondType.Ge;
        } else if (bI.isGt()) {
            return CondType.Gt;
        } else if (bI.isEq()) {
            return CondType.Eq;
        } else if (bI.isNe()) {
            return CondType.Ne;
        } else {
            assert (false);
            return CondType.Ge;
        }
    }

    //计算来自某一前驱块的一堆phiTarget中哪些在环中，共有几个环。
    private ArrayList<ArrayList<VirtualReg>> calcCircle(HashMap<VirtualReg, VirtualReg> graph, int i) {
        ArrayList<ArrayList<VirtualReg>> result = new ArrayList<>();
        while (!graph.isEmpty()) {
            //从剩余图中获得一个节点
            Iterator<Map.Entry<VirtualReg, VirtualReg>> ite = graph.entrySet().iterator();
            VirtualReg now = ite.next().getKey();
            Stack<VirtualReg> stack = new Stack<>();
            //深度优先搜索
            while (true) {
                //如果一个节点没有出度，退出循环
                if (!graph.containsKey(now)) {
                    break;
                } else if (stack.contains(now)) {
                    break;
                } else {
                    stack.push(now);
                    now = graph.get(now);
                }
            }
            //如果以该点出发没有环路，那么从graph中删去把栈内所有点
            if (!graph.containsKey(now)) {
                while (!stack.isEmpty()) {
                    VirtualReg r = stack.pop();
                    assert (graph.containsKey(r));
                    assert (phiRows.get(r).size() == i);
                    phiRows.get(r).add(true);
                    graph.remove(r);
                }
            } else {
                ArrayList<VirtualReg> circle = new ArrayList<>();
                assert (stack.contains(now));
                while (stack.contains(now)) {
                    VirtualReg r = stack.pop();
                    circle.add(r);
                    assert (graph.containsKey(r));
                    assert (phiRows.get(r).size() == i);
                    phiRows.get(r).add(false);
                    graph.remove(r);
                }
                while (!stack.isEmpty()) {
                    VirtualReg r = stack.pop();
                    assert (graph.containsKey(r));
                    assert (phiRows.get(r).size() == i);
                    phiRows.get(r).add(true);
                    graph.remove(r);
                }
                result.add(circle);
            }
        }
        return result;
    }


    //pred->(succ->MCs)
    private HashMap<MachineBlock, HashMap<MachineBlock, ArrayList<MachineCode>>> waiting = new HashMap<>();
//
//    private MachineOperand analyzeValue(Value v, MachineFunction mf, Function f, HashMap<BasicBlock, MachineBlock> bMap) {
//
//    }

}
