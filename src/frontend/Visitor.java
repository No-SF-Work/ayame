package frontend;


import driver.Config;
import frontend.SysYParser.*;
import ir.MyFactoryBuilder;
import ir.types.ArrayType;
import ir.types.FunctionType;
import ir.types.IntegerType;
import ir.types.PointerType;
import ir.types.Type;
import ir.types.Type.VoidType;
import ir.values.BasicBlock;
import ir.values.Constant;
import ir.values.Constants.ConstantArray;
import ir.values.Constants.ConstantInt;
import ir.values.Function;
import ir.values.Value;
import ir.values.ValueCloner;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.TerminatorInst.BrInst;
import ir.values.instructions.TerminatorInst.RetInst;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import ir.values.instructions.Instruction;
import util.Mylogger;

/**
 * 我们并不需要用返回值传递信息，所以将类型标注为Void
 */

public class Visitor extends SysYBaseVisitor<Void> {

  Logger log = Mylogger.getLogger(Visitor.class);


  private class Scope {

    Scope() {
      tables_ = new ArrayList<>();
      tables_.add(new HashMap<>());
    }

    //因为涉及了往上层查找参数，所以这里不用stack用arraylist
    private final ArrayList<HashMap<String, Value>> tables_;

    private HashMap<String, Value> top() {
      return tables_.get(tables_.size() - 1);
    }

    public Value find(String name) {
      for (int i = tables_.size() - 1; i >= 0; i--) {
        Value t = tables_.get(i).get(name);
        if (t != null) {
          return t;
        }
      }
      return null;
    }

    public void put(String name, Value v) {
      if (top().get(name) != null) {
        throw new SyntaxException("name already exists");
      } else {
        top().put(name, v);
      }
    }

    public boolean preEnter = false;

    public void addLayer() {
      if (preEnter) {
        preEnter = false;
        return;
      }
      tables_.add(new HashMap<>());

    }

    public void popLayer() {
      tables_.remove(tables_.size() - 1);
    }

    public boolean isGlobal() {
      return this.tables_.size() == 1;
    }
  }

  // 不用捕获，程序出错语义肯定出问题了
  private class SyntaxException extends RuntimeException {

    SyntaxException(String msg) {
      log.severe(msg);
    }
  }

  private void changeFunc(Function f) {
    curFunc_ = f;
  }

  private void changeBB(BasicBlock b) {
    curBB_ = b;
  }

  // translation context
  private final MyFactoryBuilder f = MyFactoryBuilder.getInstance();
  private final Scope scope_ = new Scope(); // symbol table
  private BasicBlock curBB_; // current basicBlock
  private Function curFunc_; // current function
  // pass values between `visit` functions
  private ArrayList<Value> tmpArr_;//只允许赋值以及被赋值，不能直接操作
  private Value tmp_;
  private int tmpInt_;
  private Type tmpTy_;
  private ArrayList<Type> tmpTyArr;
  // singleton variables
  private final ConstantInt CONST0 = ConstantInt.CONST0();
  private final Type i32Type_ = f.getI32Ty();
  private final Type voidType_ = f.getVoidTy();
  //status word
  private boolean usingInt_ = false;//常量初始化要对表达式求值，并且用的Ident也要是常量
  private boolean globalInit_ = false;
  private boolean buildCall = false;
  private boolean expInRel = false;
  private boolean isO2 = Config.getInstance().isO2;

  /**
   * program : compUnit ;
   * <p>
   * 初始化 module，定义内置函数
   */
  @Override
  public Void visitProgram(ProgramContext ctx) {
    log.info("Syntax begin");
    IntegerType i32Type = f.getI32Ty();
    VoidType voidType = f.getVoidTy();
    PointerType ptri32Type = f.getPointTy(i32Type);

    ArrayList<Type> params_empty = new ArrayList<>(Collections.emptyList());
    ArrayList<Type> params_int = new ArrayList<>(Collections.singletonList(i32Type));
    ArrayList<Type> params_array = new ArrayList<>(Collections.singletonList(ptri32Type));
    ArrayList<Type> params_int_and_array = new ArrayList<>(Arrays.asList(i32Type, ptri32Type));
    // TODO what about putf(string, int, ...) ?
    ArrayList<Type> params_putf = new ArrayList<>(Collections.emptyList());
    ArrayList<Type> param_memset = new ArrayList<>(Arrays.asList(ptri32Type, i32Type, i32Type));

    //libc function
    scope_.put("memset", f.buildFunction("memset", f.getFuncTy(voidType, param_memset), true));

    //sysy Lib function
    scope_.put("getint", f.buildFunction("getint", f.getFuncTy(i32Type, params_empty), true));
    scope_.put("getarray", f.buildFunction("getarray", f.getFuncTy(i32Type, params_array), true));
    scope_.put("getch", f.buildFunction("getch", f.getFuncTy(i32Type, params_empty), true));
    scope_.put("putint", f.buildFunction("putint", f.getFuncTy(voidType, params_int), true));
    scope_.put("putch", f.buildFunction("putch", f.getFuncTy(voidType, params_int), true));
    scope_.put("putarray",
        f.buildFunction("putarray", f.getFuncTy(voidType, params_int_and_array), true));
    scope_.put("putf", f.buildFunction("putf", f.getFuncTy(voidType, params_putf), true));
    scope_.put("starttime",
        f.buildFunction("_sysy_starttime", f.getFuncTy(voidType, params_empty), true));
    scope_
        .put("stoptime",
            f.buildFunction("_sysy_stoptime", f.getFuncTy(voidType, params_empty), true));

    return super.visitProgram(ctx);
  }

  /**
   * compUnit : (funcDef | decl)+ ;
   * <p>
   * 在 visit funcDef/constDecl/varDecl 时再进行翻译
   */
  @Override
  public Void visitCompUnit(CompUnitContext ctx) {
    ctx.children.forEach(this::visit);
    return null;
  }

  /**
   * decl : constDecl | varDecl ;
   */
  @Override
  public Void visitDecl(DeclContext ctx) {
    ctx.children.forEach(this::visit);
    return null;
  }

  /**
   * constDecl : CONST_KW bType constDef (COMMA constDef)* SEMICOLON ;
   */

  @Override
  public Void visitConstDecl(ConstDeclContext ctx) {
    ctx.constDef().forEach(this::visitConstDef);
    return null;
  }

