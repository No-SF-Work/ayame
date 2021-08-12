package pass.ir;

import ir.MyModule;
import ir.values.Function;
import ir.values.Value;
import java.util.ArrayList;
import java.util.HashSet;
import pass.Pass.IRPass;

public class InterProceduralDCE implements IRPass {


  @Override
  public String getName() {
    return "interproceduraldce";
  }

  /*
   * todo
   *  这个pass需要在mem2reg之前调用
   *   1.跨过程的数据流好麻啊,失败了失败了失败了失败了失败了
   *   2.如果一个函数的返回值实际上没用，那么标记这个函数，并且把其所有返回值更换为CONST0
   *     2.1:假设：使用了与输出的BUILTIN函数相关的函数的与被BUILTIN函数使用的所有值都是不可删除的
   *     2.3:跑这个之前跑一次inter procedural ana与bb pred succ
   *     2.4:如果一个函数的所有返回值在所有地方都没有用到（递归函数的情况要进行特判），就把这个函数的所有返回值替换为Const0并且接触Ret的关系
   *     2.5:对每个gv判断其在每个函数里面是否有用，如果在所有函数里面都没用，就把这个gv删除
   * */
  MyModule m;
  HashSet<Function> funcs = new HashSet<>();//输出函数
  HashSet<Function> needtokeep = new HashSet<>();
  HashSet<Value> cd = new HashSet<>();//can't be deleted
  HashSet<Value> useless = new HashSet<>();//useless Value

  @Override
  public void run(MyModule m) {
    this.m = m;
    m.__functions.forEach(
        node -> {
          var name = node.getVal().getName();
          if (name.equals("putch") || name.equals("putarray") || name.equals("putint")) {
            funcs.add(node.getVal());
            cd.add(node.getVal());
          }
          if (name.equals("getint")||name.equals("getch")||name.equals("getarray"));
        }
    );
  }
}
