package pass.ir;

import ir.Analysis.DomInfo;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.UndefValue;
import ir.values.Value;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst.AllocaInst;
import ir.values.instructions.MemInst.LoadInst;
import ir.values.instructions.MemInst.Phi;
import ir.values.instructions.MemInst.StoreInst;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.IList.INode;
import util.Mylogger;

public class Mem2reg implements IRPass {

  Logger log = Mylogger.getLogger(IRPass.class);
  MyFactoryBuilder factory = MyFactoryBuilder.getInstance();

  private static class RenameData {

    BasicBlock bb;
    BasicBlock pred;
    ArrayList<Value> values;

    public RenameData(BasicBlock bb, BasicBlock pred, ArrayList<Value> values) {
      this.bb = bb;
      this.pred = pred;
      this.values = values;
    }
  }

  @Override
  public String getName() {
    return "Mem2reg";
  }

  public void run(MyModule m) {
    log.info("Running pass : mem2reg");

    for (INode<Function, MyModule> funcNode : m.__functions) {
      Function func = funcNode.getVal();
      if (!func.isBuiltin_()) {
        runMem2reg(func);
      }
    }
  }

  public void runMem2reg(Function func) {
    // prepare
//    log.info("Running compute dominance info");
    DomInfo.computeDominanceInfo(func);
//    log.info("Running compute dominance frontier");
    DomInfo.computeDominanceFrontier(func);

    ArrayList<ArrayList<BasicBlock>> defBlocks = new ArrayList<>();
    ArrayList<AllocaInst> allocas = new ArrayList<>();
    HashMap<AllocaInst, Integer> allocaLookup = new HashMap<AllocaInst, Integer>();

    // initialize `defs`
    for (INode<BasicBlock, Function> bbNode : func.getList_()) {
      BasicBlock bb = bbNode.getVal();
      for (INode<Instruction, BasicBlock> instNode : bb.getList()) {
        var inst = instNode.getVal();
        if (inst.tag == TAG_.Alloca) {
          AllocaInst allocaInst = (AllocaInst) inst;
          // only local int
          if (allocaInst.getAllocatedType().equals(factory.getI32Ty())) {
//            defs.put(allocaInst, new ArrayList<>());
            allocas.add(allocaInst);
            allocaLookup.put(allocaInst, allocas.size() - 1);
            defBlocks.add(new ArrayList<>());
          }
        }
      }
    }

    for (INode<BasicBlock, Function> bbNode : func.getList_()) {
      BasicBlock bb = bbNode.getVal();
      for (INode<Instruction, BasicBlock> instNode : bb.getList()) {
        Instruction inst = instNode.getVal();
        if (inst.tag == TAG_.Store) {
          StoreInst storeInst = (StoreInst) inst;
          if (!(storeInst.getOperands().get(1) instanceof AllocaInst)) {
            continue;
          }
          Integer index = allocaLookup.get((AllocaInst) storeInst.getOperands().get(1));
          if (index != null) {
            defBlocks.get(index).add(bb);
          }
        }
      }
    }

    // Feature: add deleteUnusedAlloca, rewriteSingleStoreAlloca, promoteSingleBlockAlloca

    // insert phi-instructions
    // Algorithm: Static Single Assignment Book P31
    log.info("mem2reg: inserting phi-instructions");
    Queue<BasicBlock> W = new LinkedList<>();
    HashMap<Phi, Integer> phiToAllocaMap = new HashMap<>();
    for (AllocaInst allocaInst : allocas) {
      int index = allocaLookup.get(allocaInst);

      for (INode<BasicBlock, Function> bbNode : func.getList_()) {
        bbNode.getVal().setDirty(false);
      }

      W.addAll(defBlocks.get(index));

      while (!W.isEmpty()) {
        BasicBlock bb = W.remove();
        for (BasicBlock y : bb.getDominanceFrontier()) {
          if (!y.isDirty()) {
            y.setDirty(true);
            Phi phiInst = new Phi(TAG_.Phi, factory.getI32Ty(), y.getPredecessor_().size(), y);
            phiToAllocaMap.put(phiInst, index);
            if (!defBlocks.get(index).contains(y)) {
              W.add(y);
            }
          }
        }
      }
    }

    // variable renaming
    // Algorithm: https://llvm-clang-study-notes.readthedocs.io/en/latest/ssa/Mem2Reg.html#id1
    log.info("mem2reg: variable renaming");
    ArrayList<Value> values = new ArrayList<>();
    for (int i = 0; i < allocas.size(); i++) {
      values.add(new UndefValue());
    }
    for (INode<BasicBlock, Function> bbNode : func.getList_()) {
      bbNode.getVal().setDirty(false);
    }

    Stack<RenameData> renameDataStack = new Stack<>();
    renameDataStack.push(new RenameData(func.getList_().getEntry().getVal(), null, values));
    while (!renameDataStack.isEmpty()) {
      RenameData data = renameDataStack.pop();

      ArrayList<Value> currValues = new ArrayList<>(data.values);

      // 对于插入的 phi 指令，更新 incomingVals 为 values 中的对应值
      for (INode<Instruction, BasicBlock> instNode : data.bb.getList()) {
        Instruction inst = instNode.getVal();
        if (inst.tag != TAG_.Phi) {
          break;
        }

        Phi phiInst = (Phi) inst;
        if (!phiToAllocaMap.containsKey(phiInst)) {
          continue;
        }
        int predIndex = data.bb.getPredecessor_().indexOf(data.pred);
        phiInst.setIncomingVals(predIndex, data.values.get(phiToAllocaMap.get(phiInst)));
      }

      // 已经删除过 alloca/load/store，但是可能有来自其他前驱基本块的 incomingVals，所以在这里才 `continue;`
      if (data.bb.isDirty()) {
        continue;
      }
      data.bb.setDirty(true);
      for (var instNode = data.bb.getList().getEntry(); instNode != null; ) {
        Instruction inst = instNode.getVal();
        var tmp = instNode.getNext();
        // AllocaInst
        if (inst.tag == TAG_.Alloca) {
          instNode.removeSelf();
        }
        // LoadInst
        else if (inst.tag == TAG_.Load) {
          LoadInst loadInst = (LoadInst) inst;
          if (!(loadInst.getOperands().get(0) instanceof AllocaInst)) {
            instNode = tmp;
            continue;
          }
          int allocaIndex = allocaLookup.get((AllocaInst) loadInst.getOperands().get(0));
          loadInst.COReplaceAllUseWith(currValues.get(allocaIndex));
          instNode.removeSelf();
        }
        // StoreInst
        else if (inst.tag == TAG_.Store) {
          StoreInst storeInst = (StoreInst) inst;
          if (!(storeInst.getOperands().get(1) instanceof AllocaInst)) {
            instNode = tmp;
            continue;
          }
          int allocaIndex = allocaLookup.get((AllocaInst) storeInst.getOperands().get(1));
          currValues.set(allocaIndex, storeInst.getOperands().get(0));
          instNode.removeSelf();
        }
        // Phi
        else if (inst.tag == TAG_.Phi) {
          Phi phiInst = (Phi) inst;
          int allocaIndex = phiToAllocaMap.get(phiInst);
          currValues.set(allocaIndex, phiInst);
        }
        instNode = tmp;
      }

      for (BasicBlock bb : data.bb.getSuccessor_()) {
        renameDataStack.push(new RenameData(bb, data.bb, currValues));
      }
    }
  }
}
