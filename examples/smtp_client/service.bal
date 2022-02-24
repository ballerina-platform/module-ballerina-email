import ballerina/http;

type PerfTestResult readonly & record {|
    boolean completed;
    int sentCount?;
    int errorCount?;
    string duration?;
|};

service /perf\-test on new http:Listener(9090) {
    // initiate the perf-test
    resource function get 'start(int duration) returns http:Accepted|error? {
        
    }

    // status check
    resource function get status() returns PerfTestResult {
        return {
            completed: false
        };
    }
}