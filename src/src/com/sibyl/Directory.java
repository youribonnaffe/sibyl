package com.sibyl;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;

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

    private static void searchIn(File dir, String extension, ArrayList<String> files){
        // list files matching extension
        for(File f : dir.listFiles(new ExtensionFilter(extension))){
            files.add(f.getAbsolutePath());            
        }

        // go in each directories
        for(File f: dir.listFiles(new DirectoryFilter())){
            searchIn(f, extension, files);
        }
    }

    /**
     * list all files matching extension starting from path, recursive search
     * @param path the path where to start the search (included)
     * @param extension the file extension
     * @return
     */
    public static ArrayList<String> scanFiles(String path, String extension){
        ArrayList<String> files = new ArrayList<String>();
        File directory = new File(path);
        // go in each directories
        searchIn(directory, extension, files);
        return files;
    }
}
