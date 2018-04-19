package dk.alexandra.fresco.lib.compare.gt;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThread;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadFactory;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.framework.value.SInt;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;

public class LessThanZeroTests {

  public static class TestLessThanZero<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    private final List<BigInteger> openInputs;
    private final List<BigInteger> expected;

    public TestLessThanZero(BigInteger modulus) {
      this.openInputs = Arrays.asList(
          BigInteger.ZERO,
          BigInteger.ONE,
          BigInteger.valueOf(-1)
      );
      this.expected = computeExpected(openInputs, modulus);
    }

    private static List<BigInteger> computeExpected(List<BigInteger> inputs, BigInteger modulus) {
      List<BigInteger> expected = new ArrayList<>(inputs.size());
      for (BigInteger input : inputs) {
        boolean lessThanZero = input.compareTo(BigInteger.ZERO) < 0;
        expected.add(lessThanZero ? BigInteger.ONE : BigInteger.ZERO);
      }
      return expected;
    }

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<ResourcePoolT, ProtocolBuilderNumeric>() {

        @Override
        public void test() {
          Application<List<BigInteger>, ProtocolBuilderNumeric> app = builder -> {
            Numeric numeric = builder.numeric();
            int k = builder.getBasicNumericContext().getModulus().bitLength();
            int kappa = 40;
            List<DRes<SInt>> inputs = numeric.known(openInputs);
            List<DRes<SInt>> actualInner = new ArrayList<>(inputs.size());
            for (DRes<SInt> input : inputs) {
              actualInner.add(builder.seq(new LessThanZero(input, k, kappa)));
            }
            DRes<List<DRes<BigInteger>>> opened = builder.collections().openList(() -> actualInner);
            return () -> opened.out().stream().map(DRes::out).collect(Collectors.toList());
          };
          List<BigInteger> actual = runApplication(app);
          Assert.assertEquals(expected, actual);
          System.out.println(expected);
        }
      };
    }
  }

}
