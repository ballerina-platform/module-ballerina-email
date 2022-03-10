// Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/http;
import ballerina/lang.runtime;
import ballerina/lang.value;
import ballerina/time;
import ballerina/log;
import ballerina/io;

type TestResults record {|
    boolean completed;
    int sentCount?;
    int errorCount?;
    int receivedCount?;
|};

configurable string URL = "http://smtp-client-svc.default.svc.cluster.local:9090/perf-test";

public function main(string label, string output_csv_path) returns error? {
    http:Client loadTestClient = check new (URL);
    decimal loadTestDuration = 3600;
    http:Response response = check loadTestClient->get(string `/start?duration=${loadTestDuration}`);
    if response.statusCode != http:STATUS_ACCEPTED {
        log:printError("Unaccepted response code received for load-test initiation, hence aborting", code = response.statusCode);
        return;
    }

    runtime:sleep(loadTestDuration);
    TestResults testResults = check loadTestClient->get("/status");
    log:printInfo("Received initial load-test results", result = testResults);
    while !testResults.completed {
        runtime:sleep(60);
        testResults = check loadTestClient->get("/status");
        log:printInfo("Received load-test results", result = testResults);
    }

    int sentCount = check value:ensureType(testResults?.sentCount);
    int errorCount = check value:ensureType(testResults?.errorCount);
    int receivedCount = check value:ensureType(testResults?.receivedCount);
    log:printInfo("Test summary: ", sent = sentCount, received = receivedCount, errors = errorCount, duration = loadTestDuration);
    any[] results = [label, sentCount, <float>loadTestDuration/<float>receivedCount, 0, 0, 0, 0, 0, 0, <float>errorCount/<float>sentCount, 
        <float>receivedCount/<float>loadTestDuration, 0, 0, time:utcNow()[0], 0, 1];
    check writeResultsToCsv(results, output_csv_path);
}

function writeResultsToCsv(any[] results, string output_path) returns error? {
    string[][] summary_data = check io:fileReadCsv(output_path);
    string[] final_results = [];
    foreach var result in results {
        final_results.push(result.toString());
    }
    summary_data.push(final_results);
    check io:fileWriteCsv(output_path, summary_data);
}
