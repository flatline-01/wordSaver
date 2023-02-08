# WordSaver
A java program for saving unknown English words (actually the language is not important) to a file, so you can translate them later. 

## Libraries
- [jnativehook](https://github.com/kwhat/jnativehook)
- [picocli](https://github.com/remkop/picocli/blob/main/picocli-shell-jline3/README.md)

## Using Guide

1. Create a command for Linux by adding the line below to the ~/.bashrc file:
> alias wsaver='java -jar "patch to the downloaded jar file"'
2. Run the command **wsaver** in terminal. By default, words are saved into words.txt file in the Documents directory.
3. To add a new word to the file, select it and press the left Shift key.
4. To change the file path run the command below: 
> $ wsaver -f "file path"
5. To change the key run:
> $ wsaver -k keyCode