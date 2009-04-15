/* 
 *
 * Copyright (C) 2007-2008 sibyl project
 * http://code.google.com/p/sibyl/
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
 
package com.sibyl;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Small utility class to scan recursively into directory and search for files 
 * matching an extension
 * 
 * @author sibyl
 */
public class Directory {

    private static class ExtensionFilter implements FilenameFilter {
        private String extension;

        public ExtensionFilter(String ext){
            extension = ext;
        }

        public boolean accept(File dir, String filename) {
            return filename.endsWith(extension);
        }
    }

    private static class DirectoryFilter implements FileFilter {
        public boolean accept(File pathname) {
            return pathname.isDirectory() && pathname.canRead();
        }
    }

    private static void searchIn(File dir, String extension, List<String> files){
        // list files matching extension
        File[] listFiles = dir.listFiles(new ExtensionFilter(extension));
        if (listFiles !=null )
        {
            for(File f : listFiles)
            {
                files.add(f.getAbsolutePath());            
            }
            for(File f: dir.listFiles(new DirectoryFilter())){
                searchIn(f, extension, files);
            }
        }
    }

    /**
     * list all files matching extension starting from path, recursive search
     * @param path the path where to start the search (included)
     * @param extension the file extension
     * @return
     */
    public static List<String> scanFiles(String path, String extension){
        ArrayList<String> files = new ArrayList<String>();
        File directory = new File(path);
        // go in each directories
        searchIn(directory, extension, files);
        return files;
    }
}
