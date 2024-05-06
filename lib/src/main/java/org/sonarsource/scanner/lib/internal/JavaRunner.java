/*
 * SonarScanner Java Library
 * Copyright (C) 2011-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.scanner.lib.internal;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaRunner {
  private static final Logger LOG = LoggerFactory.getLogger(JavaRunner.class);

  private final Path javaExecutable;
  private final JreCacheHit jreCacheHit;

  public JavaRunner(Path javaExecutable, JreCacheHit jreCacheHit) {
    this.javaExecutable = javaExecutable;
    this.jreCacheHit = jreCacheHit;
  }

  public JreCacheHit getJreCacheHit() {
    return jreCacheHit;
  }

  public void execute(List<String> args, @Nullable String input) {
    try {
      List<String> command = new ArrayList<>(args);
      command.add(0, javaExecutable.toString());
      LOG.atDebug().addArgument(() -> String.join(" ", command)).log("Executing: {}");
      Process process = new ProcessBuilder(command).start();
      if (input != null) {
        try (var stdin = process.getOutputStream(); var osw = new OutputStreamWriter(stdin, StandardCharsets.UTF_8)) {
          osw.write(input);
        }
      }
      var stdoutConsummer = new StreamGobbler(process.getInputStream(), this::tryParse);
      var stdErrConsummer = new StreamGobbler(process.getErrorStream(), stderr -> LOG.error("[stderr] {}", stderr));
      stdErrConsummer.start();
      stdoutConsummer.start();
      var exitCode = process.waitFor();
      stdoutConsummer.join();
      stdErrConsummer.join();
      if (exitCode != 0) {
        throw new IllegalStateException("Error returned by the Java command execution: " + process.exitValue());
      }
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Failed to run the Java command", e);
    }
  }

  public Path getJavaExecutable() {
    return javaExecutable;
  }

  void tryParse(String stdout) {
    try {
      var log = new Gson().fromJson(stdout, Log.class);
      StringBuilder sb = new StringBuilder();
      if (log.message != null) {
        sb.append(log.message);
      }
      if (log.message != null && log.stacktrace != null) {
        sb.append("\n");
      }
      if (log.stacktrace != null) {
        sb.append(log.stacktrace);
      }
      log(log.level, sb.toString());
    } catch (Exception e) {
      LOG.info("[stdout] {}", stdout);
    }
  }

  private static void log(String level, String msg) {
    switch (level) {
      case "ERROR":
        LOG.error(msg);
        break;
      case "WARN":
        LOG.warn(msg);
        break;
      case "DEBUG":
        LOG.debug(msg);
        break;
      case "TRACE":
        LOG.trace(msg);
        break;
      case "INFO":
      default:
        LOG.info(msg);
    }
  }

  private static class Log {
    @SerializedName("level")
    private String level;
    @SerializedName("message")
    private String message;
    @SerializedName("stacktrace")
    private String stacktrace;
  }

  private static class StreamGobbler extends Thread {
    private final InputStream inputStream;
    private final Consumer<String> consumer;

    public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
      this.inputStream = inputStream;
      this.consumer = consumer;
    }

    @Override
    public void run() {
      new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
        .forEach(consumer);
    }
  }
}
