package frontend;

import ir.Type;

public class Visitor extends SysYBaseVisitor {

  @Override
  public Type visitProgram(SysYParser.ProgramContext ctx) {
    return visitProgram(ctx);
  }

  @Override
  public Type visitCompUnit(SysYParser.CompUnitContext ctx) {
    return visitCompUnit(ctx);
  }

  @Override
  public Type visitDecl(SysYParser.DeclContext ctx) {
    return visitDecl(ctx);
  }

  @Override
  public Type visitConstDecl(SysYParser.ConstDeclContext ctx) {
    return visitConstDecl(ctx);
  }

  @Override
  public Type visitBType(SysYParser.BTypeContext ctx) {
    return visitBType(ctx);
  }

  @Override
  public Type visitConstDef(SysYParser.ConstDefContext ctx) {
    return visitConstDef(ctx);
  }

  @Override
  public Type visitConstInitVal(SysYParser.ConstInitValContext ctx) {
    return visitConstInitVal(ctx);
  }

  @Override
  public Type visitVarDecl(SysYParser.VarDeclContext ctx) {
    return visitVarDecl(ctx);
  }

  @Override
  public Type visitVarDef(SysYParser.VarDefContext ctx) {
    return visitVarDef(ctx);
  }

  @Override
  public Type visitInitVal(SysYParser.InitValContext ctx) {
    return visitInitVal(ctx);
  }

  @Override
  public Type visitFuncDef(SysYParser.FuncDefContext ctx) {
    return visitFuncDef(ctx);
  }

  @Override
  public Type visitFuncType(SysYParser.FuncTypeContext ctx) {
    return visitFuncType(ctx);
  }

  @Override
  public Type visitFuncFParams(SysYParser.FuncFParamsContext ctx) {
    return visitFuncFParams(ctx);
  }

  @Override
  public Type visitFuncFParam(SysYParser.FuncFParamContext ctx) {
    return visitFuncFParam(ctx);
  }

  @Override
  public Type visitBlock(SysYParser.BlockContext ctx) {
    return visitBlock(ctx);
  }

  @Override
  public Type visitBlockItem(SysYParser.BlockItemContext ctx) {
    return visitBlockItem(ctx);
  }

  @Override
  public Type visitStmt(SysYParser.StmtContext ctx) {
    return visitStmt(ctx);
  }

  @Override
  public Type visitAssignStmt(SysYParser.AssignStmtContext ctx) {
    return visitAssignStmt(ctx);
  }

  @Override
  public Type visitExpStmt(SysYParser.ExpStmtContext ctx) {
    return visitExpStmt(ctx);
  }

  @Override
  public Type visitConditionStmt(SysYParser.ConditionStmtContext ctx) {
    return visitConditionStmt(ctx);
  }

  @Override
  public Type visitWhileStmt(SysYParser.WhileStmtContext ctx) {
    return visitWhileStmt(ctx);
  }

  @Override
  public Type visitBreakStmt(SysYParser.BreakStmtContext ctx) {
    return visitBreakStmt(ctx);
  }

  @Override
  public Type visitContinueStmt(SysYParser.ContinueStmtContext ctx) {
    return visitContinueStmt(ctx);
  }

  @Override
  public Type visitReturnStmt(SysYParser.ReturnStmtContext ctx) {
    return visitReturnStmt(ctx);
  }

  @Override
  public Type visitExp(SysYParser.ExpContext ctx) {
    return visitExp(ctx);
  }

  @Override
  public Type visitCond(SysYParser.CondContext ctx) {
    return visitCond(ctx);
  }

  @Override
  public Type visitLVal(SysYParser.LValContext ctx) {
    return visitLVal(ctx);
  }

  @Override
  public Type visitPrimaryExp(SysYParser.PrimaryExpContext ctx) {
    return visitPrimaryExp(ctx);
  }

  @Override
  public Type visitNumber(SysYParser.NumberContext ctx) {
    return visitNumber(ctx);
  }

  @Override
  public Type visitIntConst(SysYParser.IntConstContext ctx) {
    return visitIntConst(ctx);
  }

  @Override
  public Type visitUnaryExp(SysYParser.UnaryExpContext ctx) {
    return visitUnaryExp(ctx);
  }

  @Override
  public Type visitCallee(SysYParser.CalleeContext ctx) {
    return visitCallee(ctx);
  }

  @Override
  public Type visitUnaryOp(SysYParser.UnaryOpContext ctx) {
    return visitUnaryOp(ctx);
  }

  @Override
  public Type visitFuncRParams(SysYParser.FuncRParamsContext ctx) {
    return visitFuncRParams(ctx);
  }

  @Override
  public Type visitParam(SysYParser.ParamContext ctx) {
    return visitParam(ctx);
  }

  @Override
  public Type visitMulExp(SysYParser.MulExpContext ctx) {
    return visitMulExp(ctx);
  }

  @Override
  public Type visitMulOp(SysYParser.MulOpContext ctx) {
    return visitMulOp(ctx);
  }

  @Override
  public Type visitAddExp(SysYParser.AddExpContext ctx) {
    return visitAddExp(ctx);
  }

  @Override
  public Type visitAddOp(SysYParser.AddOpContext ctx) {
    return visitAddOp(ctx);
  }

  @Override
  public Type visitRelExp(SysYParser.RelExpContext ctx) {
    return visitRelExp(ctx);
  }

  @Override
  public Type visitRelOp(SysYParser.RelOpContext ctx) {
    return visitRelOp(ctx);
  }

  @Override
  public Type visitEqExp(SysYParser.EqExpContext ctx) {
    return visitEqExp(ctx);
  }

  @Override
  public Type visitEqOp(SysYParser.EqOpContext ctx) {
    return visitEqOp(ctx);
  }

  @Override
  public Type visitLAndExp(SysYParser.LAndExpContext ctx) {
    return visitLAndExp(ctx);
  }

  @Override
  public Type visitLOrExp(SysYParser.LOrExpContext ctx) {
    return visitLOrExp(ctx);
  }

  @Override
  public Type visitConstExp(SysYParser.ConstExpContext ctx) {
    return visitConstExp(ctx);
  }
}
