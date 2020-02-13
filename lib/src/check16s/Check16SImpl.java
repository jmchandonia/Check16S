package check16s;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;

import org.strbio.IO;
import org.strbio.io.*;
import org.strbio.mol.*;
import org.strbio.util.*;
import org.strbio.local.Program;

import us.kbase.auth.AuthToken;
import us.kbase.common.service.*;

import com.fasterxml.jackson.databind.*;
import com.opencsv.*;

import assemblyutil.*;
import datafileutil.*;
import genomefileutil.*;
import kbasereport.*;

public class Check16SImpl {
    /**
       Download assembly as FASTA, and return path to file
    */
    public static java.io.File downloadAssembly(AuthToken token,
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
        return new java.io.File(uf.getFilePath());
    }

    /**
       extract 16S sequences from a genomeSet, saving them
       in a fasta file.  Checks their names are OK from list of
       isolates we have Sanger seqs for.
    */
    public static java.io.File download16S(AuthToken token,
                                           java.io.File baseDir,
                                           HashSet<String> isolatesWith16S,
                                           String genomesetRef) throws Exception {
        // load genomeset
        DataFileUtilClient dfuClient = new DataFileUtilClient(new URL(System.getenv("SDK_CALLBACK_URL")), token);
        dfuClient.setIsInsecureHttpConnectionAllowed(true);
        GenomefileutilClient gfuClient = new GenomefileutilClient(new URL(System.getenv("SDK_CALLBACK_URL")), token);
        gfuClient.setIsInsecureHttpConnectionAllowed(true);
        GetObjectsResults res = dfuClient.getObjects(new GetObjectsParams()
                                                     .withObjectRefs(Arrays.asList(genomesetRef)));
        JsonNode genomeSet = res.getData().get(0).getData().asJsonNode();
        System.out.println("Got GenomeSet");

        // make a FASTA file to save all the 16S sequences into
        java.io.File fileGenome16S = java.io.File.createTempFile("genome16s",".fa",
                                                                 baseDir);
        fileGenome16S.delete();
        PrintfStream outfile = new PrintfStream(fileGenome16S.getPath());


        // loop over genomes and find all their annotated 16S sequences
        for (Iterator<String> iter = genomeSet.get("elements").fieldNames(); iter.hasNext(); ) {
            JsonNode n = genomeSet.get("elements").get(iter.next());
            String genomeRef = n.get("ref").textValue();

            res = dfuClient.getObjects(new GetObjectsParams()
                                       .withObjectRefs(Arrays.asList(genomeRef)));
            String genomeName = res.getData().get(0).getInfo().getE2();
            int pos = genomeName.indexOf(".genome");
            if (pos==-1)
                throw new Exception("Genome name '"+genomeName+"' is not in the form of isolate_name.genome");
            String isolateName = genomeName.substring(0,pos);
            System.out.println("  Found isolate "+isolateName);
            if (!isolatesWith16S.contains(isolateName))
                throw new Exception("Isolate name '"+isolateName+"' does not have a Sanger 16S sequence in the 16S file provided.");

            // save all the noncoding regions into a fasta file
            // then put all the annotated 16S sequences into their own file
            // warn if no 16S or multiple 16S found (per isolate)
            FASTAResult fa = gfuClient.genomeFeaturesToFasta(new GenomeFeaturesToFastaParams()
                                                             .withGenomeRef(genomeRef)
                                                             .withFeatureLists(Arrays.asList("non_coding_features")));
            String pathFA = fa.getFilePath();
            BufferedReader seqs = IO.openReader(pathFA);
            PolymerSet ps = new PolymerSet();
            Enumeration pe = ps.polymersInFile(seqs, null);
            int n16S = 0;
            while ((pe.hasMoreElements() && (seqs.ready()))) {
                Polymer p;
                p = (Polymer)pe.nextElement();
            
                if (p==null)
                    continue;
            
                String seqName = p.name;
                if (seqName.indexOf("16S ribosomal RNA") > -1) {
                    // rename according to the isolate name
                    p.name = isolateName+"-"+(n16S++);
                    // and save it in the "genome 16S" file
                    p.writeFasta(outfile);
                }
            }
            seqs.close();
            java.io.File fileFA = new java.io.File(pathFA);
            fileFA.delete();

            // warn about not enough or too many 16S found
            if (n16S == 0)
                System.out.println("  Warning - no 16S annotated for "+isolateName);
            else if (n16S > 1)
                System.out.println("  Warning - "+n16S+" different 16S annotated for "+isolateName);
        }
        outfile.close();
        return fileGenome16S;
    }

