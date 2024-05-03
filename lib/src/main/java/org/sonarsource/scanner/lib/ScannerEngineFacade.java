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
package org.sonarsource.scanner.lib;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonarsource.scanner.lib.internal.JreCacheHit;
import org.sonarsource.scanner.lib.internal.cache.Logger;

public abstract class ScannerEngineFacade implements AutoCloseable {
  protected final LogOutput logOutput;
  private final Map<String, String> bootstrapProperties;
  private final Logger logger;
  private final boolean isSonarCloud;
  private final String serverVersion;
  private final boolean wasEngineCacheHit;
  private final JreCacheHit wasJreCacheHit;

  ScannerEngineFacade(Map<String, String> bootstrapProperties, LogOutput logOutput, boolean isSonarCloud, @Nullable String serverVersion,
    boolean wasEngineCacheHit, @Nullable JreCacheHit wasJreCacheHit) {
    this.bootstrapProperties = bootstrapProperties;
    this.logger = new LoggerAdapter(logOutput);
    this.logOutput = logOutput;
    this.isSonarCloud = isSonarCloud;
    this.serverVersion = serverVersion;
    this.wasEngineCacheHit = wasEngineCacheHit;
    this.wasJreCacheHit = wasJreCacheHit;
  }

  public String getServerVersion() {
    if (isSonarCloud) {
      throw new UnsupportedOperationException("Server version is not available for SonarCloud.");
    }
    return serverVersion;
  }

  public boolean isSonarCloud() {
    return isSonarCloud;
  }

  public void analyze(Map<String, String> analysisProps) {
    Map<String, String> allProps = new HashMap<>();
    allProps.putAll(bootstrapProperties);
    allProps.putAll(analysisProps);
    initAnalysisProperties(allProps);
    addStatsProperties(allProps);
    doAnalyze(allProps);
  }

  private void addStatsProperties(Map<String, String> allProps) {
    if (wasJreCacheHit != null) {
      allProps.put("sonar.scanner.wasJreCacheHit", wasJreCacheHit.name());
    }
    allProps.put("sonar.scanner.wasEngineCacheHit", String.valueOf(wasEngineCacheHit));
  }

  abstract void doAnalyze(Map<String, String> allProps);

  private void initAnalysisProperties(Map<String, String> p) {
    new Dirs(logger).init(p);
  }

  public Map<String, String> getBootstrapProperties() {
    return bootstrapProperties;
  }
}
