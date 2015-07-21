package com.jetbrains.ther.debugger.interpreter;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.ther.debugger.data.TheRDebugConstants;
import com.jetbrains.ther.debugger.data.TheRProcessResponse;
import com.jetbrains.ther.debugger.data.TheRProcessResponseType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.jetbrains.ther.debugger.data.TheRDebugConstants.*;
import static com.jetbrains.ther.debugger.data.TheRProcessResponseType.*;
import static com.jetbrains.ther.debugger.data.TheRProcessResponseType.DEBUGGING_IN;
import static com.jetbrains.ther.debugger.data.TheRProcessResponseType.DEBUG_AT;

final class TheRProcessResponseCalculator {

  @NotNull
  private static final Pattern START_TRACE_PATTERN = Pattern.compile("^" + TRACING + " .* on entry( )*$");

  @NotNull
  private static final Pattern END_TRACE_PATTERN = Pattern.compile("^" + TRACING + " .* on exit( )*$");

  @NotNull
  private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("(\r|\n|\r\n)");

  public static boolean isComplete(@NotNull final CharSequence response) {
    return endsLineBreakAndPlusAndSpace(response) || endsLineBreakAndBrowseAndSpace(response);
  }

  @NotNull
  public static TheRProcessResponse calculate(@NotNull final CharSequence response) {
    final String[] lines =
      LINE_BREAK_PATTERN.split(response); // Don't forget that first line is command and the last is invitation for the next one

    return calculateResult(
      lines,
      calculateTypeAndOutputLineBounds(lines)
    );
  }

  private static boolean endsLineBreakAndPlusAndSpace(@NotNull final CharSequence sequence) {
    final int length = sequence.length();

    return isSubsequence(PLUS_AND_SPACE, sequence, length - PLUS_AND_SPACE.length()) && // ends with PLUS_AND_SPACE
           StringUtil.isLineBreak(sequence.charAt(length - PLUS_AND_SPACE.length() - 1)); // line break before PLUS_AND_SPACE
  }

  private static boolean endsLineBreakAndBrowseAndSpace(@NotNull final CharSequence sequence) {
    final int length = sequence.length();

    if (isSubsequence(BROWSE_SUFFIX, sequence, length - BROWSE_SUFFIX.length())) { // ends with BROWSE_SUFFIX
      final int index = readDigitsBackward(sequence, length - BROWSE_SUFFIX.length() - 1); // read digits before BROWSE_SUFFIX

      return index != -1 && // there are symbols before digits
             index != length - BROWSE_SUFFIX.length() - 1 && // there are any digits before BROWSE_SUFFIX
             isSubsequence(BROWSE_PREFIX, sequence, index - BROWSE_PREFIX.length() + 1) && // there is BROWSE_PREFIX before digits
             StringUtil.isLineBreak(sequence.charAt(index - BROWSE_PREFIX.length())); // line break before all mentioned above
    }
    else {
      return false;
    }
  }

  @NotNull
  private static TheRProcessResponse calculateResult(@NotNull final String[] lines,
                                                     @NotNull final TypeAndOutputLineBounds typeAndOutputLineBounds) {
    final StringBuilder sb = new StringBuilder();

    final TextRange preCalculatedRange =
      (typeAndOutputLineBounds.myOutputEnd <= typeAndOutputLineBounds.myOutputBegin) ? TextRange.EMPTY_RANGE : null;

    int outputBegin = 0;
    int outputEnd = 0;

    for (int i = 1; i < lines.length - 1; i++) {
      sb.append(lines[i]);

      if (i != lines.length - 2) {
        sb.append(TheRDebugConstants.LINE_SEPARATOR);
      }

      if (preCalculatedRange == null) {
        outputBegin += calculateOutputBeginAddition(lines, typeAndOutputLineBounds.myOutputBegin, i);
        outputEnd += calculateOutputEndAddition(lines, typeAndOutputLineBounds.myOutputEnd, i);
      }
    }

    return new TheRProcessResponse(
      sb.toString(),
      typeAndOutputLineBounds.myType,
      preCalculatedRange == null ? new TextRange(outputBegin, outputEnd) : preCalculatedRange
    );
  }

