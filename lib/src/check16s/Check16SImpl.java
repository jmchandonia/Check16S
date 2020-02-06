package check16s;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;

import us.kbase.auth.AuthToken;
import us.kbase.common.service.*;

import com.fasterxml.jackson.databind.*;

import assemblyutil.*;
import datafileutil.*;
import genomefileutil.*;

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

        // load genomeset
        DataFileUtilClient dfuClient = new DataFileUtilClient(new URL(System.getenv("SDK_CALLBACK_URL")), token);
        dfuClient.setIsInsecureHttpConnectionAllowed(true);
        GenomefileutilClient gfuClient = new GenomefileutilClient(new URL(System.getenv("SDK_CALLBACK_URL")), token);
        gfuClient.setIsInsecureHttpConnectionAllowed(true);
        GetObjectsResults res = dfuClient.getObjects(new GetObjectsParams()
                                                     .withObjectRefs(Arrays.asList(genomesetRef)));
        JsonNode genomeSet = res.getData().get(0).getData().asJsonNode();
        for (Iterator<String> iter = genomeSet.get("elements").fieldNames(); iter.hasNext(); ) {
            String genomeRef = iter.next();
            System.out.println("found genome "+genomeRef);
        }

        return null;
    }
}
