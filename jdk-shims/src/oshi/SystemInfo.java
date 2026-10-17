package oshi;

import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

/**
 * Stub of oshi.SystemInfo (oshi-core 6.2.2).
 *
 * Part of the OSHI stub set that replaces the real jar; read
 * oshi/hardware/CentralProcessor.java first -- it has the rationale and the error counts,
 * including why JNA cannot be made to work here.
 *
 * Narrowed hard: of the real class's many get* accessors, only getHardware() is kept, since
 * getHardware().getProcessor() is the entire call chain ServerMetricsSamplersProvider uses.
 * The real constructor probes the OS immediately -- platform name, version, the physical
 * and logical CPU counts -- before any accessor is called, which is precisely the native
 * work that made linking the real jar cost forty-five errors. This one allocates.
 *
 * HardwareAbstractionLayer is an interface in the real jar too, so the one implementation
 * here is anonymous rather than a stubbed concrete class.
 */
public class SystemInfo {

   private final HardwareAbstractionLayer hardware = new HardwareAbstractionLayer() {
      @Override
      public CentralProcessor getProcessor() {
         return new CentralProcessor();
      }
   };

   public HardwareAbstractionLayer getHardware() {
      return this.hardware;
   }
}
