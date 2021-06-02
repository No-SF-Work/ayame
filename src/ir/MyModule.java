package ir;

import ir.values.Function;
import ir.values.GlobalVariable;
import ir.values.instructions.Instruction;

import java.util.ArrayList;
import java.util.HashMap;
import util.IList;

/**
 * IR结构中的顶层container,存有函数，全局变量，符号表，指令的list以及其他所有需要的信息 由于SysY只需要支持单文件编译，所以Module事实上是以单例存在的
 */
public class MyModule {

  public static MyModule getInstance() {
    return myModule;
  }

  public ArrayList<GlobalVariable> __globalVariables;
  public IList<Function, MyModule> __functions;
  public HashMap<Integer, Instruction> __instructions;
  private static final MyModule myModule = new MyModule();
  private MyModule() {
    __functions = new IList<>(this);
    __instructions = new HashMap<>();
    __globalVariables = new ArrayList<>();
  }

}
