import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.Inflater;
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
       }
       case "cat-file" ->{
         final String objectHash = args[2];
         final String objectFolder = objectHash.substring(0,2);
         final String fileName = objectHash.substring(2);
         try{
           Inflater in = new Inflater();
           byte[] fileContent =  Files.readAllBytes(Paths.get(".git/objects/" + objectFolder + "/" + fileName));
           in.setInput(fileContent);
           OutputStream out = new ByteArrayOutputStream();
           byte[] buffer = new byte[1024];
           int length;
           while((length = in.inflate(buffer)) > 0){
             out.write(buffer,0,length);
           }
           String content = out.toString();
           System.out.println(content.substring(content.indexOf("\0")+1));
         } catch (Exception e) {
           throw new RuntimeException(e);
         }
       }
       default -> System.out.println("Unknown command: " + command);
     }
  }
}
