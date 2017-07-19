package dk.alexandra.fresco.suite.spdz;

import dk.alexandra.fresco.framework.ProtocolEvaluator;
import dk.alexandra.fresco.framework.TestThreadRunner;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadConfiguration;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadFactory;
import dk.alexandra.fresco.framework.configuration.NetworkConfiguration;
import dk.alexandra.fresco.framework.configuration.TestConfiguration;
import dk.alexandra.fresco.framework.network.NetworkingStrategy;
import dk.alexandra.fresco.framework.sce.configuration.TestSCEConfiguration;
import dk.alexandra.fresco.framework.sce.evaluator.EvaluationStrategy;
import dk.alexandra.fresco.framework.sce.resources.storage.InMemoryStorage;
import dk.alexandra.fresco.lib.arithmetic.BasicArithmeticTests;
import dk.alexandra.fresco.suite.spdz.configuration.PreprocessingStrategy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.junit.Test;

public class TestBasicArithmeticWithEncryption {

  private void runTest(TestThreadFactory f, int noOfParties) throws Exception {
    Level logLevel = Level.INFO;

    // Since SCAPI currently does not work with ports > 9999 we use fixed
    // ports
    // here instead of relying on ephemeral ports which are often > 9999.
    List<Integer> ports = new ArrayList<>(noOfParties);
    int noOfVMThreads = 2;
    for (int i = 1; i <= noOfParties; i++) {
      ports.add(9000 + i * 10);
    }

    Map<Integer, NetworkConfiguration> netConf = TestConfiguration
        .getNetworkConfigurations(noOfParties, ports, logLevel);
    Map<Integer, TestThreadConfiguration> conf = new HashMap<>();
    for (int playerId : netConf.keySet()) {
      TestThreadConfiguration ttc = new TestThreadConfiguration();
      ttc.netConf = netConf.get(playerId);

      SpdzProtocolSuite spdzConf = new SpdzProtocolSuite(150,
          PreprocessingStrategy.DUMMY, null);
      boolean useSecureConnection = true;

      int noOfThreads = 3;
      ProtocolEvaluator evaluator = EvaluationStrategy
          .fromEnum(EvaluationStrategy.SEQUENTIAL_BATCHED);
      dk.alexandra.fresco.framework.sce.resources.storage.Storage storage = new InMemoryStorage();
      ttc.sceConf = new TestSCEConfiguration(spdzConf, NetworkingStrategy.KRYONET,
          evaluator, ttc.netConf, storage,
          useSecureConnection);
      conf.put(playerId, ttc);
    }
    TestThreadRunner.run(f, conf);
  }

  @Test
  public void testManyMultsWithEnc2Parties() throws Exception {
    runTest(new BasicArithmeticTests.TestLotsMult(), 2);
  }

  @Test
  public void testManyMultsWithEnc3Parties() throws Exception {
    runTest(new BasicArithmeticTests.TestLotsMult(), 3);
  }
}
