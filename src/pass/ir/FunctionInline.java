package pass.ir;

import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.types.IntegerType;
import ir.values.Function;
import ir.values.instructions.Instruction;
import ir.values.instructions.TerminatorInst.CallInst;
import java.util.ArrayList;
import java.util.Arrays;
import pass.Pass;
import pass.Pass.IRPass;
import util.Mylogger;

public class FunctionInline implements IRPass {

  @Override
  public String getName() {
    return "simpleFuncInline";
  }

  /*
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
   *   1.
   *   2.
   * */
  public void hardInline() {

  }

  /*todo :
   *   1.参数替换（IntegerType直接新建个Alloca,PointerType 替换成对应指针的GEP）
   *   2.统一出口（新建个基本块，让被内联函数的所有ret出口都变成这个块，并且把这个块加一个无条件跳转到原本的下一条指令，相当于把一个块拆成三个）
   *   3.
   *   */
  public void inlineMe(Function f) {
    if (f.getCalleeList().isEmpty()) {
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


  }
}
