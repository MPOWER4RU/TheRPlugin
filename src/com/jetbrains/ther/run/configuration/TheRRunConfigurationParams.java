package com.jetbrains.ther.run.configuration;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

interface TheRRunConfigurationParams {

  @NotNull
  String getScriptPath();

  void setScriptPath(@NotNull final String scriptPath);

  @NotNull
  String getScriptArgs();

  void setScriptArgs(@NotNull final String scriptArgs);

  @NotNull
  String getWorkingDirectoryPath();

  void setWorkingDirectoryPath(@NotNull final String workingDirectoryPath);

  @NotNull
  Map<String, String> getEnvs();

  void setEnvs(@NotNull final Map<String, String> envs);

  boolean isPassParentEnvs();

  void setPassParentEnvs(final boolean passParentEnvs);
}

