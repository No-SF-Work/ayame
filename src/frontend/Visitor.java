package frontend;


import frontend.SysYParser.*;

public class Visitor extends SysYBaseVisitor<Void> {

  @Override
  public Void visitProgram(ProgramContext ctx) {
    return null;
  }

  @Override
  public Void visitCompUnit(CompUnitContext ctx) {
    return null;
  }

  @Override
  public Void visitDecl(DeclContext ctx) {
    return super.visitDecl(ctx);
  }

  @Override
  public Void visitConstDecl(ConstDeclContext ctx) {
    return super.visitConstDecl(ctx);
  }

  @Override
  public Void visitBType(BTypeContext ctx) {
    return super.visitBType(ctx);
  }

  @Override
  public Void visitConstDef(ConstDefContext ctx) {
    return super.visitConstDef(ctx);
  }

  @Override
  public Void visitConstInitVal(ConstInitValContext ctx) {
    return super.visitConstInitVal(ctx);
  }

  @Override
  public Void visitVarDecl(VarDeclContext ctx) {
    return super.visitVarDecl(ctx);
  }

  @Override
  public Void visitVarDef(VarDefContext ctx) {
    return super.visitVarDef(ctx);
  }

  @Override
  public Void visitInitVal(InitValContext ctx) {
    return super.visitInitVal(ctx);
  }

  @Override
  public Void visitFuncDef(FuncDefContext ctx) {
    return super.visitFuncDef(ctx);
  }

  @Override
  public Void visitFuncType(FuncTypeContext ctx) {
    return super.visitFuncType(ctx);
  }

  @Override
  public Void visitFuncFParams(FuncFParamsContext ctx) {
    return super.visitFuncFParams(ctx);
  }

  @Override
  public Void visitFuncFParam(FuncFParamContext ctx) {
    return super.visitFuncFParam(ctx);
  }

  @Override
  public Void visitBlock(BlockContext ctx) {
    return super.visitBlock(ctx);
  }

  @Override
  public Void visitBlockItem(BlockItemContext ctx) {
    return super.visitBlockItem(ctx);
  }

  @Override
  public Void visitStmt(StmtContext ctx) {
    return super.visitStmt(ctx);
  }

  @Override
  public Void visitAssignStmt(AssignStmtContext ctx) {
    return super.visitAssignStmt(ctx);
  }

  @Override
  public Void visitExpStmt(ExpStmtContext ctx) {
    return super.visitExpStmt(ctx);
  }

  @Override
  public Void visitConditionStmt(ConditionStmtContext ctx) {
    return super.visitConditionStmt(ctx);
  }

  @Override
  public Void visitWhileStmt(WhileStmtContext ctx) {
    return super.visitWhileStmt(ctx);
  }

  @Override
  public Void visitBreakStmt(BreakStmtContext ctx) {
    return super.visitBreakStmt(ctx);
  }

  @Override
  public Void visitContinueStmt(ContinueStmtContext ctx) {
    return super.visitContinueStmt(ctx);
  }

  @Override
  public Void visitReturnStmt(ReturnStmtContext ctx) {
    return super.visitReturnStmt(ctx);
  }

  @Override
  public Void visitExp(ExpContext ctx) {
    return super.visitExp(ctx);
  }

  @Override
  public Void visitCond(CondContext ctx) {
    return super.visitCond(ctx);
  }

  @Override
  public Void visitLVal(LValContext ctx) {
    return super.visitLVal(ctx);
  }

  @Override
  public Void visitPrimaryExp(PrimaryExpContext ctx) {
    return super.visitPrimaryExp(ctx);
  }

  @Override
  public Void visitNumber(NumberContext ctx) {
    return super.visitNumber(ctx);
  }

  @Override
  public Void visitIntConst(IntConstContext ctx) {
    return super.visitIntConst(ctx);
  }

  @Override
  public Void visitUnaryExp(UnaryExpContext ctx) {
    return super.visitUnaryExp(ctx);
  }

  @Override
  public Void visitCallee(CalleeContext ctx) {
    return super.visitCallee(ctx);
  }

  @Override
  public Void visitUnaryOp(UnaryOpContext ctx) {
    return super.visitUnaryOp(ctx);
  }

  @Override
  public Void visitFuncRParams(FuncRParamsContext ctx) {
    return super.visitFuncRParams(ctx);
  }

  @Override
  public Void visitParam(ParamContext ctx) {
    return super.visitParam(ctx);
  }

  @Override
  public Void visitMulExp(MulExpContext ctx) {
    return super.visitMulExp(ctx);
  }

  @Override
  public Void visitMulOp(MulOpContext ctx) {
    return super.visitMulOp(ctx);
  }

  @Override
  public Void visitAddExp(AddExpContext ctx) {
    return super.visitAddExp(ctx);
  }

  @Override
  public Void visitAddOp(AddOpContext ctx) {
    return super.visitAddOp(ctx);
  }

  @Override
  public Void visitRelExp(RelExpContext ctx) {
    return super.visitRelExp(ctx);
  }

  @Override
  public Void visitRelOp(RelOpContext ctx) {
    return super.visitRelOp(ctx);
  }

  @Override
  public Void visitEqExp(EqExpContext ctx) {

    return super.visitEqExp(ctx);
  }

  @Override
  public Void visitEqOp(EqOpContext ctx) {
    return super.visitEqOp(ctx);
  }

  @Override
  public Void visitLAndExp(LAndExpContext ctx) {
    return super.visitLAndExp(ctx);
  }

  @Override
  public Void visitLOrExp(LOrExpContext ctx) {
    return super.visitLOrExp(ctx);
  }

  @Override
  public Void visitConstExp(ConstExpContext ctx) {
    return super.visitConstExp(ctx);
  }
}
