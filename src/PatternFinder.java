/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.ArrayUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by abailey on 07.04.16.
 */
public class PatternFinder extends AnAction {


  @Override
  public void actionPerformed(final AnActionEvent anActionEvent) {
    //Get all the required data from data keys
    final Editor editor = anActionEvent.getRequiredData(CommonDataKeys.EDITOR);
    final Project project = anActionEvent.getRequiredData(CommonDataKeys.PROJECT);

    final SelectionModel selectionModel = editor.getSelectionModel();


    Runnable runnable = new Runnable() {
      @Override
      public void run() {

        //Get Basepath for project
        String basePath = project.getBasePath();

        try {

        //Array list for all found filenames
        List<String> filenames = new ArrayList<String>();
        String currentlySelectedPattern = selectionModel.getSelectedText();

          //Clean search for multiple select scenarious {{> atoms-text-paragraph-medium}}
          currentlySelectedPattern = currentlySelectedPattern.trim();

          currentlySelectedPattern = currentlySelectedPattern.replace("{{> ", "");
          currentlySelectedPattern = currentlySelectedPattern.replace("}}", "");

        //Path to patterns
        getFileNames(currentlySelectedPattern, filenames, Paths.get(basePath + "/source/_patterns"));

        //File found
        if(filenames.get(0) != null)
        {
          VirtualFile fileToOpen = LocalFileSystem.getInstance().findFileByPath(filenames.get(0));
          FileEditorManager.getInstance(project).openFile(fileToOpen, true);
        }

        }catch(Exception e)
        {
          System.out.println("No pattern found");
        }

      }
    };

    runnable.run();

    selectionModel.removeSelection();
  }

  private List<String> getFileNames(String search, List<String> fileNames, Path dir) {
    try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      for (Path path : stream) {
        if(path.toFile().isDirectory()) {
          getFileNames(search, fileNames, path);
        } else {

          String filePath = path.toAbsolutePath().toString().toLowerCase();

          String[] data = search.split("-");
          data = (String[])ArrayUtils.remove(data, 0);

          String differentSearchTerm = String.join("-", data).toLowerCase();

          //Search for patternlab identifier in filesystem
          if(filePath.contains(search.toLowerCase()) || filePath.contains(differentSearchTerm)) {
            fileNames.add(path.toAbsolutePath().toString());
          }

        }
      }
    } catch(IOException e) {
      //e.printStackTrace();
    }
    return fileNames;
  }

  @Override
  public void update(final AnActionEvent e) {

    final Editor editor = e.getData(CommonDataKeys.EDITOR);

    //Show only on mustache files
    VirtualFile currentFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
    String fileName = currentFile.getPath();

    if(fileName.contains("mustache"))
    {
      e.getPresentation().setVisible(true);
    } else {
      e.getPresentation().setVisible(false);
    }

  }


}
