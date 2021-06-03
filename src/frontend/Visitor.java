package frontend;


import frontend.SysYParser.*;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.types.FunctionType;
import ir.types.Type;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.Value;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.logging.Logger;
import org.antlr.v4.runtime.tree.ParseTree;
import util.Mylogger;

/**
 * 我们并不需要用返回值传递信息，所以将类型标注为Void
 */

public class Visitor extends SysYBaseVisitor<Void> {

  Logger log = Mylogger.getLogger(Visitor.class);


  private class Scope {

    private Stack<HashMap<String, Value>> symbols_;
    private Stack<HashMap<String, Value>> params;

    public void find(String name) {

    }

    public void addLayer() {
      symbols_.add(new HashMap<>());
      params.add(new HashMap<>());
    }

    public void pop() {
      symbols_.pop();
      params.pop();
    }

    public void addParams() {
      //todo 咱先把没数组的部分弄完
    }

    public void popParams() {
      //todo
    }

    public boolean isGlobal() {
      return this.symbols_.size() == 1;
    }

  }

  // translation context
  private final MyModule m = MyModule.getInstance();
  private final MyFactoryBuilder f = MyFactoryBuilder.getInstance();
  private Scope scope_ = new Scope(); // symbol table
  private BasicBlock curBB_; // current basicBlock
  private Function curFunc_; // current function

  // pass values between `visit` functions
  private Type tmpType_;
  private Value tmp_;
  private int tmpInt_;

  // singleton variables
  private final Type i32Type = f.getI32Ty();
  private final Type voidType = f.getVoidTy();
  private final Type ptri32Type = f.getPointTy(i32Type);

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
   */
  @Override
  public Void visitConstDecl(ConstDeclContext ctx) {
//todo
    return super.visitConstDecl(ctx);
  }

  /**
   * bType : INT_KW ;
   */
  @Override
  public Void visitBType(BTypeContext ctx) {
    //todo
    return super.visitBType(ctx);
  }

  /**
   * constDef : IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN constInitVal ;
   */
  @Override
  public Void visitConstDef(ConstDefContext ctx) {
    //todo
    return super.visitConstDef(ctx);
  }

  /**
   * constInitVal : constExp | (L_BRACE (constInitVal (COMMA constInitVal)*)? R_BRACE) ;
   */
  @Override
  public Void visitConstInitVal(ConstInitValContext ctx) {
    //todo
    return super.visitConstInitVal(ctx);
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
    Type retType = voidType;
    String typeStr = ctx.getChild(0).getText();
    if (typeStr.equals("int")) {
      retType = i32Type;
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
   * funcType : VOID_KW | INT_KW ;
   */
  @Override
  public Void visitFuncType(FuncTypeContext ctx) {
    return super.visitFuncType(ctx);
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
    return super.visitLVal(ctx);
  }

  /**
   * primaryExp : (L_PAREN exp R_PAREN) | lVal | number ;
   */
  @Override
  public Void visitPrimaryExp(PrimaryExpContext ctx) {
    //todo
    return super.visitPrimaryExp(ctx);
  }

  /**
   * number : intConst ;
   */
  @Override
  public Void visitNumber(NumberContext ctx) {
    //todo

    return null;
  }

  /**
   * intConst : DECIMAL_CONST | OCTAL_CONST | HEXADECIMAL_CONST ;
   */
  @Override
  public Void visitIntConst(IntConstContext ctx) {
    //todo
    return super.visitIntConst(ctx);
  }

  /**
   * unaryExp : primaryExp | callee | (unaryOp unaryExp) ;
   */
  @Override
  public Void visitUnaryExp(UnaryExpContext ctx) {
    //todo
    return super.visitUnaryExp(ctx);
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
    //todo
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
    return super.visitMulExp(ctx);
  }

  /**
   * mulOp : MUL | DIV | MOD ;
   */
  @Override
  public Void visitMulOp(MulOpContext ctx) {
    //todo
    return super.visitMulOp(ctx);
  }

  /**
   * addExp : mulExp (addOp mulExp)* ;
   */
  @Override
  public Void visitAddExp(AddExpContext ctx) {
    //todo
    return super.visitAddExp(ctx);
  }

  /**
   * addOp : PLUS | MINUS ;
   */
  @Override
  public Void visitAddOp(AddOpContext ctx) {
    //todo
    return super.visitAddOp(ctx);
  }

  /**
   * relExp : addExp (relOp addExp)* ;
   */
  @Override
  public Void visitRelExp(RelExpContext ctx) {
    //todo
    return super.visitRelExp(ctx);
  }

  /**
   * relOp : LT | GT | LE | GE ;
   */
  @Override
  public Void visitRelOp(RelOpContext ctx) {
    //todo
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
   */
  @Override
  public Void visitConstExp(ConstExpContext ctx) {//todo
    return super.visitConstExp(ctx);
  }
}
