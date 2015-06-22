/*
 * SonarQube Runner - Batch Interface
 * Copyright (C) 2011 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.runner.batch;

import org.sonar.home.log.LogListener;

import java.util.List;
import java.util.Properties;

public interface IsolatedLauncher {
  void start(Properties properties, List<Object> extensions);

  void start(Properties properties, List<Object> extensions, LogListener logListener);

  void stop();

  void execute(Properties properties);

  void executeOldVersion(Properties properties, List<Object> extensions);

  String getVersion();
}
