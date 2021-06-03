package frontend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ir.MyModule;
import ir.types.FunctionType;
import ir.types.IntegerType;
import ir.types.PointerType;
import ir.types.Type;
import ir.types.Type.VoidType;
import ir.values.Constant;
import ir.values.Function;
import ir.values.GlobalVariable;
import ir.values.Value;

// visit 的传参使用全局变量，返回尽量使用 Value
public class Visitor extends SysYBaseVisitor<Value> {
  private MyModule module;
  private List<Value> globList = new ArrayList<Value>();
  private Type integerType = IntegerType.getI32();
  private Type voidType = VoidType.getType();

  // 因为I32Ty,I1yT,I32PtrTy,VoidTy本身不存储任何信息，所以就只声明一次。

  /**
   * program : compUnit ; 初始化 module，定义内置函数
   */
  @Override
  public Value visitProgram(SysYParser.ProgramContext ctx) {
    module = MyModule.getInstance();

    ArrayList<Type> params_empty = new ArrayList<Type>(Arrays.asList());
    ArrayList<Type> params_int = new ArrayList<Type>(Arrays.asList(integerType));
    ArrayList<Type> params_array = new ArrayList<Type>(Arrays.asList(new PointerType(integerType)));
    ArrayList<Type> params_int_and_array = new ArrayList<Type>(
        Arrays.asList(integerType, new PointerType(integerType)));
    // TODO what about putf(string, int, ...) ?

    Function func_getint = new Function("getint", new FunctionType(integerType, params_empty), module, true);
    Function func_getch = new Function("getch", new FunctionType(integerType, params_empty), module, true);
    Function func_getarray = new Function("getarray", new FunctionType(integerType, params_array), module, true);
    Function func_putint = new Function("putint", new FunctionType(voidType, params_int), module, true);
    Function func_putch = new Function("putch", new FunctionType(voidType, params_int), module, true);
    Function func_putarray = new Function("putarray", new FunctionType(voidType, params_int_and_array), module, true);
    Function func_putf = new Function("putf", voidType, module, true);
    Function func_starttime = new Function("_sysy_starttime", new FunctionType(voidType, params_empty), module, true);
    Function func_stoptime = new Function("_sysy_stoptime", new FunctionType(voidType, params_empty), module, true);

    return super.visitProgram(ctx);
  }

  /**
   * compUnit : (funcDef | decl)+ ; 访问每个全局变量或函数，维护当前已经定义的全局变量或函数列表
   */
  @Override
  public Value visitCompUnit(SysYParser.CompUnitContext ctx) {
    int globNum = ctx.getChildCount();

    for (int i = 0; i < globNum; i++) {
      Value globItem = visit(ctx.getChild(i));
      globList.add(globItem);
    }

    return super.visitCompUnit(ctx);
  }

  /**
   * decl : constDecl | varDecl ;
   */
  @Override
  public Value visitDecl(SysYParser.DeclContext ctx) {
    Value decl;
    if (ctx.constDecl() != null) {
      decl = visit(ctx.constDecl());
    } else {
      decl = visit(ctx.varDecl());
    }
    return decl;
    // return super.visitDecl(ctx);
  }

  /**
   * constDecl : CONST_KW bType constDef (COMMA constDef)* SEMICOLON ;
   */
  @Override
  public Constant visitConstDecl(SysYParser.ConstDeclContext ctx) {
    return null;
    // return super.visitConstDecl(ctx);
  }

  /**
   * bType : INT_KW ;
   */
  @Override
  public Value visitBType(SysYParser.BTypeContext ctx) {
    return super.visitBType(ctx);
  }

  /**
   * constDef : IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN constInitVal ;
   */
  @Override
  public Value visitConstDef(SysYParser.ConstDefContext ctx) {
    return super.visitConstDef(ctx);
  }

  /**
   * constInitVal : constExp | (L_BRACE (constInitVal (COMMA constInitVal)*)?
   * R_BRACE) ;
   */
  @Override
  public Value visitConstInitVal(SysYParser.ConstInitValContext ctx) {
    return super.visitConstInitVal(ctx);
  }

  /**
   * varDecl : bType varDef (COMMA varDef)* SEMICOLON ;
   */
  @Override
  public GlobalVariable visitVarDecl(SysYParser.VarDeclContext ctx) {
    return null;
    // return super.visitVarDecl(ctx);
  }

