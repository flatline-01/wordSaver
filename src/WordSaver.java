import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import picocli.CommandLine;
import picocli.CommandLine.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.*;
import java.util.logging.Logger;

@Command(name = "wsaver",
         addMethodSubcommands = false,
         mixinStandardHelpOptions = true,
         helpCommand = true,
         description = "Saves the selected words to a file after pressing a key.")
public class WordSaver implements Runnable{
    private static final String CLASS_NAME = WordSaver.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);
    @Option(names = {"-f", "--file"}, description = "Path to file where the words will be saved.")
    private static String fileName;
    @Option(names = {"-k", "--keycode"}, description = "The key after pressing it saves the selected word to a file.")
    private static int keyCode = 42;

    static {
        try {
            String username = getSystemUsername();
            if(username == null){
                throw new IOException("Username of the system is null.");
            }
            fileName = "/home/" + username + "/Documents/words.txt";
        } catch (IOException e) {
            LOGGER.throwing(CLASS_NAME, "static block", e);
        }
    }

    public static void main(String[] args) {
        new CommandLine(new WordSaver()).execute(args);
    }

    private static String getSystemUsername() throws IOException {
        String username;
        Process process = Runtime.getRuntime().exec("whoami");
        try(InputStreamReader isr = new InputStreamReader(process.getInputStream());
            BufferedReader br = new BufferedReader(isr)){
            username = br.readLine();
        }
        return username;
    }

    @Override
    public void run() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Clipboard clipboard = toolkit.getSystemSelection();
        MainKeyListener mkl = new MainKeyListener(clipboard);
        try{
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            LOGGER.throwing(CLASS_NAME, "run", e);
        }
        GlobalScreen.addNativeKeyListener(mkl);
    }

    private static class MainKeyListener implements NativeKeyListener{
        private final Clipboard clipboard;
        public MainKeyListener(Clipboard clipboard){
            this.clipboard = clipboard;
        }

        @Override
        public void nativeKeyPressed(NativeKeyEvent nativeEvent) {
            if(nativeEvent.getKeyCode() == keyCode){
                try(FileWriter fw = new FileWriter(fileName, true);
                    BufferedWriter bw = new BufferedWriter(fw)){
                    String word = (String) clipboard.getData(DataFlavor.stringFlavor);
                    bw.append(word).append("\n");
                } catch (UnsupportedFlavorException | IOException e) {
                    LOGGER.throwing(CLASS_NAME, "nativeKeyPressed", e);
                }
            }
        }
    }
}
