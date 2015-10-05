package com.jetbrains.ther.debugger.function;

import com.jetbrains.ther.debugger.TheROutputReceiver;
import com.jetbrains.ther.debugger.exception.TheRDebuggerException;
import com.jetbrains.ther.debugger.exception.TheRUnexpectedExecutionResultException;
import com.jetbrains.ther.debugger.executor.TheRExecutionResult;
import com.jetbrains.ther.debugger.executor.TheRExecutionResultType;
import com.jetbrains.ther.debugger.executor.TheRExecutor;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.ther.debugger.executor.TheRExecutionResultType.*;

class TheRBraceFunctionDebugger extends TheRFunctionDebuggerBase {

  public TheRBraceFunctionDebugger(@NotNull final TheRExecutor executor,
                                   @NotNull final TheRFunctionDebuggerFactory debuggerFactory,
                                   @NotNull final TheRFunctionDebuggerHandler debuggerHandler,
                                   @NotNull final TheROutputReceiver outputReceiver,
                                   @NotNull final String functionName) throws TheRDebuggerException {
    super(executor, debuggerFactory, debuggerHandler, outputReceiver, functionName);
  }

  @Override
  protected void handleExecutionResult(@NotNull final TheRExecutionResult result) throws TheRDebuggerException {
    switch (result.getType()) {
      case CONTINUE_TRACE:
        handleContinueTrace(result);
        break;
      case DEBUG_AT:
        handleDebugAt(result, true, true);
        break;
      case DEBUGGING_IN:
        handleDebuggingIn(result);
        break;
      case EMPTY:
        handleEmpty(result);
        break;
      case EXITING_FROM:
        handleEndTrace(result);
        break;
      case RECURSIVE_EXITING_FROM:
        handleRecursiveEndTrace(result);
        break;
      default:
        throw new TheRUnexpectedExecutionResultException(
          "Actual type is not the same as expected: " +
          "[" +
          "actual: " + result.getType() + ", " +
          "expected: " +
          "[" +
          CONTINUE_TRACE + ", " +
          DEBUG_AT + ", " +
          DEBUGGING_IN + ", " +
          EMPTY + ", " +
          EXITING_FROM + ", " +
          RECURSIVE_EXITING_FROM +
          "]" +
          "]"
        );
    }
  }

  @NotNull
  @Override
  protected TheRExecutionResultType getStartTraceType() {
    return START_TRACE_BRACE;
  }
}
