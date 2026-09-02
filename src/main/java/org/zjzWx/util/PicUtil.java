package org.zjzWx.util;

import org.springframework.core.io.ByteArrayResource;
import org.zjzWx.entity.Photo;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.stream.Stream;

//图片工具类
public class PicUtil {


    //使用照片ID建立临时目录，并把用户上传的原图保存进去
    public static String saveUpload(byte[] imageBytes,Integer photoId,String directory,String extension) throws IOException {
        String relativePath = "temp/" + photoId + "/" + getImageFileName(imageBytes,extension);
        Path imagePath = getFile(relativePath,directory);
        Files.createDirectories(imagePath.getParent());
        Files.write(imagePath,imageBytes);
        return relativePath;
    }

    //按照当前业务确定的图片格式保存临时图片，每次都生成不同文件名
    public static String saveTempImage(Photo photo,byte[] imageBytes,String directory,String extension) throws IOException {
        String relativePath = "temp/" + photo.getId() + "/" + getImageFileName(imageBytes,extension);
        Path imagePath = getFile(relativePath,directory);
        Files.createDirectories(imagePath.getParent());
        Files.write(imagePath,imageBytes);
        return relativePath;
    }

    //按照当前业务确定的图片格式保存正式图片并返回公开访问地址
    public static String savePermanentImage(String folderName,byte[] imageBytes,String directory,String picDomain,String extension) throws IOException {
        String relativePath = folderName + "/" + getImageFileName(imageBytes,extension);
        Path imagePath = getFile(relativePath,directory);
        Files.createDirectories(imagePath.getParent());
        Files.write(imagePath,imageBytes);
        return getPublicUrl(relativePath,picDomain);
    }

    //根据相对路径读取图片，最终路径只能位于配置的图片根目录内
    public static Path getFile(String relativePath,String directory) {
        Path rootPath = Paths.get(directory).toAbsolutePath().normalize();
        Path imagePath = rootPath.resolve(relativePath).normalize();

        //当最终路径跑到图片根目录外面时，拒绝读取这个路径
        if(!imagePath.startsWith(rootPath)){
            throw new IllegalArgumentException("图片路径非法");
        }
        return imagePath;
    }

    //将图片相对路径转换为公开访问地址
    public static String getPublicUrl(String relativePath,String picDomain) {
        return picDomain + relativePath.replace("\\","/");
    }

    //删除一张已经不再使用的临时图片
    public static void deleteFile(String relativePath,String directory) {

        //当没有文件路径时，不需要执行删除
        if(relativePath==null){
            return;
        }

        try {
            Files.deleteIfExists(getFile(relativePath,directory));
        } catch (Exception ignored) {
        }
    }

    //删除一张照片对应的整个临时目录
    public static void deleteTempDirectory(Integer photoId,String directory) {
        Path target = getFile("temp/" + photoId,directory);
        try (Stream<Path> files = Files.walk(target)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }


    //图片上传
    //folderName          文件夹目录
    //directory           图片路径
    //extension           图片真实扩展名
    //imageBytes          前端传过来的图片字节
    public static String filesCopy(String folderName,String directory,String extension,byte[] imageBytes) throws IOException {
        Path uploadFolder = getFile(folderName,directory);
        Files.createDirectories(uploadFolder);
        String filename = getImageFileName(imageBytes,extension);
        Files.write(uploadFolder.resolve(filename),imageBytes);

        return filename;
    }

    //按照系统原有的时间戳加图片内容哈希规则生成文件名
    public static String getImageFileName(byte[] imageBytes,String extension) throws IOException {
        try {
            //当扩展名有值但是没有点号时，在文件名和扩展名中间补上点号
            if(extension.length()>0 && !extension.startsWith(".")){
                extension = "." + extension;
            }
            return new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + sha256(imageBytes) + extension;
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("无法生成图片文件名",e);
        }
    }

    //原子移动目录，不支持原子移动时使用普通移动
    public static void moveDirectory(Path source,Path target) throws IOException {
        try {
            Files.move(source,target,StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source,target);
        }
    }

    //递归删除目录及目录内全部文件
    public static void deleteDirectory(Path path) throws IOException {
        if(!Files.exists(path)){
            return;
        }
        try (Stream<Path> files = Files.walk(path)) {
            files.sorted(Comparator.reverseOrder()).forEach(file -> {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }

    //计算字节内容的SHA-256
    public static String sha256(byte[] bytes) throws NoSuchAlgorithmException {
        StringBuilder hash = new StringBuilder();
        for(byte value : MessageDigest.getInstance("SHA-256").digest(bytes)){
            hash.append(String.format("%02x",value));
        }
        return hash.toString();
    }

    //读取图片字节的真实格式，返回保存文件时应该使用的扩展名
    public static String getImageExtension(byte[] imageBytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {

            //当字节内容不能作为图片输入流打开时，不返回扩展名
            if(input==null){
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

            //当系统找不到能读取这些字节的图片解码器时，不返回扩展名
            if(!readers.hasNext()){
                return null;
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                String extension = null;

                //当图片真实格式是JPEG时，统一使用jpg扩展名
                if("jpeg".equalsIgnoreCase(format) || "jpg".equalsIgnoreCase(format)){
                    extension = "jpg";

                //当图片真实格式是PNG时，使用png扩展名
                }else if("png".equalsIgnoreCase(format)){
                    extension = "png";
                }

                //当真实格式不是支持的图片格式时，不返回扩展名
                if(extension==null){
                    return null;
                }
                reader.setInput(input,true,true);

                //当图片能够完整读取并且宽高有效时，返回真实扩展名
                if(reader.getWidth(0)>0 && reader.getHeight(0)>0 && reader.read(0)!=null){
                    return extension;
                }
                return null;
            } finally {
                reader.dispose();
            }
        } catch (Exception e) {
            return null;
        }
    }

    //图片删除
    public static void deleteImage(String picUrl,String directory) {

        //当图片地址为空时，不执行删除
        if(picUrl==null){
            return;
        }
        try {
            String imagePath = new URI(picUrl.trim().replace("\\","/")).getPath();

            //当地址没有文件夹和文件名时，不执行删除
            if(imagePath==null || imagePath.endsWith("/")){
                return;
            }
            String[] paths = imagePath.split("/");

            //当地址中拆不出文件夹和文件名时，不执行删除
            if(paths.length<2){
                return;
            }
            deleteFile(paths[paths.length-2] + "/" + paths[paths.length-1],directory);
        } catch (Exception ignored) {
        }

    }
    //把已经读取的图片字节作为文件发送给图片接口，并保留原文件名
    public static class MultipartByteArrayResource extends ByteArrayResource {
        private final String filename;

        public MultipartByteArrayResource(byte[] imageBytes,String filename) {
            super(imageBytes);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return this.filename;
        }
    }


}
