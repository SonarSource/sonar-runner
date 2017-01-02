/*
 * SonarQube Scanner API
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonarsource.scanner.api.internal;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonarsource.scanner.api.internal.cache.FileCache;
import org.sonarsource.scanner.api.internal.cache.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class JarsTest {
  private ServerConnection connection = mock(ServerConnection.class);
  private JarExtractor jarExtractor = mock(JarExtractor.class);
  private FileCache fileCache = mock(FileCache.class);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_download_jar_files() throws Exception {
    File batchJar = temp.newFile("sonar-scanner-api-batch.jar");
    when(jarExtractor.extractToTemp("sonar-scanner-api-batch")).thenReturn(batchJar.toPath());
    // index of the files to download
    when(connection.downloadString("/batch/index")).thenReturn(
      "cpd.jar|CA124VADFSDS\n" +
        "squid.jar|34535FSFSDF\n");

    Jars jars = new Jars(fileCache, connection, jarExtractor, mock(Logger.class));
    List<File> files = jars.download();

    assertThat(files).isNotNull();
    verify(connection, times(1)).downloadString("/batch/index");
    verifyNoMoreInteractions(connection);
    verify(fileCache, times(1)).get(eq("cpd.jar"), eq("CA124VADFSDS"), any(FileCache.Downloader.class));
    verify(fileCache, times(1)).get(eq("squid.jar"), eq("34535FSFSDF"), any(FileCache.Downloader.class));
    verifyNoMoreInteractions(fileCache);
  }

  @Test
  public void should_honor_sonarUserHome() throws IOException {
    Properties props = new Properties();
    File f = temp.newFolder();
    props.put("sonar.userHome", f.getAbsolutePath());
    Jars jars = new Jars(connection, jarExtractor, mock(Logger.class), props);
    assertThat(jars.getFileCache().getDir()).isEqualTo(new File(f, "cache"));
  }

  @Test
  public void should_fail_to_download_files() throws Exception {
    File batchJar = temp.newFile("sonar-scanner-api-batch.jar");
    when(jarExtractor.extractToTemp("sonar-scanner-api-batch")).thenReturn(batchJar.toPath());
    // index of the files to download
    when(connection.downloadString("/batch/index")).thenThrow(new IllegalStateException());

    Jars jars = new Jars(fileCache, connection, jarExtractor, mock(Logger.class));
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Fail to get bootstrap index from server");

    jars.download();
  }

  @Test
  public void test_invalid_index() throws Exception {
    File batchJar = temp.newFile("sonar-scanner-api-batch.jar");
    when(jarExtractor.extractToTemp("sonar-scanner-api-batch")).thenReturn(batchJar.toPath());
    // index of the files to download
    when(connection.downloadString("/batch/index")).thenReturn(
      "cpd.jar\n");

    Jars jars = new Jars(fileCache, connection, jarExtractor, mock(Logger.class));
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Fail to bootstrap from server. Bootstrap index was:\ncpd.jar\n");

    jars.download();
  }

  @Test
  public void test_jar_downloader() throws Exception {
    Jars.ScannerFileDownloader downloader = new Jars.ScannerFileDownloader(connection);
    File toFile = temp.newFile();
    downloader.download("squid.jar", toFile);
    verify(connection).downloadFile("/batch/file?name=squid.jar", toFile.toPath());
  }
}
