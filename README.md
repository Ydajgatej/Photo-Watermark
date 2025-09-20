# Photo-Watermark

一个Java命令行程序，用于读取图片的EXIF信息中的拍摄时间并将其作为水印添加到图片上。

## 功能特性

- 读取图片文件的EXIF信息中的拍摄时间
- 支持自定义水印字体大小
- 支持自定义水印颜色
- 支持自定义水印位置（左上角、右上角、左下角、右下角、居中）
- 将处理后的图片保存到指定目录

## 技术栈

- Java 8
- Maven
- metadata-extractor库（用于读取EXIF信息）

## 构建项目

确保已安装Java 8和Maven，然后执行以下命令：

```bash
mvn clean package
```

构建成功后，将在`target`目录下生成可执行的JAR文件：`photo-watermark-1.0-SNAPSHOT-jar-with-dependencies.jar`

## 使用方法

1. 打开命令行窗口
2. 导航到JAR文件所在目录
3. 执行以下命令：

```bash
java -jar photo-watermark-1.0-SNAPSHOT-jar-with-dependencies.jar
```

4. 根据提示输入图片文件路径、水印字体大小、颜色和位置

## 命令行参数说明

程序运行时会引导用户输入以下参数：

- **图片文件路径**：包含图片的目录路径
- **水印字体大小**：可选，默认为36
- **水印颜色**：可选，默认为白色，可选值：BLACK, WHITE, RED, GREEN, BLUE, YELLOW
- **水印位置**：可选，默认为右下角，可选值：TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER

## 输出说明

处理后的图片将保存在原目录的子目录中，子目录名为`原目录名_watermark`。

## 支持的图片格式

- JPG/JPEG
- PNG

## 注意事项

- 确保图片文件包含EXIF信息中的拍摄时间
- 对于没有EXIF信息或无法读取EXIF信息的图片，程序将显示错误信息
- 处理大尺寸图片时可能需要较长时间

## 依赖项

- metadata-extractor 2.16.0
- xmpcore 6.1.11
