package com.sk89q.minecraft.util.commands;

public class CommandNumberFormatException extends CommandException {

    private final String actualText;

    public CommandNumberFormatException(String actualText) {
        super("Number expected in place of '" + actualText + "'");
        this.actualText = actualText;
    }

    public String getActualText() {
        return actualText;
    }
}