    /**
       format a blast database
    */
    public static void formatDB(java.io.File f) throws Exception {
        String programName = "formatdb";
        Program p = new Program(programName);
        String[] i = new String[6];
        i[0] = "-p";
        i[1] = "F";
        i[2] = "-o";
        i[3] = "T";
        i[4] = "-i";
        i[5] = f.getPath();
        java.io.File baseDir = f.getParentFile();
        p.run(i, null, baseDir);
    }

    /**
       run blast, returning output file
    */
    public static java.io.File runBlast(java.io.File query,
                                        java.io.File db) throws Exception {
        java.io.File baseDir = query.getParentFile();
        java.io.File blastOutput = java.io.File.createTempFile("bla",".txt",
                                                               baseDir);
        blastOutput.delete();
        
        String programName = "blastn";
        Program p = new Program(programName);
        String[] i = new String[8];
        i[0] = "-query";
        i[1] = query.getPath();
        i[2] = "-db";
        i[3] = db.getPath();
        i[4] = "-out";
        i[5] = blastOutput.getPath();
        i[6] = "-outfmt";
        i[7] = "6 qseqid sseqid length nident qstart qseq sstart sseq";
        p.run(i, null, baseDir);

        return (blastOutput);
    }

    /**
       add a match
    */
    public static void addMatch(HashMap<String,HashSet<String>> map,
                                String key,
                                String value) {
        HashSet<String> values = map.get(key);
        if (values==null)
            values = new HashSet<String>();
        values.add(value);
        map.put(key,values);
    }

    /**
       get list of sequence ids in a file
       */
    public static HashSet<String> getSeqNames(java.io.File f) throws Exception {
        HashSet<String> rv = new HashSet<String>();
        BufferedReader seqs = IO.openReader(f.getPath());
        PolymerSet ps = new PolymerSet();
        Enumeration pe = ps.polymersInFile(seqs, null);
        while ((pe.hasMoreElements() && (seqs.ready()))) {
            Polymer p;
            p = (Polymer)pe.nextElement();
            
            if (p==null)
                continue;
            
            String seqName = p.name;
            rv.add(seqName);
        }
        seqs.close();
        return rv;
    }

    /**
       Make and save Report object, returning its name and reference
    */
    public static ReportInfo makeReport(AuthToken token,
                                        String wsName,
                                        List<kbasereport.File> files,
                                        String reportText) throws Exception {
        KBaseReportClient reportClient = new KBaseReportClient(new URL(System.getenv("SDK_CALLBACK_URL")), token);
        reportClient.setIsInsecureHttpConnectionAllowed(true);

        String reportName = "check16s_report_"+UUID.randomUUID().toString();
        return reportClient.createExtendedReport(new CreateExtendedReportParams()
                                                 .withWorkspaceName(wsName)
                                                 .withMessage(reportText)
                                                 .withReportObjectName(reportName)
                                                 .withFileLinks(files));
    }

