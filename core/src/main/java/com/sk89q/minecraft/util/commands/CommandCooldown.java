package com.sk89q.minecraft.util.commands;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates a usage cooldown that should be enforced.
 *
 * @author Isaac Moore
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandCooldown {
    /**
     * The time (in seconds) of the cooldown. Should be a positive integer greater than 0. Can be a decimal.
     */
    double time();

    /**
     * The message to be displayed to the user on an unsuccessful command execution.<br/><b>Important:</b> Use <b>@s</b>
     * where you would like the number of seconds remaining in the cooldown to be displayed.<br/>Default message is
     * <b>You must wait @s more seconds to use that command again.</b>
     */
    String message() default "You must wait @s more seconds to use that command again.";


    /**
     * Whether or not to add to the cooldown time on an unsuccessful command execution. Defaults to false.
     * TODO: Finish documentation
     */
    boolean addTimeOnAttempt() default false;
}
