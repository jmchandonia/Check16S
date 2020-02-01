/*
A KBase module: Check16S
*/

module Check16S {
    typedef structure {
        string report_name;
        string report_ref;
    } ReportResults;

    /*
        This example function accepts any number of parameters and returns results in a KBaseReport
    */
    funcdef run_Check16S(mapping<string,UnspecifiedObject> params) returns (ReportResults output) authentication required;

};
