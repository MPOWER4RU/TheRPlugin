package com.jetbrains.ther.debugger.function;

import com.intellij.openapi.util.TextRange;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.ther.debugger.MockitoUtils;
import com.jetbrains.ther.debugger.data.TheRLocation;
import com.jetbrains.ther.debugger.exception.TheRDebuggerException;
import com.jetbrains.ther.debugger.exception.TheRRuntimeException;
import com.jetbrains.ther.debugger.executor.TheRExecutionResult;
import com.jetbrains.ther.debugger.executor.TheRExecutionResultType;
import com.jetbrains.ther.debugger.executor.TheRExecutor;
import com.jetbrains.ther.debugger.mock.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.ther.debugger.data.TheRCommands.EXECUTE_AND_STEP_COMMAND;
import static com.jetbrains.ther.debugger.data.TheRFunctionConstants.SERVICE_ENTER_FUNCTION_SUFFIX;
import static com.jetbrains.ther.debugger.data.TheRFunctionConstants.SERVICE_FUNCTION_PREFIX;
import static com.jetbrains.ther.debugger.data.TheRResponseConstants.*;
import static com.jetbrains.ther.debugger.mock.MockTheRExecutor.LS_FUNCTIONS_ERROR;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TheRUnbraceFunctionDebuggerTest {

  @Test
  public void ordinary() throws TheRDebuggerException {
    /*
    `x + 1`
    */

    final OrdinaryTheRExecutor executor = new OrdinaryTheRExecutor();
    final MockTheROutputReceiver receiver = new MockTheROutputReceiver();

    final TheRUnbraceFunctionDebugger debugger = new TheRUnbraceFunctionDebugger(
      executor,
      new MockTheRFunctionDebuggerFactory(null),
      new IllegalTheRFunctionDebuggerHandler(),
      receiver,
      "abc"
    );

    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation("abc", 0), debugger.getLocation());
    assertEquals(1, executor.getCounter());
    assertEquals(Collections.emptyList(), receiver.getOutputs());
    assertEquals(Collections.singletonList(LS_FUNCTIONS_ERROR), receiver.getErrors());

    receiver.reset();
    debugger.advance();

    assertFalse(debugger.hasNext());
    assertEquals(new TheRLocation("abc", -1), debugger.getLocation());
    assertEquals("", debugger.getResult());
    assertEquals(2, executor.getCounter());
    assertEquals(Collections.emptyList(), receiver.getOutputs());
    assertEquals(Collections.singletonList("error_exit"), receiver.getErrors());
  }

  @Test
  public void function() throws TheRDebuggerException {
    /*
    f()
    */

    final FunctionTheRExecutor executor = new FunctionTheRExecutor();
    final MockTheRFunctionDebuggerFactory factory = new MockTheRFunctionDebuggerFactory(new IllegalTheRFunctionDebugger());
    final FunctionTheRFunctionDebuggerHandler handler = new FunctionTheRFunctionDebuggerHandler();
    final MockTheROutputReceiver receiver = new MockTheROutputReceiver();

    final TheRUnbraceFunctionDebugger debugger = new TheRUnbraceFunctionDebugger(
      executor,
      factory,
      handler,
      receiver,
      "abc"
    );

    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation("abc", 0), debugger.getLocation());
    assertEquals(1, executor.getCounter());
    assertEquals(0, factory.getCounter());
    assertEquals(0, handler.getCounter());
    assertEquals(Collections.emptyList(), receiver.getOutputs());
    assertEquals(Collections.singletonList(LS_FUNCTIONS_ERROR), receiver.getErrors());

    receiver.reset();
    debugger.advance();

    // one of next debuggers will handle `RECURSIVE_EXITING_FROM`

    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation("abc", 0), debugger.getLocation());
    assertEquals(2, executor.getCounter());
    assertEquals(1, factory.getCounter());
    assertEquals(1, handler.getCounter());
    assertEquals(Collections.emptyList(), receiver.getOutputs());
    assertEquals(Collections.singletonList("error_debugging"), receiver.getErrors());
  }

  @Test
  public void recursiveReturnAndOutputBefore() throws TheRDebuggerException {
    /*
    `x + 1` with recursive return
    */

    final TheRExecutionResult secondResult = new TheRExecutionResult(
      "[1] 1 2 3\n" +
      EXITING_FROM_PREFIX + "FUN(c(-1, 0, 1)[[3L]], ...)\n" +
      EXITING_FROM_PREFIX + "def()\n" +
      EXITING_FROM_PREFIX + "abc(1:10)\n" +
      BROWSE_PREFIX + "1" + BROWSE_SUFFIX,
      TheRExecutionResultType.RECURSIVE_EXITING_FROM,
      new TextRange(0, 9),
      "error_exit"
    );

    recursiveReturn(secondResult, true, -1);
  }

  @Test
  public void recursiveReturnAndOutputInside() throws TheRDebuggerException {
    /*
    `x + 1` with recursive return
    */

    final TheRExecutionResult secondResult = new TheRExecutionResult(
      EXITING_FROM_PREFIX + "FUN(c(-1, 0, 1)[[3L]], ...)\n" +
      "[1] 1 2 3\n" +
      EXITING_FROM_PREFIX + "def()\n" +
      EXITING_FROM_PREFIX + "abc(1:10)\n" +
      BROWSE_PREFIX + "1" + BROWSE_SUFFIX,
      TheRExecutionResultType.RECURSIVE_EXITING_FROM,
      new TextRange(42, 51),
      "error_exit"
    );

    recursiveReturn(secondResult, true, -1);
  }

  @Test
  public void recursiveReturnAndOutputAfter() throws TheRDebuggerException {
    /*
    `x + 1` with recursive return
    */

    final TheRExecutionResult secondResult = new TheRExecutionResult(
      EXITING_FROM_PREFIX + "FUN(c(-1, 0, 1)[[3L]], ...)\n" +
      EXITING_FROM_PREFIX + "def()\n" +
      EXITING_FROM_PREFIX + "abc(1:10)\n" +
      "[1] 1 2 3\n" +
      BROWSE_PREFIX + "1" + BROWSE_SUFFIX,
      TheRExecutionResultType.RECURSIVE_EXITING_FROM,
      new TextRange(86, 95),
      "error_exit"
    );

    recursiveReturn(secondResult, false, -1);
  }

  @Test
  public void recursiveReturnAndOutputBeforeAndDebugAt() throws TheRDebuggerException {

    /*
    `x + 1` with recursive return and `debug at` at the end
    */

    final TheRExecutionResult secondResult = new TheRExecutionResult(
      "[1] 1 2 3\n" +
      EXITING_FROM_PREFIX + "FUN(c(-1, 0, 1)[[3L]], ...)\n" +
      EXITING_FROM_PREFIX + "def()\n" +
      EXITING_FROM_PREFIX + "abc(1:10)\n" +
      DEBUG_AT_LINE_PREFIX + "6: x <- c(1)" +
      BROWSE_PREFIX + "1" + BROWSE_SUFFIX,
      TheRExecutionResultType.RECURSIVE_EXITING_FROM,
      new TextRange(0, 9),
      "error_exit"
    );

    recursiveReturn(secondResult, true, 5);
  }

  @Test
  public void recursiveReturnAndOutputInsideAndDebugAt() throws TheRDebuggerException {
    /*
    `x + 1` with recursive return and `debug at` at the end
    */

    final TheRExecutionResult secondResult = new TheRExecutionResult(
      EXITING_FROM_PREFIX + "FUN(c(-1, 0, 1)[[3L]], ...)\n" +
      "[1] 1 2 3\n" +
      EXITING_FROM_PREFIX + "def()\n" +
      EXITING_FROM_PREFIX + "abc(1:10)\n" +
      DEBUG_AT_LINE_PREFIX + "6: x <- c(1)" +
      BROWSE_PREFIX + "1" + BROWSE_SUFFIX,
      TheRExecutionResultType.RECURSIVE_EXITING_FROM,
      new TextRange(42, 51),
      "error_exit"
    );

    recursiveReturn(secondResult, true, 5);
  }

  @Test
  public void recursiveReturnAndOutputAfterAndDebugAt() throws TheRDebuggerException {
    /*
    `x + 1` with recursive return and `debug at` at the end
    */

    final TheRExecutionResult secondResult = new TheRExecutionResult(
      EXITING_FROM_PREFIX + "FUN(c(-1, 0, 1)[[3L]], ...)\n" +
      EXITING_FROM_PREFIX + "def()\n" +
      EXITING_FROM_PREFIX + "abc(1:10)\n" +
      "[1] 1 2 3\n" +
      DEBUG_AT_LINE_PREFIX + "6: x <- c(1)" +
      BROWSE_PREFIX + "1" + BROWSE_SUFFIX,
      TheRExecutionResultType.RECURSIVE_EXITING_FROM,
      new TextRange(86, 95),
      "error_exit"
    );

    recursiveReturn(secondResult, false, 5);
  }

  @Test
  public void exitingFromWithOutputBefore() throws TheRDebuggerException {
    /*
    `print(x + 1)`
    */

    final TheRExecutionResult secondResult = new TheRExecutionResult(
      "[1] 1 2 3\n" +
      EXITING_FROM_PREFIX + "abc(c(1:10))\n" +
      BROWSE_PREFIX + "1" + BROWSE_SUFFIX,
      TheRExecutionResultType.EXITING_FROM,
      new TextRange(0, 9),
      "error_exit"
    );

    exitingFromWithOutput(secondResult, -1);
  }

  @Test
  public void exitingFromWithOutputAfter() throws TheRDebuggerException {
    /*
    `x + 1` with `debug at` at the end
    */

    final TheRExecutionResult secondResult = new TheRExecutionResult(
      EXITING_FROM_PREFIX + "abc(c(1:10))\n" +
      "[1] 1 2 3\n" +
      DEBUG_AT_LINE_PREFIX + "6: x <- c(1)\n" +
      BROWSE_PREFIX + "1" + BROWSE_SUFFIX,
      TheRExecutionResultType.EXITING_FROM,
      new TextRange(27, 36),
      "error_exit"
    );

    exitingFromWithOutput(secondResult, 5);
  }

  @Test
  public void continueTrace() throws TheRDebuggerException {
    /*
    `x + 1` with `continue trace`
    */

    final ContinueTraceTheRExecutor executor = new ContinueTraceTheRExecutor();
    final MockTheROutputReceiver receiver = new MockTheROutputReceiver();

    final TheRUnbraceFunctionDebugger debugger = new TheRUnbraceFunctionDebugger(
      executor,
      new MockTheRFunctionDebuggerFactory(null),
      new IllegalTheRFunctionDebuggerHandler(),
      receiver,
      "abc"
    );

    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation("abc", 0), debugger.getLocation());
    assertEquals(1, executor.getCounter());
    assertEquals(Collections.emptyList(), receiver.getOutputs());
    assertEquals(Collections.singletonList(LS_FUNCTIONS_ERROR), receiver.getErrors());

    receiver.reset();
    debugger.advance();

    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation("abc", 0), debugger.getLocation());
    assertEquals(5, executor.getCounter());
    assertEquals(Collections.emptyList(), receiver.getOutputs());
    assertEquals(Arrays.asList("error_continue", "error_entry", "error_entry", LS_FUNCTIONS_ERROR), receiver.getErrors());

    receiver.reset();
    debugger.advance();

    assertFalse(debugger.hasNext());
    assertEquals(new TheRLocation("abc", -1), debugger.getLocation());
    assertEquals("[1] 4 5 6", debugger.getResult());
    assertEquals(6, executor.getCounter());
    assertEquals(Collections.singletonList("[1] 4 5 6"), receiver.getOutputs());
    assertEquals(Collections.singletonList("error_exit"), receiver.getErrors());
  }

  @Test(expected = TheRRuntimeException.class)
  public void error() throws TheRDebuggerException {
    /*
    if (10 > log(-1)) {
      print("ok")
    }
    */

    final ErrorTheRExecutor executor = new ErrorTheRExecutor();
    final MockTheROutputReceiver receiver = new MockTheROutputReceiver();

    final TheRUnbraceFunctionDebugger debugger = new TheRUnbraceFunctionDebugger(
      executor,
      new MockTheRFunctionDebuggerFactory(null),
      new IllegalTheRFunctionDebuggerHandler(),
      receiver,
      "abc"
    );

    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation("abc", 0), debugger.getLocation());
    assertEquals(1, executor.getCounter());
    assertEquals(Collections.emptyList(), receiver.getOutputs());
    assertEquals(Collections.singletonList(LS_FUNCTIONS_ERROR), receiver.getErrors());

    debugger.advance();
  }


  @Test
  public void loop() throws TheRDebuggerException {
    /*
    for (i in 1:2) ...
    */

    final LoopTheRExecutor executor = new LoopTheRExecutor();
    final MockTheROutputReceiver receiver = new MockTheROutputReceiver();

    final TheRUnbraceFunctionDebugger debugger = new TheRUnbraceFunctionDebugger(
      executor,
      new MockTheRFunctionDebuggerFactory(null),
      new IllegalTheRFunctionDebuggerHandler(),
      receiver,
      "abc"
    );

    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation("abc", 0), debugger.getLocation());
    assertEquals(1, executor.getCounter());
    assertEquals(Collections.emptyList(), receiver.getOutputs());
    assertEquals(Collections.singletonList(LS_FUNCTIONS_ERROR), receiver.getErrors());

    receiver.reset();
    debugger.advance();

    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation("abc", 0), debugger.getLocation());
    assertEquals(3, executor.getCounter());
    assertEquals(Collections.emptyList(), receiver.getOutputs());
    assertEquals(Arrays.asList("error_dbg_at_1", LS_FUNCTIONS_ERROR), receiver.getErrors());

    receiver.reset();
    debugger.advance();

    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation("abc", 0), debugger.getLocation());
    assertEquals(5, executor.getCounter());
    assertEquals(Collections.singletonList("[1] 1 2 3"), receiver.getOutputs());
    assertEquals(Arrays.asList("error_dbg_at_2", LS_FUNCTIONS_ERROR), receiver.getErrors());

    receiver.reset();
    debugger.advance();

    assertFalse(debugger.hasNext());
    assertEquals(new TheRLocation("abc", -1), debugger.getLocation());
    assertEquals("[1] 1\n[1] 2", debugger.getResult());
    assertEquals(6, executor.getCounter());
    assertEquals(Collections.singletonList("[1] 1\n[1] 2"), receiver.getOutputs());
    assertEquals(Collections.singletonList("error_exit"), receiver.getErrors());
  }

  @Test
  public void loopWithFunction() throws TheRDebuggerException {
    /*
    for (i in 1:2) d(i)
    */

    final LoopWithFunctionTheRExecutor executor = new LoopWithFunctionTheRExecutor();
    final MockTheRFunctionDebuggerFactory factory = new MockTheRFunctionDebuggerFactory(new IllegalTheRFunctionDebugger());
    final LoopWithFunctionTheRFunctionDebuggerHandler handler = new LoopWithFunctionTheRFunctionDebuggerHandler();
    final MockTheROutputReceiver receiver = new MockTheROutputReceiver();

    final TheRUnbraceFunctionDebugger debugger = new TheRUnbraceFunctionDebugger(
      executor,
      factory,
      handler,
      receiver,
      "abc"
    );

    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation("abc", 0), debugger.getLocation());
    assertEquals(1, executor.getCounter());
    assertEquals(0, factory.getCounter());
    assertEquals(0, handler.getCounter());
    assertEquals(Collections.emptyList(), receiver.getOutputs());
    assertEquals(Collections.singletonList(LS_FUNCTIONS_ERROR), receiver.getErrors());

    receiver.reset();
    debugger.advance();

    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation("abc", 0), debugger.getLocation());
    assertEquals(3, executor.getCounter());
    assertEquals(0, factory.getCounter());
    assertEquals(0, handler.getCounter());
    assertEquals(Collections.emptyList(), receiver.getOutputs());
    assertEquals(Arrays.asList("error_dbg_at", LS_FUNCTIONS_ERROR), receiver.getErrors());

    receiver.reset();
    debugger.advance();

    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation("abc", 0), debugger.getLocation());
    assertEquals(4, executor.getCounter());
    assertEquals(1, factory.getCounter());
    assertEquals(1, handler.getCounter());
    assertEquals(Collections.emptyList(), receiver.getOutputs());
    assertEquals(Collections.singletonList("error_debugging"), receiver.getErrors());

    receiver.reset();
    debugger.advance();

    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation("abc", 0), debugger.getLocation());
    assertEquals(5, executor.getCounter());
    assertEquals(2, factory.getCounter());
    assertEquals(2, handler.getCounter());
    assertEquals(Collections.emptyList(), receiver.getOutputs());
    assertEquals(Collections.singletonList("error_debugging"), receiver.getErrors());
  }

  @Test
  public void ifStatement() throws TheRDebuggerException {
    /*
    if (i > 5) print(i) else print(i + 1)
    */

    final IfStatementTheRExecutor executor = new IfStatementTheRExecutor();
    final MockTheROutputReceiver receiver = new MockTheROutputReceiver();

    final TheRUnbraceFunctionDebugger debugger = new TheRUnbraceFunctionDebugger(
      executor,
      new MockTheRFunctionDebuggerFactory(null),
      new IllegalTheRFunctionDebuggerHandler(),
      receiver,
      "abc"
    );

    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation("abc", 0), debugger.getLocation());
    assertEquals(1, executor.getCounter());
    assertEquals(Collections.emptyList(), receiver.getOutputs());
    assertEquals(Collections.singletonList(LS_FUNCTIONS_ERROR), receiver.getErrors());

    receiver.reset();
    debugger.advance();

    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation("abc", 0), debugger.getLocation());
    assertEquals(3, executor.getCounter());
    assertEquals(Collections.emptyList(), receiver.getOutputs());
    assertEquals(Arrays.asList("error_body", LS_FUNCTIONS_ERROR), receiver.getErrors());

    receiver.reset();
    debugger.advance();

    assertFalse(debugger.hasNext());
    assertEquals(new TheRLocation("abc", -1), debugger.getLocation());
    assertEquals(4, executor.getCounter());
    assertEquals(Collections.singletonList("[1] 2"), receiver.getOutputs());
    assertEquals(Collections.singletonList("error_exit"), receiver.getErrors());
  }

  private void recursiveReturn(@NotNull final TheRExecutionResult secondResult, final boolean output, final int returnLineNumber)
    throws TheRDebuggerException {
    final TheRExecutor executor = MockitoUtils.setupExecutor(
      new ContainerUtil.ImmutableMapBuilder<String, List<TheRExecutionResult>>()
        .put(TheRTraceAndDebugUtilsTest.LS_FUNCTIONS_COMMAND, Collections.singletonList(TheRTraceAndDebugUtilsTest.NO_FUNCTIONS_RESULT))
        .put(EXECUTE_AND_STEP_COMMAND, Collections.singletonList(secondResult))
        .build()
    );

    final TheRFunctionDebuggerHandler handler = mock(TheRFunctionDebuggerHandler.class);
    final MockTheROutputReceiver receiver = new MockTheROutputReceiver();

    final TheRUnbraceFunctionDebugger debugger = new TheRUnbraceFunctionDebugger(
      executor,
      new MockTheRFunctionDebuggerFactory(null),
      handler,
      receiver,
      "abc"
    );

    final List<String> currentCommands = new ArrayList<String>(Collections.singletonList(TheRTraceAndDebugUtilsTest.LS_FUNCTIONS_COMMAND));

    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation("abc", 0), debugger.getLocation());
    MockitoUtils.verifyExecutor(executor, currentCommands);
    verifyZeroInteractions(handler);
    assertEquals(Collections.emptyList(), receiver.getOutputs());
    assertEquals(Collections.singletonList(LS_FUNCTIONS_ERROR), receiver.getErrors());

    receiver.reset();
    currentCommands.add(EXECUTE_AND_STEP_COMMAND);
    debugger.advance();

    assertFalse(debugger.hasNext());
    assertEquals(new TheRLocation("abc", -1), debugger.getLocation());
    assertEquals("[1] 1 2 3", debugger.getResult());
    MockitoUtils.verifyExecutor(executor, currentCommands);
    verify(handler, times(1)).setDropFrames(3);
    if (returnLineNumber != -1) verify(handler, times(1)).setReturnLineNumber(returnLineNumber);
    assertEquals(output ? Collections.singletonList("[1] 1 2 3") : Collections.emptyList(), receiver.getOutputs());
    assertEquals(Collections.singletonList("error_exit"), receiver.getErrors());

    verifyNoMoreInteractions(executor, handler);
  }

  private void exitingFromWithOutput(@NotNull final TheRExecutionResult secondResult, final int returnLineNumber)
    throws TheRDebuggerException {
    final TheRExecutor executor = MockitoUtils.setupExecutor(
      new ContainerUtil.ImmutableMapBuilder<String, List<TheRExecutionResult>>()
        .put(TheRTraceAndDebugUtilsTest.LS_FUNCTIONS_COMMAND, Collections.singletonList(TheRTraceAndDebugUtilsTest.NO_FUNCTIONS_RESULT))
        .put(EXECUTE_AND_STEP_COMMAND, Collections.singletonList(secondResult))
        .build()
    );

    final TheRFunctionDebuggerHandler handler = mock(TheRFunctionDebuggerHandler.class);
    final MockTheROutputReceiver receiver = new MockTheROutputReceiver();

    final TheRUnbraceFunctionDebugger debugger = new TheRUnbraceFunctionDebugger(
      executor,
      new MockTheRFunctionDebuggerFactory(null),
      handler,
      receiver,
      "abc"
    );

    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation("abc", 0), debugger.getLocation());
    MockitoUtils.verifyExecutor(executor, Collections.singletonList(TheRTraceAndDebugUtilsTest.LS_FUNCTIONS_COMMAND));
    verifyZeroInteractions(handler);
    assertEquals(Collections.emptyList(), receiver.getOutputs());
    assertEquals(Collections.singletonList(LS_FUNCTIONS_ERROR), receiver.getErrors());

    receiver.reset();
    debugger.advance();

    assertFalse(debugger.hasNext());
    assertEquals(new TheRLocation("abc", -1), debugger.getLocation());
    assertEquals("[1] 1 2 3", debugger.getResult());
    MockitoUtils.verifyExecutor(executor, Arrays.asList(TheRTraceAndDebugUtilsTest.LS_FUNCTIONS_COMMAND, EXECUTE_AND_STEP_COMMAND));
    if (returnLineNumber != -1) verify(handler, times(1)).setReturnLineNumber(returnLineNumber);
    assertEquals(Collections.singletonList("[1] 1 2 3"), receiver.getOutputs());
    assertEquals(Collections.singletonList("error_exit"), receiver.getErrors());

    verifyNoMoreInteractions(executor, handler);
  }

  private static class OrdinaryTheRExecutor extends MockTheRExecutor {

    @NotNull
    @Override
    protected TheRExecutionResult doExecute(@NotNull final String command) throws TheRDebuggerException {
      if (command.equals(EXECUTE_AND_STEP_COMMAND)) {
        return new TheRExecutionResult(
          EXITING_FROM_PREFIX + "abc(c(1:10))\n" +
          BROWSE_PREFIX + "1" + BROWSE_SUFFIX,
          TheRExecutionResultType.EXITING_FROM,
          TextRange.EMPTY_RANGE,
          "error_exit"
        );
      }

      throw new IllegalStateException("Unexpected command");
    }
  }

  private static class FunctionTheRExecutor extends MockTheRExecutor {

    @NotNull
    @Override
    protected TheRExecutionResult doExecute(@NotNull final String command) throws TheRDebuggerException {
      if (command.equals(EXECUTE_AND_STEP_COMMAND)) {
        return new TheRExecutionResult(
          DEBUGGING_IN_PREFIX + "abc(c(1:10))\n" +
          DEBUG_AT_PREFIX + "{\n" +
          "    .doTrace(" + SERVICE_FUNCTION_PREFIX + "abc" + SERVICE_ENTER_FUNCTION_SUFFIX + "(), \"on entry\")\n" +
          "    {\n" +
          "        x + 1\n" +
          "    }\n" +
          "}\n" +
          BROWSE_PREFIX + "3" + BROWSE_SUFFIX,
          TheRExecutionResultType.DEBUGGING_IN,
          TextRange.EMPTY_RANGE,
          "error_debugging"
        );
      }

      throw new IllegalStateException("Unexpected command");
    }
  }

  private static class FunctionTheRFunctionDebuggerHandler extends IllegalTheRFunctionDebuggerHandler {

    private int myCounter = 0;

    @Override
    public void appendDebugger(@NotNull final TheRFunctionDebugger debugger) {
      myCounter++;
    }

    public int getCounter() {
      return myCounter;
    }
  }

  private static class ContinueTraceTheRExecutor extends MockTheRExecutor {

    @NotNull
    @Override
    protected TheRExecutionResult doExecute(@NotNull final String command) throws TheRDebuggerException {
      if (command.equals(EXECUTE_AND_STEP_COMMAND) && getCounter() == 2) {
        return new TheRExecutionResult(
          EXITING_FROM_PREFIX + "abc()\n" +
          "[1] 1 2 3\n" +
          DEBUGGING_IN_PREFIX + "abc()\n" +
          DEBUG_AT_PREFIX + "{\n" +
          "    .doTrace(" + SERVICE_FUNCTION_PREFIX + "abc" + SERVICE_ENTER_FUNCTION_SUFFIX + "(), \"on entry\")\n" +
          "    {\n" +
          "        c(1:3)\n" +
          "    }\n" +
          "}\n" +
          BROWSE_PREFIX + "3" + BROWSE_SUFFIX,
          TheRExecutionResultType.CONTINUE_TRACE,
          new TextRange(20, 29),
          "error_continue"
        );
      }

      if (command.equals(EXECUTE_AND_STEP_COMMAND) && getCounter() == 3) {
        return new TheRExecutionResult(
          "output",
          TheRExecutionResultType.DEBUG_AT,
          TextRange.EMPTY_RANGE,
          "error_entry"
        );
      }

      if (command.equals(EXECUTE_AND_STEP_COMMAND) && getCounter() == 4) {
        return new TheRExecutionResult(
          TRACING_PREFIX + "abc() on entry \n" +
          "[1] \"abc\"\n" +
          DEBUG_AT_PREFIX + "c(4:6)\n" +
          BROWSE_PREFIX + "3" + BROWSE_SUFFIX,
          TheRExecutionResultType.START_TRACE_UNBRACE,
          TextRange.EMPTY_RANGE,
          "error_entry"
        );
      }

      if (command.equals(EXECUTE_AND_STEP_COMMAND) && getCounter() == 6) {
        return new TheRExecutionResult(
          EXITING_FROM_PREFIX + "abc()\n" +
          "[1] 4 5 6\n" +
          BROWSE_PREFIX + "1" + BROWSE_SUFFIX,
          TheRExecutionResultType.EXITING_FROM,
          new TextRange(20, 29),
          "error_exit"
        );
      }

      throw new IllegalStateException("Unexpected command");
    }
  }

  private static class ErrorTheRExecutor extends MockTheRExecutor {

    @NotNull
    @Override
    protected TheRExecutionResult doExecute(@NotNull final String command) throws TheRDebuggerException {
      if (command.equals(EXECUTE_AND_STEP_COMMAND) && getCounter() == 2) {
        return new TheRExecutionResult(
          "",
          TheRExecutionResultType.EMPTY,
          TextRange.EMPTY_RANGE,
          "Error in if (10 > log(-1)) { : missing value where TRUE/FALSE needed\n" +
          "In addition: Warning message:\n" +
          "In log(-1) : NaNs produced"
        );
      }

      throw new IllegalStateException("Unexpected command");
    }
  }

  private static class LoopTheRExecutor extends MockTheRExecutor {

    @NotNull
    @Override
    protected TheRExecutionResult doExecute(@NotNull final String command) throws TheRDebuggerException {
      if (command.equals(EXECUTE_AND_STEP_COMMAND) && getCounter() == 2) {
        return new TheRExecutionResult(
          DEBUG_AT_PREFIX + "print(i)",
          TheRExecutionResultType.DEBUG_AT,
          TextRange.EMPTY_RANGE,
          "error_dbg_at_1"
        );
      }

      if (command.equals(EXECUTE_AND_STEP_COMMAND) && getCounter() == 4) {
        return new TheRExecutionResult(
          "[1] 1 2 3\n" +
          DEBUG_AT_PREFIX + "print(i)",
          TheRExecutionResultType.DEBUG_AT,
          new TextRange(0, 9),
          "error_dbg_at_2"
        );
      }

      if (command.equals(EXECUTE_AND_STEP_COMMAND) && getCounter() == 6) {
        return new TheRExecutionResult(
          "[1] 1\n[1] 2\n" +
          EXITING_FROM_PREFIX + "abc()\n" +
          BROWSE_PREFIX + "1" + BROWSE_SUFFIX,
          TheRExecutionResultType.EXITING_FROM,
          new TextRange(0, 11),
          "error_exit"
        );
      }

      throw new IllegalStateException("Unexpected command");
    }
  }

  private static class LoopWithFunctionTheRExecutor extends MockTheRExecutor {

    @NotNull
    @Override
    protected TheRExecutionResult doExecute(@NotNull final String command) throws TheRDebuggerException {
      if (command.equals(EXECUTE_AND_STEP_COMMAND) && getCounter() == 2) {
        return new TheRExecutionResult(
          DEBUG_AT_PREFIX + "h()",
          TheRExecutionResultType.DEBUG_AT,
          TextRange.EMPTY_RANGE,
          "error_dbg_at"
        );
      }

      if (command.equals(EXECUTE_AND_STEP_COMMAND) && (getCounter() == 4 || getCounter() == 5)) {
        return new TheRExecutionResult(
          DEBUGGING_IN_PREFIX + "d(i)\n" +
          DEBUG_AT_PREFIX + "{\n" +
          "    .doTrace(" + SERVICE_FUNCTION_PREFIX + "abc" + SERVICE_ENTER_FUNCTION_SUFFIX + "(), \"on entry\")\n" +
          "    print(i)\n" +
          "}\n" +
          BROWSE_PREFIX + "3" + BROWSE_SUFFIX,
          TheRExecutionResultType.DEBUGGING_IN,
          TextRange.EMPTY_RANGE,
          "error_debugging"
        );
      }

      throw new IllegalStateException("Unexpected command");
    }
  }

  private static class LoopWithFunctionTheRFunctionDebuggerHandler extends IllegalTheRFunctionDebuggerHandler {

    private int myCounter = 0;

    @Override
    public void appendDebugger(@NotNull final TheRFunctionDebugger debugger) {
      myCounter++;
    }

    public int getCounter() {
      return myCounter;
    }
  }

  private static class IfStatementTheRExecutor extends MockTheRExecutor {

    @NotNull
    @Override
    protected TheRExecutionResult doExecute(@NotNull final String command) throws TheRDebuggerException {
      if (command.equals(EXECUTE_AND_STEP_COMMAND) && getCounter() == 2) {
        return new TheRExecutionResult(
          DEBUG_AT_LINE_PREFIX + "2: print(i + 1)\n" +
          BROWSE_PREFIX + "2" + BROWSE_SUFFIX,
          TheRExecutionResultType.DEBUG_AT,
          TextRange.EMPTY_RANGE,
          "error_body"
        );
      }

      if (command.equals(EXECUTE_AND_STEP_COMMAND) && getCounter() == 4) {
        return new TheRExecutionResult(
          "[1] 2\n" +
          EXITING_FROM_PREFIX + "f(1)\n" +
          BROWSE_PREFIX + "1" + BROWSE_SUFFIX,
          TheRExecutionResultType.EXITING_FROM,
          new TextRange(0, 5),
          "error_exit"
        );
      }

      throw new IllegalStateException("Unexpected command");
    }
  }
}