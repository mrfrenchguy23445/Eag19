package net.minecraft.world.entity.schedule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import java.util.Collection;
import java.util.List;

public class Timeline {
   private final List<Keyframe> keyframes = Lists.newArrayList();
   private int previousIndex;

   public ImmutableList<Keyframe> getKeyframes() {
      return ImmutableList.copyOf(this.keyframes);
   }

   public Timeline addKeyframe(int param0, float param1) {
      this.keyframes.add(new Keyframe(param0, param1));
      this.sortAndDeduplicateKeyframes();
      return this;
   }

   public Timeline addKeyframes(Collection<Keyframe> param0) {
      this.keyframes.addAll(param0);
      this.sortAndDeduplicateKeyframes();
      return this;
   }

   private void sortAndDeduplicateKeyframes() {
      Int2ObjectSortedMap<Keyframe> var1 = new Int2ObjectAVLTreeMap<>();
      this.keyframes.forEach(l_param1 -> var1.put(l_param1.getTimeStamp(), l_param1));
      this.keyframes.clear();
      this.keyframes.addAll(var1.values());
      this.previousIndex = 0;
   }

   public float getValueAt(int param0) {
      if (this.keyframes.size() <= 0) {
         return 0.0F;
      }

      Keyframe var2 = this.keyframes.get(this.previousIndex);
      Keyframe var3 = this.keyframes.get(this.keyframes.size() - 1);
      boolean var4 = param0 < var2.getTimeStamp();
      int var5 = var4 ? 0 : this.previousIndex;
      float var6 = var4 ? var3.getValue() : var2.getValue();

      for (int var7 = var5; var7 < this.keyframes.size(); var7++) {
         Keyframe var8 = this.keyframes.get(var7);
         if (var8.getTimeStamp() > param0) {
            break;
         }

         this.previousIndex = var7;
         var6 = var8.getValue();
      }

      return var6;
   }
}
