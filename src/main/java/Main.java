import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.Deflater;
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
           System.out.print(content.substring(content.indexOf("\0")+1));
         } catch (Exception e) {
           throw new RuntimeException(e);
         }
       }
       case "hash-object" -> {
        final String fileName = args[2];
        try{
          Deflater compress = new Deflater();
          byte[] output = new byte[100];
          byte[] fileContent = Files.readAllBytes(Paths.get(fileName));
          compress.setInput(fileContent);
          compress.finish();
          final Integer compressedLength = compress.deflate(output);
          compress.end();

          String blobObject = "blob "+compressedLength+"\0"+new String(fileContent);
          String blobHash = Integer.toHexString(blobObject.hashCode());
          System.out.print(blobHash);
          if(args[1].equals("-w")){
            final String objectFolder = blobHash.substring(0,2);
            final String objectFilename = blobHash.substring(2);
            final File  objects = new File(".git/objects/"+objectFolder);
            objects.mkdirs();
            final File objectFile = new File(objects, objectFilename);
            objectFile.createNewFile();
            Files.write(objectFile.toPath(), output);
          }

        }catch (Exception e){
          throw new RuntimeException();
        }
       }
       default -> System.out.println("Unknown command: " + command);
     }
  }
}
