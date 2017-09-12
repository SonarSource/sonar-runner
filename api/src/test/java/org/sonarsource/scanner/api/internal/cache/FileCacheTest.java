/*
 * SonarQube Scanner API
 * Copyright (C) 2011-2017 SonarSource SA
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
package org.sonarsource.scanner.api.internal.cache;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileCacheTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void not_in_cache() throws IOException {
    FileCache cache = FileCache.create(temp.newFolder().toPath(), mock(Logger.class));
    assertThat(cache.get("sonar-foo-plugin-1.5.jar", "ABCDE")).isNull();
  }

  @Test
  public void found_in_cache() throws IOException {
    FileCache cache = FileCache.create(temp.newFolder().toPath(), mock(Logger.class));

    // populate the cache. Assume that hash is correct.
    File cachedFile = new File(new File(cache.getDir(), "ABCDE"), "sonar-foo-plugin-1.5.jar");
    write(cachedFile, "body");

    assertThat(cache.get("sonar-foo-plugin-1.5.jar", "ABCDE")).isNotNull().exists().isEqualTo(cachedFile);
  }

  @Test
  public void download_and_add_to_cache() throws IOException {
    FileHashes hashes = mock(FileHashes.class);
    FileCache cache = new FileCache(temp.newFolder().toPath(), hashes, mock(Logger.class));
    when(hashes.of(any(File.class))).thenReturn("ABCDE");

    FileCache.Downloader downloader = new FileCache.Downloader() {
      boolean single = false;

      public void download(String filename, File toFile) throws IOException {
        if (single) {
          throw new IllegalStateException("Already called");
        }
        write(toFile, "body");
        single = true;
      }
    };
    File cachedFile = cache.get("sonar-foo-plugin-1.5.jar", "ABCDE", downloader);
    assertThat(cachedFile).isNotNull().exists().isFile();
    assertThat(cachedFile.getName()).isEqualTo("sonar-foo-plugin-1.5.jar");
    assertThat(cachedFile.getParentFile().getParentFile()).isEqualTo(cache.getDir());
    assertThat(read(cachedFile)).isEqualTo("body");

    File againFromCache = cache.get("sonar-foo-plugin-1.5.jar", "ABCDE", downloader);
    assertThat(againFromCache).isNotNull().exists().isFile();
    assertThat(againFromCache.getName()).isEqualTo("sonar-foo-plugin-1.5.jar");
    assertThat(againFromCache.getParentFile().getParentFile()).isEqualTo(cache.getDir());
    assertThat(read(againFromCache)).isEqualTo("body");
  }

  @Test
  public void download_corrupted_file() throws IOException {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("INVALID HASH");

    FileHashes hashes = mock(FileHashes.class);
    FileCache cache = new FileCache(temp.newFolder().toPath(), hashes, mock(Logger.class));
    when(hashes.of(any(File.class))).thenReturn("VWXYZ");

    FileCache.Downloader downloader = new FileCache.Downloader() {
      public void download(String filename, File toFile) throws IOException {
        write(toFile, "corrupted body");
      }
    };
    cache.get("sonar-foo-plugin-1.5.jar", "ABCDE", downloader);
  }

  @Test
  public void concurrent_download() throws IOException {
    FileHashes hashes = mock(FileHashes.class);
    when(hashes.of(any(File.class))).thenReturn("ABCDE");
    final FileCache cache = new FileCache(temp.newFolder().toPath(), hashes, mock(Logger.class));

    FileCache.Downloader downloader = new FileCache.Downloader() {
      public void download(String filename, File toFile) throws IOException {
        // Emulate a concurrent download that adds file to cache before
        File cachedFile = new File(new File(cache.getDir(), "ABCDE"), "sonar-foo-plugin-1.5.jar");
        write(cachedFile, "downloaded by other");

        write(toFile, "downloaded by me");
      }
    };

    // do not fail
    File cachedFile = cache.get("sonar-foo-plugin-1.5.jar", "ABCDE", downloader);
    assertThat(cachedFile).isNotNull().exists().isFile();
    assertThat(cachedFile.getName()).isEqualTo("sonar-foo-plugin-1.5.jar");
    assertThat(cachedFile.getParentFile().getParentFile()).isEqualTo(cache.getDir());
    assertThat(read(cachedFile)).contains("downloaded by");
  }

  private static void write(File f, String txt) throws IOException {
    Files.createDirectories(f.toPath().getParent());
    Files.write(f.toPath(), txt.getBytes(StandardCharsets.UTF_8));
  }

  private static String read(File f) throws IOException {
    return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
  }
}
