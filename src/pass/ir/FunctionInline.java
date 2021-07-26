package pass.ir;

import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.Use;
import ir.types.IntegerType;
import ir.values.BasicBlock;
import ir.values.Constant;
import ir.values.Function;
import ir.values.Function.Arg;
import ir.values.User;
import ir.values.Value;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.MemInst;
import ir.values.instructions.MemInst.AllocaInst;
import ir.values.instructions.MemInst.GEPInst;
import ir.values.instructions.TerminatorInst;
import ir.values.instructions.TerminatorInst.BrInst;
import ir.values.instructions.TerminatorInst.CallInst;
import ir.values.instructions.TerminatorInst.RetInst;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;
import pass.Pass;
import pass.Pass.IRPass;
import util.IList;
import util.IList.INode;
import util.Mylogger;

public class FunctionInline implements IRPass {

  @Override
  public String getName() {
    return "funcinline";
  }

  /*
   * warning: 运行完这个pass以后需要重新进行 bbpred succ 以及 interval analysis
   * 这个pass 遍历basicblock使用的顺序是bbnode出现的顺序，这在未进行其他的结构变化的时候是正确的，
   * 但是如果有别的方法修改了func的ilist，并且没有保持拓扑排序，那么这个变化就是错误的
   * todo :
   *  1.计算函数间调用关系图，dfs找到端点函数并且往回内联
   *  2.挑出不在调用关系中的强联通分量中的函数,将其内联至caller
   *  3.替换函数参数(int 类型的声明新值，array类型的替换引用(alloca **int -> gep or others))
   *  4.构建新基本块(函数的多个出口)
   *
   * */
  private MyFactoryBuilder factory = MyFactoryBuilder.getInstance();
  private MyModule m;
  private boolean changed = false;

  @Override
  public void run(MyModule m) {
    this.m = m;
    simpleInline();
  }

  /**
   * 在simpleInline 运行结束后，调用图中应该只剩下了main函数以及各个强联通分量
   */
  public void simpleInline() {
    ArrayList<Function> tobeProcessed = new ArrayList<>();
    changed = true;
    while (changed) {
      changed = false;
      m.__functions.forEach(funcNode -> {
        var val = funcNode.getVal();
        if (!val.isBuiltin_() && !val.getName().equals("main")) {
          //只要caller和callee没有交集，就可以把这个func inline到parent里
          if (val.getCalleeList().stream().distinct()
              .noneMatch(x -> val.getCallerList().stream().anyMatch(y -> equals(x)))) {
            tobeProcessed.add(val);
          }
        }
      });
      tobeProcessed.forEach(this::inlineMe);
      tobeProcessed.clear();
    }
  }

  /*todo:
   *   1.将递归函数展开一定次数
   * */
  public void hardInline() {

  }

  /*todo :
   *   1.参数替换（IntegerType直接新建个Alloca,PointerType 替换成对应指针的GEP）
   *   2.统一出口（新建个基本块，让被内联函数的所有ret出口都变成这个块，并且把这个块加一个无条件跳转到原本的下一条指令，相当于把一个块拆成三个）
   *   3.
   *   */
  private void inlineMe(Function f) {
    if (f.getCallerList().isEmpty()) {
      return;
    }
    changed = true;
    ArrayList<Instruction> toBeReplaced = new ArrayList<>();
    //dfs找到需要替换的call指令，不原地替换了
    for (Function caller : f.getCallerList().stream().distinct().collect(Collectors.toList())) {
      for (INode<BasicBlock, Function> bbnode : caller.getList_()) {
        for (INode<Instruction, BasicBlock> instNode : bbnode.getVal().getList()) {
          var inst = instNode.getVal();
          if (inst instanceof CallInst) {
            if (((CallInst) inst).getFunc().getName().equals(f.getName())) {
              toBeReplaced.add(inst);
            }
          }
        }
      }
    }
    toBeReplaced.forEach(inst -> inlineOneCall((CallInst) inst));
    f.getCallerList().forEach(list -> {
      //如果一个函数的caller和callee没有交集，那么其一定可以被内联到caller中
      list.getCalleeList().removeIf(i -> i.equals(f));
      list.getCalleeList().addAll(f.getCalleeList());
    });
    f.getCallerList().clear();
  }

