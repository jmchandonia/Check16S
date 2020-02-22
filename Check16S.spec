/*
A KBase module: Check16S
*/

module Check16S {
    typedef structure {
        string report_name;
        string report_ref;
    } ReportResults;

    typedef structure {
        string genomeset_ref;
        string assembly_ref;
        string ws_name;
        int ws_id;
	int max_differences;
	int min_significant_differences;
	string output_genomeset_pass;
	string output_genomeset_fail;
	string output_genomeset_unknown;
    } Check16SInput;

    /*
        Check the genomes in genomeset_ref against the 16S sequences in assembly_ref
    */
    funcdef check_16S(Check16SInput params) returns (ReportResults output) authentication required;

};
