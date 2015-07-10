package com.jetbrains.ther.xdebugger;

import com.intellij.icons.AllIcons;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import com.intellij.xdebugger.frame.presentation.XValuePresentation;
import com.jetbrains.ther.debugger.TheRDebuggerEvaluator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// TODO [xdbg][test]
class TheRXDebuggerEvaluator extends XDebuggerEvaluator {

  @NotNull
  private final TheRDebuggerEvaluator myEvaluator;

  public TheRXDebuggerEvaluator(@NotNull final TheRDebuggerEvaluator evaluator) {
    myEvaluator = evaluator;
  }

  // This method is overridden because XDebugSessionImpl.breakpointReached(XBreakpoint<?>, String, XSuspendContext) calls it anyway
  @Override
  public boolean evaluateCondition(@NotNull final String expression) {
    final ConditionReceiverImpl receiver = new ConditionReceiverImpl();

    myEvaluator.evalCondition(expression, receiver);

    return receiver.myResult;
  }

  @Override
  public void evaluate(@NotNull final String expression,
                       @NotNull final XEvaluationCallback callback,
                       @Nullable final XSourcePosition expressionPosition) {
    myEvaluator.evalExpression(
      expression,
      new ExpressionReceiverImpl(callback)
    );
  }

  private static class ConditionReceiverImpl implements TheRDebuggerEvaluator.ConditionReceiver {

    private boolean myResult = false;

    @Override
    public void receiveResult(final boolean result) {
      myResult = result;
    }

    @Override
    public void receiveError(@NotNull final Exception e) {
      // TODO [xdbg][update]
    }
  }

  private static class ExpressionReceiverImpl implements TheRDebuggerEvaluator.ExpressionReceiver {

    @NotNull
    private final XEvaluationCallback myCallback;

    public ExpressionReceiverImpl(@NotNull final XEvaluationCallback callback) {
      myCallback = callback;
    }

    @Override
    public void receiveResult(@NotNull final String result) {
      myCallback.evaluated(new EvaluatedXValue(result));
    }

    @Override
    public void receiveError(@NotNull final Exception e) {
      // TODO [xdbg][update]
    }

    private static class EvaluatedXValue extends XValue {

      @NotNull
      private final String myValue;

      public EvaluatedXValue(@NotNull final String value) {
        myValue = value;
      }

      @Override
      public void computePresentation(@NotNull final XValueNode node, @NotNull final XValuePlace place) {
        node.setPresentation(
          AllIcons.Debugger.Value,
          new EvaluatedXValuePresentation(myValue),
          false
        );
      }

      private static class EvaluatedXValuePresentation extends XValuePresentation {

        @NotNull
        private final String myValue;

        public EvaluatedXValuePresentation(@NotNull final String value) {
          myValue = value;
        }

        @Override
        public void renderValue(@NotNull final XValueTextRenderer renderer) {
          renderer.renderValue(myValue);
        }
      }
    }
  }
}
