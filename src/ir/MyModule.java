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
    IntegerType i32Type = f.getI32Ty();
    IntegerType i1Type = f.getI1Ty();
    VoidType voidType = f.getVoidTy();
    PointerType ptri32Type = f.getPointTy(i32Type);

    log.warning("begin to build builtin functions");
    ArrayList<Type> params_empty = new ArrayList<>(Collections.emptyList());
    ArrayList<Type> params_int = new ArrayList<>(Collections.singletonList(i32Type));
    ArrayList<Type> params_array = new ArrayList<>(Collections.singletonList(ptri32Type));
    ArrayList<Type> params_int_and_array = new ArrayList<>(Arrays.asList(i32Type, ptri32Type));
    // TODO what about putf(string, int, ...) ?
    ArrayList<Type> params_putf = new ArrayList<>(Collections.emptyList());

    f.buildFunction("getint", f.getFuncTy(i32Type, params_empty), true);
    f.buildFunction("getch", f.getFuncTy(i32Type, params_empty), true);
    f.buildFunction("getarray", f.getFuncTy(i32Type, params_array), true);
    f.buildFunction("putint", f.getFuncTy(voidType, params_int), true);
    f.buildFunction("putch", f.getFuncTy(voidType, params_int), true);
    f.buildFunction("putarray", f.getFuncTy(voidType, params_int_and_array), true);
    f.buildFunction("putf", f.getFuncTy(voidType, params_putf), true);

    f.buildFunction("starttime", f.getFuncTy(voidType, params_empty), true);
    f.buildFunction("stoptime", f.getFuncTy(voidType, params_empty), true);

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
