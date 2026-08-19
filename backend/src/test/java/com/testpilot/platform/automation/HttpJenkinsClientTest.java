package com.testpilot.platform.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class HttpJenkinsClientTest {

    private HttpServer server;
    private String baseUrl;
    private String lastTriggerPath;
    private String lastTriggerBody;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/", this::handle);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void triggerEncodesNestedJobAndSyncsQueueBuildAndResult() {
        HttpJenkinsClient client = new HttpJenkinsClient(
                RestClient.builder(),
                "HTTP",
                baseUrl,
                "jenkins-user",
                "jenkins-token"
        );
        AutomationProject project = new AutomationProject(
                1L,
                "Order API",
                "API",
                "https://example.com/order.git",
                "main",
                null,
                "pytest",
                "tests",
                "pytest",
                "TEST",
                "folder/order api"
        );
        TestTask task = new TestTask(
                1L,
                "Order regression",
                "REGRESSION",
                2L,
                "folder/order api",
                "{\"alluredir\":\"allure-results\"}"
        );

        JenkinsClient.TriggerResult trigger = client.trigger(project, task);

        assertNull(trigger.buildNumber());
        assertEquals(baseUrl + "/queue/item-7/", trigger.queueUrl());
        assertEquals("/job/folder/job/order%20api/buildWithParameters", lastTriggerPath);
        assertTrue(lastTriggerBody.contains("BRANCH=main"));
        assertTrue(lastTriggerBody.contains("ENVIRONMENT=TEST"));
        assertTrue(lastTriggerBody.contains("PARAMETERS_JSON=%7B%22alluredir%22%3A%22allure-results%22%7D"));

        TestExecution execution = new TestExecution(
                1L,
                3L,
                2L,
                "folder/order api",
                trigger.queueUrl(),
                null,
                "QUEUED",
                LocalDateTime.now()
        );
        JenkinsClient.SyncResult sync = client.sync(project, task, execution);

        assertEquals("17", sync.buildNumber());
        assertEquals("PASSED", sync.status());
        assertEquals("abc123", sync.commitSha());
        assertEquals(baseUrl + "/job/folder/job/order%20api/17/consoleText", sync.consoleLogRef());
        assertEquals(baseUrl + "/job/folder/job/order%20api/17/allure", sync.allureUrl());
        assertEquals(0, sync.totalCount());
        assertEquals(0, sync.passedCount());
        assertEquals(0, sync.failedCount());

    }

    @Test
    void discoversNestedFreestylePipelineAndMultibranchJobsWithParameterMetadata() {
        HttpJenkinsClient client = new HttpJenkinsClient(
                RestClient.builder(),
                "HTTP",
                baseUrl,
                "jenkins-user",
                "jenkins-token"
        );

        List<JenkinsClient.JenkinsJob> jobs = client.listJobs(
                new JenkinsClient.JenkinsTarget(baseUrl, "jenkins-user", "jenkins-token", 5000, 15000)
        );

        assertEquals(3, jobs.size());
        assertEquals("FREESTYLE_JOB", jobs.get(0).pipelineType());
        assertEquals("folder/order-api", jobs.get(1).path());
        assertEquals("PIPELINE_JOB", jobs.get(1).pipelineType());
        assertTrue(jobs.get(1).parametersJson().contains("ENVIRONMENT"));
        assertEquals("MULTIBRANCH_PIPELINE", jobs.get(2).pipelineType());
    }

    @Test
    void cancelsBuildUsingStopEndpoint() {
        HttpJenkinsClient client = new HttpJenkinsClient(
                RestClient.builder(), "HTTP", baseUrl, "jenkins-user", "jenkins-token"
        );
        AutomationProject project = new AutomationProject(
                1L, "Order API", "API", "https://example.com/order.git", "main",
                null, "pytest", "tests", "pytest", "TEST", "folder/order api"
        );
        TestTask task = new TestTask(
                1L, "Order regression", "REGRESSION", 2L, "folder/order api", null
        );
        TestExecution execution = new TestExecution(
                1L, 3L, 2L, "folder/order api", null, "17", "RUNNING", LocalDateTime.now()
        );

        client.cancel(
                project, task, execution,
                new JenkinsClient.JenkinsTarget(baseUrl, "jenkins-user", "jenkins-token", 5000, 15000)
        );

        assertEquals("/job/folder/job/order%20api/17/stop", lastTriggerPath);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getRawPath();
        if (path.equals("/api/json")) {
            send(exchange, 200, """
                    {
                      "jobs": [
                        {
                          "name": "test01",
                          "url": "%s/job/test01/",
                          "_class": "hudson.model.FreeStyleProject",
                          "buildable": true
                        },
                        {
                          "name": "folder",
                          "url": "%s/job/folder/",
                          "_class": "com.cloudbees.hudson.plugins.folder.Folder",
                          "buildable": false
                        },
                        {
                          "name": "p1d-multibranch",
                          "url": "%s/job/p1d-multibranch/",
                          "_class": "org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject",
                          "buildable": true
                        }
                      ]
                    }
                    """.formatted(baseUrl, baseUrl, baseUrl));
            return;
        }
        if (path.equals("/job/folder/api/json")) {
            send(exchange, 200, """
                    {
                      "jobs": [
                        {
                          "name": "order-api",
                          "url": "%s/job/folder/job/order-api/",
                          "_class": "org.jenkinsci.plugins.workflow.job.WorkflowJob",
                          "buildable": true,
                          "property": [
                            {
                              "parameterDefinitions": [
                                {"name":"ENVIRONMENT","type":"ChoiceParameterDefinition"}
                              ]
                            }
                          ]
                        }
                      ]
                    }
                    """.formatted(baseUrl));
            return;
        }
        if (path.endsWith("/buildWithParameters")) {
            lastTriggerPath = path;
            lastTriggerBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Location", baseUrl + "/queue/item-7/");
            send(exchange, 201, "");
            return;
        }
        if (path.endsWith("/stop")) {
            lastTriggerPath = path;
            send(exchange, 200, "");
            return;
        }
        if (path.contains("/queue/item-7/api/json")) {
            String body = "{\"executable\":{\"number\":17,\"url\":\""
                    + baseUrl + "/job/folder/job/order%20api/17/\"}}";
            send(exchange, 200, body);
            return;
        }
        if (path.contains("/job/folder/job/order") && path.endsWith("/17/api/json")) {
            send(exchange, 200, """
                    {
                      "building": false,
                      "result": "SUCCESS",
                      "timestamp": 1700000000000,
                      "duration": 1200,
                      "changeSet": {"items": [{"commitId": "abc123"}]}
                    }
                    """);
            return;
        }
        send(exchange, 404, "{}");
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
