package com.mycompany.app;

import picocli.CommandLine;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.CommandSpec;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

@Command(name = "wsaver",
         mixinStandardHelpOptions = true,
         description = "Saves the selected words to a file after pressing a key.",
         subcommands = { WordSaver.StartService.class, WordSaver.ListKeys.class },
         version = "2.0.0")
public class WordSaver {
    private static final String CLASS_NAME = WordSaver.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private static final Map<String, Integer> AVAILABLE_KEY_CODES = new HashMap<>();
    @Spec CommandSpec spec;

    static {
        AVAILABLE_KEY_CODES.put("B", 48);
        AVAILABLE_KEY_CODES.put("F", 33);
        AVAILABLE_KEY_CODES.put("I", 23);
        AVAILABLE_KEY_CODES.put("L", 38);
        AVAILABLE_KEY_CODES.put("M", 50);
        AVAILABLE_KEY_CODES.put("Q", 16);
        AVAILABLE_KEY_CODES.put("X", 45);
        AVAILABLE_KEY_CODES.put("Y", 21);
    }

    public static void main(String[] args) {
        new CommandLine(new WordSaver()).execute(args);
    }
   
    @Command(name = "start", description = "Starts the background service to save words.")
    public static class StartService implements Runnable {
        private static final String DEFAULT_FILE_PATH = System.getProperty("user.home") + "\\Documents\\words.txt";

        @Option(names = {"-f", "--file"}, 
                description = "Path to file where the words will be saved. The defauld file path is Documents/words.txt")
        private String filePath = DEFAULT_FILE_PATH;
        
        @Option(names = {"-k", "--keycode"}, 
                description = "The key after pressing it saves the selected word to a file. The default key is 'B'.",
                converter = KeyCodeConverter.class)
        private int keyCode = AVAILABLE_KEY_CODES.get("B");
 
        @Override
        public void run() {
            System.out.println("Starting word saver service. Pressing key " + getKeyByValue(keyCode) + " will save to " + filePath);
            startKeyListener();   
        }

        private void startKeyListener() {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Clipboard clipboard = null;

            if (System.getProperty("os.name").contains("Windows")) {
                clipboard = toolkit.getSystemClipboard();
            }
            else {
                clipboard = toolkit.getSystemSelection();
            }

            MainKeyListener mkl = new MainKeyListener(clipboard, filePath, keyCode);

            try {
                GlobalScreen.registerNativeHook();
                GlobalScreen.addNativeKeyListener(mkl);
            
                System.out.println("Press Ctrl+C to stop the listener.");
            } 
            catch (NativeHookException e) {
                LOGGER.log(Level.SEVERE, "Error registering native hook", e);
            } 
        }

        private class MainKeyListener implements NativeKeyListener{
            private final Clipboard clipboard;
            private final String filePath;
            private final int keyCode;

            public MainKeyListener(Clipboard clipboard, String filePath, int keyCode) {
                this.clipboard = clipboard;
                this.filePath = filePath;
                this.keyCode = keyCode;
            }

            @Override
            public void nativeKeyPressed(NativeKeyEvent nativeEvent) {
                if(nativeEvent.getKeyCode() == keyCode){

                    if (System.getProperty("os.name").contains("Windows")) {
                        simulateCopying();
                    }
                    
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true))) {
                        String text = (String) clipboard.getData(DataFlavor.stringFlavor);
                        if (text != null && text != " ") {
                            bw.write(text + System.lineSeparator()); //Save the selected text to a file, split by the standard system delimiter (e.g. ‘\n’).
                        } 
                    }
                    catch (UnsupportedFlavorException | IOException e) {
                        LOGGER.log(Level.SEVERE, "Error while saving word", e);
                    }
                }
            }

            private void simulateCopying() {
                try {
                    Robot robot = new Robot();
                    robot.keyPress(KeyEvent.VK_CONTROL);
                    robot.keyPress(KeyEvent.VK_C);
                    robot.delay(100);
                    robot.keyRelease(KeyEvent.VK_C);
                    robot.keyRelease(KeyEvent.VK_CONTROL);
                    Thread.sleep(150);
                } 
                catch (AWTException | InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "Error while simulate copying", e);
                }
            }
        }
    }

    @Command(name = "lk", description = "List all available key codes.")
    public static class ListKeys implements Runnable {
        @Override
        public void run() {
            System.out.println("You can use the following key codes:\n");
            AVAILABLE_KEY_CODES.forEach((k, v) -> System.out.println(k + ": " + v));
        }
    }   

    public static String getKeyByValue(int value) {
        for (Map.Entry<String, Integer> entry : AVAILABLE_KEY_CODES.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static class KeyCodeConverter implements CommandLine.ITypeConverter<Integer> {
        @Override
        public Integer convert(String value) {
            int code = Integer.parseInt(value);
            if (!AVAILABLE_KEY_CODES.values().contains(code)) {
                String allowed = AVAILABLE_KEY_CODES.values().stream().map(Object::toString).collect(Collectors.joining(", "));
                throw new CommandLine.TypeConversionException("Invalid key code: " + code + ". Allowed: " + allowed + ".");
            }
            return code;
        }
    }
}