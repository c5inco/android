/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.gradle.model.java;

import static com.google.common.truth.Truth.assertThat;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

/**
 * Tests for {@link JavaModuleContentRoot}.
 */
public class JavaModuleContentRootTest {
  @Test
  public void testCopyWithGradleContentRoot() {
    IdeaContentRoot originalContentRoot = createMock(IdeaContentRoot.class);

    File rootDirPath = new File("root");
    expect(originalContentRoot.getRootDirectory()).andStubReturn(rootDirPath);

    Pair<File, IdeaSourceDirectory> mock = createSourceDirectoryMock("src", false);
    File sourceDirPath = mock.getFirst();
    IdeaSourceDirectory sourceDir = mock.getSecond();
    mock = createSourceDirectoryMock("gen-src", true);
    File genSourceDirPath = mock.getFirst();
    IdeaSourceDirectory genSourceDir = mock.getSecond();
    originalContentRoot.getSourceDirectories();
    expectLastCall().andStubReturn(DomainObjectHashSet.newSet(sourceDir, genSourceDir));

    mock = createSourceDirectoryMock("test", false);
    File testDirPath = mock.getFirst();
    IdeaSourceDirectory testDir = mock.getSecond();
    mock = createSourceDirectoryMock("gen-test", true);
    File genTestDirPath = mock.getFirst();
    IdeaSourceDirectory genTestDir = mock.getSecond();
    originalContentRoot.getTestDirectories();
    expectLastCall().andStubReturn(DomainObjectHashSet.newSet(testDir, genTestDir));

    mock = createSourceDirectoryMock("resource", false);
    File resourceDirPath = mock.getFirst();
    IdeaSourceDirectory resourceDir = mock.getSecond();
    originalContentRoot.getResourceDirectories();
    expectLastCall().andStubReturn(DomainObjectHashSet.newSet(resourceDir));

    mock = createSourceDirectoryMock("test-resource", false);
    File testResourceDirPath = mock.getFirst();
    IdeaSourceDirectory testResourceDir = mock.getSecond();
    originalContentRoot.getTestResourceDirectories();
    expectLastCall().andStubReturn(DomainObjectHashSet.newSet(testResourceDir));

    File exclude = new File("exclude");
    Set<File> allExclude = Sets.newSet(exclude, null);
    expect(originalContentRoot.getExcludeDirectories()).andStubReturn(allExclude);

    replay(originalContentRoot, sourceDir, genSourceDir, testDir, genTestDir, resourceDir, testResourceDir);

    JavaModuleContentRoot contentRoot = JavaModuleContentRoot.copy(originalContentRoot);
    assertNotNull(contentRoot);
    assertSame(contentRoot.getRootDirPath(), rootDirPath);
    assertThat(contentRoot.getSourceDirPaths()).containsExactly(sourceDirPath);
    assertThat(contentRoot.getGenSourceDirPaths()).containsExactly(genSourceDirPath);
    assertThat(contentRoot.getTestDirPaths()).containsExactly(testDirPath);
    assertThat(contentRoot.getGenTestDirPaths()).containsExactly(genTestDirPath);
    assertThat(contentRoot.getResourceDirPaths()).containsExactly(resourceDirPath);
    assertThat(contentRoot.getTestResourceDirPaths()).containsExactly(testResourceDirPath);

    verify(originalContentRoot, sourceDir, genSourceDir, testDir, genTestDir, resourceDir, testResourceDir);
  }

  @NotNull
  private static Pair<File, IdeaSourceDirectory> createSourceDirectoryMock(@NotNull String fileName, boolean isGenerated) {
    File path = new File(fileName);
    IdeaSourceDirectory dir = createMock(IdeaSourceDirectory.class);
    expect(dir.getDirectory()).andStubReturn(path);
    expect(dir.isGenerated()).andStubReturn(isGenerated);
    return Pair.create(path, dir);
  }

  private static class DomainObjectHashSet<T> extends java.util.HashSet<T> implements DomainObjectSet<T> {
    static <T> DomainObjectHashSet<T> newSet(@NotNull T...elements) {
      return new DomainObjectHashSet<>(elements);
    }

    DomainObjectHashSet(@NotNull T...elements) {
      super(Arrays.asList(elements));
    }

    @Override
    @Nullable
    public T getAt(int index) throws IndexOutOfBoundsException {
      return getAll().get(index);
    }

    @Override
    @NotNull
    public List<T> getAll() {
      return Lists.newArrayList(this);
    }
  }
}