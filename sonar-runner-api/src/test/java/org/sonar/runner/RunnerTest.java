/*
 * Sonar Runner - API
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RunnerTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldHaveDefaultEncodingIfNotForce() {
    Runner runner = Runner.create(new Properties());
    assertThat(runner.getSourceCodeEncoding()).isEqualTo(Charset.defaultCharset().name());
    assertThat(runner.isEncodingPlatformDependant()).isTrue();
  }

  @Test
  public void shouldKeepEncodingIfSpecified() {
    Properties props = new Properties();
    // Yeah, windows charset!
    props.setProperty("sonar.sourceEncoding", "cp1252");
    Runner runner = Runner.create(props);
    assertThat(runner.getSourceCodeEncoding()).isEqualTo("cp1252");
    assertThat(runner.isEncodingPlatformDependant()).isFalse();
  }

  @Test
  public void shouldHaveDefaultEnvironmentInformationValues() {
    Runner runner = Runner.create(new Properties());
    assertThat(runner.getProperties().getProperty(Runner.PROPERTY_ENVIRONMENT_INFORMATION_KEY)).isEqualTo("Runner");
    assertThat(runner.getProperties().getProperty(Runner.PROPERTY_ENVIRONMENT_INFORMATION_VERSION)).contains(".");
    assertThat(runner.getProperties().getProperty(Runner.PROPERTY_ENVIRONMENT_INFORMATION_VERSION)).doesNotContain("$");
  }

  @Test
  public void shouldOverwriteDefaultEnvironmentInformationValues() {
    Runner runner = Runner.create(new Properties());
    runner.setEnvironmentInformation("Ant", "1.2.3");
    assertThat(runner.getProperties().getProperty(Runner.PROPERTY_ENVIRONMENT_INFORMATION_KEY)).isEqualTo("Ant");
    assertThat(runner.getProperties().getProperty(Runner.PROPERTY_ENVIRONMENT_INFORMATION_VERSION)).isEqualTo("1.2.3");
  }

  @Test
  public void shouldCheckVersion() {
    assertThat(Runner.isUnsupportedVersion("1.0")).isTrue();
    assertThat(Runner.isUnsupportedVersion("2.0")).isTrue();
    assertThat(Runner.isUnsupportedVersion("2.1")).isTrue();
    assertThat(Runner.isUnsupportedVersion("2.2")).isTrue();
    assertThat(Runner.isUnsupportedVersion("2.3")).isTrue();
    assertThat(Runner.isUnsupportedVersion("2.4")).isTrue();
    assertThat(Runner.isUnsupportedVersion("2.4.1")).isTrue();
    assertThat(Runner.isUnsupportedVersion("2.5")).isTrue();
    assertThat(Runner.isUnsupportedVersion("2.11")).isFalse();
    assertThat(Runner.isUnsupportedVersion("3.0")).isFalse();
    assertThat(Runner.isUnsupportedVersion("3.1")).isFalse();
    assertThat(Runner.isUnsupportedVersion("3.2")).isFalse();
    assertThat(Runner.isUnsupportedVersion("3.3")).isFalse();
    assertThat(Runner.isUnsupportedVersion("3.4")).isFalse();
    assertThat(Runner.isUnsupportedVersion("3.5")).isFalse();
  }

  @Test
  public void shouldCheckVersionForTasks() {
    assertThat(Runner.isUnsupportedVersionForTasks("1.0")).isTrue();
    assertThat(Runner.isUnsupportedVersionForTasks("2.0")).isTrue();
    assertThat(Runner.isUnsupportedVersionForTasks("2.1")).isTrue();
    assertThat(Runner.isUnsupportedVersionForTasks("2.2")).isTrue();
    assertThat(Runner.isUnsupportedVersionForTasks("2.3")).isTrue();
    assertThat(Runner.isUnsupportedVersionForTasks("2.4")).isTrue();
    assertThat(Runner.isUnsupportedVersionForTasks("2.4.1")).isTrue();
    assertThat(Runner.isUnsupportedVersionForTasks("2.5")).isTrue();
    assertThat(Runner.isUnsupportedVersionForTasks("2.11")).isTrue();
    assertThat(Runner.isUnsupportedVersionForTasks("3.0")).isTrue();
    assertThat(Runner.isUnsupportedVersionForTasks("3.1")).isTrue();
    assertThat(Runner.isUnsupportedVersionForTasks("3.2")).isTrue();
    assertThat(Runner.isUnsupportedVersionForTasks("3.3")).isTrue();
    assertThat(Runner.isUnsupportedVersionForTasks("3.4")).isTrue();
    assertThat(Runner.isUnsupportedVersionForTasks("3.5")).isFalse();
  }

  @Test
  public void shouldGetServerUrl() {
    Properties properties = new Properties();
    Runner runner = Runner.create(properties);
    assertThat(runner.getSonarServerURL()).isEqualTo("http://localhost:9000");
    properties.setProperty("sonar.host.url", "foo");
    assertThat(runner.getSonarServerURL()).isEqualTo("foo");
  }

  @Test
  public void shouldGetSonarUser() {
    Properties properties = new Properties();
    properties.setProperty("sonar.login", "sonar");
    Runner runner = Runner.create(properties);
    assertThat(runner.getSonarUser()).isEqualTo("sonar");
  }

  @Test
  public void shouldGetSonarPassword() {
    Properties properties = new Properties();
    properties.setProperty("sonar.password", "sonarpass");
    Runner runner = Runner.create(properties);
    assertThat(runner.getSonarPassword()).isEqualTo("sonarpass");
  }

  @Test
  public void shouldInitDirs() throws Exception {
    Properties props = new Properties();
    File home = tempFolder.newFolder("shouldInitDirs").getCanonicalFile();
    props.setProperty(Runner.PROPERTY_SONAR_PROJECT_BASEDIR, home.getCanonicalPath());
    Runner runner = Runner.create(props);
    assertThat(runner.getProperties().get(Runner.PROPERTY_SONAR_PROJECT_BASEDIR)).isEqualTo(home.getCanonicalPath());

    assertThat(runner.getProjectDir().getCanonicalFile()).isEqualTo(home);
    assertThat(runner.getWorkDir().getCanonicalFile()).isEqualTo(new File(home, ".sonar"));
  }

  @Test
  public void shouldInitProjectDirWithCurrentDir() throws Exception {
    Runner runner = Runner.create(new Properties());

    assertThat(runner.getProjectDir().isDirectory()).isTrue();
    assertThat(runner.getProjectDir().exists()).isTrue();
  }

  @Test
  public void shouldSetValidBaseDirOnConstructor() {
    File baseDir = tempFolder.newFolder("shouldInitDirs");
    Runner runner = Runner.create(new Properties(), baseDir);
    assertThat(runner.getProjectDir()).isEqualTo(baseDir);
  }

  @Test
  public void shouldFailIfBaseDirDoesNotExist() {
    File fakeBasedir = new File("fake");

    thrown.expect(RunnerException.class);
    thrown.expectMessage("Project home must be an existing directory: " + fakeBasedir.getAbsolutePath());

    Runner.create(new Properties(), fakeBasedir);
  }

  @Test
  public void shouldSpecifyWorkingDirectory() {
    Properties properties = new Properties();
    Runner runner = Runner.create(properties);
    assertThat(runner.getWorkDir()).isEqualTo(new File(".", ".sonar"));

    // empty string
    properties.setProperty(Runner.PROPERTY_WORK_DIRECTORY, "    ");
    runner = Runner.create(properties);
    assertThat(runner.getWorkDir()).isEqualTo(new File(".", ".sonar").getAbsoluteFile());

    // real relative path
    properties.setProperty(Runner.PROPERTY_WORK_DIRECTORY, "temp-dir");
    runner = Runner.create(properties);
    assertThat(runner.getWorkDir()).isEqualTo(new File(".", "temp-dir").getAbsoluteFile());

    // real absolute path
    properties.setProperty(Runner.PROPERTY_WORK_DIRECTORY, new File("target", "temp-dir2").getAbsolutePath());
    runner = Runner.create(properties);
    assertThat(runner.getWorkDir()).isEqualTo(new File("target", "temp-dir2").getAbsoluteFile());
  }

  @Test
  public void shouldDeleteWorkingDirectory() {
    Properties properties = new Properties();
    File workDir = new File("target", "temp-dir-should-be-deleted");
    workDir.mkdirs();
    assertThat(workDir.exists()).isTrue();
    // real absolute path
    properties.setProperty(Runner.PROPERTY_WORK_DIRECTORY, workDir.getAbsolutePath());
    Runner.create(properties);
    assertThat(workDir.exists()).isFalse();
  }

  @Test
  public void shouldCheckSonarVersion() {
    Properties properties = new Properties();
    Runner runner = Runner.create(properties);
    Bootstrapper bootstrapper = mock(Bootstrapper.class);

    // nothing happens, OK
    when(bootstrapper.getServerVersion()).thenReturn("3.0");
    runner.checkSonarVersion(bootstrapper);

    // but fails with older versions
    when(bootstrapper.getServerVersion()).thenReturn("2.1");
    thrown.expect(RunnerException.class);
    thrown.expectMessage("Sonar 2.1 is not supported. Please upgrade Sonar to version 2.11 or more.");
    runner.checkSonarVersion(bootstrapper);
  }

  @Test
  public void shouldCheckSonarVersionForTasks() {
    Properties properties = new Properties();
    Runner runner = Runner.create("foo-cmd", properties, properties);
    Bootstrapper bootstrapper = mock(Bootstrapper.class);

    // nothing happens, OK
    when(bootstrapper.getServerVersion()).thenReturn("3.5");
    runner.checkSonarVersion(bootstrapper);

    // but fails with older versions
    when(bootstrapper.getServerVersion()).thenReturn("3.4");
    thrown.expect(RunnerException.class);
    thrown.expectMessage("Sonar 3.4 doesn't support tasks. Please upgrade Sonar to version 3.5 or more.");
    runner.checkSonarVersion(bootstrapper);
  }
}
