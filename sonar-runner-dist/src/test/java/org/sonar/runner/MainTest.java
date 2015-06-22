/*
 * SonarQube Runner - Distribution
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
package org.sonar.runner;

import org.mockito.MockitoAnnotations;
import org.mockito.Mock;
import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.InOrder;
import org.junit.Test;
import org.sonar.runner.api.Runner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.mockito.Mockito.times;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MainTest {

  @Mock
  private Exit exit;
  @Mock
  private Cli cli;
  @Mock
  private Conf conf;
  @Mock
  private Properties properties;
  @Mock
  private RunnerFactory runnerFactory;
  @Mock
  private Runner<?> runner;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    when(runnerFactory.create(any(Properties.class))).thenReturn(runner);
    when(conf.properties()).thenReturn(properties);

  }

  @Test
  public void should_execute_runner() {
    Main main = new Main(exit, cli, conf, runnerFactory);
    main.execute();

    verify(exit).exit(Exit.SUCCESS);
    verify(runnerFactory).create(properties);

    verify(runner, times(1)).start();
    verify(runner, times(1)).runAnalysis(properties);
    verify(runner, times(1)).stop();
  }

  @Test
  public void should_fail_on_error() {
    Runner<?> runner = mock(Runner.class);
    doThrow(new IllegalStateException("Error")).when(runner).runAnalysis(any(Properties.class));
    when(runnerFactory.create(any(Properties.class))).thenReturn(runner);

    Main main = new Main(exit, cli, conf, runnerFactory);
    main.execute();

    verify(exit).exit(Exit.ERROR);
  }

  @Test
  public void should_only_display_version() throws IOException {

    Properties p = new Properties();
    when(cli.isDisplayVersionOnly()).thenReturn(true);
    when(conf.properties()).thenReturn(p);

    Main main = new Main(exit, cli, conf, runnerFactory);
    main.execute();

    InOrder inOrder = Mockito.inOrder(exit, runnerFactory);

    inOrder.verify(exit, times(1)).exit(Exit.SUCCESS);
    inOrder.verify(runnerFactory, times(1)).create(p);
    inOrder.verify(exit, times(1)).exit(Exit.SUCCESS);
  }

  @Test(timeout=30000)
  public void test_interactive_mode() throws IOException {
    String inputStr = "qwe" + System.lineSeparator() + "qwe" + System.lineSeparator();
    InputStream input = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
    System.setIn(input);
    input.close();

    when(cli.isInteractive()).thenReturn(true);
    when(cli.isDebugMode()).thenReturn(true);
    when(cli.isDisplayStackTrace()).thenReturn(true);

    Main main = new Main(exit, cli, conf, runnerFactory);
    main.execute();

    verify(runner, times(1)).start();
    verify(runner, times(3)).runAnalysis(any(Properties.class));
    verify(runner, times(1)).stop();
  }
}
