package net.minecraft.sounds;

public enum SoundSource {
   MASTER("master"),
   MUSIC("music"),
   RECORDS("record"),
   WEATHER("weather"),
   BLOCKS("block"),
   HOSTILE("hostile"),
   NEUTRAL("neutral"),
   PLAYERS("player"),
   AMBIENT("ambient"),
   VOICE("voice");

   private final String name;

   SoundSource(String param2) {
      this.name = param2;
   }

   public String getName() {
      return this.name;
   }
}
