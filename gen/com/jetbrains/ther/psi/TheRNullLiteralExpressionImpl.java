// This is a generated file. Not intended for manual editing.
package com.jetbrains.ther.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.jetbrains.ther.parsing.TheRElementTypes.*;
import com.jetbrains.ther.psi.api.*;

public class TheRNullLiteralExpressionImpl extends TheRExpressionImpl implements TheRNullLiteralExpression {

  public TheRNullLiteralExpressionImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof TheRVisitor) ((TheRVisitor)visitor).visitNullLiteralExpression(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public PsiElement getNull() {
    return findNotNullChildByType(THE_R_NULL);
  }

}
