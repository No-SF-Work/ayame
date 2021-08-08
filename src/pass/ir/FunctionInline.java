package pass.ir;

import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.values.BasicBlock;
import ir.values.Constant;
import ir.values.Function;
import ir.values.Value;
import ir.values.ValueCloner;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst;
import ir.values.instructions.MemInst.AllocaInst;
import ir.values.instructions.MemInst.LoadInst;
import ir.values.instructions.MemInst.Phi;
import ir.values.instructions.TerminatorInst;
import ir.values.instructions.TerminatorInst.CallInst;
import ir.values.instructions.TerminatorInst.RetInst;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import pass.Pass.IRPass;
import util.IList.INode;

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
          ArrayList<Function> noBuiltInfuncs = new ArrayList<>();
          val.getCalleeList().forEach(func -> {
            if (!func.isBuiltin_()) {
              noBuiltInfuncs.add(func);
            }
          });
          if (noBuiltInfuncs.isEmpty()) {
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
    var copy = new ValueCloner() {
      @Override
      public Value findValue(Value val) {
        if (val instanceof Constant) {
          return val;
        } else {
          if (this.valueMap.get(val) == null) {
            throw new RuntimeException();
          }
          return this.valueMap.get(val);
        }
      }
    }.getFunctionCopy((Function) call.getOperands().get(0));
    var originBB = call.getBB();
    arrive.node_.insertAfter(originBB.node_);
    //在call指令前面插入一个到目标函数的entry的跳转
    var br2entry = factory.getBr(copy.getList_().getEntry().getVal());
    var funcArgs = copy.getArgList();
    var tmp = call.node.getNext();
    ArrayList<BasicBlock> originSuccStore = new ArrayList<>();
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
        tmparg.COReplaceAllUseWith(callerArg);
      } else {
        /* todo:
            1.找到arg对应的alloca
            2.找到这个alloca的load
            3.把这些个load换成对callerarg的使用（不是RAU,load的是）
        * */
        tmparg.getUsesList().forEach(
            use -> {
              if (use.getUser() instanceof LoadInst){
                use.getUser().COReplaceAllUseWith(callerArg);
              }
            }
        );
        tmparg.COReplaceAllUseWith(callerArg);
      }
    }
    //维护originBB和funcEntry的前驱后继关系 fixme
    br2entry.node.insertAtEnd(originBB.getList());
    arrive.getSuccessor_().addAll(originBB.getSuccessor_());
    arrive.getSuccessor_().forEach(bb -> {
      for (int i = 0; i < bb.getPredecessor_().size(); i++) {
        if (bb.getPredecessor_().get(i).equals(originBB)) {
          bb.getPredecessor_().set(i, arrive);
        }
      }
    });
    originBB.getSuccessor_().clear();
    originBB.getSuccessor_().add(copy.getList_().getEntry().getVal());
    copy.getList_().getEntry().getVal().getPredecessor_().add(originBB);

    //将目标函数中对arg的使用替换为call指令中对对应元素的使用
    //根据返回类型设置返回逻辑
    if (retType.isI32()) {
      var phi = new Phi(TAG_.Phi, factory.getI32Ty(), 0, arrive);
      call.COReplaceAllUseWith(phi);

      ArrayList<RetInst> rets = new ArrayList<>();
      copy.getList_().forEach(bbNode -> {
        bbNode.getVal().getList().forEach(instNode -> {
          if (instNode.getVal() instanceof RetInst) {
            rets.add((RetInst) instNode.getVal());
          }
        });
      });
      rets.forEach(ret -> {
        //var tmpstore = factory.getStore(ret.getOperands().get(0), alloca);
        phi.COaddOperand(ret.getOperands().get(0));
        arrive.getPredecessor_().add(ret.getBB());
        ret.getBB().getSuccessor_().add(arrive);
        ret.CORemoveAllOperand();
        var tmpBr = factory.getBr(arrive);
        //tmpstore.node.insertBefore(ret.node);
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
        arrive.getPredecessor_().add(ret.getBB());
        ret.getBB().getSuccessor_().add(arrive);
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


}


