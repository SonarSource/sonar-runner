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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class UtilsTest {

  @Test
  void delete_non_empty_directory(@TempDir Path tmp) throws IOException {
    /*-
     * Create test structure:
     * tmp
     *   |-folder1
     *        |- file1
     *        |- folder2
     *             |- file2
     */
    Path tmpDir = Files.createTempDirectory(tmp, "junit");
    Path folder1 = tmpDir.resolve("folder1");
    Files.createDirectories(folder1);
    Path file1 = folder1.resolve("file1");
    Files.write(file1, "test1".getBytes());

    Path folder2 = folder1.resolve("folder2");
    Files.createDirectories(folder2);
    Path file2 = folder1.resolve("file2");
    Files.write(file2, "test2".getBytes());

    // delete it
    assertThat(tmpDir.toFile()).exists();
    Utils.deleteQuietly(tmpDir);
    assertThat(tmpDir.toFile()).doesNotExist();
  }

}
