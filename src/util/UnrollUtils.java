package util;

import ir.Loop;
import ir.MyFactoryBuilder;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.Value;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.MemInst;
import ir.values.instructions.MemInst.AllocaInst;
import ir.values.instructions.MemInst.Phi;
import ir.values.instructions.TerminatorInst;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;

public class UnrollUtils {

  private static final MyFactoryBuilder factory = MyFactoryBuilder.getInstance();
  // return a new instruction but uses the old inst operands
  public static Instruction copyInstruction(Instruction inst) {
    Instruction copy = null;
    var ops = inst.getOperands();
    if (inst instanceof BinaryInst) {
      copy = factory.getBinary(inst.tag, ops.get(0), ops.get(1));
    } else if (inst instanceof MemInst) {
      copy = switch (inst.tag) {
        case Alloca -> factory.getAlloca(((AllocaInst) inst).getAllocatedType());
        case Load -> factory.getLoad(inst.getType(), ops.get(0));
        case Store -> factory.getStore(ops.get(0), ops.get(1));
        case GEP -> factory.getGEP(ops.get(0),
            new ArrayList<>() {{
              for (int i = 1; i < ops.size(); i++) {
                add(ops.get(i));
              }
            }});
        case Zext -> factory.getZext(ops.get(0));
        case Phi -> new Phi(inst.tag, inst.getType(), inst.getNumOP(), inst.getOperands());
        default -> throw new RuntimeException();
      };
    } else if (inst instanceof TerminatorInst) {
      //      assert inst.tag != TAG_.Call;
      switch (inst.tag) {
        case Br -> {
          if (ops.size() == 3) {
            copy = factory.getBr(ops.get(0), (BasicBlock) ops.get(1), (BasicBlock) ops.get(2));
          }
          if (ops.size() == 1) {
            copy = factory.getBr((BasicBlock) ops.get(0));
          }
        }
        case Ret -> {
          if (ops.size() == 1) {
            copy = factory.getRet(ops.get(0));
          } else {
            copy = factory.getRet();
          }
        }
        case Call -> {
          copy = factory.getFuncCall((Function) ops.get(0), new ArrayList<>() {{
            for (int i = 1; i < ops.size(); i++) {
              add(ops.get(i));
            }
          }});
        }
      }
    }

    if (copy == null) {
      throw new RuntimeException();
    }
    return copy;
  }

  public static BasicBlock getLoopBasicBlockCopy(BasicBlock source, HashMap<Value, Value> vMap) {
    var func = source.getParent();
    var target = factory.buildBasicBlock("", func);
    source.getList().forEach(instNode -> {
      var inst = instNode.getVal();
      var copyInst = copyInstruction(inst);
      vMap.put(inst, copyInst);
      copyInst.node.insertAtEnd(target.getList());
    });

    return target;
  }

  public static void remapInstruction(Instruction inst, HashMap<Value, Value> vMap) {
    for (int i = 0; i < inst.getOperands().size(); i++) {
      var op = inst.getOperands().get(i);
      if (vMap.containsKey(op) && !op.isFunction()) {
        var newOp = vMap.get(op);
        inst.CoReplaceOperandByIndex(i, newOp);
      }
    }
  }

  public static void addLoopToQueue(Loop loop, Queue<Loop> queue) {
    for (var subLoop : loop.getSubLoops()) {
      if (subLoop != null) {
        addLoopToQueue(subLoop, queue);
      }
    }
    queue.add(loop);
  }

  public static void rearrangeBBOrder(Loop loop) {
    for (var bb : loop.getBlocks()) {
      bb.setDirty(false);
    }
    var header = loop.getLoopHeader();
    ArrayList<BasicBlock> tmp = new ArrayList<>();
    header.setDirty(true);
    rearrangeDFS(header, loop, tmp);
    loop.getBlocks().clear();
    loop.getBlocks().addAll(tmp);
  }

  public static void rearrangeDFS(BasicBlock curr, Loop loop, ArrayList<BasicBlock> tmp) {
    tmp.add(curr);
    for (var bb : curr.getSuccessor_()) {
      if (loop.getBlocks().contains(bb)) {
        if (!bb.isDirty()) {
          bb.setDirty(true);
          rearrangeDFS(bb, loop, tmp);
        }
      }
    }
  }
}
