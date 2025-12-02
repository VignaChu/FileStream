package fun.vignachu.model;
import java.io.File;
public class ActionRecord {
    // 定义每次用户操作的详细信息
    public final ActionType type;
    public final File originalPath;
    public final File targetPath;
    public final String fileName;
    public ActionRecord(ActionType type, File originalPath, File targetPath) {
        this.type = type; this.originalPath = originalPath; this.targetPath = targetPath; this.fileName = originalPath.getName();
    }
}