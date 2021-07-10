package frontend;


import frontend.SysYParser.*;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.types.FunctionType;
import ir.types.IntegerType;
import ir.types.PointerType;
import ir.types.Type;
import ir.values.BasicBlock;
import ir.values.Constant;
import ir.values.Constants.ConstantInt;
import ir.values.Function;
import ir.values.Value;
import ir.values.instructions.Instruction.TAG_;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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
    private ArrayList<HashMap<String, Value>> tables_;
    private ArrayList<HashMap<String, ArrayList<Value>>> paramTables_;
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

    public void addLayer() {
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
  private final MyModule m = MyModule.getInstance();
  private final MyFactoryBuilder f = MyFactoryBuilder.getInstance();
  private Scope scope_ = new Scope(); // symbol table
  private BasicBlock curBB_; // current basicBlock
  private Function curFunc_; // current function

  // pass values between `visit` functions
  private ArrayList<Value> tmpArr_;//只允许赋值以及被赋值，不能直接操作
  private Value tmp_;
  private Value tmpPtr_;
  private int tmpInt_;
  private Type tmpTy_;
  private ArrayList<Type> tmpTyArr;
  // singleton variables
  private final ConstantInt CONST0 = ConstantInt.CONST0();
  private final Type i32Type_ = f.getI32Ty();
  private final Type voidType_ = f.getVoidTy();
  private final Type labelType_ = f.getLabelTy();
  private final Type ptri32Type_ = f.getPointTy(i32Type_);
  //status word
  private boolean usingInt_ = false;//常量初始化要对表达式求值，并且用的Ident也要是常量
  private boolean globalInit_ = false;

  /**
   * program : compUnit ;
   * <p>
   * 初始化 module，定义内置函数
   */
  @Override
  public Void visitProgram(ProgramContext ctx) {
    log.info("Syntax begin");
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

    for (int i = dims.size() - 1; i > 0; i--) {
      arrTy = f.getArrayTy(arrTy, dims.get(i));
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
      for (var i = dims.size() - 1; i > 0; i--) {
        arrty = f.getArrayTy(arrty, dims.get(i));// arr(arr(arr(i32,dim1),dim2),dim3)
      }
      if (scope_.isGlobal()) {
        if (ctx.constInitVal() != null) {
          ctx.constInitVal().dimInfo_ = dims;
          globalInit_ = true;
          visit(ctx.constInitVal());//dim.size()=n
          globalInit_ = false;
          var initializer = genConstArr(dims, tmpArr_);
          var variable = f.getGlobalvariable(ctx.IDENT().getText(), arrty, initializer);
          variable.setConst();
          scope_.put(ctx.IDENT().getText(), variable);
        } else {
          var variable = f.getGlobalvariable(ctx.IDENT().getText(), arrty, CONST0);
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
          }}, curBB_);
          for (int i = 1; i < ctx.constInitVal().dimInfo_.size(); i++) {
            ptr = f.buildGEP(ptr, new ArrayList<>() {{
              add(CONST0);
            }}, curBB_);
            ;
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
          for (int i = 0; i < sizeOfEachEle - (pos % sizeOfEachEle) % sizeOfEachEle; i++) {
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
    log.info("visiting VarDef name:" + varName);
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
          var v = f.getGlobalvariable(varName, i32Type_, initializer);
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
      for (var i = dims.size() - 1; i > 0; i--) {
        arrTy = f.getArrayTy(arrTy, dims.get(i));
      }
      if (scope_.isGlobal()) {
        if (!ctx.initVal().isEmpty()) {

          ctx.initVal().dimInfo_ = dims;
          globalInit_ = true;
          visit(ctx.initVal());
          globalInit_ = false;

          var arr = tmpArr_;
          var init = genConstArr(dims, arr);
          var glo = f.getGlobalvariable(ctx.IDENT().getText(), arrTy, init);
          scope_.put(ctx.IDENT().getText(), glo);
        } else {
          var v = f.getGlobalvariable(ctx.IDENT().getText(), arrTy, CONST0);
          scope_.put(ctx.IDENT().getText(), v);
        }
      } else {//local arr init
        var alloc = f.buildAlloca(curBB_, arrTy);
        scope_.put(ctx.IDENT().getText(), alloc);
        if (!ctx.initVal().isEmpty()) {
          alloc.setInit();
          ctx.initVal().dimInfo_ = dims;
          visit(ctx.initVal());
          var arr = tmpArr_;
          var pointer = f.buildGEP(alloc, new ArrayList<>() {{
            add(CONST0);
          }}, curBB_);

          for (var i = 1; i < dims.size(); i++) {
            pointer = f.buildGEP(pointer, new ArrayList<>() {{
              add(CONST0);
            }}, curBB_);
          }

          for (int i = 0; i < arr.size(); i++) {
            var t = arr.get(i);
            if (t instanceof ConstantInt) {
              if (((ConstantInt) t).getVal() == 0) {
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
    if (ctx.exp() != null && ctx.dimInfo_ == null) {
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
          for (var i = 0; i < finalSizeOfEachEle - (pos % finalSizeOfEachEle) % finalSizeOfEachEle;
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
        for (int i = arrOfCurDim.size(); i < curDimLength * finalSizeOfEachEle1; i++) {
          arrOfCurDim.add(CONST0);
        }
        tmpArr_ = arrOfCurDim;
      });
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
    ArrayList<Type> paramTypeList;
    //get type to create function`
    if (ctx.funcFParams() != null) {
      visit(ctx.funcFParams());
    }
    paramTypeList = tmpTyArr;
    // build function object
    FunctionType functionType = f.getFuncTy(retType, paramTypeList);
    var func = f.buildFunction(functionName, functionType);
    changeFunc(func);
    // add to symbol table
    scope_.put(functionName, curFunc_);
    //在entryBlock加入函数的形参
    var bb = f.buildBasicBlock(curFunc_.getName() + "_ENTRY", curFunc_);
    // visit block and create basic blocks
    //将函数的形参放到block中，将对Function的arg的初始化delay到visit(ctx.block)
    changeBB(bb);
    if (ctx.funcFParams() != null) {
      ctx.funcFParams().initBB = true;
      visit(ctx.funcFParams());
    }
    visit(ctx.block());
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
          if (!p.isEmpty()) { //which means this param is not arr
            var paramList = new ArrayList<Value>();
            var arrAlloc = f.buildAlloca(curBB_, ptri32Type_);
            f.buildStore(argList.get(i), arrAlloc, curBB_);
            paramList.add(CONST0);//第一个置空
            p.exp().forEach(exp -> {
              visit(exp);
              paramList.add(tmp_);
            });
            scope_.put(p.IDENT().getText(), arrAlloc);
            //todo
          } else {
            var alloc = f.getAlloca(i32Type_);

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
      //只要是个数组，就全部拿i32ptr代替
      //因为只有常量数组，所以偏移可以直接在函数里拿Arg算
      tmpTy_ = ptri32Type_;
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
    visit(ctx.getChild(0));
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

    // Parse [cond]
    visitCond(ctx.cond());
    f.buildBr(tmp_, trueBlock, falseBlock, parentBB);

    // Parse [then] branch
    curBB_ = trueBlock;
    visitStmt(ctx.stmt(0));
    f.buildBr(nxtBlock, trueBlock);

    // Parse [else] branch
    if (ctx.ELSE_KW() != null) {
      curBB_ = falseBlock;
      visitStmt(ctx.stmt(1));
      f.buildBr(nxtBlock, falseBlock);
    }

    curBB_ = nxtBlock;
    return super.visitConditionStmt(ctx);
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
    var whileCondBlock = f.buildBasicBlock(name + "_whileCondition", curFunc_);
    var trueBlock = f.buildBasicBlock(name + "_body", curFunc_);
    var nxtBlock = f.buildBasicBlock(name + "_nxtBlock", curFunc_);

    f.buildBr(whileCondBlock, parentBB);

    // Parse [whileCond]
    visitCond(ctx.cond());
    f.buildBr(tmp_, trueBlock, nxtBlock, whileCondBlock);

    // Parse [loop]
    curBB_ = trueBlock;
    visitStmt(ctx.stmt());
    f.buildBr(whileCondBlock, trueBlock);

    // [Backpatch] for break & continue
    backpatch(BreakInstructionMark, trueBlock, nxtBlock, nxtBlock);
    backpatch(ContinueInstructionMark, trueBlock, nxtBlock, whileCondBlock);

    curBB_ = nxtBlock;
    return null;
  }

  private void backpatch(String key, BasicBlock startBlock, BasicBlock endBlock,
      BasicBlock targetBlock) {
    var blockList = new LinkedList<BasicBlock>();
    blockList.add(startBlock);

    // BFS through [BBs]
    while (!blockList.isEmpty()) {
      var curBlock = blockList.poll();
      var firstEntry = curBlock.getList().getEntry();
      var lastEntry = curBlock.getList().getLast();

      // Iterate through [Instructions] in [BB]
      // 如果某个块的名字是 KEY，则识别成功，替换成 targetBlock
      // 否则加入待替换的列表
      for (var curEntry = firstEntry; curEntry != lastEntry; curEntry = curEntry.getNext()) {
        var curInstr = curEntry.getVal();
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
              curInstr.CoSetOperand(0, targetBlock);
            } else if (trueBlock != endBlock) { // 遇到 endBlock 则不再加入，endBlock 是尾后 BB
              blockList.add(trueBlock);
            }
          } else {
            // 条件跳转，则第 2, 3 个参数为目标块
            // Check TrueBlock
            assert curInstr.getOperands().get(1) instanceof BasicBlock;
            var trueBlock = (BasicBlock) curInstr.getOperands().get(1);

            if (trueBlock.getName().equals(key)) {
              curInstr.CoSetOperand(1, targetBlock);
            } else if (trueBlock != endBlock) {
              blockList.add(trueBlock);
            }

            // Check FalseBlock
            assert curInstr.getOperands().get(2) instanceof BasicBlock;
            var falseBlock = (BasicBlock) curInstr.getOperands().get(2);

            if (falseBlock.getName().equals(key)) {
              curInstr.CoSetOperand(2, targetBlock);
            } else if (falseBlock != endBlock) {
              blockList.add(falseBlock);
            }
          }
        }
      }

    }
  }

  /**
   * breakStmt : BREAK_KW SEMICOLON ;
   */
  @Override
  public Void visitBreakStmt(BreakStmtContext ctx) {
    f.buildBr(f.getBasicBlock(BreakInstructionMark), curBB_);
    //todo
    return null;
  }

  /**
   * continueStmt : CONTINUE_KW SEMICOLON ;
   */
  @Override
  public Void visitContinueStmt(ContinueStmtContext ctx) {
    f.buildBr(f.getBasicBlock(ContinueInstructionMark), curBB_);
    return null;
  }

  /**
   * returnStmt : RETURN_KW (exp)? SEMICOLON ;
   */
  @Override
  public Void visitReturnStmt(ReturnStmtContext ctx) {
    //todo
    return super.visitReturnStmt(ctx);
  }

  /**
   * exp : addExp ;
   */
  @Override
  public Void visitExp(ExpContext ctx) {
    return super.visitExp(ctx);
  }

  /**
   * cond : lOrExp ;
   */
  @Override
  public Void visitCond(CondContext ctx) {
    return super.visitCond(ctx);
  }

  /**
   * lVal : IDENT (L_BRACKT exp R_BRACKT)* ;
   */
  @Override
  public Void visitLVal(LValContext ctx) {
    //todo
    var name = ctx.IDENT().getText();
    var t = scope_.find(name);
    if (t == null) {
      throw new SyntaxException("undefined value name" + name);
    }
    //const value
    if (t.getType().isIntegerTy()) {
      log.info("Lval inttype :" + name);
      tmp_ = t;
      return null;
    }
    //直接指向int
    var INT = ((PointerType) t.getType()).getContained().isIntegerTy();
    //function call
    var PTR = ((PointerType) t.getType()).getContained().isPointerTy();
    //指向一个数组
    var ARR = ((PointerType) t.getType()).getContained().isArrayTy();
    if (INT) {
      if (!ctx.exp().isEmpty()) {
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
      if (!ctx.exp().isEmpty()) {
        tmp_ = f.buildLoad(((PointerType) t.getType()).getContained(), t, curBB_);
        return null;
      } else {
        //todo function call
        return null;
      }
    }

    if (ARR) {
      if (!ctx.exp().isEmpty()) {
        tmp_ = f.buildGEP(t, new ArrayList<>() {{
          add(CONST0);
        }}, curBB_);
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
        if (true) {
          //todo function call
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
    visit(ctx);
    if (!usingInt_) {
      tmp_ = f.getConstantInt(tmpInt_);
    }//using int 会在visitConst里面处理
    log.info("syntax constant num: " + tmpInt_);
    return null;
  }

  /**
   * @author : ai
   * @value: tmpInt_ -> child解析出的int intConst : DECIMAL_CONST | OCTAL_CONST | HEXADECIMAL_CONST ;
   */

  @Override
  public Void visitIntConst(IntConstContext ctx) {
    //todo
    if (ctx.DECIMAL_CONST() != null) {
      tmpInt_ = Integer.parseInt(ctx.DECIMAL_CONST().getText(), 10);
      return null;
    }
    if (ctx.HEXADECIMAL_CONST() != null) {
      tmpInt_ = Integer.parseInt(ctx.HEXADECIMAL_CONST().getText().substring(2), 16);
      return null;
    }
    if (ctx.OCTAL_CONST() != null) {
      tmpInt_ = Integer.parseInt(ctx.OCTAL_CONST().getText(), 8);
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
        if (ctx.unaryOp().NOT() != null) {
          v = f.buildBinary(TAG_.Eq, t, CONST0, curBB_);
          tmp_ = v;
        }
        if (ctx.unaryOp().PLUS() != null) {
          //do nothing
        }
        if (ctx.unaryOp().MINUS() != null) {
          v = f.buildBinary(TAG_.Sub, CONST0, t, curBB_);
          tmp_ = v;
        }
        return null;
      }
      if (ctx.callee() != null) {
        //todo
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

    return null;
  }

  /**
   * funcRParams : param (COMMA param)* ;
   */
  @Override
  public Void visitFuncRParams(FuncRParamsContext ctx) {
    return null;
  }

  /**
   * param : exp | STRING ; STRING 是 void putf() 函数的需要
   */
  @Override
  public Void visitParam(ParamContext ctx) {
    return super.visitParam(ctx);
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
      if (lhs.getType().isI1()) {
        lhs = f.buildZext(lhs, i32Type_, curBB_);
      }
      for (var i = 1; i < ctx.unaryExp().size(); i++) {
        visit(ctx.unaryExp(i));
        var rhs = tmp_;
        //cast i1 value 2 i32
        //llvm is a strong typed language
        if (rhs.getType().isI1()) {
          rhs = f.buildZext(rhs, i32Type_, curBB_);
        }
        if (ctx.mulOp(i - 1).MUL() != null) {
          lhs = f.buildBinary(TAG_.Mul, lhs, rhs, curBB_);
        }
        if (ctx.mulOp(i - 1).DIV() != null) {
          lhs = f.buildBinary(TAG_.Div, lhs, rhs, curBB_);
        }
        if (ctx.mulOp(i - 1).MOD() != null) {
          //x%y=x - (x/y)*y
          var a = f.buildBinary(TAG_.Div, lhs, rhs, curBB_);
          var b = f.buildBinary(TAG_.Mul, a, rhs, curBB_);
          lhs = f.buildBinary(TAG_.Sub, lhs, b, curBB_);
        }
      }
      tmp_ = lhs;
    }
    return null;
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
      if (lhs.getType().isI1()) {
        lhs = f.buildZext(lhs, i32Type_, curBB_);
      }
      for (int i = 1; i < ctx.mulExp().size(); i++) {
        visit(ctx.mulExp(i));
        var rhs = tmp_;

        if (rhs.getType().isI1()) {
          rhs = f.buildZext(rhs, i32Type_, curBB_);
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
    //todo

    return null;
  }


  /**
   * eqExp : relExp (eqOp relExp)* ;
   */
  @Override
  public Void visitEqExp(EqExpContext ctx) {
    //todo

    return super.visitEqExp(ctx);
  }

  /**
   * lAndExp : eqExp (AND eqExp)* ;
   */
  @Override
  public Void visitLAndExp(LAndExpContext ctx) {//todo
    return super.visitLAndExp(ctx);
  }

  /**
   * lOrExp : lAndExp (OR lAndExp)* ;
   */
  @Override
  public Void visitLOrExp(LOrExpContext ctx) {//todo
    return super.visitLOrExp(ctx);
  }

  /**
   * constExp : addExp ;
   *
   * @value : tmpint_ -> res of exp
   * <p>
   * 表达式求和
   */
  @Override
  public Void visitConstExp(ConstExpContext ctx) {//todo
    usingInt_ = true;
    visit(ctx.addExp());
    tmp_ = f.getConstantInt(tmpInt_);
    usingInt_ = false;
    return null;
  }
}