  /**
   * varDef : IDENT (L_BRACKT constExp R_BRACKT)* (ASSIGN initVal)? ;
   */
  @Override
  public Value visitVarDef(SysYParser.VarDefContext ctx) {
    return super.visitVarDef(ctx);
  }

  /**
   * initVal : exp | (L_BRACE (initVal (COMMA initVal)*)? R_BRACE) ;
   */
  @Override
  public Value visitInitVal(SysYParser.InitValContext ctx) {
    return super.visitInitVal(ctx);
  }

  /**
   * funcDef : funcType IDENT L_PAREN funcFParams? R_PAREN block ;
   */
  @Override
  public Function visitFuncDef(SysYParser.FuncDefContext ctx) {
    String typeStr = ctx.getChild(0).getText();
    Type retType;
    switch (typeStr) {
      case "void":
        retType = voidType;
        break;
      case "int":
        retType = integerType;
        break;
      default:
        // throw new Exception();
    }
    return null;
    // return super.visitFuncDef(ctx);
  }

  /**
   * funcType : VOID_KW | INT_KW ;
   */
  @Override
  public Value visitFuncType(SysYParser.FuncTypeContext ctx) {
    return super.visitFuncType(ctx);
  }

  /**
   * funcFParams : funcFParam (COMMA funcFParam)* ;
   */
  @Override
  public Value visitFuncFParams(SysYParser.FuncFParamsContext ctx) {
    return super.visitFuncFParams(ctx);
  }

  /**
   * funcFParam : bType IDENT (L_BRACKT R_BRACKT (L_BRACKT exp R_BRACKT)*)? ;
   */
  @Override
  public Value visitFuncFParam(SysYParser.FuncFParamContext ctx) {
    return super.visitFuncFParam(ctx);
  }

  /**
   * block : L_BRACE blockItem* R_BRACE ;
   */
  @Override
  public Value visitBlock(SysYParser.BlockContext ctx) {
    return super.visitBlock(ctx);
  }

  /**
   * blockItem : constDecl | varDecl | stmt ;
   */
  @Override
  public Value visitBlockItem(SysYParser.BlockItemContext ctx) {
    return super.visitBlockItem(ctx);
  }

  /**
   * stmt : assignStmt | expStmt | block | conditionStmt | whileStmt | breakStmt |
   * continueStmt | returnStmt ;
   */
  @Override
  public Value visitStmt(SysYParser.StmtContext ctx) {
    return super.visitStmt(ctx);
  }

  /**
   * assignStmt : lVal ASSIGN exp SEMICOLON ;
   */
  @Override
  public Value visitAssignStmt(SysYParser.AssignStmtContext ctx) {
    return super.visitAssignStmt(ctx);
  }

  /**
   * expStmt : exp? SEMICOLON ;
   */
  @Override
  public Value visitExpStmt(SysYParser.ExpStmtContext ctx) {
    return super.visitExpStmt(ctx);
  }

  /**
   * conditionStmt : IF_KW L_PAREN cond R_PAREN stmt (ELSE_KW stmt)? ;
   */
  @Override
  public Value visitConditionStmt(SysYParser.ConditionStmtContext ctx) {
    return super.visitConditionStmt(ctx);
  }

  /**
   * whileStmt : WHILE_KW L_PAREN cond R_PAREN stmt ;
   */
  @Override
  public Value visitWhileStmt(SysYParser.WhileStmtContext ctx) {
    return super.visitWhileStmt(ctx);
  }

  /**
   * breakStmt : BREAK_KW SEMICOLON ;
   */
  @Override
  public Value visitBreakStmt(SysYParser.BreakStmtContext ctx) {
    return super.visitBreakStmt(ctx);
  }

  /**
   * continueStmt : CONTINUE_KW SEMICOLON ;
   */
  @Override
  public Value visitContinueStmt(SysYParser.ContinueStmtContext ctx) {
    return super.visitContinueStmt(ctx);
  }

  /**
   * returnStmt : RETURN_KW (exp)? SEMICOLON ;
   */
  @Override
  public Value visitReturnStmt(SysYParser.ReturnStmtContext ctx) {
    return super.visitReturnStmt(ctx);
  }

  /**
   * exp : addExp ;
   */
  @Override
  public Value visitExp(SysYParser.ExpContext ctx) {
    return super.visitExp(ctx);
  }