  @NotNull
  private static TypeAndOutputLineBounds calculateTypeAndOutputLineBounds(@NotNull final String[] lines) {
    TypeAndOutputLineBounds candidate = tryJustPlusAndSpace(lines);

    if (candidate != null) {
      return candidate;
    }

    candidate = tryJustBrowseAndSpace(lines);

    if (candidate != null) {
      return candidate;
    }

    if (!endsBrowseAndSpace(lines)) {
      throw new IllegalArgumentException("Response is incomplete");
    }

    candidate = tryDebugging(lines);

    if (candidate != null) {
      return candidate;
    }

    candidate = tryContinueTrace(lines);

    if (candidate != null) {
      return candidate;
    }

    candidate = tryEndTrace(lines);

    if (candidate != null) {
      return candidate;
    }

    candidate = tryDebugAt(lines);

    if (candidate != null) {
      return candidate;
    }

    candidate = tryStartTrace(lines);

    if (candidate != null) {
      return candidate;
    }

    return new TypeAndOutputLineBounds(RESPONSE, 0, lines.length);
  }

  private static boolean isSubsequence(@NotNull final CharSequence sequence,
                                       @NotNull final CharSequence text,
                                       final int beginIndex) {
    if (beginIndex < 0 || beginIndex + sequence.length() > text.length()) {
      return false;
    }

    for (int i = 0; i < sequence.length(); i++) {
      if (sequence.charAt(i) != text.charAt(beginIndex + i)) {
        return false;
      }
    }

    return true;
  }

  private static int calculateOutputBeginAddition(@NotNull final String[] lines,
                                                  final int outputLineBegin,
                                                  final int i) {
    int result = 0;

    if (i < outputLineBegin) {
      result += lines[i].length();

      if (i != lines.length - 2) {
        result += TheRDebugConstants.LINE_SEPARATOR.length();
      }
    }

    return result;
  }

  private static int calculateOutputEndAddition(@NotNull final String[] lines,
                                                final int outputLineEnd,
                                                final int i) {
    int result = 0;

    if (i < outputLineEnd) {
      result += lines[i].length();

      if (i != lines.length - 2 && i != outputLineEnd - 1) {
        result += TheRDebugConstants.LINE_SEPARATOR.length();
      }
    }

    return result;
  }

  @Nullable
  private static TypeAndOutputLineBounds tryJustPlusAndSpace(@NotNull final String[] lines) {
    if (lines.length == 2 && lines[1].equals(PLUS_AND_SPACE)) {
      return new TypeAndOutputLineBounds(PLUS, 1, 1);
    }
    else {
      return null;
    }
  }

  @Nullable
  private static TypeAndOutputLineBounds tryJustBrowseAndSpace(@NotNull final String[] lines) {
    if (lines.length == 2 && justBrowseAndSpace(lines[1])) {
      return new TypeAndOutputLineBounds(EMPTY, 1, 1);
    }
    else {
      return null;
    }
  }

  private static boolean endsBrowseAndSpace(@NotNull final String[] lines) {
    return lines.length > 1 && justBrowseAndSpace(lines[lines.length - 1]);
  }

  @Nullable
  private static TypeAndOutputLineBounds tryDebugging(@NotNull final String[] lines) {
    if (lines.length > 1 && lines[1].startsWith(TheRDebugConstants.DEBUGGING_IN)) {
      return new TypeAndOutputLineBounds(DEBUGGING_IN, 1, 1);
    }
    else {
      return null;
    }
  }

  @Nullable
  private static TypeAndOutputLineBounds tryContinueTrace(@NotNull final String[] lines) {
    final int endOffset = -1  // "[1] \"...\"" line
                          - 1 // "exiting from ..." line
                          - 1 // "debugging in..." line
                          - 1; // "debug: {..." line

    for (int i = 1; i < lines.length + endOffset - 1; i++) {
      if (lines[i + 2].startsWith(EXITING_FROM) && END_TRACE_PATTERN.matcher(lines[i]).find()) {
        for (int j = i + 3; j < lines.length; j++) {
          if (lines[j].startsWith(TheRDebugConstants.DEBUGGING_IN)) {
            if (i == 1) {
              // output could be located inside trace information between "exiting from ..." and "debugging in..." lines
              return new TypeAndOutputLineBounds(CONTINUE_TRACE, i + 3, j);
            }
            else {
              // output could be located before trace information
              return new TypeAndOutputLineBounds(CONTINUE_TRACE, 1, i);
            }
          }
        }

        return null;
      }
    }

    return null;
  }

