package io.resttestgen.implementation.strategy;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.HttpStatusCode;
import io.resttestgen.core.helper.DeepReinforcementLearningProxy;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.*;
import io.resttestgen.core.testing.operationsorter.OperationsSorter;
import io.resttestgen.implementation.oracle.StatusCodeOracle;
import io.resttestgen.implementation.fuzzer.ExperienceFuzzer;
import io.resttestgen.implementation.fuzzer.IntensificationFuzzer;
import io.resttestgen.implementation.fuzzer.NominalFuzzer;
import io.resttestgen.implementation.operationssorter.DeepReinforcementLearningOperationsSorter;
import io.resttestgen.implementation.operationssorter.RandomOperationsSorter;
import io.resttestgen.implementation.strategy.configuration.DeepReinforcementLearningStrategyConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;

@SuppressWarnings("unused")
public class DeepReinforcementLearningStrategy extends Strategy {

    private static final Logger logger = LogManager.getLogger(DeepReinforcementLearningStrategy.class);

    private final DeepReinforcementLearningStrategyConfiguration config =
            StrategyConfiguration.loadConfiguration(DeepReinforcementLearningStrategyConfiguration.class);

    HashSet<Operation> intensificatedOperations = new HashSet<>();

    @Override
    public void start() {

        DeepReinforcementLearningProxy.initializeDeepReinforcementLearning(config.getNamedPipesPath(),
                Environment.getInstance().getOpenAPI().getOperations().size());

        OperationsSorter sorter = new DeepReinforcementLearningOperationsSorter();

        // If DRL is disabled, use random sorter
        if (config.isDisableDrl()) {
            sorter = new RandomOperationsSorter();
        }

        while (!sorter.isEmpty()) {

            Operation operationToTest = sorter.getFirst();

            logger.debug("Testing operation {}", operationToTest);
            TestSequence nominalSequence;
            if (config.getFuzzer().equals("experience")) {
                ExperienceFuzzer experienceFuzzer = new ExperienceFuzzer(operationToTest);
                nominalSequence = experienceFuzzer.generateTestSequences(1).get(0);
            } else {
                NominalFuzzer nominalFuzzer = new NominalFuzzer(operationToTest);
                nominalSequence = nominalFuzzer.generateTestSequences(1).get(0);
            }

            TestRunner.getInstance().run(nominalSequence);
            HttpStatusCode statusCode = nominalSequence.get(0).getResponseStatusCode();
            if (statusCode == null) {
                logger.warn("Found NULL status code");
                // Fallback
                statusCode = new HttpStatusCode(400);
            }

            StatusCodeOracle statusCodeOracle = new StatusCodeOracle();
            statusCodeOracle.assertTestSequence(nominalSequence);

            // restberus patch: the per-iteration ReportWriter (JSON) and RestAssuredWriter
            // (JUnit/REST-assured) disk writes are removed. They only produce offline
            // artifacts that restberus never consumes (it measures the SUT via its proxy),
            // and RestAssuredWriter.operationsInitialization recurses over the operation
            // dependency graph without a visited-set guard, spinning at ~100% CPU for hours
            // on rich-ODG APIs (languagetool, features-service, some restcountries seeds)
            // and starving the RL loop. The older restgym/deeprest-tool:1.0.0 jar likewise
            // does not call these writers in the strategy loop.

            DeepReinforcementLearningProxy.sendResult(statusCode);

            // In case of successful interaction, invoke intensification testing, but only the first time, and then with
            // a low probability. Of course, only if intensification is enabled by configuration
            if (statusCode.isSuccessful() && config.isIntensification()) {
                if (!intensificatedOperations.contains(operationToTest) || (Environment.getInstance().getRandom().nextInt(0, 100) < config.getIntensificationProbability())) {

                    logger.info("Performed successful interaction. Starting intensification for {}.", operationToTest);

                    IntensificationFuzzer intensificationFuzzer = new IntensificationFuzzer(nominalSequence);
                    intensificationFuzzer.generateTestSequences(0); // Input to this method is currently ignored

                    intensificatedOperations.add(operationToTest);

                    logger.info("Intensification completed. Continuing with testing.");
                }
            }

            sorter.removeFirst();
        }
    }
}
