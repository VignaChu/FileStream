package fun.vignachu.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class FileOrganizerModel {

    // 文件分流的业务核心引擎
    private final LastActionManager actionManager = new LastActionManager();
    private Path recycleBinPath;
    private File currentSourceDir;
    private File currentTargetDir;

    public void initRecycleBin() throws IOException {
        String userDir = System.getProperty("user.dir");
        recycleBinPath = Paths.get(userDir, ".recycle_bin");
        Files.createDirectories(recycleBinPath);
    }

    public List<File> getSourceFiles() {
        if (currentSourceDir == null) return Collections.emptyList();
        // 暂时只列出所有文件，具体筛选交给显示逻辑或这里加过滤
        File[] files = currentSourceDir.listFiles(File::isFile);
        return files != null ? Arrays.asList(files) : Collections.emptyList();
    }

    public List<File> getTargetSubDirs() {
        if (currentTargetDir == null) return Collections.emptyList();
        File[] dirs = currentTargetDir.listFiles(File::isDirectory);
        return dirs != null ? Arrays.asList(dirs) : Collections.emptyList();
    }

    public void moveFile(String fileName, File targetSubDir) throws IOException {
        File srcFile = new File(currentSourceDir, fileName);
        Path targetPath = targetSubDir.toPath().resolve(fileName);
        Files.move(srcFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        actionManager.record(ActionType.MOVE, srcFile, targetSubDir);
    }

    public void deleteFile(String fileName) throws IOException {
        File srcFile = new File(currentSourceDir, fileName);
        String uniqueName = fileName + "_" + System.currentTimeMillis();
        Path targetPath = recycleBinPath.resolve(uniqueName);
        Files.move(srcFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        actionManager.record(ActionType.DELETE_PIC, srcFile, targetPath.toFile());
    }

    public void createDirectory(String name) throws IOException {
        Path newPath = currentTargetDir.toPath().resolve(name);
        Files.createDirectories(newPath);
        actionManager.record(ActionType.CREATE_DIR, newPath.toFile(), currentTargetDir);
    }

    public void deleteDirectory(File dir) throws IOException {
        String uniqueName = dir.getName() + "_DIR_" + System.currentTimeMillis();
        Path targetPath = recycleBinPath.resolve(uniqueName);
        Files.move(dir.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        actionManager.record(ActionType.DELETE_DIR, dir, targetPath.toFile());
    }

    public ActionRecord undo() throws IOException {
        ActionRecord record = actionManager.pop();
        if (record == null) return null;

        switch (record.type) {
            case MOVE:
                Path movedFile = record.targetPath.toPath().resolve(record.fileName);
                Files.move(movedFile, record.originalPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
                break;
            case DELETE_PIC:
            case DELETE_DIR:
                Files.move(record.targetPath.toPath(), record.originalPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
                break;
            case CREATE_DIR:
                Files.delete(record.originalPath.toPath());
                break;
        }
        return record;
    }

    public ActionType peekLastActionType() {
        if (!actionManager.hasAction()) return null;
        ActionRecord record = actionManager.peek(); // 调用 LastActionManager 的 peek
        return record != null ? record.type : null;
    }

    // 【新增】静态清理方法，供 Main App 调用
    public static void cleanUp() {
        try {
            String userDir = System.getProperty("user.dir");
            File recycleBin = new File(userDir, ".recycle_bin");
            if (recycleBin.exists()) {
                deleteDirRecursively(recycleBin);
                // 如果你想保留空文件夹只清空内容，就把下面这行注释掉
                recycleBin.delete();
                System.out.println("回收站已清理");
            }
        } catch (Exception e) {
            System.err.println("清理回收站失败: " + e.getMessage());
        }
    }

    // 【新增】递归删除工具方法
    private static void deleteDirRecursively(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirRecursively(file);
                }
                file.delete();
            }
        }
    }


    // Getters & Setters
    public void setCurrentSourceDir(File f) { this.currentSourceDir = f; }
    public File getCurrentSourceDir() { return currentSourceDir; }
    public void setCurrentTargetDir(File f) { this.currentTargetDir = f; }
    public File getCurrentTargetDir() { return currentTargetDir; }
    public boolean hasUndo() { return actionManager.hasAction(); }
    public int getUndoCount() { return actionManager.size(); }
}