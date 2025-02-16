/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.adb;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.MultiLineReceiver;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.android.tools.idea.adblib.ddmlibcompatibility.AdbLibMigrationUtils;
import com.intellij.openapi.diagnostic.Logger;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class AdbShellCommandsUtil {
  @NotNull private static final Logger LOGGER = Logger.getInstance(AdbShellCommandsUtil.class);
  @NotNull private static final String ERROR_LINE_MARKER = "ERR-ERR-ERR-ERR";
  @NotNull private static final String COMMAND_ERROR_CHECK_SUFFIX = " || echo " + ERROR_LINE_MARKER;
  @NotNull private static final AdbShellCommandsUtil sInstance = new AdbShellCommandsUtil(false);

  private final boolean myUseAdbLib;

  public static AdbShellCommandsUtil getInstance() {
    return sInstance;
  }

  public AdbShellCommandsUtil(boolean useAdbLib) {
    myUseAdbLib = useAdbLib;
  }

  public AdbShellCommandResult executeCommand(@NotNull IDevice device, @NotNull String command)
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
    return executeCommandImpl(device, command, true);
  }

  public AdbShellCommandResult executeCommandNoErrorCheck(@NotNull IDevice device, @NotNull String command)
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
    return executeCommandImpl(device, command, false);
  }

  public void executeRawCommand(@NotNull IDevice device, @NotNull String command, IShellOutputReceiver receiver)
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
    long startTime = System.nanoTime();

    executeCommandImpl(device, command, receiver);

    if (LOGGER.isTraceEnabled()) {
      long endTime = System.nanoTime();
      LOGGER.trace(String.format(Locale.US, "Command took %,d ms to execute: %s", (endTime - startTime) / 1_000_000, command));
    }
  }

  private AdbShellCommandResult executeCommandImpl(@NotNull IDevice device, @NotNull String command, boolean errorCheck)
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {

    List<String> commandOutput = new ArrayList<>();
    // Adding the " || echo xxx" command to the command allows us to detect non-zero status code
    // from the command by analysing the output and looking for the "xxx" marker.
    String fullCommand = errorCheck ? String.format("%s%s", command, COMMAND_ERROR_CHECK_SUFFIX) : command;
    MultiLineReceiver receiver = new MultiLineReceiver() {
      @Override
      public void processNewLines(@NotNull String[] lines) {
        Arrays.stream(lines).forEach(commandOutput::add);
      }

      @Override
      public boolean isCancelled() {
        return false;
      }
    };
    receiver.setTrimLine(false);
    executeCommandImpl(device, fullCommand, receiver);

    // Look for error marker in the last 2 output lines
    boolean isError = false;
    if (errorCheck &&
        commandOutput.size() >= 2 &&
        Objects.equals(commandOutput.get(commandOutput.size() - 2), ERROR_LINE_MARKER) &&
        Objects.equals(commandOutput.get(commandOutput.size() - 1), "")) {
      isError = true;
      commandOutput.remove(commandOutput.get(commandOutput.size() - 1));
      commandOutput.remove(commandOutput.get(commandOutput.size() - 1));
    }

    // Log first tow lines of the output for diagnostic purposes
    if (commandOutput.size() >= 1) {
      LOGGER.info(String.format(Locale.US, "  Output line 1 (out of %d): %s", commandOutput.size(), commandOutput.get(0)));
    }
    if (commandOutput.size() >= 2) {
      LOGGER.info(String.format(Locale.US, "  Output line 2 (out of %d): %s", commandOutput.size(), commandOutput.get(1)));
    }
    if (LOGGER.isDebugEnabled()) {
      for (int i = 2; i < commandOutput.size(); i++) {
        LOGGER.debug(String.format(Locale.US, "  Output line %d (out of %d): %s", i + 1, commandOutput.size(), commandOutput.get(i)));
      }
    }
    return new AdbShellCommandResult(command, commandOutput, isError);
  }

  private void executeCommandImpl(@NotNull IDevice device, @NotNull String command, IShellOutputReceiver receiver)
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
    long startTime = System.nanoTime();
    if (myUseAdbLib) {
      AdbLibMigrationUtils.executeShellCommand(device, command, receiver);
    }
    else {
      device.executeShellCommand(command, receiver);
    }
    long endTime = System.nanoTime();
    LOGGER.info(String.format(Locale.US, "Command took %,d ms to execute: %s", (endTime - startTime) / 1_000_000, command));
  }
}
