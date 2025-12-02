# FileStream v2.4升级报告

> **版本**: v2.4  
> **架构**: JavaFX + MVC + Modular Strategy Pattern  
> **构建工具**: Maven

## 1. 项目概述与核心变革

本项目最初是一个简单的**图片文件分流工具**（v1.0），代码逻辑与界面高度耦合。经过全面的重构与迭代，现已升级为一个**支持多格式预览、MIDI 可视化、批量处理**的综合性桌面应用。

### 核心架构升级：从 "God Class" 到 "MVC"
*   **v1.0 (旧版)**: 所有的逻辑（文件操作、界面构建、事件监听）全部写在一个 `App.java` 文件中，代码臃肿，难以维护，扩展性极差。
*   **v2.0+ (新版)**: 采用了严格的 **MVC (Model-View-Controller)** 架构模式。
    *   **Model (模型层)**: 负责业务逻辑（如 `FileOrganizerModel` 处理文件移动、撤销栈；`AudioEngine` 处理 MIDI 播放）。
    *   **View (视图层)**: 使用 **FXML** 定义界面布局，使用 **CSS** 定义样式，实现了逻辑与展示的彻底分离。
    *   **Controller (控制层)**: 负责响应用户交互，调度 Model 与 View。

---

## 2. 详细修改点对比 (v1 vs v2)

| 维度 | v1.0 (初始版本)                       | v2.2 (当前版本) | 提升价值 |
| :--- |:----------------------------------| :--- | :--- |
| **代码结构** | 单文件 (`App.java`) "大杂烩"            | **模块化 MVC** (`controller`, `model`, `view`, `service`, `preview` 分包) | 高内聚低耦合，易于扩展新功能 |
| **界面构建** | Java 代码硬编码 UI (`new Button(...)`) | **FXML** 声明式布局 + **CSS** 样式表 | 界面更美观，支持 SceneBuilder 可视化编辑 |
| **国际化 (I18n)** | 硬编码中文，无法切换                        | **ResourceBundle** (支持中/英热切换) | 支持多语言，无需重启即可切换语言 |
| **主题系统** | 硬编码颜色 (`setStyle`)                | **CSS 变量** (支持亮色/暗色模式热切换) | 现代化 UI 体验，夜间模式支持 |
| **文件预览** | 仅支持图片 (`ImageView`)               | **策略模式** (支持 图片/音视频/MIDI/Office/代码/Markdown) | 极强的扩展性，新增格式只需加一个类 |
| **MIDI 功能** | 无                                 | **专业可视化** (瀑布流、SF2音源、延迟校准) | 增加了多媒体娱乐属性 |
| **文件操作** | 简单的移动/删除                          | **带撤销栈 (Undo)**、回收站机制、批量重命名 | 数据更安全，操作更人性化 |
| **性能优化** | 大文件/大图直接读取 (易 OOM)                | **降采样加载**、**流式读取**、**资源及时释放** | 内存占用降低 90%，大文件秒开 |


---

## 3. 新增功能模块详解

### 3.1 强大的预览子系统 (Preview System)
利用 **工厂模式 (Factory Pattern)** 和 **策略模式 (Strategy Pattern)** 实现了对不同文件类型的动态支持。

*   **图片 (Image)**: 支持鼠标滚轮缩放、拖拽查看、大图内存优化。
*   **代码/文本 (Text/Code)**:
    *   集成 **WebView** 和 **Highlight.js** (本地离线版)，支持几十种编程语言的**语法高亮**。
    *   支持 **Markdown** 文件的源码/渲染视图切换，且支持 **表格渲染**。
    *   提供**轻量级编辑模式**，可直接修改并保存代码/文本。
*   **Office 文档**:
    *   **Word (.docx)**: 利用 Apache POI 解析文档结构并转换为 HTML 预览。
    *   **Excel/CSV**: 解析为 JavaFX `TableView`，支持列排序和数据查看。
    *   **PPT/PDF**: 利用 Apache PDFBox 和 POI 将每一页渲染为高清图片，支持翻页浏览。
*   **多媒体**:
    *   **视频/音频**: 支持 MP4/MP3 播放，黑色影院模式，带进度条控制。

### 3.2 MIDI 可视化工作站
从原本的文件工具中衍生出的独立功能模块。
*   **可视化**: 实现了类似 "Synthesia" 的钢琴卷帘（瀑布流）效果。
*   **音频引擎**: 自研 `AudioEngine`，封装了 `javax.sound.midi`。
*   **高级功能**:
    *   支持加载外部 **SF2 音色库**。
    *   **延迟校准 (Latency Sync)**：解决音画不同步问题。
    *   **透明度调节**。
    *   **智能提示**：在文件列表中点击 MIDI 文件会自动弹窗建议跳转。

### 3.3 批量后缀修改器 (Extension Changer)
新增的独立 Tab 页面。
*   采用 **FlowPane + ToggleButton** 布局，支持可视化多选文件。
*   一键批量修改文件扩展名。

---

## 4. 技术栈与依赖库

为了实现上述功能，项目引入了以下关键技术：

*   **UI 框架**: JavaFX 21 (Controls, FXML, Web, Media, Swing)
*   **Office 解析**: Apache POI (Word/Excel/PPT)
*   **PDF 处理**: Apache PDFBox
*   **Markdown 引擎**: CommonMark (Core + Tables Extension)
*   **JSON 解析**: Google Gson (用于处理 ipynb)
*   **CSV 解析**: Apache Commons CSV
*   **打包工具**: Maven Shade Plugin, Launch4j (生成 EXE)

---

## 5. 目录结构说明

```text
src/main
├── java/fun/vignachu
│   ├── AppLauncher.java          // [入口] 绕过模块检查的启动器
│   ├── FileStreamApp.java        // [核心] JavaFX 应用生命周期管理
│   ├── controller/               // [C层] 处理交互逻辑
│   │   ├── FileOrganizerController.java // 文件分流与预览逻辑
│   │   ├── MidiController.java          // MIDI 渲染与播放逻辑
│   │   └── ...
│   ├── model/                    // [M层] 业务逻辑与数据实体
│   │   ├── FileOrganizerModel.java      // 文件操作底层实现
│   │   ├── FileType.java                // 文件类型枚举定义
│   │   └── ...
│   ├── preview/                  // [策略模式] 预览核心模块
│   │   ├── PreviewFactory.java          // 工厂类
│   │   ├── TextPreviewStrategy.java     // 文本/代码/MD 预览策略
│   │   ├── MediaPreviewStrategy.java    // 音视频预览策略
│   │   └── ...
│   ├── service/                  // [服务层]
│   │   └── AudioEngine.java             // MIDI 音频引擎
│   └── util/                     // [工具层]
│       ├── I18n.java                    // 国际化绑定工具
│       └── ThemeManager.java            // 主题切换工具
└── resources/fun/vignachu
    ├── css/                      // 样式表 (含 Highlight.js 主题)
    ├── i18n/                     // 多语言资源文件 (.properties)
    ├── view/                     // FXML 界面布局文件
    └── assets/                   // 离线资源 (JS/CSS库)
```