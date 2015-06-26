package jnlp;


    /*
     * Taken from Oracle documentation.
     *
     */

    // add javaws.jar to the classpath during compilation
    import javax.jnlp.FileOpenService;
    import javax.jnlp.FileSaveService;
    import javax.jnlp.FileContents;
    import javax.jnlp.ServiceManager;
    import javax.jnlp.UnavailableServiceException;
    import java.io.*;

    public class JNLPFileHandler {

        static private FileOpenService fos = null;
        static private FileSaveService fss = null;
//        static private FileContents fc = null;

        // retrieves a reference to the JNLP services
        private static synchronized void initialize() {
            if (fss != null) {
                return;
            }
            try {
                fos = (FileOpenService) ServiceManager.lookup("javax.jnlp.FileOpenService");
                fss = (FileSaveService) ServiceManager.lookup("javax.jnlp.FileSaveService");
            } catch (UnavailableServiceException e) {
                System.out.println("ERROR UnavailableServiceException:" + e.getMessage());
                fos = null;
                fss = null;
            }
        }

        public static FileContents getReadFile(String pathHint, String[] extensions, int minLength, int maxLength)
                throws IOException {
            initialize();
            FileContents fc = fos.openFileDialog(pathHint, extensions);
            if (fc != null) {
                long len = fc.getLength();
                if (((minLength > 0) && (len < minLength)) || ((maxLength > 0) &&(len > maxLength) ))
                    fc = null;
            }
            return fc;
        }

        public static FileContents getReadFile(String[] extensions)
                throws IOException {
            return getReadFile(null, extensions, 0, 0);
        }

        public static String readFile(FileContents fc, int len) throws IOException {
            if (fc == null) {
                return null;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(fc.getInputStream()));
            char[] charArr;
            if (len > 0) {
                charArr = new char[len + 10];
                br.read(charArr, 0, len);
            }
            else {
                charArr = new char[(int) fc.getLength()];
                br.read(charArr);
            }
            br.close();
            return new String(charArr);
        }

        public static String readFile(FileContents fc) throws IOException {
            return readFile(fc, 0);    // read all
        }

        public static boolean saveToFile(String txt, String extension) {
            boolean retVal = false;
            try {
                FileContents fc = fss.saveFileDialog(null, new String[]{extension},
                        new ByteArrayInputStream(txt.getBytes()), "@." + extension);
                if (fc != null) {
////                    if (extension != null) {
////
////                    }
//                        writeToFile(txt, fc);
                     retVal = true;
                }
            } catch (IOException ioe) {
                ioe.printStackTrace(System.out);
            }
            return retVal;
        }

        public static boolean saveToFile(byte[] bytes, String extension) {
            boolean retVal = false;
            try {
                FileContents fc = fss.saveFileDialog(null, new String[]{extension},
                        new ByteArrayInputStream(bytes), "@." + extension);
                if (fc != null) {
////                    if (extension != null) {
////
////                    }
//                        writeToFile(txt, fc);
                     retVal = true;
                }
            } catch (IOException ioe) {
                ioe.printStackTrace(System.out);
            }
            return retVal;
        }

        // displays saveFileDialog and saves file using FileSaveService
/*
        public static void save(String txt) {
            initialize();
            try {
                // Show save dialog if no name is already given
                FileContents fc = null;
                if (fc == null) {
                    fc = fss.saveFileDialog(null, null,
                            new ByteArrayInputStream(txt.getBytes()), null);
                    // file saved, done
                    return;
                }
                // use this only when filename is known
                if (fc != null) {
                    writeToFile(txt, fc);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace(System.out);
            }
        }
*/

        private static void writeToFile(String txt, FileContents fc) throws IOException {
            int sizeNeeded = txt.length() * 2;
            if (sizeNeeded > fc.getMaxLength()) {
                fc.setMaxLength(sizeNeeded);
            }
            BufferedWriter os = new BufferedWriter(new OutputStreamWriter(fc.getOutputStream(true)));
            os.write(txt);
            os.close();
        }

        private static String readFromFile(FileContents fc) throws IOException {
            if (fc == null) {
                return null;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(fc.getInputStream()));
            StringBuffer sb = new StringBuffer((int) fc.getLength());
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            br.close();
            return sb.toString();
        }
    }


