package pass.ir;

import ir.MyModule;
import pass.Pass;
import pass.Pass.IRPass;

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
  @Override
  public void run(MyModule m) {

  }


}