  @Nullable
  private static TypeAndOutputLineBounds tryEndTrace(@NotNull final String[] lines) {
    final int endOffset = -2; // "[1] \"...\" and "exiting from ..." lines
    final List<Integer> endTraceIndices = new ArrayList<Integer>();

    for (int i = 1; i < lines.length + endOffset - 1; i++) {
      if (END_TRACE_PATTERN.matcher(lines[i]).find()) {
        endTraceIndices.add(i);
      }
    }

    if (endTraceIndices.isEmpty()) {
      return null;
    }

    final TheRProcessResponseType type = (endTraceIndices.size() == 1) ? END_TRACE : RECURSIVE_END_TRACE;

    final Integer firstEndTrace = endTraceIndices.get(0);
    final Integer lastEndTrace = endTraceIndices.get(endTraceIndices.size() - 1);

    if (firstEndTrace == 1) {
      // output could be located inside trace information between last "exiting from ..." and "debug at #..." lines
      // or just after last "exiting from ..." line if there is no "debug at #..." line

      final int exitingFromOffset = lastEndTrace + 2; // "[1] \"...\" and "exiting from ..." lines
      final int outputLineBegin = findExitingFrom(lines, exitingFromOffset) + 1;
      final int outputLineEnd = findDebugAt(lines, outputLineBegin);

      return new TypeAndOutputLineBounds(type, outputLineBegin, outputLineEnd);
    }
    else {
      // output could be located before trace information
      return new TypeAndOutputLineBounds(type, 1, firstEndTrace);
    }
  }

  @Nullable
  private static TypeAndOutputLineBounds tryDebugAt(@NotNull final String[] lines) {
    if (lines.length > 2) {
      final int debugAtLine = findDebugAt(lines, 0);
      final boolean debugAtExists = debugAtLine < lines.length - 1;

      if (debugAtExists) {
        return new TypeAndOutputLineBounds(DEBUG_AT, 1, debugAtLine);
      }
      else {
        return null;
      }
    }
    else {
      return null;
    }
  }

  @Nullable
  private static TypeAndOutputLineBounds tryStartTrace(@NotNull final String[] lines) {
    if (START_TRACE_PATTERN.matcher(lines[1]).find()) {
      final int unbraceFunctionStartTraceLength = 1 // previous command
                                                  + 1 // "Tracing on ... entry"
                                                  + 1 // "[1] \"...\""
                                                  + 1 // "debug: ..,"
                                                  + 1; // invitation for the next command

      if (lines.length == unbraceFunctionStartTraceLength) {
        return new TypeAndOutputLineBounds(START_TRACE_UNBRACE, 1, 1);
      }
      else {
        return new TypeAndOutputLineBounds(START_TRACE_BRACE, 1, 1);
      }
    }

    return null;
  }

  private static int readDigitsBackward(@NotNull final CharSequence sequence, final int beginIndex) {
    if (sequence.length() <= beginIndex || beginIndex <= -1) {
      return beginIndex;
    }

    for (int i = beginIndex; i > -1; i--) {
      if (!Character.isDigit(sequence.charAt(i))) {
        return i;
      }
    }

    return -1;
  }

  private static boolean justBrowseAndSpace(@NotNull final String line) {
    return line.startsWith(BROWSE_PREFIX) &&
           line.endsWith(BROWSE_SUFFIX) &&
           isDigits(line, BROWSE_PREFIX.length(), line.length() - BROWSE_SUFFIX.length() - 1);
  }

  private static int findExitingFrom(@NotNull final String[] lines, final int index) {
    int result = index;

    while (result < lines.length - 1 && !lines[result].startsWith(EXITING_FROM)) {
      result++;
    }

    return result;
  }

  private static int findDebugAt(@NotNull final String[] lines, final int index) {
    int result = index;

    while (result < lines.length - 1 && !lines[result].startsWith(TheRDebugConstants.DEBUG_AT)) {
      result++;
    }

    return result;
  }

  private static boolean isDigits(@NotNull final CharSequence sequence, final int beginIndex, final int endIndex) { // [l..r]
    return beginIndex >= 0 &&
           endIndex >= beginIndex &&
           endIndex < sequence.length() &&
           readDigitsBackward(sequence, endIndex) == beginIndex - 1;
  }

  private static class TypeAndOutputLineBounds {

    @NotNull
    private final TheRProcessResponseType myType;

    private final int myOutputBegin;

    private final int myOutputEnd;

    public TypeAndOutputLineBounds(@NotNull final TheRProcessResponseType type, final int outputBegin, final int outputEnd) {
      myType = type;
      myOutputBegin = outputBegin;
      myOutputEnd = outputEnd;
    }
  }
}
