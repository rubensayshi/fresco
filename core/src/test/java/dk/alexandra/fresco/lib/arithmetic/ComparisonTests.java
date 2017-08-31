/*
 * Copyright (c) 2015, 2016 FRESCO (http://github.com/aicis/fresco).
 *
 * This file is part of the FRESCO project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * FRESCO uses SCAPI - http://crypto.biu.ac.il/SCAPI, Crypto++, Miracl, NTL,
 * and Bouncy Castle. Please see these projects for any further licensing issues.
 *******************************************************************************/
package dk.alexandra.fresco.lib.arithmetic;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.Computation;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThread;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadFactory;
import dk.alexandra.fresco.framework.builder.numeric.ComparisonBuilder;
import dk.alexandra.fresco.framework.builder.numeric.NumericBuilder;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.ResourcePoolCreator;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.framework.value.SInt;
import java.math.BigInteger;
import org.junit.Assert;

public class ComparisonTests {

  /**
   * Compares the two numbers 3 and 5 and checks that 3 < 5. Also checks that 5 is not < 3
   *
   * @author Kasper Damgaard
   */
  public static class TestCompareLT<ResourcePoolT extends ResourcePool> extends
      TestThreadFactory {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<ResourcePoolT, ProtocolBuilderNumeric>() {

        @Override
        public void test() throws Exception {
          Application<Pair<BigInteger, BigInteger>, ProtocolBuilderNumeric> app =
              new Application<Pair<BigInteger, BigInteger>, ProtocolBuilderNumeric>() {
                private BigInteger three = BigInteger.valueOf(3);
                private BigInteger five = BigInteger.valueOf(5);

                @Override
                public Computation<Pair<BigInteger, BigInteger>> buildComputation(
                    ProtocolBuilderNumeric builder) {
                  NumericBuilder input = builder.numeric();
                  Computation<SInt> x = input.known(three);
                  Computation<SInt> y = input.known(five);
                  ComparisonBuilder comparison = builder.comparison();
                  Computation<SInt> compResult1 = comparison.compareLEQ(x, y);
                  Computation<SInt> compResult2 = comparison.compareLEQ(y, x);
                  NumericBuilder open = builder.numeric();
                  Computation<BigInteger> res1;
                  Computation<BigInteger> res2;
                  res1 = open.open(compResult1);
                  res2 = open.open(compResult2);
                  return () -> new Pair<>(res1.out(), res2.out());
                }
              };
          Pair<BigInteger, BigInteger> output = secureComputationEngine
              .runApplication(app, ResourcePoolCreator.createResourcePool(conf.sceConf));
          Assert.assertEquals(BigInteger.ONE, output.getFirst());
          Assert.assertEquals(BigInteger.ZERO, output.getSecond());
        }
      };
    }
  }

  /**
   * Compares the two numbers 3 and 5 and checks that 3 == 3. Also checks that 3 != 5
   *
   * @author Kasper Damgaard
   */
  public static class TestCompareEQ<ResourcePoolT extends ResourcePool> extends
      TestThreadFactory {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<ResourcePoolT, ProtocolBuilderNumeric>() {

        @Override
        public void test() throws Exception {
          Application<Pair<BigInteger, BigInteger>, ProtocolBuilderNumeric> app =
              new Application<Pair<BigInteger, BigInteger>, ProtocolBuilderNumeric>() {

                private BigInteger three = BigInteger.valueOf(3);
                private BigInteger five = BigInteger.valueOf(5);

                @Override
                public Computation<Pair<BigInteger, BigInteger>> buildComputation(
                    ProtocolBuilderNumeric builder) {
                  NumericBuilder input = builder.numeric();
                  Computation<SInt> x = input.known(three);
                  Computation<SInt> y = input.known(five);
                  ComparisonBuilder comparison = builder.comparison();
                  Computation<SInt> compResult1 = comparison.equals(x, x);
                  Computation<SInt> compResult2 = comparison.equals(x, y);
                  NumericBuilder open = builder.numeric();
                  Computation<BigInteger> res1 = open.open(compResult1);
                  Computation<BigInteger> res2 = open.open(compResult2);
                  return () -> new Pair<>(res1.out(), res2.out());
                }
              };
          Pair<BigInteger, BigInteger> output = secureComputationEngine
              .runApplication(app, ResourcePoolCreator.createResourcePool(conf.sceConf));
          Assert.assertEquals(BigInteger.ONE, output.getFirst());
          Assert.assertEquals(BigInteger.ZERO, output.getSecond());
        }
      };
    }
  }
}
