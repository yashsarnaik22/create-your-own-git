import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.zip.*;

public class Main {
  public static void main(String[] args){
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.err.println("Logs from your program will appear here!");

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
         try{
           File objectFile = getFileFromObjectHash(objectHash);
           String fileContent = decompressObject(objectFile);
           System.out.print(fileContent.substring(fileContent.indexOf("\0")+1));
         } catch (Exception e) {
           throw new RuntimeException(e);
         }
       }
       case "hash-object" -> {
        final String fileName = args[2];
        try{
          byte[] fileContent = Files.readAllBytes(Paths.get(fileName));
          String header = "blob "+fileContent.length+"\0";
          byte[] blobObject = concatenate(header.getBytes(StandardCharsets.UTF_8), fileContent);
          String blobHash = computeSHA1(blobObject);
          System.out.print(blobHash);

          if(args[1].length() >1 && args[1].equals("-w")){
            final String objectFolder = blobHash.substring(0,2);
            final String objectFilename = blobHash.substring(2);
            final File dir = new File(".git/objects/"+objectFolder);
            dir.mkdirs();
           try( FileOutputStream fos = new FileOutputStream(new File(dir, objectFilename));
             DeflaterOutputStream dos = new DeflaterOutputStream(fos)) {
             dos.write(blobObject);
           }
          }

        }catch (Exception e){
          e.printStackTrace();
          throw new RuntimeException();
        }
       }
       case "write-tree" -> {
         File rootDir = new File(".");
            String treeHash = writeTree(rootDir);
            System.out.print(treeHash);
       }
       case "ls-tree" -> {
         final File treeObjectFile= getFileFromObjectHash(args[2]);
         final String content = decompressObject(treeObjectFile);
//         System.out.println(content);
         String[] entries = content.split("\0");
         for(int i =0 ; i<entries.length; i++){
//           System.out.println(entries[i]);
           String[] permissionModeAndName = entries[i].split(" ");
           if(permissionModeAndName.length < 2) break;

           String filename = entries[i].split(" ")[1];
           System.out.println(filename);
         }

       }
       case "commit-tree" ->{
         String commitHash = commitTree(args);
         System.out.println(commitHash);
       }
       default -> System.out.println("Unknown command: " + command);
     }
  }

  private static String commitTree(String[] args){

    String treeHash = args[1];
    List<String> parents = new ArrayList<>();
    for(int i =2 ; i< args.length; i++){
      if(args[i].equals("-p")){
        parents.add(args[i+1]);
      }
    }
    final String authorAndCommitterName = "Yash Sarnaik"; //hardcoded
    final String email = "yash123@gmail.com";

    long timestamp = System.currentTimeMillis()/1000;
    String parentCommits = String.join(" ", parents);
    String msg = args[args.length -1];

    StringBuilder commitContent = new StringBuilder();
    commitContent.append("tree ").append(treeHash).append("\n");
    commitContent.append("parent ").append(parentCommits).append("\n");
    commitContent.append("author ").append(authorAndCommitterName).append(" <").append(email).append("> ").append(timestamp).append("\n");
    commitContent.append("committer ").append(authorAndCommitterName).append(" <").append(email).append("> ").append(timestamp).append("\n\n");
    commitContent.append(msg).append("\n");

    byte[] commitBytes = commitContent.toString().getBytes(StandardCharsets.UTF_8);
    String commitHeader = "commit " + commitBytes.length + "\0";
    byte[] commitObject = concatenate(commitHeader.getBytes(StandardCharsets.UTF_8), commitBytes);
    String commitHash = computeSHA1(commitObject);

    writeGitObject(commitHash, commitObject);
    return commitHash;


  }
  private static File getFileFromObjectHash(String objectHash){
    final String objectFolder = objectHash.substring(0,2);
    final String fileName = objectHash.substring(2);
    return new File(".git/objects/" + objectFolder + "/" + fileName);
  }

  private static String decompressObject (File objectFile){
    try{
      Inflater in = new Inflater();
      byte[] fileContent =  Files.readAllBytes(objectFile.toPath());
      in.setInput(fileContent);
      OutputStream out = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int length;
      while((length = in.inflate(buffer)) > 0){
        out.write(buffer,0,length);
      }

        return out.toString();
    }catch (IOException | DataFormatException e){
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private static String writeTree(File dir) {
    File[] files = dir.listFiles((d, name) -> !name.equals(".git"));
    if(files == null) return null;

    Arrays.sort(files, Comparator.comparing(File::getName));
    ByteArrayOutputStream treeContent = new ByteArrayOutputStream();
    try {
      for(File file : files){
            String mode;
            String sha1;
            String name = file.getName();
            if (file.isDirectory()) {
              mode = "40000";
              sha1 = writeTree(file);
            } else {
                byte[] content = Files.readAllBytes(file.toPath());
                String header = "blob " + content.length + "\0";
                byte[] blob = concatenate(header.getBytes(StandardCharsets.UTF_8), content);
                sha1 = computeSHA1(blob);

                String dirName = ".git/objects/" + sha1.substring(0, 2);
                String fileName = sha1.substring(2);

                File objFile = new File(dirName, fileName);
                if (!objFile.exists()) {
                  new File(dirName).mkdirs();
                  FileOutputStream fo = new FileOutputStream(objFile);
                  DeflaterOutputStream dos = new DeflaterOutputStream(fo);
                  dos.write(blob);
              }
              mode = "100644";
            }
            treeContent.write((mode + " " + name + "\0").getBytes(StandardCharsets.UTF_8));
            treeContent.write(hexStringToByteArray(sha1));

      }
      byte[] treeBytes = treeContent.toByteArray();
      String treeHeader = "tree " + treeBytes.length + "\0";
      byte[] treeObject = concatenate(treeHeader.getBytes(StandardCharsets.UTF_8), treeBytes);
      String treeSha = computeSHA1(treeObject);

      String treeDir = ".git/objects/" + treeSha.substring(0,2);
      String treeFile = treeSha.substring(2);
      new File(treeDir).mkdirs();

     try( FileOutputStream fo = new FileOutputStream(new File(treeDir, treeFile));
        DeflaterOutputStream dos = new DeflaterOutputStream(fo)) {
        dos.write(treeObject);
     }
      return treeSha;

    }catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private static String computeSHA1(byte[] data) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] hash = md.digest(data);
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        hexString.append(String.format("%02x", b));
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-1 algorithm not found", e);
    }
  }

  private static byte[] hexStringToByteArray(String s) {
    byte[] bytes = new byte[s.length()/2 ];
    for(int i = 0 ; i<s.length(); i+=2){
        bytes[i/2] = ((byte) Integer.parseInt(s.substring(i, i+2), 16));
    }
    return bytes;
  }

  private static byte[] concatenate(byte[] a , byte[] b){
    byte[] result = new byte[a.length + b.length];
    System.arraycopy(a,0,result,0,a.length);
    System.arraycopy(b,0,result,a.length,b.length);
    return result;
  }

  private static void writeGitObject(String treeHash, byte[] treeObject) {
    File objectDir =  new File(".git/objects/" + treeHash.substring(0, 2));
    if (!objectDir.exists())
      objectDir.mkdirs();
    File objectFile = new File(objectDir, treeHash.substring(2));
    if (!objectFile.exists()) {
      //            System.out.println("creating file");
      try (FileOutputStream fileOutputStream =  new FileOutputStream(objectFile);
           BufferedOutputStream outputStream = new BufferedOutputStream(fileOutputStream);
           DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(outputStream)) {
        deflaterOutputStream.write(treeObject);
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
