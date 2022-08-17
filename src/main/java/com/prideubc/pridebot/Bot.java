package com.prideubc.pridebot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.util.Collections;

public class Bot extends ListenerAdapter {
    public static void main(String[] args) throws LoginException {
        if (args.length < 1) {
            System.out.println("You have to provide a bot token as the first argument!");
            System.exit(1);
        }

        // Taken from example
        JDA jda = JDABuilder.createLight(args[0], Collections.emptyList())
                .addEventListeners(new Bot())
                .setActivity(Activity.playing("Activation successful"))
                .build();

        System.out.println("Bot is running");

        jda.upsertCommand("ping", "Calculate ping of the bot").queue(); // This can take an hour to appear
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping":
                long time = System.currentTimeMillis();
                event.reply("Pong!").setEphemeral(true) // shows to only user, acknowledge
                        .flatMap(v ->
                                event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time) //then edit original
                        ).queue(); // Queue both reply and edit
                break;
            default:
                event.reply("I can't handle that command right now :(").setEphemeral(true).queue();
                break;
        }
    }

}