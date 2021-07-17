package ir;

import driver.Config;
import ir.types.FunctionType;
import ir.types.IntegerType;
import ir.types.PointerType;
import ir.types.Type;
import ir.types.Type.VoidType;
import ir.values.Function;
import ir.values.GlobalVariable;
import ir.values.instructions.Instruction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;
import util.IList;
import util.Mylogger;

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

  public void init() {
    Logger log = Mylogger.getLogger(MyModule.class);

    MyFactoryBuilder f = MyFactoryBuilder.getInstance();

    /**
     * lib IO functions
     * */
//    f.buildFunction("getint", f.getFuncTy(i32, new ArrayList<>()));
//    f.buildFunction("getch", f.getFuncTy(i32, new ArrayList<>()));
//
//    ArrayList<Type> getArray = new ArrayList<>();
//    getArray.add(f.getPointTy(i32));
//    f.buildFunction("getarray", f.getFuncTy(i32, getArray));
//
//    ArrayList<Type> putint = new ArrayList<>();
//    putint.add(i32);
//    f.buildFunction("putint", f.getFuncTy(f.getVoidTy(), putint));
//
//    ArrayList<Type> putch = new ArrayList<>();
//    putch.add(i32);
//    f.buildFunction("putch", f.getFuncTy(f.getVoidTy(), putch));
//
//    ArrayList<Type> putArray = new ArrayList<>();
//    putArray.add(i32);
//    putArray.add(f.getPointTy(i32));
//    f.buildFunction("putarray", f.getFuncTy(f.getVoidTy(), putArray));

    /**
     * lib timing functions
     * */
//    f.buildFunction("starttime", f.getFuncTy(f.getVoidTy(), new ArrayList<>()));
//    f.buildFunction("stoptime", f.getFuncTy(f.getVoidTy(), new ArrayList<>()));
    //todo multi threads func
    log.warning("built finished");
  }

  private MyModule() {
    __functions = new IList<>(this);
    __instructions = new HashMap<>();
    __globalVariables = new ArrayList<>();

  }

}
