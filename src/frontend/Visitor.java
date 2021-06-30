package frontend;


import frontend.SysYParser.*;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.types.FunctionType;
import ir.types.Type;
import ir.values.BasicBlock;
import ir.values.Constant;
import ir.values.Constants.ConstantInt;
import ir.values.Function;
import ir.values.Value;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.logging.Logger;
import util.Mylogger;

/**
 * 我们并不需要用返回值传递信息，所以将类型标注为Void
 */

public class Visitor extends SysYBaseVisitor<Void> {

  Logger log = Mylogger.getLogger(Visitor.class);


  private class Scope {

    //因为涉及了往上层查找参数，所以这里不用stack用arraylist
    private ArrayList<HashMap<String, Value>> tables_;
    private Stack<HashMap<String, Value>> params;

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
      params.add(new HashMap<>());
    }

    public void pop() {
      tables_.remove(tables_.size() - 1);
      params.pop();
    }

    public void addParams() {
      //todo for functionDef
    }

    public void popParams() {
      //todo
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

  // translation context
  private final MyModule m = MyModule.getInstance();
  private final MyFactoryBuilder f = MyFactoryBuilder.getInstance();
  private Scope scope_ = new Scope(); // symbol table
  private BasicBlock curBB_; // current basicBlock
  private Function curFunc_; // current function

  // pass values between `visit` functions
  private ArrayList<Value> tmpArr;//只能赋值以及被赋值，不能直接在上面add
  private Type tmpType_;
  private Value tmp_;
  private int tmpInt_;
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
    return super.visitCompUnit(ctx);
  }

  /**
   * decl : constDecl | varDecl ;
   */
  @Override
  public Void visitDecl(DeclContext ctx) {
    return super.visitDecl(ctx);
  }

  /**
   * constDecl : CONST_KW bType constDef (COMMA constDef)* SEMICOLON ;
   *
   */

  @Override
  public Void visitConstDecl(ConstDeclContext ctx) {
    //Btyoe is always int
    for (ConstDefContext constDefContext : ctx.constDef()) {
      visit(constDefContext);
    }
    return null;
  }

  /**
   * bType : INT_KW ;
   */
  @Override
  public Void visitBType(BTypeContext ctx) {
    return super.visitBType(ctx);
  }

  //把一堆Constant封装为一个按照dims排列的ConstArr
  public Constant genConstArr(ArrayList<Integer> dims, ArrayList<Value> inits) {
    //todo
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
    log.info("visit ConstDef ");
    var name = ctx.IDENT().getText();
    if (scope_.top().get(name) != null) {
      throw new SyntaxException("name already exists");
    }
    if (ctx.constExp().isEmpty()) { //not array
      if (ctx.constInitVal() != null) {
        visit(ctx.constInitVal());
      } else {
        throw new SyntaxException("Defining const without initVal");
      }

    } else {// array
      //calculate dims of array
      var arrty = i32Type_;
      var dims = new ArrayList<Integer>();//ndims
      for (ConstExpContext constExpContext : ctx.constExp()) {
        visit(constExpContext);
        dims.add(((ConstantInt) tmp_).getVal());
      }
      for (var i = dims.size() - 1; i > 0; i--) {
        arrty = f.getArrayTy(arrty, dims.get(i));// arr(arr(arr(i32,dim1),dim2),dim3)
      }
      if (scope_.isGlobal()) {
        if (!ctx.constInitVal().isEmpty()) {
          ctx.constInitVal().dimInfo_ = dims;//todo check
          visit(ctx.constInitVal());//dim.size()=n
          var initializer = genConstArr(dims, tmpArr);
          var variable = f.getGlobalvariable(ctx.IDENT().getText(), arrty, initializer);
          variable.setConst();
          scope_.put(ctx.IDENT().getText(), variable);
        } else {
          var variable = f.getGlobalvariable(ctx.IDENT().getText(), arrty, CONST0);
          scope_.put(ctx.IDENT().getText(), variable);
        }
      } else {
        //todo local const var def

      }
    }
    return null;
  }

