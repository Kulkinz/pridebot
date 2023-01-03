import asyncio

import discord
from apscheduler.schedulers.asyncio import AsyncIOScheduler

class MyClient(discord.Client):
    async def on_ready(self):
        print(f'Logged on as {self.user}!')

    async def on_message(self, message):
        print(f'Message from {message.author}: {message.content} in {message.guild.id}')

async def pause(guild: discord.Guild, state: bool):
    if guild is None:
        print("Guild isn't accessible")
        return
    if any('COMMUNITY' in feat for feat in guild.features):
        await guild.edit(invites_disabled=state)
        if state:
            print("Invites have been paused")
        else:
            print("Invites have been resumed")
    else:
        print(f"Guild-{guild} does not have the invites_disabled feature")

async def scheduler():
    print("Setting up schedules")

    guild = await client.fetch_guild(425348984723210261)

    scheduler = AsyncIOScheduler()
    # scheduler.add_job(pause, 'interval', seconds=20, args=[guild, True])
    scheduler.add_job(pause, 'cron', hour=23, args=[guild, True])
    scheduler.add_job(pause, 'cron', hour=8, args=[guild, False])

    print("Start scheduler")

    scheduler.start()

async def bot():
    print("Setting up client")

    intents = discord.Intents.default()
    intents.message_content = True

    global client
    client = MyClient(intents=intents)

    print("Start client")

    token = open("token", "r")
    await client.start(token.read())

async def setup():
    await asyncio.gather(bot(), scheduler())

asyncio.run(setup())
