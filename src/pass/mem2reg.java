package pass;

import ir.CFGInfo;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.values.BasicBlock;
import ir.values.Function;
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

public class mem2reg implements IRPass {

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
    return "mem2reg";
  }

  public void run(MyModule m) {
    log.info("Running pass : mem2reg");

    for (INode<Function, MyModule> funcNode : m.__functions) {
      runMem2reg(funcNode.getVal());
    }
  }

  public void runMem2reg(Function func) {
    // prepare
    log.info("Running compute dominance info");
    CFGInfo.computeDominanceInfo(func);
    log.info("Running compute dominance frontier");
    CFGInfo.computeDominanceFrontier(func);

    ArrayList<ArrayList<BasicBlock>> defBlocks = new ArrayList<>();
    ArrayList<AllocaInst> allocas = new ArrayList<>();
    HashMap<AllocaInst, Integer> allocaLookup = new HashMap<AllocaInst, Integer>();

    // initialize `defs`
    for (INode<BasicBlock, Function> bbNode : func.getList_()) {
      BasicBlock bb = bbNode.getVal();
      for (INode<Instruction, BasicBlock> instNode : bb.getList()) {
        Instruction inst = instNode.getVal();
        if (inst.tag == TAG_.Alloca) {
          AllocaInst allocaInst = (AllocaInst) inst;
          // only local int
          if (allocaInst.getAllocatedType().equals(factory.getI32Ty())) {
//            defs.put(allocaInst, new ArrayList<>());
            allocas.add(allocaInst);
            allocaLookup.put(allocaInst, allocas.size() - 1);
            defBlocks.set(allocas.size() - 1, new ArrayList<>());
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
            Phi phiInst = new Phi(TAG_.Phi, factory.getI32Ty(), 0, y);
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
    ArrayList<Value> values = new ArrayList<Value>();
    for (int i = 0; i < allocas.size(); i++) {
      values.set(i, null);
    }
    for (INode<BasicBlock, Function> bbNode : func.getList_()) {
      bbNode.getVal().setDirty(false);
    }

    Stack<RenameData> renameDataStack = new Stack<>();
    renameDataStack.push(new RenameData(func.getList_().getEntry().getVal(), null, values));
    while (!renameDataStack.isEmpty()) {
      RenameData data = renameDataStack.pop();
      if (data.bb.isDirty()) {
        continue;
      }
//      renamePass(data.bb, data.pred, data.values);
      for (INode<Instruction, BasicBlock> instNode : data.bb.getList()) {
        Instruction inst = instNode.getVal();
        // AllocaInst
        if (inst.tag == TAG_.Load) {
          LoadInst loadInst = (LoadInst) inst;
        }
        // StoreInst
        else if (inst.tag == TAG_.Store) {
          StoreInst storeInst = (StoreInst) inst;
        }
        // Phi
        else if (inst.tag == TAG_.Phi) {
          Phi phiInst = (Phi) inst;
        }
      }
    }
  }
}
