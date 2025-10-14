package fun.vignachu;

import java.io.File;

/**
 * 单步操作记录类。
 * 存储执行操作时的状态和路径信息。
 */
public class ActionRecord {
    public final ActionType type;
    public final File originalPath; // 原始路径 (MOVE/DELETE_PIC/DELETE_DIR: 操作前的完整路径；CREATE_DIR: 新建文件夹的完整路径)
    public final File targetPath;   // 目标路径 (MOVE: 目标分类文件夹；DELETE_PIC/DELETE_DIR: 回收站中的路径；CREATE_DIR: 父目录路径)
    public final String fileName;   // 原始文件名/文件夹名

    public ActionRecord(ActionType type, File originalPath, File targetPath) {
        this.type = type;
        this.originalPath = originalPath;
        this.targetPath = targetPath;
        this.fileName = originalPath.getName();
    }
}