  /**
   constInitVal : constExp | (L_BRACE (constInitVal (COMMA constInitVal)*)? R_BRACE)
   * ;
   */
  @Override
  public Void visitConstInitVal(ConstInitValContext ctx) {
    //ConstInitVal 和 数组结构一样，是嵌套的，逻辑是把每一层的初始值放进去，然后不足的补0
    if ((!ctx.constExp().isEmpty()) && ctx.dimInfo_.isEmpty()) {
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
          arrOfCurDim.addAll(tmpArr);
        } else {
          visit(constInitValContext);
          arrOfCurDim.add(tmp_);
        }
      }
      for (int i = arrOfCurDim.size(); i < curDimLength * sizeOfEachEle; i++) {
        arrOfCurDim.add(CONST0);
      }//长度不足一个ele*dimsize 的补0
      tmpArr = arrOfCurDim;
    }
    return null;
  }

  /**
   * varDecl : bType varDef (COMMA varDef)* SEMICOLON ;
   */
  @Override
  public Void visitVarDecl(VarDeclContext ctx) {
    //todo

    return super.visitVarDecl(ctx);
  }

  /**
   * varDef : IDENT (L_BRACKT constExp R_BRACKT)* (ASSIGN initVal)? ;
   */
  @Override
  public Void visitVarDef(VarDefContext ctx) {
    //todo
    return super.visitVarDef(ctx);
  }

  /**
   * initVal : exp | (L_BRACE (initVal (COMMA initVal)*)? R_BRACE) ;
   */
  @Override
  public Void visitInitVal(InitValContext ctx) {
    //todo
    return super.visitInitVal(ctx);
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
    ArrayList<Type> paramTypeList = new ArrayList<>();
    if (ctx.funcFParams() != null) {
      FuncFParamsContext paramsContext = ctx.funcFParams();
      int paramNum = (ctx.funcFParams().getChildCount() + 1) / 2;
      for (int i = 0; i < paramNum; i++) {
        FuncFParamContext paramContext = paramsContext.funcFParam(i);
        // get each parameter information
      }
    }

    // build function object
    FunctionType functionType = f.getFuncTy(retType, paramTypeList);
    f.buildFunction(functionName, functionType);
    curFunc_ = m.__functions.getLast().getVal();

    // add to symbol table

    // visit block and create basic blocks

    log.info("funcDef end@" + functionName);

    return null;
//    return super.visitFuncDef(ctx);
  }

  /**
   * funcType : VOID_KW | INT_KW ; * @value : tmpType-> stored type
   */
  @Override
  public Void visitFuncType(FuncTypeContext ctx) {
    if (ctx.INT_KW() != null) {
      tmpType_ = voidType_;
    } else if (ctx.VOID_KW() != null) {
      tmpType_ = i32Type_;
    }
    return null;
  }

  /**
   * funcFParams : funcFParam (COMMA funcFParam)* ;
   */
  @Override
  public Void visitFuncFParams(FuncFParamsContext ctx) {
    return super.visitFuncFParams(ctx);
  }

  /**
   * funcFParam : bType IDENT (L_BRACKT R_BRACKT (L_BRACKT exp R_BRACKT)*)? ;
   */
  @Override
  public Void visitFuncFParam(FuncFParamContext ctx) {
    //todo
    return super.visitFuncFParam(ctx);
  }

  /**
   * block : L_BRACE blockItem* R_BRACE ;
   */
  @Override
  public Void visitBlock(BlockContext ctx) {
    //todo
    return super.visitBlock(ctx);
  }

  /**
   * blockItem : constDecl | varDecl | stmt ;
   */
  @Override
  public Void visitBlockItem(BlockItemContext ctx) {
    //todo
    return super.visitBlockItem(ctx);
  }

  /**
   * stmt : assignStmt | expStmt | block | conditionStmt | whileStmt | breakStmt | continueStmt |
   * returnStmt ;
   */
  @Override
  public Void visitStmt(StmtContext ctx) {
    //todo
    return super.visitStmt(ctx);
  }

  /**
   * assignStmt : lVal ASSIGN exp SEMICOLON ;
   */
  @Override
  public Void visitAssignStmt(AssignStmtContext ctx) {
    //todo
    return super.visitAssignStmt(ctx);
  }

  /**
   * expStmt : exp? SEMICOLON ;
   */
  @Override
  public Void visitExpStmt(ExpStmtContext ctx) {
    //todo
    return super.visitExpStmt(ctx);
  }

  /**
   * conditionStmt : IF_KW L_PAREN cond R_PAREN stmt (ELSE_KW stmt)? ;
   */
  @Override
  public Void visitConditionStmt(ConditionStmtContext ctx) {
    //todo
    return super.visitConditionStmt(ctx);
  }

  /**
   * whileStmt : WHILE_KW L_PAREN cond R_PAREN stmt ;
   */
  @Override
  public Void visitWhileStmt(WhileStmtContext ctx) {
    //todo
    return super.visitWhileStmt(ctx);
  }

  /**
   * breakStmt : BREAK_KW SEMICOLON ;
   */
  @Override
  public Void visitBreakStmt(BreakStmtContext ctx) {
    //todo
    return super.visitBreakStmt(ctx);
  }

  /**
   * continueStmt : CONTINUE_KW SEMICOLON ;
   */
  @Override
  public Void visitContinueStmt(ContinueStmtContext ctx) {
    //todo
    return super.visitContinueStmt(ctx);
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
    //todo
    return super.visitExp(ctx);
  }

  /**
   * cond : lOrExp ;
   */
  @Override
  public Void visitCond(CondContext ctx) {
    //todo
    return super.visitCond(ctx);
  }

  /**
   * lVal : IDENT (L_BRACKT exp R_BRACKT)* ;
   */
  @Override
  public Void visitLVal(LValContext ctx) {
    //todo
    String name = ctx.IDENT().getText();
    tmp_ = scope_.find(name);
    if (tmp_ == null) {
      throw new SyntaxException("undefined value name" + name);
    }
    if (tmp_.getType().isIntegerTy()) { //todo
      log.info("Lval inttype :" + name);
      return null;
    }
    //todo
    return null;
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
      //todo
    }

    //todo 处理函数调用
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
      }
    } else {
      //todo
    }

    return null;
  }

  /**
   * callee : IDENT L_PAREN funcRParams? R_PAREN ;
   */
  @Override
  public Void visitCallee(CalleeContext ctx) {

    //todo
    return super.visitCallee(ctx);
  }

  /**
   * unaryOp : PLUS | MINUS | NOT ;
   */
  @Override
  public Void visitUnaryOp(UnaryOpContext ctx) {
    //useless
    return super.visitUnaryOp(ctx);
  }

  /**
   * funcRParams : param (COMMA param)* ;
   */
  @Override
  public Void visitFuncRParams(FuncRParamsContext ctx) {
    //todo
    return super.visitFuncRParams(ctx);
  }

  /**
   * param : exp | STRING ; STRING 是 void putf() 函数的需要
   */
  @Override
  public Void visitParam(ParamContext ctx) {
    //todo
    return super.visitParam(ctx);
  }

  /**
   * mulExp : unaryExp (mulOp unaryExp)* ;
   */
  @Override
  public Void visitMulExp(MulExpContext ctx) {
    //todo
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
      //todo
    }
    return null;
  }

  /**
   * mulOp : MUL | DIV | MOD ;
   */
  @Override
  public Void visitMulOp(MulOpContext ctx) {
    //useless
    return null;
  }

  /**
   * @value :
   * <p>
   * addExp : mulExp (addOp mulExp)* ;
   */
  @Override
  public Void visitAddExp(AddExpContext ctx) {
    //todo
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
      //todo
    }
    return null;
  }

  /**
   * addOp : PLUS | MINUS ;
   */
  @Override
  public Void visitAddOp(AddOpContext ctx) {
    //useless
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
   * relOp : LT | GT | LE | GE ;
   */
  @Override
  public Void visitRelOp(RelOpContext ctx) {
    //useless
    return super.visitRelOp(ctx);
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
   * eqOp : EQ | NEQ ;
   */
  @Override
  public Void visitEqOp(EqOpContext ctx) {//todo

    return super.visitEqOp(ctx);
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
