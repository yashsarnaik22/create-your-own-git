import java.io.*;
import java.nio.file.Files;
import java.util.zip.InflaterInputStream;

public class Main {
  public static void main(String[] args){
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.err.println("Logs from your program will appear here!");

//     Uncomment this block to pass the first stage

     final String command = args[0];

     switch (command) {
       case "init" -> {
         final File root = new File(".git");
         final File objects = new File(root, "objects");
         objects.mkdirs();
         new File(root, "refs").mkdirs();
         final File head = new File(root, "HEAD");

         try {
           head.createNewFile();
           Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
           System.out.println("Initialized git directory");
         } catch (IOException e) {
           throw new RuntimeException(e);
         }
         System.out.println("Files in .git/objects directory :" + objects.listFiles());
         File[] filesinobjects = objects.listFiles();
         if(filesinobjects != null ){
           for(File f : filesinobjects){
             if(f.isDirectory() && f.listFiles() != null){
               for(File f1 : f.listFiles()){
                 File content = new File("content.txt");
                 try(InputStream in = new InflaterInputStream(new FileInputStream(f1))){
                   OutputStream out = new FileOutputStream(content);
                   byte[] buffer = new byte[1024];
                   int length;
                   while((length = in.read(buffer)) > 0){
                     out.write(buffer,0,length);
                   }
                 } catch (Exception e) {
                   throw new RuntimeException(e);
                 }
                 try{
                   System.out.println(Files.readString(content.toPath()));
                 }catch (Exception e){
                     throw new RuntimeException(e);
                 }
               }
             }
           }
         }


       }
       default -> System.out.println("Unknown command: " + command);
     }

  }
}
