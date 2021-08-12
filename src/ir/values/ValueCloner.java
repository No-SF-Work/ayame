package ir.values;

import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.MemInst;
import ir.values.instructions.MemInst.AllocaInst;
import ir.values.instructions.MemInst.Phi;
import ir.values.instructions.TerminatorInst;
import java.util.ArrayList;
import java.util.HashMap;
import util.IList.INode;
import ir.MyFactoryBuilder;

//因为SSA形式特殊的User Value关系，对对象的直接clone并不能满足我们的需求
//而我们又需要在多个地方对多个Value进行复杂的clone
//所以实现了这个类
//不同的地方对取出的Value的需求是不同的，在这里用对findValue的重写来区分
public abstract class ValueCloner {

  public ValueCloner() {
  }

  HashMap<BasicBlock, Boolean> visitMap = new HashMap<>();
  protected HashMap<Value, Value> valueMap = new HashMap<>();
  private MyFactoryBuilder factory = MyFactoryBuilder.getInstance();

  public void put(Value key, Value value) {
    valueMap.put(key, value);
  }

  public abstract Value findValue(Value value);

  //fixme FuncCopy在Visitor结束前都不能调用，否则一定会出错
  public Function getFunctionCopy(Function source) {
    valueMap.clear();
    var m = source.getNode().getParent().getVal();
    m.__globalVariables.forEach(gv -> {
      valueMap.put(gv, gv);
    });
    var copy = factory.getFunction("", source.getType());//只要body,不要function的head
    //初始化所有的bb,并且放到valueMap里面（由于Br指令的存在，basicBlock的对象需要在初始化之前就存在）
    var sourceArgs = source.getArgList();
    var copyArgs = copy.getArgList();
    for (int i = 0; i < copy.getArgList().size(); i++) {
      valueMap.put(sourceArgs.get(i), copyArgs.get(i));
    }
    for (INode<BasicBlock, Function> bbNode : source.getList_()) {
      valueMap.put(bbNode.getVal(), factory.buildBasicBlock("", copy));
    }
    source.getList_().forEach(bbnode -> {
      var val = bbnode.getVal();
      BasicBlock copyBB = (BasicBlock) findValue(val);
      //复制predecessor和successor，用于生成phi指令
      for (BasicBlock basicBlock : val.getPredecessor_()) {
        copyBB.getPredecessor_().add((BasicBlock) findValue(basicBlock));
      }
      for (BasicBlock basicBlock : val.getSuccessor_()) {
        copyBB.getSuccessor_().add((BasicBlock) findValue(basicBlock));
      }
    });
    //基于这么一个假设：function的Ilist中的bb是按照拓扑排序排列的，如果后续发现出现问题，我会把这个遍历改为bfs
    BasicBlock root = source.getList_().getEntry().getVal();
    bbProcessor(root);
    visitMap.clear();
    ArrayList<Phi> phis = new ArrayList<>();
    source.getList_().forEach(
        bblist -> {
          bblist.getVal().getList().forEach(instnode -> {
            if (instnode.getVal() instanceof Phi) {
              phis.add((Phi) instnode.getVal());
            }
          });
        }
    );
    for (Phi phi : phis) {
      for (int i = 0; i < ((Phi) phi).getIncomingVals().size(); i++) {
        ((Phi) findValue(phi)).setIncomingVals(i, findValue(phi.getIncomingVals().get(i)));
      }
    }
    return copy;
  }


  private void bbProcessor(BasicBlock bb) {
    processBasicblock(bb, (BasicBlock) valueMap.get(bb));
     if (!bb.getSuccessor_().isEmpty()) {
      bb.getSuccessor_().stream().distinct().forEach(b -> {
        if (visitMap.get(b) == null) {
          visitMap.put(b, true);
          bbProcessor(b);
        }
      });
    }
  }


  private void processBasicblock(BasicBlock source, BasicBlock target) {
    source.getList().forEach(node -> {
      var copy = getInstCopy(node.getVal());
      valueMap.put(node.getVal(), copy);
      copy.node.insertAtEnd(target.getList());
    });
  }
  //对Phi指令的Copy只copy了phi本身，不会对IncomingVals copy，需要自己想办法维护
  public Instruction getInstCopy(Instruction instruction) {
    Instruction copy = null;
    var ops = instruction.getOperands();
    if (instruction instanceof BinaryInst) {
      copy = factory.getBinary(instruction.tag, findValue(ops.get(0)), findValue(ops.get(1)));
    }
    if (instruction instanceof MemInst) {
      copy = switch (instruction.tag) {
        case Alloca -> factory.getAlloca(((AllocaInst) instruction).getAllocatedType());
        case Load -> factory.getLoad(instruction.getType(), findValue(ops.get(0)));
        case Store -> factory.getStore(findValue(ops.get(0)), findValue(ops.get(1)));
        case GEP -> factory.getGEP(findValue(ops.get(0)),
            new ArrayList<>() {{
              for (int i = 1; i < ops.size(); i++) {
                add(findValue(ops.get(i)));
              }
            }});
        case Zext -> factory.getZext(findValue(ops.get(0)));
        case Phi -> new Phi(instruction.tag, instruction.getType(), instruction.getNumOP());
        default -> throw new RuntimeException();
      };
    }

    if (instruction instanceof TerminatorInst) {
      switch (instruction.tag) {
        case Br -> {
          if (ops.size() == 3) {
            copy = factory.getBr(findValue(ops.get(0)), (BasicBlock) findValue(ops.get(1)),
                (BasicBlock) findValue(ops.get(2)));
          }
          if (ops.size() == 1) {
            copy = factory.getBr((BasicBlock) findValue(ops.get(0)));
          }
        }
        case Call -> {
          copy = factory.getFuncCall((Function) ops.get(0), new ArrayList<>() {{
            for (int i = 1; i < ops.size(); i++) {
              add(findValue(ops.get(i)));
            }
          }});
        }
        case Ret -> {
          if (ops.size() == 1) {
            copy = factory.getRet(findValue(ops.get(0)));
          } else {
            copy = factory.getRet();
          }
        }
      }
    }
    if (copy == null) {
      throw new RuntimeException();
    }
    return copy;
  }

}
