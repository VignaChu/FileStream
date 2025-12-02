package fun.vignachu.model;

import java.io.File;

public class ExtensionChangerModel {

    // 读取源文件，新后缀，并修改
    public boolean changeExtension(File file, String newExtension) {
        if (file == null || !file.exists() || file.isDirectory()) return false;

        String oldName = file.getName();
        String newName;

        int dotIndex = oldName.lastIndexOf('.');
        if (dotIndex == -1) {
            // 原文件没后缀，直接追加
            newName = oldName + "." + newExtension;
        } else {
            // 替换后缀
            newName = oldName.substring(0, dotIndex + "." .length()) + newExtension;
        }

        File newFile = new File(file.getParent(), newName);

        // 如果目标文件已存在，防止覆盖，返回失败
        if (newFile.exists()) return false;

        return file.renameTo(newFile);
    }
}