  //把一堆Constant封装为一个按照dims排列的ConstArr
  public Constant genConstArr(ArrayList<Integer> dims, ArrayList<Value> inits) {
    var curDimLength = dims.get(0);
    var curDimArr = new ArrayList<Constant>();
    var length = inits.size() / curDimLength;
    var arrTy = i32Type_;
    if (length == 1) {
      for (int i = 0; i < curDimLength; i++) {
        curDimArr.add((Constant) inits.get(i));
      }
    } else {
      for (int i = 0; i < curDimLength; i++) {
        //fix subDims and add to curDimArr
        curDimArr.add(
            genConstArr(
                new ArrayList<>(dims.subList(1, dims.size())),
                new ArrayList<>(inits.subList(length * i, length * (i + 1)))));

      }
    }

    for (int i = dims.size(); i > 0; i--) {
      arrTy = f.getArrayTy(arrTy, dims.get(i - 1));
    }
    return f.getConstantArray(arrTy, curDimArr);
  }

  /**
   * constDef : IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN constInitVal ;
   */
  @Override
  public Void visitConstDef(ConstDefContext ctx) {
    var name = ctx.IDENT().getText();
    log.info("visiting ConstDef name :" + name);
    if (scope_.top().get(name) != null) {
      throw new SyntaxException("name already exists");
    }
    if (ctx.constExp().isEmpty()) { //not array
      if (ctx.constInitVal() != null) {
        visit(ctx.constInitVal());
        scope_.put(ctx.IDENT().getText(), tmp_);
      }
    } else {// array
      //calculate dims of array
      var arrty = i32Type_;
      var dims = new ArrayList<Integer>();//ndims
      ctx.constExp().forEach(context -> {
        visit(context);
        dims.add(((ConstantInt) tmp_).getVal());
      });
      for (var i = dims.size(); i > 0; i--) {
        arrty = f.getArrayTy(arrty, dims.get(i - 1));// arr(arr(arr(i32,dim1),dim2),dim3)
      }
      if (scope_.isGlobal()) {
        if (ctx.constInitVal() != null) {
          ctx.constInitVal().dimInfo_ = dims;
          globalInit_ = true;
          visit(ctx.constInitVal());//dim.size()=n
          globalInit_ = false;
          var arr = tmpArr_;
          ArrayList<Constant> g = new ArrayList<>();
          arr.forEach(i -> g.add(((ConstantInt) i)));
          var plainInit = new ConstantArray(arrty, g);
          var foldedInit = genConstArr(dims, tmpArr_);
          var variable = f.getGlobalvariable(ctx.IDENT().getText(), arrty, foldedInit, plainInit);
          variable.setConst();
          scope_.put(ctx.IDENT().getText(), variable);
        } else {
          var variable = f.getGlobalvariable(ctx.IDENT().getText(), arrty, null, null);
          scope_.put(ctx.IDENT().getText(), variable);
        }
      } else {
        var allocatedArray = f
            .buildAlloca(curBB_, arrty);//alloca will be move to first bb in cur functioon
        scope_.put(ctx.IDENT().getText(), allocatedArray);
        if (ctx.constInitVal() != null) {
          allocatedArray.setInit();
          ctx.constInitVal().dimInfo_ = dims;
          visit(ctx.constInitVal());
          var arr = tmpArr_;
          // GEP只计算指针，不访问内存，因此每次需要访问内存的时候需要解出需要的内存的指针并且build新的GEP指令
          var ptr = f.buildGEP(allocatedArray, new ArrayList<>() {{
            add(CONST0);
            add(CONST0);
          }}, curBB_);
          for (int i = 1; i < ctx.constInitVal().dimInfo_.size(); i++) {
            ptr = f.buildGEP(ptr, new ArrayList<>() {{
              add(CONST0);
              add(CONST0);
            }}, curBB_);
          }
          for (int i = 0; i < arr.size(); i++) {
            if (i == 0) {
              //first value is just ptr
              f.buildStore(arr.get(i), ptr, curBB_);
            } else {
              int finalI = i;
              //build  GEP and store value
              var p = f.buildGEP(ptr, new ArrayList<>() {{
                add(ConstantInt.newOne(IntegerType.getI32(), finalI));
              }}, curBB_);
              f.buildStore(arr.get(i), p, curBB_);
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * constInitVal : constExp | (L_BRACE (constInitVal (COMMA constInitVal)*)? R_BRACE) ;
   */
  @Override
  public Void visitConstInitVal(ConstInitValContext ctx) {
    //ConstInitVal 和 数组结构一样，是嵌套的，逻辑是把每一层的初始值放进去，然后不足的补0
    if ((ctx.constExp() != null) && ctx.dimInfo_ == null) {
      visit(ctx.constExp());//非数组形式变量的初始化
    } else {
      var curDimLength = ctx.dimInfo_.get(0);
      var sizeOfEachEle = 1;//每个元素（i32或者是数组）的长度
      var arrOfCurDim = new ArrayList<Value>();//
      //calculate Size of Ele in cur dim
      for (int i = 1; i < ctx.dimInfo_.size(); i++) {
        sizeOfEachEle *= ctx.dimInfo_.get(i);
      }
      //recursively init each dim
      for (ConstInitValContext constInitValContext : ctx.constInitVal()) {
        if (constInitValContext.constExp() == null) {
          var pos = arrOfCurDim.size();
          for (int i = 0; i < (sizeOfEachEle - (pos % sizeOfEachEle)) % sizeOfEachEle; i++) {
            arrOfCurDim.add(CONST0);//长度不足一个ele的补0为一个ele长
          }
          constInitValContext.dimInfo_ = new ArrayList<>(
              ctx.dimInfo_.subList(1, ctx.dimInfo_.size()));
          visit(constInitValContext);
          arrOfCurDim.addAll(tmpArr_);
        } else {
          visit(constInitValContext);
          arrOfCurDim.add(tmp_);
        }
      }
      for (int i = arrOfCurDim.size(); i < curDimLength * sizeOfEachEle; i++) {
        arrOfCurDim.add(CONST0);
      }//长度不足一个ele*dimsize 的补0
      tmpArr_ = arrOfCurDim;
    }
    return null;
  }

  /**
   * varDecl : bType varDef (COMMA varDef)* SEICOLON ;
   */
  @Override
  public Void visitVarDecl(VarDeclContext ctx) {
    ctx.varDef().forEach(this::visit);
    return null;
  }

  /**
   * varDef : IDENT (L_BRACKT constExp R_BRACKT)* (ASSIGN initVal)? ;
   */
  @Override
  public Void visitVarDef(VarDefContext ctx) {
    var varName = ctx.IDENT().getText();
    if (scope_.top().get(varName) != null) {
      throw new SyntaxException("name already exists in cur scope");
    }

    if (ctx.constExp().isEmpty()) {
      if (scope_.isGlobal()) {    // 非数组全局变量
        if (ctx.initVal() != null) {
          //global变量的 initelement 只能是 constant
          globalInit_ = true;
          visit(ctx.initVal());
          globalInit_ = false;
          var initializer = (Constant) tmp_;
          var v = f.getGlobalvariable(varName, i32Type_, initializer, initializer);
          scope_.put(varName, v);
        } else {
          var initializer = CONST0;
          var v = f.getGlobalvariable(varName, i32Type_, initializer, initializer);
          scope_.put(varName, v);
        }
      } else {//非数组局部
        var allocator = f.buildAlloca(curBB_, i32Type_);
        scope_.put(varName, allocator);
        if (ctx.initVal() != null) {
          visit(ctx.initVal());
          f.buildStore(tmp_, allocator, curBB_);
        }
      }
    } else {//array
      var arrTy = i32Type_;
      var dims = new ArrayList<Integer>();
      ctx.constExp().forEach(context -> {
        visit(context);
        dims.add(((ConstantInt) tmp_).getVal());
      });
      for (var i = dims.size(); i > 0; i--) {
        arrTy = f.getArrayTy(arrTy, dims.get(i - 1));
      }
      if (scope_.isGlobal()) {
        if (!(ctx.initVal() == null)) {

          ctx.initVal().dimInfo_ = dims;
          globalInit_ = true;
          visit(ctx.initVal());
          globalInit_ = false;

          var arr = tmpArr_;
          ArrayList<Constant> g = new ArrayList<>();
          arr.forEach(i -> g.add(((ConstantInt) i)));
          var plainInit = new ConstantArray(arrTy, g);
          var fixedInit = genConstArr(dims, arr);
          var glo = f.getGlobalvariable(ctx.IDENT().getText(), arrTy, fixedInit, plainInit);
          scope_.put(ctx.IDENT().getText(), glo);
        } else {
          var v = f.getGlobalvariable(ctx.IDENT().getText(), arrTy, null, null);
          scope_.put(ctx.IDENT().getText(), v);
        }
      } else {//local arr init
        var alloc = f.buildAlloca(curBB_, arrTy);
        scope_.put(ctx.IDENT().getText(), alloc);
        if (!(ctx.initVal() == null) && !(ctx.initVal().initVal().isEmpty())) {
          alloc.setInit();
          ctx.initVal().dimInfo_ = dims;
          visit(ctx.initVal());
          var arr = tmpArr_;
          var pointer = f.buildGEP(alloc, new ArrayList<>() {{
            add(CONST0);
            add(CONST0);
          }}, curBB_);

          for (var i = 1; i < dims.size(); i++) {
            pointer = f.buildGEP(pointer, new ArrayList<>() {{
              add(CONST0);
              add(CONST0);
            }}, curBB_);
          }
          f.buildFuncCall((Function) scope_.find("memset"), new ArrayList<>(
                  Arrays.asList(pointer, CONST0, ConstantInt.newOne(i32Type_, arr.size() * 4))),
              curBB_);
          for (int i = 0; i < arr.size(); i++) {
            var t = arr.get(i);

            if (t instanceof ConstantInt) {
              if (((ConstantInt) t).getVal() == 0) {
//                f.buildStore(CONST0, pointer, curBB_);
                continue;
              }
            }
            if (i != 0) {
              int finalI = i;
              var ptr = f.buildGEP(pointer, new ArrayList<>() {{
                add(ConstantInt.newOne(i32Type_, finalI));
              }}, curBB_);
              f.buildStore(t, ptr, curBB_);
            } else {
              f.buildStore(t, pointer, curBB_);
            }
          }
        } else if (ctx.initVal() != null && ctx.initVal().initVal().isEmpty()) {//int a[4]={}
          var size = 1;
          for (Integer dim : dims) {
            size *= dim;
          }
          var pointer = f.buildGEP(alloc, new ArrayList<>() {{
            add(CONST0);
            add(CONST0);
          }}, curBB_);
          f.buildFuncCall((Function) scope_.find("memset"), new ArrayList<>(
                  Arrays.asList(pointer, CONST0, ConstantInt.newOne(i32Type_, size * 4))),
              curBB_);
          /*for (var i = 1; i < dims.size(); i++) {
            pointer = f.buildGEP(pointer, new ArrayList<>() {{
              add(CONST0);
              add(CONST0);
            }}, curBB_);
          }
          for (int i = 0; i < size; i++) {
            if (i != 0) {
              int finalI = i;
              var ptr = f.buildGEP(pointer, new ArrayList<>() {{
                add(ConstantInt.newOne(i32Type_, finalI));
              }}, curBB_);
              f.buildStore(CONST0, ptr, curBB_);
            } else {
              f.buildStore(CONST0, pointer, curBB_);
            }
          */
//        }
        }
      }
    }
    return null;
  }

  /**
   * initVal : exp | (L_BRACE (initVal (COMMA initVal)*)? R_BRACE) ;
   */
  @Override
  public Void visitInitVal(InitValContext ctx) {
    if (ctx.exp() != null && ctx.dimInfo_ == null) { //
      if (globalInit_) {
        usingInt_ = true;
        visit(ctx.exp());
        usingInt_ = false;
        tmp_ = ConstantInt.newOne(i32Type_, tmpInt_);
      } else {
        visit(ctx.exp());
      }
    } else {
      var curDimLength = ctx.dimInfo_.get(0);
      var sizeOfEachEle = 1;//每个元素（i32或者是数组）的长度,i32 长度为1
      var arrOfCurDim = new ArrayList<Value>();//
      //calculate Size of Ele in cur dim
      for (int i = 1; i < ctx.dimInfo_.size(); i++) {
        sizeOfEachEle *= ctx.dimInfo_.get(i);
      }
      int finalSizeOfEachEle = sizeOfEachEle;
      int finalSizeOfEachEle1 = sizeOfEachEle;
      ctx.initVal().forEach(context -> {
        if (context.exp() == null) {
          var pos = arrOfCurDim.size();
          for (var i = 0;
              i < (finalSizeOfEachEle - (pos % finalSizeOfEachEle)) % finalSizeOfEachEle;
              i++) {
            arrOfCurDim.add(CONST0);
          }
          context.dimInfo_ = new ArrayList<>(ctx.dimInfo_.
              subList(1, ctx.dimInfo_.size()));
          visit(context);
          arrOfCurDim.addAll(tmpArr_);
        } else {
          if (globalInit_) {
            usingInt_ = true;
            visit(context.exp());
            usingInt_ = false;
            tmp_ = ConstantInt.newOne(i32Type_, tmpInt_);
          } else {
            visit(context.exp());
          }
          arrOfCurDim.add(tmp_);
        }
      });
      for (int i = arrOfCurDim.size(); i < curDimLength * finalSizeOfEachEle1; i++) {
        arrOfCurDim.add(CONST0);
      }
      tmpArr_ = arrOfCurDim;
    }
    return null;

  }

  /**
   * funcDef : funcType IDENT L_PAREN funcFParams? R_PAREN block ;
   * <p>
   * 初始化函数类型；初始化函数参数，并对参数插入 alloca 和 store；初始化基本块
   */
  @Override
  public Void visitFuncDef(FuncDefContext ctx) {
    // get function name
    String functionName = ctx.IDENT().getText();
    log.info("funcDef begin @" + functionName);

    // get function return type
    Type retType = voidType_;
    String typeStr = ctx.getChild(0).getText();
    if (typeStr.equals("int")) {
      retType = i32Type_;
    }

    // get function params information
    //在此处只生成Func的param的TypeList,Func作为value的形参delay到funcDef的block里面做
    ArrayList<Type> paramTypeList = new ArrayList<>();
    //get type to create function`
    if (ctx.funcFParams() != null) {
      visit(ctx.funcFParams());
      paramTypeList.addAll(tmpTyArr);
    }
    // build function object
    FunctionType functionType = f.getFuncTy(retType, paramTypeList);
    var func = f.buildFunction(functionName, functionType);
    changeFunc(func);
    // add to symbol table
    scope_.put(functionName, curFunc_);
    //在entryBlock加入函数的形参
    var bb = f.buildBasicBlock(curFunc_.getName() + "_ENTRY", curFunc_);
    scope_.addLayer();
    scope_.preEnter = true;
    // visit block and create basic blocks
    //将函数的形参放到block中，将对Function的arg的初始化delay到visit(ctx.block)
    changeBB(bb);
    if (ctx.funcFParams() != null) {
      ctx.funcFParams().initBB = true;
      visit(ctx.funcFParams());
    }
    visit(ctx.block());
    //todo checkeifright
    if (curBB_.getList().getLast() != null &&
        (curBB_.getList().getLast().getVal().tag != TAG_.Br
            && curBB_.getList().getLast().getVal().tag != TAG_.Ret)) {

      if (curFunc_.getType().getRetType().isVoidTy()) {
        f.buildRet(curBB_);
      } else {
        f.buildRet(CONST0, curBB_);
      }
    }
    if (curBB_.getList().getLast() == null) {
      if (curFunc_.getType().getRetType().isVoidTy()) {
        f.buildRet(curBB_);
      } else {
        f.buildRet(CONST0, curBB_);
      }
    }
    //todo pretty not sure
    log.info("funcDef end@" + functionName);
    return null;
  }

  @Override
  public Void visitFuncFParams(FuncFParamsContext ctx) {
    if (ctx.initBB) {// 做关于fuction形参初始化的处理
      ctx.initBB = false;

      if (curFunc_.getNumArgs() != 0) {
        var argList = curFunc_.getArgList();
        for (int i = 0; i < ctx.funcFParam().size(); i++) {
          var p = ctx.funcFParam(i);
          if (!p.L_BRACKT().isEmpty()) {
            //which means this param is  arr
            var dimList = new ArrayList<Value>();
            var type = i32Type_;
            dimList.add(CONST0);//第一个置空
            for (int j = 0; j < p.exp().size(); j++) {
              usingInt_ = true;
              visit(p.exp(p.exp().size() - (j + 1)));
              usingInt_ = false;
              dimList.add(tmp_);
              type = f.getArrayTy(type, tmpInt_);
            }
            var arrAlloc = f.buildAlloca(curBB_, new PointerType(type));//todo
            f.buildStore(argList.get(i), arrAlloc, curBB_);//todo
            scope_.put(p.IDENT().getText(), arrAlloc);
            argList.get(i).setBounds(dimList);
          } else {
            var alloc = f.buildAlloca(curBB_, i32Type_);
            f.buildStore(argList.get(i), alloc, curBB_);
            scope_.put(ctx.funcFParam().get(i).IDENT().getText(), alloc);
          }
        }
      }
      return null;
    }
    //获得用来初始化Function的FuncType
    ArrayList<Type> types = new ArrayList<>();
    ctx.funcFParam().forEach(param -> {
      visit(param);
      types.add(tmpTy_);
    });
    tmpTyArr = types;
    return null;
  }

  /**
   * funcFParam : bType IDENT (L_BRACKT R_BRACKT (L_BRACKT exp R_BRACKT)*)? ;
   */
  @Override
  public Void visitFuncFParam(FuncFParamContext ctx) {
    if (!ctx.L_BRACKT().isEmpty()) {
      var ty = i32Type_;
      for (var i = 0; i < ctx.exp().size(); i++) {
        usingInt_ = true;
        visit(ctx.exp(ctx.exp().size() - (i + 1)));
        usingInt_ = false;
        ty = f.getArrayTy(ty, tmpInt_);
      }
      tmpTy_ = f.getPointTy(ty);
    } else {
      tmpTy_ = i32Type_;
    }
    return null;
  }

  /**
   * block : L_BRACE blockItem* R_BRACE ;
   */
  @Override
  public Void visitBlock(BlockContext ctx) {
    scope_.addLayer();
    ctx.blockItem().forEach(this::visit);
    scope_.popLayer();
    return null;
  }

  /**
   * blockItem : constDecl | varDecl | stmt ;
   */
  @Override
  public Void visitBlockItem(BlockItemContext ctx) {
    return super.visitBlockItem(ctx);
  }

  /**
   * stmt : assignStmt | expStmt | block | conditionStmt | whileStmt | breakStmt | continueStmt |
   * returnStmt ;
   */
  @Override
  public Void visitStmt(StmtContext ctx) {
    return super.visitStmt(ctx);
  }

  /**
   * assignStmt : lVal ASSIGN exp SEMICOLON ;
   */
  @Override
  public Void visitAssignStmt(AssignStmtContext ctx) {
    visit(ctx.lVal());
    var rhs = tmp_;
    visit(ctx.exp());
    var lhs = tmp_;
    f.buildStore(lhs, rhs, curBB_);
    return null;
  }

  /**
   * expStmt : exp? SEMICOLON ;
   */
  @Override
  public Void visitExpStmt(ExpStmtContext ctx) {
    return super.visitExpStmt(ctx);
  }

  /**
   * conditionStmt : IF_KW L_PAREN cond R_PAREN stmt (ELSE_KW stmt)? ;
   */
  @Override
  public Void visitConditionStmt(ConditionStmtContext ctx) {
    var parentBB = curBB_;
    var name = "";
    var trueBlock = f.buildBasicBlock(name + "_then", curFunc_);
    var nxtBlock = f.buildBasicBlock(name + "_nxtBlock", curFunc_);
    var falseBlock = ctx.ELSE_KW() == null ? nxtBlock :
        f.buildBasicBlock(parentBB.getName() + "_else", curFunc_);
    var ifIfEndWithRet = false;
    ctx.cond().falseblock = falseBlock;
    ctx.cond().trueblock = trueBlock;
    // Parse [cond]
    visitCond(ctx.cond());
    // Parse [then] branch
    changeBB(trueBlock);
    visitStmt(ctx.stmt(0));
    f.buildBr(nxtBlock, curBB_);
    if (curBB_.getList().getLast().getVal() instanceof RetInst) {
      ifIfEndWithRet = true;
    }
    // Parse [else] branch
    if (ctx.ELSE_KW() != null) {
      changeBB(falseBlock);
      visitStmt(ctx.stmt(1));
      f.buildBr(nxtBlock, curBB_);
      if (ifIfEndWithRet) {
        if (curBB_.getList().getLast().getVal() instanceof RetInst) {
          nxtBlock.node_.removeSelf();
        }
      }
    }
    curBB_ = nxtBlock;
    return null;
  }

  /**
   * whileStmt : WHILE_KW L_PAREN cond R_PAREN stmt ;
   */
  private static final String BreakInstructionMark = "_BREAK";
  private static final String ContinueInstructionMark = "_CONTINUE";

  @Override
  public Void visitWhileStmt(WhileStmtContext ctx) {
    var parentBB = curBB_;
    var name = "";
    var whileCondEntryBlock = f.buildBasicBlock(name + "_whileCondition", curFunc_);
    var trueBlock = f.buildBasicBlock(name + "_body", curFunc_);
    var nxtBlock = f.buildBasicBlock(name + "_nxtBlock", curFunc_);
    var cloner = new ValueCloner() {
      @Override
      public Value findValue(Value value) {
        if (value instanceof Constant) {
          return value;
        } else {
          return this.valueMap.get(value);
        }
      }
    };
    f.buildBr(whileCondEntryBlock, parentBB);

    // Parse [whileCond]
    ctx.cond().falseblock = nxtBlock;
    ctx.cond().trueblock = trueBlock;
    changeBB(whileCondEntryBlock);
    //条件分支优化
    ctx.cond().isLoopCond = true;
    visitCond(ctx.cond());
    // Parse [loop]
    changeBB(trueBlock);
    visitStmt(ctx.stmt());
    //保持几个Inst之间的use关系不乱，并且让使用了其他value的inst能够找到那些inst
    if (Config.getInstance().isO2) {
      whileCondEntryBlock.getList().forEach(instNode -> {
        var val = instNode.getVal();
        for (Value operand : val.getOperands()) {
          if (cloner.findValue(operand) == null) {
            cloner.put(operand, operand);
          }
        }
        var copy = cloner.getInstCopy(val);
        cloner.put(val, copy);
        copy.node.insertAtEnd(curBB_.getList());
      });
    } else {
      f.buildBr(whileCondEntryBlock, curBB_);
    }

    // [Backpatch] for break & continue
    backpatch(BreakInstructionMark, trueBlock, curBB_, nxtBlock, whileCondEntryBlock);
    backpatch(ContinueInstructionMark, trueBlock, curBB_, whileCondEntryBlock, whileCondEntryBlock);
    changeBB(nxtBlock);

    return null;
  }

  private void backpatch(String key, BasicBlock startBlock, BasicBlock endBlock,
      BasicBlock targetBlock, BasicBlock whileEntry) {
    ValueCloner cloner = new ValueCloner() {
      @Override
      public Value findValue(Value value) {
        if (value instanceof Constant) {
          return value;
        } else {
          return this.valueMap.get(value);
        }
      }
    };
    var blockList = new LinkedList<BasicBlock>();
    var blockSet = new HashSet<BasicBlock>();
    ArrayList<BrInst> toBeReplacedContinues = new ArrayList<>();
    blockSet.add(startBlock);
    blockList.add(startBlock);

    // BFS through [BBs]
    while (!blockList.isEmpty()) {
      var curBlock = blockList.poll();

      // Iterate through [Instructions] in [BB]
      // 如果某个块的名字是 KEY，则识别成功，替换成 targetBlock
      // 否则加入待替换的列表
      for (var curInstrEntry : curBlock.getList()) {
        var curInstr = curInstrEntry.getVal();
        if (curInstr.tag.equals(Instruction.TAG_.Br)) { // Instruction found, then [backpatch]
          var operandCount = curInstr.getOperands().size();

          // 有两种情况：
          // - [operandCount == 1] 无条件跳转，则第 1 个参数为目标块
          // - [operandCount == 3] 条件跳转，则第 2, 3 个参数为目标块
          if (operandCount == 1) {
            // 无条件跳转，则第 1 个参数为目标块
            assert curInstr.getOperands().get(0) instanceof BasicBlock;
            var trueBlock = (BasicBlock) curInstr.getOperands().get(0);

            if (trueBlock.getName().equals(key)) {
              if (Config.getInstance().isO2) {
                if (trueBlock.getName().equals("_CONTINUE")) {
                  toBeReplacedContinues.add((BrInst) curInstr);
                  trueBlock.node_.removeSelf();
                } else {
                  curInstr.CoSetOperand(0, targetBlock);
                  trueBlock.node_.removeSelf();
                }
              } else {
                curInstr.CoSetOperand(0, targetBlock);
                trueBlock.node_.removeSelf();
              }
            } else if (trueBlock != endBlock && !blockSet
                .contains(trueBlock)) { // 遇到 endBlock 则不再加入，endBlock 是尾后 BB
              blockList.add(trueBlock);
              blockSet.add(trueBlock);
            }
          } else {
            // 条件跳转，则第 2, 3 个参数为目标块
            // Check TrueBlock
            assert curInstr.getOperands().get(1) instanceof BasicBlock;
            var trueBlock = (BasicBlock) curInstr.getOperands().get(1);
            if (trueBlock.getName().equals(key)) {
              if (Config.getInstance().isO2) {
                if (trueBlock.getName().equals("_CONTINUE")) {
                  toBeReplacedContinues.add((BrInst) curInstr);
                  trueBlock.node_.removeSelf();
                } else {
                  curInstr.CoSetOperand(1, targetBlock);
                  trueBlock.node_.removeSelf();
                }
              } else {
                curInstr.CoSetOperand(1, targetBlock);
                trueBlock.node_.removeSelf();
              }
            } else if (trueBlock != endBlock && !blockSet.contains(trueBlock)) {
              blockList.add(trueBlock);
              blockSet.add(trueBlock);
            }

            // Check FalseBlock
            assert curInstr.getOperands().get(2) instanceof BasicBlock;
            var falseBlock = (BasicBlock) curInstr.getOperands().get(2);

            if (falseBlock.getName().equals(key)) {
              if (Config.getInstance().isO2) {
                if (falseBlock.getName().equals("_CONTINUE")) {
                  toBeReplacedContinues.add((BrInst) curInstr);
                  falseBlock.node_.removeSelf();
                } else {
                  curInstr.CoSetOperand(2, targetBlock);
                  falseBlock.node_.removeSelf();
                }
              } else {
                curInstr.CoSetOperand(2, targetBlock);
                falseBlock.node_.removeSelf();
              }
            } else if (falseBlock != endBlock && !blockSet.contains(falseBlock)) {
              blockList.add(falseBlock);
              blockSet.add(falseBlock);
            }
          }
        }
      }
    }
    if (Config.getInstance().isO2) {
      toBeReplacedContinues.forEach(br -> {
        whileEntry.getList().forEach(instNode -> {
          var val = instNode.getVal();
          for (Value operand : val.getOperands()) {
            if (cloner.findValue(operand) == null) {
              cloner.put(operand, operand);
            }
          }
          var copy = cloner.getInstCopy(val);
          cloner.put(val, copy);
          copy.node.insertAtEnd(br.getBB().getList());
        });
        br.node.removeSelf();
      });
    }
  }

  /**
   * breakStmt : BREAK_KW SEMICOLON ;
   */
  @Override
  public Void visitBreakStmt(BreakStmtContext ctx) {
    f.buildBr(f.buildBasicBlock(BreakInstructionMark, curFunc_), curBB_);
    return null;
  }

  /**
   * continueStmt : CONTINUE_KW SEMICOLON ;
   */
  @Override
  public Void visitContinueStmt(ContinueStmtContext ctx) {
    f.buildBr(f.buildBasicBlock(ContinueInstructionMark, curFunc_), curBB_);
    return null;
  }

  /**
   * returnStmt : RETURN_KW (exp)? SEMICOLON ;
   */
  @Override
  public Void visitReturnStmt(ReturnStmtContext ctx) {
    if (ctx.exp() != null) {
      visit(ctx.exp());
      f.buildRet(tmp_, curBB_);
    } else {
      f.buildRet(curBB_);
    }
    return null;
  }

  /**
   * exp : addExp ;
   */
  @Override
  public Void visitExp(ExpContext ctx) {
    return super.visitExp(ctx);
  }

  /**
   * lVal : IDENT (L_BRACKT exp R_BRACKT)* ;
   */
  @Override
  public Void visitLVal(LValContext ctx) {
    var name = ctx.IDENT().getText();
    var t = scope_.find(name);
    if (t == null) {
      throw new SyntaxException("undefined value name" + name);
    }
    //const value
    if (t.getType().isIntegerTy()) {
      tmp_ = t;
      return null;
    }
    //直接指向int
    boolean INT = false, PTR = false, ARR = false;
    if (t.getType().isPointerTy()) {
      INT = ((PointerType) t.getType()).getContained().isIntegerTy();
      //指向一个函数传参进来的数组
      PTR = ((PointerType) t.getType()).getContained().isPointerTy();
      //指向别的数组
      ARR = ((PointerType) t.getType()).getContained().isArrayTy();
    }
    //function call
    if (INT) {
      if (ctx.exp().isEmpty()) {
        tmp_ = t;
        return null;
      } else {
        for (ExpContext expContext : ctx.exp()) {
          visit(expContext);
          var fromExp = tmp_;
          t = f.buildGEP(t, new ArrayList<>() {{
            add(fromExp);
          }}, curBB_);
        }
        tmp_ = t;
        return null;
      }
    }

    if (PTR) {
      if (ctx.exp().isEmpty()) {
        tmp_ = f.buildLoad(((PointerType) t.getType()).getContained(), t, curBB_);
        return null;
      } else {
        PointerType allocatedType = (PointerType) t.getType();
        var containedType = (PointerType) allocatedType.getContained();
        var load = f.buildLoad(containedType, t, curBB_);
        visit(ctx.exp(0));
        var gep = f.buildGEP(load, new ArrayList<>() {{
          add(tmp_);
        }}, curBB_);
        for (var i = 1; i < ctx.exp().size(); i++) {
          visit(ctx.exp(i));
          var val = tmp_;
          gep = f.buildGEP(gep, new ArrayList<>() {{
            add(CONST0);
            add(val);
          }}, curBB_);
        }
        tmp_ = gep;
        /*  var arrayParams = scope_.params_.get(ctx.IDENT().getText());
        tmpPtr_ = f.buildLoad(((PointerType) t.getType()).getContained(), t, curBB_);
        for (int i = 0; i < ctx.exp().size(); i++) {
          visit(ctx.exp().get(i));
          var val = tmp_;
          for (int j = i + 1; j < arrayParams.size(); j++) {
            val = f.buildBinary(TAG_.Mul, val, arrayParams.get(j), curBB_);
          }
          Value finalVal = val;
          tmpPtr_ = f.buildGEP(tmpPtr_, new ArrayList<>() {{
            add(finalVal);
          }}, curBB_);
        }*/
        return null;
      }
    }

    if (ARR) {
      if (ctx.exp().isEmpty()) {
        tmp_ = f.buildGEP(t, new ArrayList<>() {{
          add(CONST0);
          add(CONST0);
        }}, curBB_);
        return null;
      } else {
        Type ty = ((PointerType) t.getType()).getContained();
        Value offset = ConstantInt.newOne(i32Type_, 0);
        t = f.buildGEP(t, new ArrayList<>() {{
          add(CONST0);
          add(CONST0);
        }}, curBB_);
        for (int i = 0; i < ctx.exp().size() - 1; i++) {
          visit(ctx.exp(i));
          assert ty instanceof ArrayType;
          var val = tmp_;
          var add = f.buildBinary(TAG_.Add, offset, val, curBB_);
          offset = f.buildBinary(TAG_.Mul, add,
              ConstantInt.newOne(i32Type_, ((ArrayType) ty).getNumEle()), curBB_);
          ty = ((ArrayType) ty).getELeType();
          t = f.buildGEP(t, new ArrayList<>() {{
            add(CONST0);
            add(CONST0);
          }}, curBB_);
        }
        visit(ctx.exp(ctx.exp().size() - 1));
        var val = tmp_;
        offset = f.buildBinary(TAG_.Add, offset, val, curBB_);
        Value finalOffset = offset;
        t = f.buildGEP(t, new ArrayList<>() {{
          add(finalOffset);
        }}, curBB_);
        tmp_ = t;
        return null;
      }
    }
    throw new SyntaxException("unreachable");
  }

  /**
   * @value : null primaryExp : (L_PAREN exp R_PAREN) | lVal | number ;
   */
  @Override
  public Void visitPrimaryExp(PrimaryExpContext ctx) {
    if (usingInt_) {
      if (ctx.exp() != null) {
        visit(ctx.exp());
        return null;
      }
      if (ctx.lVal() != null) {
        visit(ctx.lVal());
        tmpInt_ = ((ConstantInt) tmp_).getVal();
        return null;
      }
      if (ctx.number() != null) {
        visit(ctx.number());
        return null;
      }
    } else {
      if (ctx.exp() != null) {
        visit(ctx.exp());
        return null;
      }
      if (ctx.lVal() != null) {
        if (buildCall) {
          buildCall = false;
          visit(ctx.lVal());
          return null;
        } else {
          visit(ctx.lVal());
          if (tmp_.getType().isIntegerTy()) {
            return null;
          }
          tmp_ = f.buildLoad(((PointerType) tmp_.getType()).getContained(), tmp_, curBB_);
          return null;
        }
      }
      if (ctx.number() != null) {
        visit(ctx.number());
        return null;
      }
    }
    throw new SyntaxException("unreachable");
  }

  /**
   * @value : tmp_ -> 解析出的ConstantInt
   */
  @Override
  public Void visitNumber(NumberContext ctx) {
    visit(ctx.intConst());
    if (!usingInt_) {
      tmp_ = f.getConstantInt(tmpInt_);
    }//using int 会在visitConst里面处理
    return null;
  }

  /**
   * @author : ai
   */

  @Override
  public Void visitIntConst(IntConstContext ctx) {
    if (ctx.DECIMAL_CONST() != null) {
      tmpInt_ = (new BigInteger(ctx.DECIMAL_CONST().getText(), 10).intValue());
      return null;
    }
    if (ctx.HEXADECIMAL_CONST() != null) {
      tmpInt_ = (new BigInteger(ctx.HEXADECIMAL_CONST().getText().substring(2), 16).intValue());
      return null;
    }
    if (ctx.OCTAL_CONST() != null) {
      tmpInt_ = (new BigInteger(ctx.OCTAL_CONST().getText(), 8)).intValue();
      return null;
    }
    throw new SyntaxException("Unreachable");
  }

  /**
   * unaryExp : primaryExp | callee | (unaryOp unaryExp) ;
   */
  @Override
  public Void visitUnaryExp(UnaryExpContext ctx) {
    if (usingInt_) {
      if (ctx.unaryExp() != null) {
        visit(ctx.unaryExp());
        if (ctx.unaryOp().MINUS() != null) {
          tmpInt_ = -tmpInt_;
        }
        if (ctx.unaryOp().PLUS() != null) {
          tmpInt_ = +tmpInt_;
        }
        if (ctx.unaryOp().NOT() != null) {
          tmpInt_ = tmpInt_ == 0 ? 1 : 0;
        }
      } else {
        if (ctx.primaryExp() != null) {
          visit(ctx.primaryExp());
          //low confidence
        }
        if (ctx.callee() != null) {
          throw new SyntaxException("Func call in constExp");
        }
      }
    } else {

      if (ctx.unaryExp() != null) {
        Value v;
        visit(ctx.unaryExp());
        var t = tmp_;
        if (t.getType().isI1()) {
          f.buildZext(t, curBB_);
        }
        if (ctx.unaryOp().NOT() != null) {

          v = f.buildBinary(TAG_.Eq, t, CONST0, curBB_);
          tmp_ = v;
        }
        if (ctx.unaryOp().PLUS() != null) {
          //do nothing
        }
        if (ctx.unaryOp().MINUS() != null) {
          if (t.getType().isI1()) {
            t = f.buildZext(t, curBB_);
          }
          v = f.buildBinary(TAG_.Sub, CONST0, t, curBB_);
          tmp_ = v;
        }
        return null;
      }
      if (ctx.callee() != null) {
        visit(ctx.callee());
        return null;
      }
      if (ctx.primaryExp() != null) {
        visit(ctx.primaryExp());
        return null;
      }
    }

    return null;
  }

  /**
   * callee : IDENT L_PAREN funcRParams? R_PAREN ;
   */
  @Override
  public Void visitCallee(CalleeContext ctx) {
    var func = scope_.find(ctx.IDENT().getText());
    var args = new ArrayList<Value>();
    List<ParamContext> paramsCtx;
    if (ctx.funcRParams() != null) {
      paramsCtx = ctx.funcRParams().param();
      assert func != null;
      var paramTys = ((Function) func).getType().getParams();
      for (int i = 0; i < paramsCtx.size(); i++) {
        var param = paramsCtx.get(i);
        var paramTy = paramTys.get(i);
        if (paramTy.isIntegerTy()) {
          buildCall = false;
        } else {
          buildCall = true;
        }
        visit(param.exp());// 没有String
        buildCall = false;
        args.add(tmp_);
      }
    }
    tmp_ = f.buildFuncCall((Function) func, args, curBB_);
    return null;
  }

  /**
   * param : exp | STRING ; STRING 是 void putf() 函数的需要
   */
  @Override
  public Void visitParam(ParamContext ctx) {
    visit(ctx.exp());
    return null;
  }

  /**
   * mulExp : unaryExp (mulOp unaryExp)* ;
   */
  @Override
  public Void visitMulExp(MulExpContext ctx) {
    if (usingInt_) {
      visit(ctx.unaryExp(0));
      var s = tmpInt_;
      for (var i = 1; i < ctx.unaryExp().size(); i++) {
        visit(ctx.unaryExp(i));
        if (ctx.mulOp(i - 1).MUL() != null) {
          s *= tmpInt_;
        }
        if (ctx.mulOp(i - 1).DIV() != null) {
          s /= tmpInt_;
        }
        if (ctx.mulOp(i - 1).MOD() != null) {
          s %= tmpInt_;
        }
      }
      tmpInt_ = s;
      return null;
    } else {
      visit(ctx.unaryExp(0));
      var lhs = tmp_;
      for (var i = 1; i < ctx.unaryExp().size(); i++) {
        visit(ctx.unaryExp(i));
        var rhs = tmp_;
        //cast i1 value 2 i32
        if (lhs.getType().isI1()) {
          lhs = f.buildZext(lhs, curBB_);
        }
        if (rhs.getType().isI1()) {
          rhs = f.buildZext(rhs, curBB_);
        }
        if (ctx.mulOp(i - 1).MUL() != null) {
          lhs = f.buildBinary(TAG_.Mul, lhs, rhs, curBB_);
        }
        if (ctx.mulOp(i - 1).DIV() != null) {
          lhs = f.buildBinary(TAG_.Div, lhs, rhs, curBB_);
        }
        if (ctx.mulOp(i - 1).MOD() != null) {
          //x%y=x - (x/y)*y
          if (rhs instanceof ConstantInt) {
            var num = ((ConstantInt) rhs).getVal();
            if (Math.abs(num) == 1) {
              lhs = f.buildBinary(TAG_.Mod, lhs, rhs, curBB_);
            } else if ((Math.abs(num) & (Math.abs(num) - 1)) == 0) {
              lhs = f.buildBinary(TAG_.Mod, lhs, rhs, curBB_);
            } else if (num < 0) {
              var a = f.buildBinary(TAG_.Div, lhs, rhs, curBB_);
              var b = f.buildBinary(TAG_.Mul, a,
                  ConstantInt.newOne(i32Type_, Math.abs(((ConstantInt) rhs).getVal())), curBB_);
              lhs = f.buildBinary(TAG_.Sub, lhs, b, curBB_);
            } else if (num > 0) {
              var a = f.buildBinary(TAG_.Div, lhs, rhs, curBB_);
              var b = f.buildBinary(TAG_.Mul, a, rhs, curBB_);
              lhs = f.buildBinary(TAG_.Sub, lhs, b, curBB_);
            }
          } else {
            var a = f.buildBinary(TAG_.Div, lhs, rhs, curBB_);
            var b = f.buildBinary(TAG_.Mul, a, rhs, curBB_);
            lhs = f.buildBinary(TAG_.Sub, lhs, b, curBB_);
          }
          //lhs = f.buildBinary(TAG_.Mod, lhs, rhs, curBB_);
        }
        tmp_ = lhs;
      }
      return null;
    }
  }

  /**
   * @value :
   * <p>
   * addExp : mulExp (addOp mulExp)* ;
   */
  @Override
  public Void visitAddExp(AddExpContext ctx) {
    if (usingInt_) {//所有值包括ident都必须是常量
      visit(ctx.mulExp(0));
      var s = tmpInt_;
      for (var i = 1; i < ctx.mulExp().size(); i++) {
        visit(ctx.mulExp(i));
        if (ctx.addOp(i - 1).PLUS() != null) {
          s += tmpInt_;
        }
        if (ctx.addOp(i - 1).MINUS() != null) {
          s -= tmpInt_;
        }
      }
      tmpInt_ = s;
      return null;
    } else {
      visit(ctx.mulExp(0));
      var lhs = tmp_;

      for (int i = 1; i < ctx.mulExp().size(); i++) {
        visit(ctx.mulExp(i));
        var rhs = tmp_;
        if (lhs.getType().isI1()) {
          lhs = f.buildZext(lhs, curBB_);
        }
        if (rhs.getType().isI1()) {
          rhs = f.buildZext(rhs, curBB_);
        }
        if (ctx.addOp(i - 1).PLUS() != null) {
          lhs = f.buildBinary(TAG_.Add, lhs, rhs, curBB_);
        }
        if (ctx.addOp(i - 1).MINUS() != null) {
          lhs = f.buildBinary(TAG_.Sub, lhs, rhs, curBB_);
        }
      }
      tmp_ = lhs;
    }
    return null;
  }

  /**
   * relExp : addExp (relOp addExp)* ;
   */
  @Override
  public Void visitRelExp(RelExpContext ctx) {

    visit(ctx.addExp(0));
    var lhs = tmp_;
    for (int i = 1; i < ctx.addExp().size(); i++) {
      expInRel = false;
      visit(ctx.addExp(i));
      var rhs = tmp_;
      if (ctx.relOp(i - 1).LE() != null) {
        lhs = f.buildBinary(TAG_.Le, lhs, rhs, curBB_);
      }
      if (ctx.relOp(i - 1).GE() != null) {
        lhs = f.buildBinary(TAG_.Ge, lhs, rhs, curBB_);
      }
      if (ctx.relOp(i - 1).GT() != null) {
        lhs = f.buildBinary(TAG_.Gt, lhs, rhs, curBB_);
      }
      if (ctx.relOp(i - 1).LT() != null) {
        lhs = f.buildBinary(TAG_.Lt, lhs, rhs, curBB_);
      }
    }
    tmp_ = lhs;
    return null;
  }

  /**
   * eqExp : relExp (eqOp relExp)* ;
   */
  @Override
  public Void visitEqExp(EqExpContext ctx) {
    visit(ctx.relExp(0));
    var lhs = tmp_;
    for (int i = 1; i < ctx.relExp().size(); i++) {
      expInRel = false;
      visit(ctx.relExp(i));
      if (ctx.eqOp(i - 1).EQ() != null) {
        lhs = f.buildBinary(TAG_.Eq, lhs, tmp_, curBB_);
      }
      if (ctx.eqOp(i - 1).NEQ() != null) {
        lhs = f.buildBinary(TAG_.Ne, lhs, tmp_, curBB_);
      }

    }
    tmp_ = lhs;
    return null;
  }

  /**
   * lAndExp : eqExp (AND eqExp)* ;
   */
  @Override
  public Void visitLAndExp(LAndExpContext ctx) {
    ctx.eqExp().forEach(exp -> {
      var nb = f.buildBasicBlock("", curFunc_);
      expInRel = true;
      visit(exp);
      if (expInRel) {
        expInRel = false;
        tmp_ = f.buildBinary(TAG_.Ne, tmp_, CONST0, curBB_);
      }
      f.buildBr(tmp_, nb, ctx.falseblock, curBB_);
      changeBB(nb);
    });
    f.buildBr(ctx.trueblock, curBB_);
    return null;
  }

  /**
   * lOrExp : lAndExp (OR lAndExp)* ;
   */
  @Override
  public Void visitLOrExp(LOrExpContext ctx) {
    ctx.lAndExp(0).isFirstBlock = true;
    for (int i = 0; i < ctx.lAndExp().size() - 1; i++) {
      var nb = f.buildBasicBlock("", curFunc_);
      ctx.lAndExp(i).trueblock = ctx.trueblock;
      ctx.lAndExp(i).falseblock = nb;
      visit(ctx.lAndExp(i));
      changeBB(nb);
    }
    ctx.lAndExp(ctx.lAndExp().size() - 1).falseblock = ctx.falseblock;
    ctx.lAndExp(ctx.lAndExp().size() - 1).trueblock = ctx.trueblock;
    visit(ctx.lAndExp(ctx.lAndExp().size() - 1));

  /*  ctx.lAndExp(0).falseblock = ctx.falseblock;
    ctx.lAndExp(0).trueblock = ctx.trueblock;
    visit(ctx.lAndExp(0));
    for (int i = 1; i < ctx.lAndExp().size(); i++) {
      var nb = f.buildBasicBlock("", curFunc_);
      ctx.lAndExp(i).falseblock = nb;
      visit(ctx.lAndExp(i));
    }*/
    return null;
  }

  /**
   * cond : lOrExp ;
   */
  @Override
  public Void visitCond(CondContext ctx) {
    ctx.lOrExp().falseblock = ctx.falseblock;
    ctx.lOrExp().trueblock = ctx.trueblock;
    visit(ctx.lOrExp());
    return null;
  }

  /**
   * constExp : addExp ;
   *
   * @value : tmpint_ -> res of exp
   * <p>
   * 表达式求和
   */
  @Override
  public Void visitConstExp(ConstExpContext ctx) {
    usingInt_ = true;
    visit(ctx.addExp());
    tmp_ = f.getConstantInt(tmpInt_);
    usingInt_ = false;
    return null;
  }
}