    /**
       check 16S vs expected sequences
    */
    public static ReportResults check16S(AuthToken token,
                                         Check16SInput input) throws Exception {
        String genomesetRef = input.getGenomesetRef();
        String assemblyRef = input.getAssemblyRef();
        String wsName = input.getWs();
        String reportText = "";

        // dump out 16S sequences to fasta file
        java.io.File fileSanger16S = downloadAssembly(token, assemblyRef);
        System.out.println("Got Sanger 16S sequences as "+fileSanger16S.getPath());
        java.io.File baseDir = fileSanger16S.getParentFile();

        // keep track of the isolates with Sanger 16S sequences
        HashSet<String> isolatesWithSanger16S = getSeqNames(fileSanger16S);

        // dump out the 16S sequences from the genomeset to a fasta file
        java.io.File fileGenome16S = download16S(token,
                                                 baseDir,
                                                 isolatesWithSanger16S,
                                                 genomesetRef);
        System.out.println("Got genome 16S sequences as "+fileGenome16S.getPath());

        // keep track of isolates with genome 16S sequences
        HashSet<String> isolateNumsWithGenome16S = getSeqNames(fileGenome16S);
        HashSet<String> isolatesWithGenome16S = new HashSet<String>();
        for (String isolateIDNum : isolateNumsWithGenome16S) {
            int pos = isolateIDNum.lastIndexOf("-");
            String isolateID = isolateIDNum.substring(0,pos);
            isolatesWithGenome16S.add(isolateID);
        }
        
        // setup blast
        System.out.println("Setting up BLAST");
        formatDB(fileSanger16S);

        // run blast
        System.out.println("Running BLAST");
        java.io.File blastOutput = runBlast(fileGenome16S, fileSanger16S);

        // process output
        BufferedReader infile = IO.openReader(blastOutput.getPath());
        String buffer = null;
        HashMap<String,HashSet<String>> goodMatches = new HashMap<String,HashSet<String>>();
        HashMap<String,HashSet<String>> badMatches = new HashMap<String,HashSet<String>>();
        while ((buffer = infile.readLine()) != null) {
            // qseqid sseqid length nident qstart qseq sstart sseq
            String[] fields = buffer.split("\t");
            if (fields.length < 5)
                continue;
            String genomeIDNum = fields[0];
            int pos = genomeIDNum.lastIndexOf("-");
            String genomeID = genomeIDNum.substring(0,pos);
            String sangerID = fields[1];
            int length = StringUtil.atoi(fields[2]);
            int nID = StringUtil.atoi(fields[3]);
            double pctID = (double)nID / (double)length * 100.0;
            if ((length >= 500) && (pctID >= 99.0))
                addMatch(goodMatches, genomeID, sangerID);
            else if (length >= 200)
                addMatch(badMatches, genomeID, sangerID);
        }
        infile.close();

        // find all good genomes (matching only one 16S with same label)
        HashSet<String> remainingIsolates = new HashSet<String>();
        remainingIsolates.addAll(isolatesWithGenome16S);
        for (String isolateID: isolatesWithGenome16S) {
            boolean foundMatch = false;
            boolean foundMismatch = false;
            HashSet<String> matches = goodMatches.get(isolateID);
            if (matches != null) {
                for (String matchID: matches) {
                    if (matchID.equals(isolateID))
                        foundMatch = true;
                    else
                        foundMismatch = true;
                }
            }
            if (foundMatch) {
                remainingIsolates.remove(isolateID);
                if (!foundMismatch) {
                    System.out.println("Confirmed isolate "+isolateID);
                    reportText += "Confirmed isolate "+isolateID+"\n";
                }
                else {
                    System.out.println("Ambiguous isolate "+isolateID+" "+goodMatches.get(isolateID).toString());
                    reportText += "Ambiguous isolate "+isolateID+" "+goodMatches.get(isolateID).toString()+"\n";
                }
            }
            else if (foundMismatch) {
                System.out.println("Mismatched isolate "+isolateID+" "+goodMatches.get(isolateID).toString());
                reportText += "Mismatched isolate "+isolateID+" "+goodMatches.get(isolateID).toString()+"\n";
            }
        }

        for (String isolateID: isolatesWithGenome16S) {
            if (remainingIsolates.contains(isolateID)) {
                boolean foundMatch = false;
                boolean foundMismatch = false;
                HashSet<String> matches = badMatches.get(isolateID);
                if (matches != null) {
                    for (String matchID: matches) {
                        if (matchID.equals(isolateID))
                            foundMatch = true;
                        else
                            foundMismatch = true;
                    }
                }
                if (foundMatch) {
                    remainingIsolates.remove(isolateID);
                    if (!foundMismatch) {
                        System.out.println("Confirmed (low conf) isolate "+isolateID);
                        reportText += "Confirmed (low conf) isolate "+isolateID+"\n";
                    }
                    else {
                        System.out.println("Ambiguous (low conf) isolate "+isolateID+" "+badMatches.get(isolateID).toString());
                        reportText += "Ambiguous (low conf) isolate "+isolateID+" "+badMatches.get(isolateID).toString()+"\n";
                    }
                }
                else if (foundMismatch) {
                    System.out.println("Mismatched (low conf) isolate "+isolateID+" "+badMatches.get(isolateID).toString());
                    reportText += "Mismatched (low conf) isolate "+isolateID+" "+badMatches.get(isolateID).toString()+"\n";
                }
            }
        }

        for (String isolateID: remainingIsolates) {
            System.out.println("No hits for isolate "+isolateID);
            reportText += "No hits for isolate "+isolateID+"\n";
        }

        // save blast output in report
        ArrayList<kbasereport.File> files = new ArrayList<kbasereport.File>();
        files.add(new kbasereport.File()
                  .withPath(blastOutput.getPath())
                  .withDescription("BLAST output")
                  .withName(blastOutput.getName()));

        ReportInfo ri = makeReport(token,
                                   wsName,
                                   files,
                                   reportText);
        
        ReportResults rv = new ReportResults()
            .withReportName(ri.getName())
            .withReportRef(ri.getRef());
        
        return rv;
    }
}
