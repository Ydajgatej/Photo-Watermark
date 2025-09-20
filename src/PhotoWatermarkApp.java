import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PhotoWatermarkApp {
    private static final Logger logger = Logger.getLogger(PhotoWatermarkApp.class.getName());
    private static final String[] SUPPORTED_FORMATS = {"jpg", "jpeg", "png"};
    private static final String DEFAULT_FONT_NAME = "Arial";
    private static final int DEFAULT_FONT_SIZE = 36;
    private static final Color DEFAULT_FONT_COLOR = Color.WHITE;
    private static final int DEFAULT_OPACITY = 128; // 半透明
    
    public enum Position {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // 1. 获取用户输入的图片文件路径
        System.out.println("请输入图片文件路径：");
        String inputPath = scanner.nextLine().trim();
        File inputDir = new File(inputPath);
        
        if (!inputDir.exists()) {
            System.out.println("错误：路径不存在！");
            return;
        }
        
        // 2. 创建输出目录
        String outputDirName = inputDir.getName() + "_watermark";
        File outputDir = new File(inputDir.getParent(), outputDirName);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            System.out.println("错误：无法创建输出目录！");
            return;
        }
        
        // 3. 获取用户设置的水印参数
        System.out.println("请设置水印字体大小（默认：" + DEFAULT_FONT_SIZE + "）：");
        String fontSizeStr = scanner.nextLine().trim();
        int fontSize = fontSizeStr.isEmpty() ? DEFAULT_FONT_SIZE : Integer.parseInt(fontSizeStr);
        
        System.out.println("请设置水印颜色（默认：白色，可选值：BLACK, WHITE, RED, GREEN, BLUE, YELLOW）：");
        String colorStr = scanner.nextLine().trim().toUpperCase();
        Color fontColor = getColorByName(colorStr, DEFAULT_FONT_COLOR);
        
        System.out.println("请设置水印位置（默认：右下角，可选值：TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER）：");
        String positionStr = scanner.nextLine().trim().toUpperCase();
        Position position = getPositionByName(positionStr, Position.BOTTOM_RIGHT);
        
        // 4. 处理图片
        processImages(inputDir, outputDir, fontSize, fontColor, position);
        
        System.out.println("所有图片处理完成！\n处理后的图片保存在：" + outputDir.getAbsolutePath());
        scanner.close();
    }
    
    private static void processImages(File inputDir, File outputDir, int fontSize, Color fontColor, Position position) {
        File[] files = inputDir.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("警告：输入目录中没有文件！");
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) continue;
            
            String fileName = file.getName().toLowerCase();
            boolean isSupported = Arrays.stream(SUPPORTED_FORMATS)
                    .anyMatch(format -> fileName.endsWith("." + format));
            
            if (!isSupported) {
                System.out.println("跳过不支持的文件格式：" + file.getName());
                continue;
            }
            
            try {
                addWatermark(file, outputDir, fontSize, fontColor, position);
                System.out.println("已处理：" + file.getName());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "处理文件失败：" + file.getName(), e);
                System.out.println("错误：处理文件失败：" + file.getName() + "，原因：" + e.getMessage());
            }
        }
    }
    
    private static void addWatermark(File imageFile, File outputDir, int fontSize, Color fontColor, Position position) throws IOException, ImageProcessingException, ParseException {
        // 读取图片
        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            throw new IOException("无法读取图片文件");
        }
        
        // 获取图片的EXIF信息中的拍摄时间
        String watermarkText = getShootingDateFromExif(imageFile);
        if (watermarkText == null || watermarkText.isEmpty()) {
            throw new IOException("无法获取图片的拍摄时间信息");
        }
        
        // 创建Graphics2D对象以绘制水印
        Graphics2D g2d = image.createGraphics();
        
        // 设置字体和颜色（带透明度）
        Font font = new Font(DEFAULT_FONT_NAME, Font.BOLD, fontSize);
        g2d.setFont(font);
        
        // 创建带透明度的颜色
        Color colorWithOpacity = new Color(fontColor.getRed(), fontColor.getGreen(), fontColor.getBlue(), DEFAULT_OPACITY);
        g2d.setColor(colorWithOpacity);
        
        // 计算水印位置
        FontMetrics metrics = g2d.getFontMetrics(font);
        int textWidth = metrics.stringWidth(watermarkText);
        int textHeight = metrics.getHeight();
        int x, y;
        
        switch (position) {
            case TOP_LEFT:
                x = 10;
                y = metrics.getAscent() + 10;
                break;
            case TOP_RIGHT:
                x = image.getWidth() - textWidth - 10;
                y = metrics.getAscent() + 10;
                break;
            case BOTTOM_LEFT:
                x = 10;
                y = image.getHeight() - textHeight + metrics.getAscent() - 10;
                break;
            case CENTER:
                x = (image.getWidth() - textWidth) / 2;
                y = (image.getHeight() + textHeight) / 2;
                break;
            case BOTTOM_RIGHT:
            default:
                x = image.getWidth() - textWidth - 10;
                y = image.getHeight() - textHeight + metrics.getAscent() - 10;
                break;
        }
        
        // 绘制水印
        g2d.drawString(watermarkText, x, y);
        g2d.dispose();
        
        // 保存处理后的图片
        String outputFileName = imageFile.getName();
        String format = outputFileName.substring(outputFileName.lastIndexOf(".") + 1);
        File outputFile = new File(outputDir, outputFileName);
        ImageIO.write(image, format, outputFile);
    }
    
    private static String getShootingDateFromExif(File imageFile) throws ImageProcessingException, IOException, ParseException {
        Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
        ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        
        if (directory != null) {
            Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            if (date != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                return dateFormat.format(date);
            }
        }
        
        return null;
    }
    
    private static Color getColorByName(String colorName, Color defaultColor) {
        if (colorName == null || colorName.isEmpty()) {
            return defaultColor;
        }
        
        switch (colorName.toUpperCase()) {
            case "BLACK":
                return Color.BLACK;
            case "WHITE":
                return Color.WHITE;
            case "RED":
                return Color.RED;
            case "GREEN":
                return Color.GREEN;
            case "BLUE":
                return Color.BLUE;
            case "YELLOW":
                return Color.YELLOW;
            default:
                System.out.println("无效的颜色值，使用默认颜色。");
                return defaultColor;
        }
    }
    
    private static Position getPositionByName(String positionName, Position defaultPosition) {
        try {
            return Position.valueOf(positionName);
        } catch (IllegalArgumentException e) {
            System.out.println("无效的位置值，使用默认位置。");
            return defaultPosition;
        }
    }
}