  private void inlineOneCall(CallInst call) {
    var retType = call.getType();
    var originFunc = call.getBB().getParent();
    var arrive = factory.getBasicBlock("");
    var copy = getFunctionCopy((Function) call.getOperands().get(0));
    var originBB = call.getBB();
    arrive.node_.insertAfter(originBB.node_);
    //在call指令前面插入一个到目标函数的entry的跳转
    var br2entry = factory.getBr(copy.getList_().getEntry().getVal());
    var tmp = call.node.getNext();
    var funcArgs = copy.getArgList();
    //取出call指令后面的所有指令，放到arrive块中
    ArrayList<Instruction> toBeMoved = new ArrayList<>();
    while (tmp != null) {
      toBeMoved.add(tmp.getVal());
      tmp = tmp.getNext();
    }
    for (Instruction val : toBeMoved) {
      val.node.removeSelf().insertAtEnd(arrive.list_);
    }
//    将call从原块中取出
    call.node.removeSelf();
    for (int i = 0; i < funcArgs.size(); i++) {
      var tmparg = funcArgs.get(i);
      var callerArg = call.getOperands().get(i + 1);
      if (callerArg.getType().isI32()) {
        //pass value
        var alloca = factory.buildAlloca(originBB, factory.getI32Ty());
        factory.buildStore(callerArg, alloca, originBB);
        var load = factory.buildLoad(factory.getI32Ty(), alloca, originBB);
        tmparg.COReplaceAllUseWith(load);
      } else {

        tmparg.COReplaceAllUseWith(callerArg);
      }
    }
    br2entry.node.insertAtEnd(originBB.getList());
    //将目标函数中对arg的使用替换为call指令中对对应元素的使用
    //根据返回类型设置返回逻辑
    if (retType.isI32()) {
      //将对call的返回值的使用替换为对一个alloca的使用
      //将ret替换为对alloca的store
      //在arrive的开头插入一个对alloca的load将对ret值的使用替换为对load的使用
      var alloca = factory.buildAlloca(originBB, IntegerType.getI32());
      var load = factory.getLoad(factory.getI32Ty(), alloca);
      load.node.insertAtEntry(arrive.list_);
      call.COReplaceAllUseWith(load);

      ArrayList<RetInst> rets = new ArrayList<>();
      copy.getList_().forEach(bbNode -> {
        bbNode.getVal().getList().forEach(instNode -> {
          if (instNode.getVal() instanceof RetInst) {
            rets.add((RetInst) instNode.getVal());
          }
        });
      });
      rets.forEach(ret -> {
        var tmpstore = factory.getStore(ret.getOperands().get(0), alloca);
        ret.CORemoveAllOperand();
        ret.COReplaceAllUseWith(alloca);
        var tmpBr = factory.getBr(arrive);
        tmpstore.node.insertBefore(ret.node);
        tmpBr.node.insertBefore(ret.node);
        ret.node.removeSelf();
      });
    }
    if (retType.isVoidTy()) {
      ArrayList<RetInst> rets = new ArrayList<>();
      copy.getList_().forEach(bbNode -> {
        bbNode.getVal().getList().forEach(instNode -> {
          if (instNode.getVal() instanceof RetInst) {
            rets.add((RetInst) instNode.getVal());
          }
        });
      });
      rets.forEach(ret -> {
        var tmpBr = factory.getBr(arrive);
        tmpBr.node.insertBefore(ret.node);
        ret.node.removeSelf();
      });
    }
    ArrayList<BasicBlock> toBeMovedBBs = new ArrayList<>();
    copy.getList_().forEach(node -> {
      toBeMovedBBs.add(node.getVal());
    });
    toBeMovedBBs.forEach(bb -> {
      bb.node_.removeSelf();
      bb.node_.insertBefore(arrive.node_);
    });
    //将所有alloca前提
    ArrayList<Instruction> allocas = new ArrayList<>();
    originFunc.getList_().forEach(bb -> bb.getVal().getList().forEach(inst -> {
      if (inst.getVal() instanceof AllocaInst) {
        allocas.add(inst.getVal());
      }

    }));
    allocas.forEach(a -> {
      a.node.removeSelf();
      a.node.insertAtEntry(originFunc.getList_().getEntry().getVal().list_);
    });
    call.CORemoveAllOperand();
  }


  //因为使用的是双向侵入链表，对value的copy会有点复杂
  private Function getFunctionCopy(Function source) {
    valueMap.clear();
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
    //基于这么一个假设：function的Ilist中的bb是按照拓扑排序排列的，如果后续发现出现问题，我会把这个遍历改为bfs
    for (INode<BasicBlock, Function> bbNode : source.getList_()) {
      processBasicblock(bbNode.getVal(), (BasicBlock) valueMap.get(bbNode.getVal()));
    }
    return copy;
  }

  private void processBasicblock(BasicBlock source, BasicBlock target) {
    source.getList().forEach(node -> {
      var copy = getInstCopy(node.getVal());
      valueMap.put(node.getVal(), copy);
      copy.node.insertAtEnd(target.getList());
    });
  }

  private Instruction getInstCopy(Instruction instruction) {
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
    return copy;
  }

  /*private void copyOperands(User dest, User from) {
    dest.CORemoveAllOperand();
  }
*/
  private Value findValue(Value val) {
    if (val instanceof Constant) {
      return val;
    } else {
      assert valueMap.get(val) != null;
      if (valueMap.get(val) == null) {
        throw new RuntimeException();
      }
      return valueMap.get(val);
    }
  }

  //cast origin Value to copied value
  private HashMap<Value, Value> valueMap = new HashMap<>();
}


