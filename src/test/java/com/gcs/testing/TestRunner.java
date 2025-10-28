package com.gcs.testing;

import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Main test runner class for programmatic test execution.
 * This can be used as an alternative to running tests via Maven or TestNG XML.
 */
public class TestRunner {

    public static void main(String[] args) {
        TestRunner runner = new TestRunner();
        runner.runAllTests();
    }

    /**
     * Runs all GCS CLI tests.
     */
    public void runAllTests() {
        TestNG testNG = new TestNG();
        XmlSuite suite = createTestSuite("GCS CLI Test Suite");

        // Create test for all commands
        XmlTest allTests = createTest(suite, "All GCS Commands Test");
        allTests.setXmlClasses(getAllTestClasses());

        List<XmlSuite> suites = new ArrayList<>();
        suites.add(suite);

        testNG.setXmlSuites(suites);
        testNG.setVerbose(2);
        testNG.setUseDefaultListeners(true);

        System.out.println("Starting GCS CLI Test Suite...");
        testNG.run();
    }

    /**
     * Runs only the sign-url tests (priority).
     */
    public void runSignUrlTests() {
        TestNG testNG = new TestNG();
        XmlSuite suite = createTestSuite("Sign URL Test Suite");

        XmlTest signUrlTest = createTest(suite, "Sign URL Tests");
        List<XmlClass> classes = new ArrayList<>();
        classes.add(new XmlClass("com.gcs.testing.tests.SignUrlTest"));
        signUrlTest.setXmlClasses(classes);

        List<XmlSuite> suites = new ArrayList<>();
        suites.add(suite);

        testNG.setXmlSuites(suites);
        testNG.run();
    }

    /**
     * Runs a specific test class.
     *
     * @param testClassName the fully qualified test class name
     */
    public void runSpecificTest(String testClassName) {
        TestNG testNG = new TestNG();
        XmlSuite suite = createTestSuite("Specific Test Suite");

        XmlTest test = createTest(suite, "Specific Test");
        List<XmlClass> classes = new ArrayList<>();
        classes.add(new XmlClass(testClassName));
        test.setXmlClasses(classes);

        List<XmlSuite> suites = new ArrayList<>();
        suites.add(suite);

        testNG.setXmlSuites(suites);
        testNG.run();
    }

    /**
     * Creates a test suite with default configuration.
     */
    private XmlSuite createTestSuite(String suiteName) {
        XmlSuite suite = new XmlSuite();
        suite.setName(suiteName);
        suite.setParallel(XmlSuite.ParallelMode.CLASSES);
        suite.setThreadCount(4);
        suite.setVerbose(1);
        return suite;
    }

    /**
     * Creates a test within a suite.
     */
    private XmlTest createTest(XmlSuite suite, String testName) {
        XmlTest test = new XmlTest(suite);
        test.setName(testName);
        test.setPreserveOrder(false);
        return test;
    }

    /**
     * Returns all test classes.
     */
    private List<XmlClass> getAllTestClasses() {
        List<XmlClass> classes = new ArrayList<>();
        classes.add(new XmlClass("com.gcs.testing.tests.SignUrlTest"));
        classes.add(new XmlClass("com.gcs.testing.tests.CopyCommandTest"));
        classes.add(new XmlClass("com.gcs.testing.tests.ListCommandTest"));
        classes.add(new XmlClass("com.gcs.testing.tests.DeleteCommandTest"));
        return classes;
    }

    /**
     * Command-line interface for the test runner.
     * Usage:
     * - No args: runs all tests
     * - "signurl": runs only sign-url tests
     * - Fully qualified class name: runs specific test class
     */
    public static void parseArgsAndRun(String[] args) {
        TestRunner runner = new TestRunner();

        if (args.length == 0) {
            System.out.println("Running all tests...");
            runner.runAllTests();
        } else {
            String arg = args[0].toLowerCase();
            switch (arg) {
                case "signurl":
                case "sign-url":
                    System.out.println("Running Sign URL tests only...");
                    runner.runSignUrlTests();
                    break;
                case "all":
                    System.out.println("Running all tests...");
                    runner.runAllTests();
                    break;
                default:
                    if (arg.contains(".")) {
                        System.out.println("Running specific test: " + args[0]);
                        runner.runSpecificTest(args[0]);
                    } else {
                        System.err.println("Unknown argument: " + arg);
                        System.err.println("Usage: TestRunner [all|signurl|<fully.qualified.TestClass>]");
                        System.exit(1);
                    }
            }
        }
    }
}