  /**
   * cond : lOrExp ;
   */
  @Override
  public Value visitCond(SysYParser.CondContext ctx) {
    return super.visitCond(ctx);
  }

  /**
   * lVal : IDENT (L_BRACKT exp R_BRACKT)* ;
   */
  @Override
  public Value visitLVal(SysYParser.LValContext ctx) {
    return super.visitLVal(ctx);
  }

  /**
   * primaryExp : (L_PAREN exp R_PAREN) | lVal | number ;
   */
  @Override
  public Value visitPrimaryExp(SysYParser.PrimaryExpContext ctx) {
    return super.visitPrimaryExp(ctx);
  }

  /**
   * number : intConst ;
   */
  @Override
  public Value visitNumber(SysYParser.NumberContext ctx) {
    return super.visitNumber(ctx);
  }

  /**
   * intConst : DECIMAL_CONST | OCTAL_CONST | HEXADECIMAL_CONST ;
   */
  @Override
  public Value visitIntConst(SysYParser.IntConstContext ctx) {
    return super.visitIntConst(ctx);
  }

  /**
   * unaryExp : primaryExp | callee | (unaryOp unaryExp) ;
   */
  @Override
  public Value visitUnaryExp(SysYParser.UnaryExpContext ctx) {
    return super.visitUnaryExp(ctx);
  }

  /**
   * callee : IDENT L_PAREN funcRParams? R_PAREN ;
   */
  @Override
  public Value visitCallee(SysYParser.CalleeContext ctx) {
    return super.visitCallee(ctx);
  }

  /**
   * unaryOp : PLUS | MINUS | NOT ;
   */
  @Override
  public Value visitUnaryOp(SysYParser.UnaryOpContext ctx) {
    return super.visitUnaryOp(ctx);
  }

  /**
   * funcRParams : param (COMMA param)* ;
   */
  @Override
  public Value visitFuncRParams(SysYParser.FuncRParamsContext ctx) {
    return super.visitFuncRParams(ctx);
  }

  /**
   * param : exp | STRING ; STRING 是 void putf() 函数的需要
   */
  @Override
  public Value visitParam(SysYParser.ParamContext ctx) {
    return super.visitParam(ctx);
  }

  /**
   * mulExp : unaryExp (mulOp unaryExp)* ;
   */
  @Override
  public Value visitMulExp(SysYParser.MulExpContext ctx) {
    return super.visitMulExp(ctx);
  }

  /**
   * mulOp : MUL | DIV | MOD ;
   */
  @Override
  public Value visitMulOp(SysYParser.MulOpContext ctx) {
    return super.visitMulOp(ctx);
  }

  /**
   * addExp : mulExp (addOp mulExp)* ;
   */
  @Override
  public Value visitAddExp(SysYParser.AddExpContext ctx) {
    return super.visitAddExp(ctx);
  }

  /**
   * addOp : PLUS | MINUS ;
   */
  @Override
  public Value visitAddOp(SysYParser.AddOpContext ctx) {
    return super.visitAddOp(ctx);
  }

  /**
   * relExp : addExp (relOp addExp)* ;
   */
  @Override
  public Value visitRelExp(SysYParser.RelExpContext ctx) {
    return super.visitRelExp(ctx);
  }

  /**
   * relOp : LT | GT | LE | GE ;
   */
  @Override
  public Value visitRelOp(SysYParser.RelOpContext ctx) {
    return super.visitRelOp(ctx);
  }

  /**
   * eqExp : relExp (eqOp relExp)* ;
   */
  @Override
  public Value visitEqExp(SysYParser.EqExpContext ctx) {
    return super.visitEqExp(ctx);
  }

  /**
   * eqOp : EQ | NEQ ;
   */
  @Override
  public Value visitEqOp(SysYParser.EqOpContext ctx) {
    return super.visitEqOp(ctx);
  }

  /**
   * lAndExp : eqExp (AND eqExp)* ;
   */
  @Override
  public Value visitLAndExp(SysYParser.LAndExpContext ctx) {
    return super.visitLAndExp(ctx);
  }

  /**
   * lOrExp : lAndExp (OR lAndExp)* ;
   */
  @Override
  public Value visitLOrExp(SysYParser.LOrExpContext ctx) {
    return super.visitLOrExp(ctx);
  }

  /**
   * constExp : addExp ;
   */
  @Override
  public Value visitConstExp(SysYParser.ConstExpContext ctx) {
    return super.visitConstExp(ctx);
  }
}
