package net.minecraft.world.level.pathfinder;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class Node {
   public final int x;
   public final int y;
   public final int z;
   private final int hash;
   public int heapIdx = -1;
   public float g;
   public float h;
   public float f;
   @Nullable
   public Node cameFrom;
   public boolean closed;
   public float walkedDistance;
   public float costMalus;
   public BlockPathTypes type = BlockPathTypes.BLOCKED;

   public Node(int param0, int param1, int param2) {
      this.x = param0;
      this.y = param1;
      this.z = param2;
      this.hash = createHash(param0, param1, param2);
   }

   public Node cloneAndMove(int param0, int param1, int param2) {
      Node var4 = new Node(param0, param1, param2);
      var4.heapIdx = this.heapIdx;
      var4.g = this.g;
      var4.h = this.h;
      var4.f = this.f;
      var4.cameFrom = this.cameFrom;
      var4.closed = this.closed;
      var4.walkedDistance = this.walkedDistance;
      var4.costMalus = this.costMalus;
      var4.type = this.type;
      return var4;
   }

   public static int createHash(int param0, int param1, int param2) {
      return param1 & 0xFF | (param0 & 32767) << 8 | (param2 & 32767) << 24 | (param0 < 0 ? Integer.MIN_VALUE : 0) | (param2 < 0 ? 32768 : 0);
   }

   public float distanceTo(Node param0) {
      float var2 = param0.x - this.x;
      float var3 = param0.y - this.y;
      float var4 = param0.z - this.z;
      return Mth.sqrt(var2 * var2 + var3 * var3 + var4 * var4);
   }

   public float distanceToXZ(Node param0) {
      float var2 = param0.x - this.x;
      float var3 = param0.z - this.z;
      return Mth.sqrt(var2 * var2 + var3 * var3);
   }

   public float distanceTo(BlockPos param0) {
      float var2 = param0.getX() - this.x;
      float var3 = param0.getY() - this.y;
      float var4 = param0.getZ() - this.z;
      return Mth.sqrt(var2 * var2 + var3 * var3 + var4 * var4);
   }

   public float distanceToSqr(Node param0) {
      float var2 = param0.x - this.x;
      float var3 = param0.y - this.y;
      float var4 = param0.z - this.z;
      return var2 * var2 + var3 * var3 + var4 * var4;
   }

   public float distanceToSqr(BlockPos param0) {
      float var2 = param0.getX() - this.x;
      float var3 = param0.getY() - this.y;
      float var4 = param0.getZ() - this.z;
      return var2 * var2 + var3 * var3 + var4 * var4;
   }

   public float distanceManhattan(Node param0) {
      float var2 = Math.abs(param0.x - this.x);
      float var3 = Math.abs(param0.y - this.y);
      float var4 = Math.abs(param0.z - this.z);
      return var2 + var3 + var4;
   }

   public float distanceManhattan(BlockPos param0) {
      float var2 = Math.abs(param0.getX() - this.x);
      float var3 = Math.abs(param0.getY() - this.y);
      float var4 = Math.abs(param0.getZ() - this.z);
      return var2 + var3 + var4;
   }

   public BlockPos asBlockPos() {
      return new BlockPos(this.x, this.y, this.z);
   }

   public Vec3 asVec3() {
      return new Vec3(this.x, this.y, this.z);
   }

   @Override
   public boolean equals(Object param0) {
      return !(param0 instanceof Node var2) ? false : this.hash == var2.hash && this.x == var2.x && this.y == var2.y && this.z == var2.z;
   }

   @Override
   public int hashCode() {
      return this.hash;
   }

   public boolean inOpenSet() {
      return this.heapIdx >= 0;
   }

   @Override
   public String toString() {
      return "Node{x=" + this.x + ", y=" + this.y + ", z=" + this.z + "}";
   }

   public void writeToStream(FriendlyByteBuf param0) {
      param0.writeInt(this.x);
      param0.writeInt(this.y);
      param0.writeInt(this.z);
      param0.writeFloat(this.walkedDistance);
      param0.writeFloat(this.costMalus);
      param0.writeBoolean(this.closed);
      param0.writeEnum(this.type);
      param0.writeFloat(this.f);
   }

   public static Node createFromStream(FriendlyByteBuf param0) {
      Node var1 = new Node(param0.readInt(), param0.readInt(), param0.readInt());
      readContents(param0, var1);
      return var1;
   }

   protected static void readContents(FriendlyByteBuf param0, Node param1) {
      param1.walkedDistance = param0.readFloat();
      param1.costMalus = param0.readFloat();
      param1.closed = param0.readBoolean();
      param1.type = param0.readEnum(BlockPathTypes.class);
      param1.f = param0.readFloat();
   }
}
