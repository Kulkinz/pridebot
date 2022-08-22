package com.prideubc.pridebot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;

public class Bot extends ListenerAdapter {

    public static String dataLocation;
    public static void main(String[] args) throws LoginException {
        if (args.length < 1) {
            System.out.println("You have to provide a bot token as the first argument!");
            System.exit(1);
        }

        if (args.length < 2) {
            System.out.println("You have to provide a data location as the second argument!");
            System.exit(1);
        }

        dataLocation = args[1];

        File test = new File(dataLocation + "/dataWritingTest.txt");
        try {
            FileWriter myWriter = new FileWriter(test);
            myWriter.write("If this appears with the date of last boot: " + Timestamp.from(Instant.now()) + " That means the data spot is set up here.");
            myWriter.close();
            System.out.println("Successfully able to write data.");
        } catch (IOException e) {
            System.out.println("An error occurred. Unable to write to given data location.");
            e.printStackTrace();
            System.exit(1);
        }

        // Taken from example
        JDA jda = JDABuilder.createLight(args[0], Collections.emptyList())
                .addEventListeners(new Bot())
                .setActivity(Activity.competing("You can really put anything here huh"))
                .build();

        System.out.println("Bot is running");

        CommandListUpdateAction commands = jda.updateCommands();

        commands.addCommands(
                Commands.slash("ping", "Calculate ping of the bot.")
        );

        commands.addCommands(
                Commands.slash("roll", "Rolls a specified dice. Can string together multiple roles with +")
                        .addOptions(new OptionData(OptionType.STRING, "dice", "The dice to roll")
                                .setRequired(true))
                        .addOptions(new OptionData(OptionType.BOOLEAN, "show", "Show to others the roll?"))
        );

        commands.addCommands(
                Commands.slash("leave", "Make this bot leave the server")
                        .setGuildOnly(true)
                        .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
        );

        commands.addCommands(
                Commands.slash("prune", "Prune messages from this channel")
                        .addOption(OptionType.INTEGER, "amount", "how many messages to prune (Default 100)")
                        .setGuildOnly(true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))
        );

        commands.queue();


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
            case "roll":
                roll(event, event.getOption("dice", "d20", OptionMapping::getAsString));
                break;
            case "leave":
                leave(event);
                break;
            case "prune":
                prune(event);
                break;
            default:
                event.reply("I can't handle that command right now :(").setEphemeral(true).queue();
                break;
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        String[] id = event.getComponentId().split(":"); // this is the custom id we specified in our button
        String authorId = id[0];
        String type = id[1];
        // Check that the button is for the user that clicked it, otherwise just ignore the event (let interaction fail)
        if (!authorId.equals(event.getUser().getId()))
            return;
        event.deferEdit().queue(); // acknowledge the button was clicked, otherwise the interaction will fail

        MessageChannel channel = event.getChannel();
        switch (type) {
            case "prune":
                int amount = Integer.parseInt(id[2]);
                event.getChannel().getIterableHistory()
                        .skipTo(event.getMessageIdLong())
                        .takeAsync(amount)
                        .thenAccept(channel::purgeMessages);
                // fallthrough delete the prompt message with our buttons
            case "delete":
                event.getHook().deleteOriginal().queue();
        }
    }

    private void roll(SlashCommandInteractionEvent event, String dice) {

        dice = dice.toLowerCase().trim();

        if (!dice.matches("^(\\d*d\\d+)((\\+|-)((\\d*d\\d+)|\\d+))*$")) {
            event.reply("Unable to parse roll. You put: [" + dice + "]." +
                            "\n Common mistakes include:" +
                            "\n - Forgetting the d before the number ([d20]-correct vs [20]-incorrect)" +
                            "\n - Putting a negative after an addition ([d20-1]-correct vs [d20+-1]-incorrect")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        boolean show = event.getOption("show", true, OptionMapping::getAsBoolean);
        String string;
        event.reply((string = event.getMember().getNickname() + " requested [" + dice + "]") + "...")
                .setEphemeral(!show)
                .queue();

        new java.util.Random();
        // since already verified the dice is parceable.

        dice = dice.replaceAll("-", "+-");

        String[] split = dice.split("\\+");

        int total = 0;

        StringBuilder stringBuilder = new StringBuilder(string);

        stringBuilder.append(". Result: ");

        for (String roll: split) {

            if (roll.contains("d")) {

                stringBuilder.append("[");

                int sign = 1;
                if (roll.charAt(0) == '-') {
                    sign = -1;
                    roll = roll.substring(1);
                }

                String[] specifics = roll.split("d");
                int numTimes = (specifics[0].matches("^\\d+$")) ? Integer.parseInt(specifics[0]) : 1;
                int sides = Integer.parseInt(specifics[1]);

                // extra checks

                if (numTimes > 200) {
                    event.getHook().editOriginal("Unable to roll " + numTimes + " dice. Limit is 200").queue();
                    return;
                }

                for (int i = 0; i < numTimes; i++) {
                    int number = (int) Math.floor(Math.random() * sides) + 1;
                    total += sign * number;

                    if (i > 0) {
                        stringBuilder.append(",");
                    }

                    stringBuilder.append(number);

                }

                stringBuilder.append("]");

            } else {
                total += Integer.parseInt(roll);
            }
        }

        stringBuilder.append(". Total: ").append(total);

        string = stringBuilder.toString();

        event.getHook().editOriginal(string).queue();
    }

    private boolean verifyRoll(String dice) {
        return true;
    }

    private void leave(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.KICK_MEMBERS)) {
            event.reply("You do not have permissions to kick me.").setEphemeral(true).queue();
        } else {
            event.reply("Leaving the server... :wave:")
                    .flatMap(v -> event.getGuild().leave())
                    .queue();
        }
    }

    private void prune(SlashCommandInteractionEvent event) { // taken from example
        OptionMapping amountOption = event.getOption("amount"); // This is configured to be optional so check for null
        int amount = amountOption == null
                ? 100 // default 100
                : (int) Math.min(200, Math.max(2, amountOption.getAsLong())); // enforcement: must be between 2-200
        String userId = event.getUser().getId();
        event.reply("This will delete " + amount + " messages.\nAre you sure?") // prompt the user with a button menu
                .addActionRow(// this means "<style>(<id>, <label>)", you can encode anything you want in the id (up to 100 characters)
                        Button.secondary(userId + ":delete", "Nevermind!"),
                        Button.danger(userId + ":prune:" + amount, "Yes!")) // the first parameter is the component id we use in onButtonInteraction above
                .queue();
    }

}