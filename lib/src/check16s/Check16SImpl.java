package check16s;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;

import us.kbase.auth.AuthToken;

import assemblyutil.*;
import datafileutil.*;

public class Check16SImpl {
    /**
       Download assembly as FASTA, and return path to file
    */
    public static String downloadAssembly(AuthToken token,
                                          String assemblyRef) throws Exception {
        AssemblyUtilClient auClient = new AssemblyUtilClient(new URL(System.getenv("SDK_CALLBACK_URL")), token);
        auClient.setIsInsecureHttpConnectionAllowed(true);
        DataFileUtilClient dfuClient = new DataFileUtilClient(new URL(System.getenv("SDK_CALLBACK_URL")), token);

        dfuClient.setIsInsecureHttpConnectionAllowed(true);
        FastaAssemblyFile fa = auClient.getAssemblyAsFasta(new GetAssemblyParams()
                                                           .withRef(assemblyRef));
        String faPath = fa.getPath();
        UnpackFileResult uf = dfuClient.unpackFile(new UnpackFileParams()
                                                   .withFilePath(faPath));
        return uf.getFilePath();
    }
    
    public static ReportResults check16S(AuthToken token,
                                         Check16SInput input) throws Exception {
        String genomesetRef = input.getGenomesetRef();
        String assemblyRef = input.getAssemblyRef();

        // dump out 16S sequences to fasta file
        String path16S = downloadAssembly(token, assemblyRef);

        System.out.println("got 16S sequences as "+path16S);

        return null;
    }
}
