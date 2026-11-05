package com.electrocorp.electrocorpplatform.acceptance;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(
        key = GLUE_PROPERTY_NAME,
        value = "com.electrocorp.electrocorpplatform.acceptance")
// Only run the scenarios whose step definitions are already implemented.
// The remaining .feature files are delivered as executable specification and
// will run once their step definitions are added.
// @sprint-2 agrupa las pruebas de aceptacion de US-13, US-16, US-18 y US-20.
@ConfigurationParameter(
        key = FILTER_TAGS_PROPERTY_NAME,
        value = "@iam or @billing or @sprint-2")
public class CucumberRunnerTest {
}
