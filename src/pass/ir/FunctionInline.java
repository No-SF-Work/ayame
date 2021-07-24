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
import ir.values.instructions.TerminatorInst.CallInst;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import pass.Pass;
import pass.Pass.IRPass;
import util.IList;
import util.IList.INode;
import util.Mylogger;

public class FunctionInline implements IRPass {

  @Override
  public String getName() {
    return "simpleFuncInline";
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
    while (changed) {
      changed = false;
      tobeProcessed.clear();
      m.__functions.forEach(funcNode -> {
        var val = funcNode.getVal();
        if (!val.isBuiltin_() && val.getCalleeList().isEmpty()) {
          tobeProcessed.add(val);
        }
      });
      tobeProcessed.forEach(this::inlineMe);
    }
  }

  /*todo:
   *   1.将强联通分量并为一个函数
   *   2.将这个函数展开预先写好的次数
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
    f.getCallerList().forEach(caller -> {
      caller.getList_().forEach(bbnode -> {
        bbnode.getVal().getList().forEach(instNode -> {
          var inst = instNode.getVal();
          if (inst instanceof CallInst) {
            if (((CallInst) inst).getFunc().getName().equals(f.getName())) {
              toBeReplaced.add(inst);
            }
          }
        });
      });
    });
    toBeReplaced.forEach(inst -> {
      inlineOneCall((CallInst) inst);
    });
  }

  private void inlineOneCall(CallInst call) {

    var arrive = factory.buildBasicBlock("", call.getBB().getParent());
    factory.buildBasicBlock("", call.getBB().getParent());

  }

  private void replaceAllRet() {
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
      getInstCopy(node.getVal()).node.insertAtEnd(target.getList());
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
      return valueMap.get(val);
    }
  }

  //cast origin Value to copied value
  private HashMap<Value, Value> valueMap = new HashMap<>();


}


