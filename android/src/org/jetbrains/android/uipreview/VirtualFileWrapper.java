package org.jetbrains.android.uipreview;

import com.android.io.IAbstractFile;
import com.android.io.StreamException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;

public class VirtualFileWrapper implements IAbstractFile {
  private final Project myProject;
  private final VirtualFile myFile;

  public VirtualFileWrapper(@NotNull Project project, @NotNull VirtualFile file) {
    myFile = file;
    myProject = project;
  }

  @Override
  public InputStream getContents() throws StreamException {
    final String content = getFileContent();
    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
  }

  @NotNull
  private String getFileContent() {
    return ApplicationManager.getApplication().runReadAction(new Computable<>() {
      @Override
      public String compute() {
        if (!myFile.isValid()) {
          return "";
        }

        final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(myFile);
        return psiFile != null ? psiFile.getText() : "";
      }
    });
  }

  @Override
  public void setContents(InputStream source) throws StreamException {
    throw new UnsupportedOperationException();
  }

  @Override
  public OutputStream getOutputStream() throws StreamException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getOsLocation() {
    return FileUtilRt.toSystemDependentName(myFile.getPath());
  }

  @Override
  public boolean exists() {
    return myFile.exists();
  }

  @NotNull
  public VirtualFile getFile() {
    return myFile;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    VirtualFileWrapper wrapper = (VirtualFileWrapper)o;

    if (!myFile.equals(wrapper.myFile)) {
      return false;
    }
    if (!myProject.equals(wrapper.myProject)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = myProject.hashCode();
    result = 31 * result + myFile.hashCode();
    return result;
  }
}
