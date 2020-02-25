package check16s;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonServerMethod;
import us.kbase.common.service.JsonServerServlet;
import us.kbase.common.service.JsonServerSyslog;
import us.kbase.common.service.RpcContext;

//BEGIN_HEADER
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.net.MalformedURLException;

import kbasereport.CreateParams;
import kbasereport.KBaseReportClient;
import kbasereport.ReportInfo;
import kbasereport.WorkspaceObject;
import check16s.ReportResults;
//END_HEADER

/**
 * <p>Original spec-file module name: Check16S</p>
 * <pre>
 * A KBase module: Check16S
 * </pre>
 */
public class Check16SServer extends JsonServerServlet {
    private static final long serialVersionUID = 1L;
    private static final String version = "1.0.0";
    private static final String gitUrl = "git@github.com:jmchandonia/Check16S.git";
    private static final String gitCommitHash = "82e4af40d5c05e5227eae044bb78ce7aced430a7";

    //BEGIN_CLASS_HEADER
    private final URL callbackURL;
    private final Path scratch;
    //END_CLASS_HEADER

    public Check16SServer() throws Exception {
        super("Check16S");
        //BEGIN_CONSTRUCTOR
        final String sdkURL = System.getenv("SDK_CALLBACK_URL");
        try {
            callbackURL = new URL(sdkURL);
            System.out.println("Got SDK_CALLBACK_URL " + callbackURL);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid SDK callback url: " + sdkURL, e);
        }
        scratch = Paths.get(super.config.get("scratch"));
        //END_CONSTRUCTOR
    }

    /**
     * <p>Original spec-file function name: check_16S</p>
     * <pre>
     * Check the genomes in genomeset_ref against the 16S sequences in assembly_ref
     * </pre>
     * @param   params   instance of type {@link check16s.Check16SInput Check16SInput}
     * @return   parameter "output" of type {@link check16s.ReportResults ReportResults}
     */
    @JsonServerMethod(rpc = "Check16S.check_16S", async=true)
    public ReportResults check16S(Check16SInput params, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        ReportResults returnVal = null;
        //BEGIN check_16S
        java.io.File scratchDir = new java.io.File(scratch.toString());
        returnVal = Check16SImpl.check16S(authPart, scratchDir, params);
        //END check_16S
        return returnVal;
    }
    @JsonServerMethod(rpc = "Check16S.status")
    public Map<String, Object> status() {
        Map<String, Object> returnVal = null;
        //BEGIN_STATUS
        returnVal = new LinkedHashMap<String, Object>();
        returnVal.put("state", "OK");
        returnVal.put("message", "");
        returnVal.put("version", version);
        returnVal.put("git_url", gitUrl);
        returnVal.put("git_commit_hash", gitCommitHash);
        //END_STATUS
        return returnVal;
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 1) {
            new Check16SServer().startupServer(Integer.parseInt(args[0]));
        } else if (args.length == 3) {
            JsonServerSyslog.setStaticUseSyslog(false);
            JsonServerSyslog.setStaticMlogFile(args[1] + ".log");
            new Check16SServer().processRpcCall(new File(args[0]), new File(args[1]), args[2]);
        } else {
            System.out.println("Usage: <program> <server_port>");
            System.out.println("   or: <program> <context_json_file> <output_json_file> <token>");
            return;
        }
    }
}
