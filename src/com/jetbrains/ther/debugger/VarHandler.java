package com.jetbrains.ther.debugger;

import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.ther.debugger.data.TheRDebugConstants;
import com.jetbrains.ther.debugger.data.TheRProcessResponseType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class VarHandler {

  @Nullable
  public String handleType(@NotNull final TheRProcess process, @NotNull final String var, @NotNull final String type)
    throws IOException, InterruptedException {
    if (type.equals(TheRDebugConstants.FUNCTION_TYPE)) {
      if (var.startsWith(TheRDebugConstants.SERVICE_FUNCTION_PREFIX)) {
        return null;
      }
      else {
        traceAndDebug(process, var);
      }
    }

    return type;
  }

  @NotNull
  public String handleValue(@NotNull final TheRProcess process,
                            @NotNull final String name,
                            @NotNull final String type,
                            @NotNull final String value) {
    if (type.equals(TheRDebugConstants.FUNCTION_TYPE)) {
      final String[] lines = StringUtil.splitByLinesKeepSeparators(value);
      final StringBuilder sb = new StringBuilder();

      for (int i = 2; i < lines.length - 1; i++) {
        sb.append(lines[i]);
      }

      while (StringUtil.endsWithLineBreak(sb)) {
        sb.setLength(sb.length() - 1);
      }

      return sb.toString();
    }
    else {
      return value;
    }
  }

  private void traceAndDebug(@NotNull final TheRProcess process, @NotNull final String var)
    throws IOException, InterruptedException {
    TheRDebuggerUtils.executeAndCheckType(process, createEnterFunction(var), TheRProcessResponseType.JUST_BROWSE);
    TheRDebuggerUtils.executeAndCheckType(process, createExitFunction(var), TheRProcessResponseType.JUST_BROWSE);
    TheRDebuggerUtils.executeAndCheckType(process, createTraceCommand(var), TheRProcessResponseType.RESPONSE_AND_BROWSE);
    TheRDebuggerUtils.executeAndCheckType(process, createDebugCommand(var), TheRProcessResponseType.JUST_BROWSE);
  }

  @NotNull
  private static String createEnterFunction(@NotNull final String var) {
    return createEnterFunctionName(var) + " <- function() { print(\"enter " + var + "\") }";
  }

  @NotNull
  private static String createEnterFunctionName(@NotNull final String var) {
    return TheRDebugConstants.SERVICE_FUNCTION_PREFIX + var + TheRDebugConstants.SERVICE_ENTER_FUNCTION_SUFFIX;
  }

  @NotNull
  private static String createExitFunction(@NotNull final String var) {
    return createExitFunctionName(var) + " <- function() { print(\"exit " + var + "\") }";
  }

  @NotNull
  private static String createExitFunctionName(@NotNull final String var) {
    return TheRDebugConstants.SERVICE_FUNCTION_PREFIX + var + TheRDebugConstants.SERVICE_EXIT_FUNCTION_SUFFIX;
  }

  @NotNull
  private static String createTraceCommand(@NotNull final String var) {
    return TheRDebugConstants.TRACE_COMMAND +
           "(" +
           var +
           ", " +
           createEnterFunctionName(var) +
           ", exit = " +
           createExitFunctionName(var) +
           ")";
  }

  @NotNull
  private static String createDebugCommand(@NotNull final String var) {
    return TheRDebugConstants.DEBUG_COMMAND + "(" + var + ")";
  }
}
