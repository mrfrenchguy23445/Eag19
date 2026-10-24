import org.teavm.tooling.*;
import org.teavm.vm.TeaVMOptimizationLevel;
public final class BuildFixture {
 public static void main(String[] args) throws Exception {
  TeaVMTool tool = new TeaVMTool();
  tool.setTargetType(TeaVMTargetType.WEBASSEMBLY_GC);
  tool.setMainClass(args.length > 1 ? args[1] : "BooleanFixture");
  tool.setTargetDirectory(new java.io.File(args[0]));
  tool.setTargetFileName("fixture.wasm");
  tool.setOptimizationLevel(TeaVMOptimizationLevel.ADVANCED);
  tool.setFastDependencyAnalysis(false);
  tool.generate();
  if(!tool.getProblemProvider().getSevereProblems().isEmpty()) {
   for(var problem:tool.getProblemProvider().getSevereProblems()) System.err.println(problem.getText());
   throw new AssertionError("Fixture did not compile");
  }
 }
}
