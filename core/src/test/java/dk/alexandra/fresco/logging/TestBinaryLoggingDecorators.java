package dk.alexandra.fresco.logging;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import dk.alexandra.fresco.framework.ProtocolEvaluator;
import dk.alexandra.fresco.framework.TestThreadRunner;
import dk.alexandra.fresco.framework.builder.binary.ProtocolBuilderBinary;
import dk.alexandra.fresco.framework.configuration.NetworkConfiguration;
import dk.alexandra.fresco.framework.configuration.TestConfiguration;
import dk.alexandra.fresco.framework.network.KryoNetNetwork;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.sce.evaluator.BatchEvaluationStrategy;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedProtocolEvaluator;
import dk.alexandra.fresco.framework.sce.evaluator.EvaluationStrategy;
import dk.alexandra.fresco.framework.sce.resources.ResourcePoolImpl;
import dk.alexandra.fresco.framework.util.Drbg;
import dk.alexandra.fresco.framework.util.HmacDrbg;
import dk.alexandra.fresco.lib.bool.ComparisonBooleanTests;
import dk.alexandra.fresco.logging.binary.BinaryComparisonLoggingDecorator;
import dk.alexandra.fresco.logging.binary.BinaryLoggingDecorator;
import dk.alexandra.fresco.suite.dummy.bool.DummyBooleanBuilderFactory;
import dk.alexandra.fresco.suite.dummy.bool.DummyBooleanProtocolSuite;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class TestBinaryLoggingDecorators {

  //Tests that a < b and b !< a for some 5-bit a and b
  private final long bitLength = 5;
  private final long amountOfGTs = 2*1;  
  private final long amountOfEQsInTest = 0;
  private final long amountOfANDsInTest = amountOfGTs*bitLength;
  private final long amountOfXORsInTest = amountOfGTs*(1+ (bitLength-1)*3);

  
  @Test
  public void testBinaryComparisonLoggingDecorator() throws Exception {
    
    TestThreadRunner.TestThreadFactory<ResourcePoolImpl, ProtocolBuilderBinary> f
      = new ComparisonBooleanTests.TestGreaterThan<>();
    
    int noOfParties = 2;
    EvaluationStrategy evalStrategy = EvaluationStrategy.SEQUENTIAL; 
    
    List<Integer> ports = new ArrayList<>(noOfParties);
    for (int i = 1; i <= noOfParties; i++) {
      ports.add(9000 + i * (noOfParties - 1));
    }

    Map<Integer, NetworkConfiguration> netConf =
        TestConfiguration.getNetworkConfigurations(noOfParties, ports);
    Map<Integer, TestThreadRunner.TestThreadConfiguration<ResourcePoolImpl, ProtocolBuilderBinary>> conf =
        new HashMap<>();

    for (int playerId : netConf.keySet()) {
      NetworkConfiguration partyNetConf = netConf.get(playerId);
      DummyBooleanProtocolSuite ps = new DummyBooleanProtocolSuite();
      BatchEvaluationStrategy<ResourcePoolImpl> strat = evalStrategy.getStrategy();
      
      ProtocolEvaluator<ResourcePoolImpl, ProtocolBuilderBinary> evaluator =
          new BatchedProtocolEvaluator<>(strat, ps);
      SecureComputationEngine<ResourcePoolImpl, ProtocolBuilderBinary> sce = new SecureComputationEngineImpl<>(
          ps, evaluator);
      Drbg drbg = new HmacDrbg();
      TestThreadRunner.TestThreadConfiguration<ResourcePoolImpl, ProtocolBuilderBinary> ttc =
          new TestThreadRunner.TestThreadConfiguration<>(sce,
              () -> new ResourcePoolImpl(playerId, noOfParties, drbg),
              () -> {
                return new KryoNetNetwork(partyNetConf);
              });
      conf.put(playerId, ttc);
    }
    TestThreadRunner.run(f, conf);
    
    for(Integer pId: netConf.keySet()) {
      List<PerformanceLogger> pl = DummyBooleanBuilderFactory.performanceLoggers.get(pId);

      Map<String, Long> loggedValues = pl.get(1).getLoggedValues(pId);
      assertThat(loggedValues.get(BinaryComparisonLoggingDecorator.BINARY_COMPARISON_GT), is(amountOfGTs)); 
      assertThat(loggedValues.get(BinaryComparisonLoggingDecorator.BINARY_COMPARISON_EQ), is(amountOfEQsInTest));

      pl.get(1).reset();
      loggedValues = pl.get(1).getLoggedValues(pId);
      assertThat(loggedValues.get(BinaryComparisonLoggingDecorator.BINARY_COMPARISON_GT), is((long)0));
      assertThat(loggedValues.get(BinaryComparisonLoggingDecorator.BINARY_COMPARISON_EQ), is((long)0));
    }
  }
  
  @Test
  public void testBinaryLoggingDecorator() throws Exception {
    
    TestThreadRunner.TestThreadFactory<ResourcePoolImpl, ProtocolBuilderBinary> f
        = new ComparisonBooleanTests.TestGreaterThan<>();

    int noOfParties = 2;
    EvaluationStrategy evalStrategy = EvaluationStrategy.SEQUENTIAL; 

    List<Integer> ports = new ArrayList<>(noOfParties);
    for (int i = 1; i <= noOfParties; i++) {
      ports.add(9000 + i * (noOfParties - 1));
    }

    Map<Integer, NetworkConfiguration> netConf =
        TestConfiguration.getNetworkConfigurations(noOfParties, ports);
    Map<Integer, TestThreadRunner.TestThreadConfiguration<ResourcePoolImpl, ProtocolBuilderBinary>> conf =
        new HashMap<>();

    for (int playerId : netConf.keySet()) {
      NetworkConfiguration partyNetConf = netConf.get(playerId);
      DummyBooleanProtocolSuite ps = new DummyBooleanProtocolSuite();
      BatchEvaluationStrategy<ResourcePoolImpl> strat = evalStrategy.getStrategy();

      ProtocolEvaluator<ResourcePoolImpl, ProtocolBuilderBinary> evaluator =
          new BatchedProtocolEvaluator<>(strat, ps);
      SecureComputationEngine<ResourcePoolImpl, ProtocolBuilderBinary> sce = new SecureComputationEngineImpl<>(
          ps, evaluator);
      
      Drbg drbg = new HmacDrbg();
      TestThreadRunner.TestThreadConfiguration<ResourcePoolImpl, ProtocolBuilderBinary> ttc =
          new TestThreadRunner.TestThreadConfiguration<>(sce,
              () -> new ResourcePoolImpl(playerId, noOfParties, drbg),
              () -> {
                return new KryoNetNetwork(partyNetConf);
              });
      conf.put(playerId, ttc);
    }
    TestThreadRunner.run(f, conf);

    for(Integer pId: netConf.keySet()) {
      List<PerformanceLogger> pl = DummyBooleanBuilderFactory.performanceLoggers.get(pId);

      Map<String, Long> loggedValues = pl.get(0).getLoggedValues(pId);
      assertThat(loggedValues.get(BinaryLoggingDecorator.BINARY_BASIC_XOR), is(amountOfXORsInTest));
      assertThat(loggedValues.get(BinaryLoggingDecorator.BINARY_BASIC_AND), is(amountOfANDsInTest));
      assertThat(loggedValues.get(BinaryLoggingDecorator.BINARY_BASIC_RANDOM), is((long)0));
      
      pl.get(0).reset();
      loggedValues = pl.get(0).getLoggedValues(pId);
      assertThat(loggedValues.get(BinaryLoggingDecorator.BINARY_BASIC_XOR), is((long)0));
      assertThat(loggedValues.get(BinaryLoggingDecorator.BINARY_BASIC_AND), is((long)0));
      assertThat(loggedValues.get(BinaryLoggingDecorator.BINARY_BASIC_RANDOM), is((long)0));
    }
  }
}