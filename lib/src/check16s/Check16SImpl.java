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
import com.fasterxml.jackson.databind.node.*;
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
       Filter gaps out of assembly, make new file in base dir
    */
    public static java.io.File filterAssembly(java.io.File originalFile,
                                              String filteredFileName,
                                              java.io.File baseDir) throws Exception {

        java.io.File rv = new java.io.File(baseDir.getPath()+"/"+filteredFileName);
        
        PrintfStream outfile = new PrintfStream(rv.getPath());
        BufferedReader seqs = IO.openReader(originalFile.getPath());
        PolymerSet ps = new PolymerSet();
        Enumeration pe = ps.polymersInFile(seqs, null);
        while ((pe.hasMoreElements() && (seqs.ready()))) {
            Polymer p;
            p = (Polymer)pe.nextElement();
            
            if (p==null)
                continue;
            
            p.stripGaps();
            p.stripType('-');
            p.writeFasta(outfile);
        }
        seqs.close();
        outfile.close();
        return rv;
    }

    /**
       Download genomeset and return a map of genome names to references
    */
    public static HashMap<String,String> downloadGenomeset(AuthToken token,
                                                           String genomesetRef) throws Exception {

        HashMap<String,String> rv = new HashMap<String,String>();
        
        // load genomeset
        DataFileUtilClient dfuClient = new DataFileUtilClient(new URL(System.getenv("SDK_CALLBACK_URL")), token);
        dfuClient.setIsInsecureHttpConnectionAllowed(true);

        GetObjectsResults res = dfuClient.getObjects(new GetObjectsParams()
                                                     .withObjectRefs(Arrays.asList(genomesetRef)));
        JsonNode genomeset = res.getData().get(0).getData().asJsonNode();
        System.out.println("Got GenomeSet");
        // System.out.println("Debug: "+genomeset.toString());

        // loop over genomes and find all their names and references
        for (Iterator<String> iter = genomeset.get("elements").fieldNames(); iter.hasNext(); ) {
            JsonNode n = genomeset.get("elements").get(iter.next());
            String genomeRef = n.get("ref").textValue();

            res = dfuClient.getObjects(new GetObjectsParams()
                                       .withObjectRefs(Arrays.asList(genomeRef)));
            String genomeName = res.getData().get(0).getInfo().getE2();

            rv.put(genomeName,genomeRef);
        }
        return rv;
    }

    /**
       save a genomeset to a workspace
    */
    public static WorkspaceObject saveGenomeset(AuthToken token,
                                                Long wsId,
                                                String objectName,
                                                Set<String> isolates,
                                                Map<String,String> genomesetMap,
                                                String description) throws Exception {
        DataFileUtilClient dfuClient = new DataFileUtilClient(new URL(System.getenv("SDK_CALLBACK_URL")), token);
        dfuClient.setIsInsecureHttpConnectionAllowed(true);

        // build genomeset out of set of isolates
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode genomeset = objectMapper.createObjectNode();
        genomeset.put("description",description);
        ObjectNode elements = objectMapper.createObjectNode();
        genomeset.put("elements",elements);
        for (String isolate : isolates) {
            String genomeRef = genomesetMap.get(isolate+".genome");
            ObjectNode n = objectMapper.createObjectNode();
            ObjectNode refNode = n.putObject(genomeRef);
            refNode.put("ref",genomeRef);
            elements.put(genomeRef,refNode);
        }
        System.out.println("Debug2: "+genomeset.toString());

        // upload using dfuclient
        List<Tuple11<Long, String, String, String, Long, String, Long, String, String, Long, Map<String,String>>>
            res = dfuClient.saveObjects(new SaveObjectsParams()
                                        .withId(wsId)
                                        .withObjects(Arrays.asList(new ObjectSaveData()
                                                                   .withName(objectName)
                                                                   .withType("KBaseSearch.GenomeSet")
                                                                   .withData(UObject.fromJsonString(UObject.transformJacksonToString(genomeset))))));

        String ref = getRefFromObjectInfo(res.get(0));
        return (new WorkspaceObject()
                .withRef(ref)
                .withDescription(description));
    }

    /**
       helper function to get ref from object info
    */
    public static String getRefFromObjectInfo(Tuple11<Long, String, String, String, Long, String, Long, String, String, Long, Map<String,String>> info) {
        return info.getE7() + "/" + info.getE1() + "/" + info.getE5();
    }

    /**
       extract 16S sequences from a GenomeSet, saving them
       in a fasta file.  Checks their names are OK from list of
       isolates we have Sanger seqs for.
    */
    public static java.io.File download16S(AuthToken token,
                                           String fileName,
                                           java.io.File baseDir,
                                           HashMap<String,String> genomesetMap) throws Exception {
        GenomefileutilClient gfuClient = new GenomefileutilClient(new URL(System.getenv("SDK_CALLBACK_URL")), token);
        gfuClient.setIsInsecureHttpConnectionAllowed(true);

        // make a FASTA file to save all the 16S sequences into
        java.io.File rv = new java.io.File(baseDir.getPath()+"/"+fileName);
        
        PrintfStream outfile = new PrintfStream(rv.getPath());

        // loop over genomes and find all their annotated 16S sequences
        for (String genomeName: genomesetMap.keySet()) {
            String genomeRef = genomesetMap.get(genomeName);
                
            int pos = genomeName.lastIndexOf(".genome");
            if (pos==-1)
                throw new Exception("Genome name '"+genomeName+"' is not in the form of isolate_name.genome");
            String isolateID = genomeName.substring(0,pos);
            System.out.println("  Found isolate "+isolateID);

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
                    p.name = isolateID+"-"+(n16S++);
                    // strip any gaps already in the sequence
                    p.stripGaps();
                    p.stripType('-');
                    // and save it in the "genome 16S" file
                    p.writeFasta(outfile);
                }
            }
            seqs.close();
            java.io.File fileFA = new java.io.File(pathFA);
            fileFA.delete();

            // warn about not enough or too many 16S found
            if (n16S == 0)
                System.out.println("  Warning - no 16S annotated for "+isolateID);
            else if (n16S > 1)
                System.out.println("  Warning - "+n16S+" different 16S annotated for "+isolateID);
        }
        outfile.close();
        return rv;
    }

    /**
       extract 16S sequences of isolates that have multiple 16S
       sequences per genome.
       Returns null if none do.
    */
    public static java.io.File getMulti16S(java.io.File fileGenome16S) throws Exception {
        // first pass: get list of isolates with multi 16S
        BufferedReader seqs = IO.openReader(fileGenome16S.getPath());
        PolymerSet ps = new PolymerSet();
        Enumeration pe = ps.polymersInFile(seqs, null);
        HashSet<String> isolatesWithMulti = new HashSet<String>();
        while ((pe.hasMoreElements() && (seqs.ready()))) {
            Polymer p;
            p = (Polymer)pe.nextElement();
            
            if (p==null)
                continue;
            
            String seqName = p.name;
            if (seqName.endsWith("-1")) {
                isolatesWithMulti.add(seqName.substring(0,seqName.length()-2));
            }
        }
        seqs.close();

        // return null if no isolates had multiple 16S.
        if (isolatesWithMulti.size() == 0)
            return null;

        // make a FASTA file to save all the multi-16S sequences into
        java.io.File baseDir = fileGenome16S.getParentFile();
        java.io.File fileMulti16S = java.io.File.createTempFile("multi16s",".fa",
                                                                baseDir);
        fileMulti16S.delete();
        PrintfStream outfile = new PrintfStream(fileMulti16S.getPath());

        // second pass: pull out their sequences
        seqs = IO.openReader(fileGenome16S.getPath());
        ps = new PolymerSet();
        pe = ps.polymersInFile(seqs, null);        
        while ((pe.hasMoreElements() && (seqs.ready()))) {
            Polymer p;
            p = (Polymer)pe.nextElement();
            
            if (p==null)
                continue;
            
            String seqName = p.name;
            String isolateID = seqName.substring(0,seqName.length()-2);
            if (isolatesWithMulti.contains(isolateID)) {
                p.writeFasta(outfile);
            }
        }
        seqs.close();

        outfile.close();
        return fileMulti16S;
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
                                        java.io.File db,
                                        String fileName) throws Exception {
        java.io.File baseDir = query.getParentFile();
        java.io.File rv = new java.io.File(baseDir.getPath()+"/"+fileName);
        
        String programName = "blastn";
        Program p = new Program(programName);
        String[] i = new String[12];
        i[0] = "-query";
        i[1] = query.getPath();
        i[2] = "-db";
        i[3] = db.getPath();
        i[4] = "-out";
        i[5] = rv.getPath();
        i[6] = "-outfmt";
        i[7] = "6 qseqid sseqid length nident qstart qseq sstart sseq";
        i[8] = "-dust";
        i[9] = "no";
        i[10] = "-soft_masking";
        i[11] = "false";
        p.run(i, null, baseDir);

        return rv;
    }

    /**
       add a match
    */
    public static void addMatch(Map<String,HashSet<String>> map,
                                String key,
                                String value) {
        HashSet<String> values = map.get(key);
        if (values==null)
            values = new HashSet<String>();
        values.add(value);
        map.put(key,values);
    }

    /**
       get map of sequence ids to sequences in a file
    */
    public static HashMap<String,String> getSeqs(java.io.File f) throws Exception {
        HashMap<String,String> rv = new HashMap<String,String>();
        BufferedReader seqs = IO.openReader(f.getPath());
        PolymerSet ps = new PolymerSet();
        Enumeration pe = ps.polymersInFile(seqs, null);
        while ((pe.hasMoreElements() && (seqs.ready()))) {
            Polymer p;
            p = (Polymer)pe.nextElement();
            
            if (p==null)
                continue;
            
            String seqName = p.name;
            rv.put(seqName,p.sequence());
        }
        seqs.close();
        return rv;
    }

    /**
       Make and save Report object, returning its name and reference
    */
    public static ReportInfo makeReport(AuthToken token,
                                        String wsName,
                                        List<WorkspaceObject> objects,
                                        List<kbasereport.File> files,
                                        String reportText) throws Exception {
        KBaseReportClient reportClient = new KBaseReportClient(new URL(System.getenv("SDK_CALLBACK_URL")), token);
        reportClient.setIsInsecureHttpConnectionAllowed(true);

        String reportName = "check16s_report_"+UUID.randomUUID().toString();
        return reportClient.createExtendedReport(new CreateExtendedReportParams()
                                                 .withWorkspaceName(wsName)
                                                 .withMessage(reportText)
                                                 .withReportObjectName(reportName)
                                                 .withObjectsCreated(objects)
                                                 .withFileLinks(files));
    }

    /**
       number of Ns
    */
    public static int nN(String s) {
        int rv = 0;
        for (int i=0; i < s.length(); i++)
            if (s.charAt(i)=='N')
                rv++;
        return rv;
    }

    /**
       number of identical residues, counting Ns as matches, except to
       gaps
    */
    public static int nIDN(String s1, String s2) {
        if (s1.length() != s2.length())
            throw new IllegalArgumentException("string lengths don't match");
        int rv = 0;
        for (int i=0; i < s1.length(); i++)
            if ((s1.charAt(i)==s2.charAt(i)) ||
                ((s1.charAt(i)=='N') && (s2.charAt(i)!='-')) ||
                (s2.charAt(i)=='N')&& (s1.charAt(i)!='-'))
                rv++;
        return rv;
    }

    /**
       process blast output into a map of queries to hits, with at least
       500 bp length required per hit.  Each hit is encoded as a string:
       hit-id length nSNPs
    */
    public static HashMap<String,HashSet<String>> processBlast(java.io.File blastOutput) throws Exception {
        BufferedReader infile = IO.openReader(blastOutput.getPath());
        String buffer = null;
        HashMap<String,HashSet<String>> rv = new HashMap<String,HashSet<String>>();
        while ((buffer = infile.readLine()) != null) {
            // qseqid sseqid length nident qstart qseq sstart sseq
            String[] fields = buffer.split("\t");
            if (fields.length < 5)
                continue;
            String queryID = fields[0];
            String hitID = fields[1];
            int length = StringUtil.atoi(fields[2]);
            if (length >= 500) {
                // int nID = StringUtil.atoi(fields[3]);
                int nIDN = nIDN(fields[5],fields[7]);
                int nSNPs = length-nIDN;
                // double pctID = (double)nIDN / (double)length * 100.0;
                addMatch(rv, queryID, hitID+" "+length+" "+nSNPs);
            }
        }
        infile.close();
        return rv;
    }

    /**
       check 16S vs expected sequences
    */
    public static ReportResults check16S(AuthToken token,
                                         java.io.File scratchDir,
                                         Check16SInput input) throws Exception {
        String genomesetRef = input.getGenomesetRef();
        String assemblyRef = input.getAssemblyRef();
        int maxDiff = input.getMaxDifferences().intValue();
        int minSigDiff = input.getMinSignificantDifferences().intValue();
        String wsName = input.getWsName();
        Long wsId = input.getWsId();
        String reportText = "";

        // make a temporary directory for results
        java.io.File baseDir = java.io.File.createTempFile("dir","",
                                                           scratchDir);
        baseDir.delete();
        baseDir.mkdirs();

        // dump out 16S sequences to fasta file
        java.io.File fileSanger16SUF = downloadAssembly(token,
                                                        assemblyRef);
        System.out.println("Got Sanger 16S sequences as "+fileSanger16SUF.getPath());
        java.io.File fileSanger16S = filterAssembly(fileSanger16SUF,
                                                    "sanger16S.fa",
                                                    baseDir);
        
        System.out.println("Filtered Sanger 16S sequences as "+fileSanger16S.getPath());

        // keep track of the isolates with Sanger 16S sequences
        Map<String,String> sanger16S = getSeqs(fileSanger16S);
        Set<String> isolatesWithSanger16S = sanger16S.keySet();

        // download genomeset to hashmap
        HashMap<String,String> genomesetMap = downloadGenomeset(token,
                                                                genomesetRef);

        // dump out the 16S sequences from the genomeset to a fasta file
        java.io.File fileGenome16S = download16S(token,
                                                 "genome16S.fa",
                                                 baseDir,
                                                 genomesetMap);
        System.out.println("Got genome 16S sequences as "+fileGenome16S.getPath());

        // copy isolates with multiple genome 16S into another file
        java.io.File fileMulti16S = getMulti16S(fileGenome16S);
        if (fileMulti16S == null) {
            System.out.println("No genomes had multiple 16S sequences");
        }
        else {
            System.out.println("Got multi 16S sequences as "+fileMulti16S.getPath());
        }

        // keep track of isolates with genome 16S sequences,
        // and how many
        Map<String,String> genome16S = getSeqs(fileGenome16S);
        Set<String> isolatesWithGenome16S = new HashSet<String>();
        Map<String,Integer> numGenome16S = new HashMap<String,Integer>();
        for (String isolateIDNum : genome16S.keySet()) {
            int pos = isolateIDNum.lastIndexOf("-");
            String isolateID = isolateIDNum.substring(0,pos);
            int n16S = StringUtil.atoi(isolateIDNum.substring(pos+1))+1;
            numGenome16S.put(isolateID,new Integer(n16S));

            if (!isolatesWithGenome16S.contains(isolateID)) {
                isolatesWithGenome16S.add(isolateID);

                // check whether match is possible
                String seq = genome16S.get(isolateIDNum);
                int n = nN(seq);
                if (seq.length() < 500) {
                    System.out.println("Warning: "+isolateID+" genome 16S is too short to validate; length "+seq.length());
                    reportText += "Warning: "+isolateID+" genome 16S is too short to validate; length "+seq.length();
                }
                else if (seq.length()-n < 500) {
                    System.out.println("Warning: "+isolateID+" genome 16S has too many Ns to validate well; length "+seq.length()+", nN "+n);
                    reportText += "Warning: "+isolateID+" genome 16S has too many Ns to validate well; length "+seq.length()+", nN "+n;
                }
                seq = sanger16S.get(isolateID);
                if (seq==null) {
                    System.out.println("Warning: "+isolateID+" Sanger 16S not provided in Sanger set");
                    reportText += "Warning: "+isolateID+" Sanger 16S not provided in Sanger set";
                }
                else {
                    n = nN(seq);
                    if (seq.length() < 500) {
                        System.out.println("Warning: "+isolateID+" Sanger 16S is too short to validate; length "+seq.length());
                        reportText += "Warning: "+isolateID+" Sanger 16S is too short to validate; length "+seq.length();
                    }
                    else if (seq.length()-n < 500) {
                        System.out.println("Warning: "+isolateID+" Sanger 16S has too many Ns to validate well; length "+seq.length()+", nN "+n);
                        reportText += "Warning: "+isolateID+" Sanger 16S has too many Ns to validate well; length "+seq.length()+", nN "+n;
                    }
                }
            }
            else {
                System.out.println("Warning - "+isolateID+" has multiple 16S annotated");
                reportText += "Warning - "+isolateID+" has multiple 16S annotated";
            }
        }

        // warn about isolates with missing 16S in genome
        for (String genomeName : genomesetMap.keySet()) {
            int pos = genomeName.lastIndexOf(".genome");
            if (pos==-1)
                throw new Exception("Genome name '"+genomeName+"' is not in the form of isolate_name.genome");
            String isolateID = genomeName.substring(0,pos);
            if (!isolatesWithGenome16S.contains(isolateID)) {
                System.out.println("Warning - "+isolateID+" has no 16S annotated");
                reportText += "Warning - "+isolateID+" has no 16S annotated";
                numGenome16S.put(isolateID,new Integer(0));
            }
        }

        // setup blast
        System.out.println("Setting up BLAST");
        formatDB(fileSanger16S);

        // run blast
        System.out.println("Running BLAST");
        java.io.File blastOutput = runBlast(fileGenome16S, fileSanger16S, "genome-vs-sanger-blast.tsv");

        // process output
        Map<String,HashSet<String>> matches = processBlast(blastOutput);

        // find snps to same and other genomes
        HashMap<String,Integer> maxExpectedSNPs = new HashMap<String,Integer>();
        HashMap<String,Integer> minOtherSangerSNPs = new HashMap<String,Integer>();
        HashMap<String,Integer> bestOtherSangerLength = new HashMap<String,Integer>();
        HashMap<String,String> bestOtherSanger = new HashMap<String,String>();
        HashMap<String,Integer> minOtherGenomeSNPs = new HashMap<String,Integer>();
        HashMap<String,Integer> bestOtherGenomeLength = new HashMap<String,Integer>();
        HashMap<String,String> bestOtherGenome = new HashMap<String,String>();
        
        for (String isolateID: isolatesWithGenome16S) {
            int n = numGenome16S.get(isolateID).intValue();
            for (int i=0; i<n; i++) {
                String isolateIDNum = isolateID+"-"+i;
            
                HashSet<String> hits = matches.get(isolateIDNum);
                if (hits != null)
                    for (String hit: hits) {
                        int pos = hit.lastIndexOf(" ");
                        int nSNPs = StringUtil.atoi(hit.substring(pos+1));
                        if (hit.startsWith(isolateID+" ")) {
                            // matches expected isolate
                            Integer curMax = maxExpectedSNPs.get(isolateID);
                            if ((curMax == null) ||
                                (nSNPs > curMax))
                                maxExpectedSNPs.put(isolateID, new Integer(nSNPs));
                        }
                        else {
                            // matches another isolate
                            int pos2 = hit.indexOf(" ");
                            String otherIsolate = hit.substring(0,pos2);
                            int len = StringUtil.atoi(hit.substring(pos2+1,pos));
                            Integer curMin = minOtherSangerSNPs.get(isolateID);
                            Integer curLen = bestOtherSangerLength.get(isolateID);
                            if ((curMin == null) ||
                                (nSNPs < curMin) ||
                                ((nSNPs == curMin) && (len > curLen))) {
                                minOtherSangerSNPs.put(isolateID, new Integer(nSNPs));
                                bestOtherSangerLength.put(isolateID, new Integer(len));
                                bestOtherSanger.put(isolateID, otherIsolate);
                            }

                            // do the same comparison in the other direction
                            curMin = minOtherGenomeSNPs.get(otherIsolate);
                            curLen = bestOtherGenomeLength.get(otherIsolate);
                            if ((curMin == null) ||
                                (nSNPs < curMin) ||
                                ((nSNPs == curMin) && (len > curLen))) {
                                minOtherGenomeSNPs.put(otherIsolate, new Integer(nSNPs));
                                bestOtherGenomeLength.put(otherIsolate, new Integer(len));
                                bestOtherGenome.put(otherIsolate, isolateID);
                            }
                        }
                    }
            }
        }

        // do similar process for multi-16S genomes
        java.io.File blastOutputMulti = null;
        // maximum number of snps between multiple 16S seqs in same genome
        HashMap<String,Integer> maxMultiSNPs = new HashMap<String,Integer>();
        if (fileMulti16S != null) {
            System.out.println("Setting up BLAST for multi-16S genomes");
            formatDB(fileMulti16S);

            // run blast
            System.out.println("Running BLAST for multi-16S genomes");
            blastOutputMulti = runBlast(fileMulti16S, fileMulti16S, "genome-multi-16s-blast.tsv");

            // process output
            Map<String,HashSet<String>> matchesMulti = processBlast(blastOutputMulti);

            // find max SNPs between 16S from the same isolate
            for (String isolateID: isolatesWithGenome16S) {
                int n = numGenome16S.get(isolateID).intValue();
                if (n == 1)
                    continue;
                int maxSNPs = 0;
                for (int i=0; i<n; i++) {
                    String isolateIDNum = isolateID+"-"+i;
                    HashSet<String> hits = matches.get(isolateIDNum);
                    if (hits != null)
                        for (String hit: hits)
                            if (hit.startsWith(isolateID+"-")) {
                                int pos = hit.lastIndexOf(" ");
                                int nSNPs = StringUtil.atoi(hit.substring(pos+1));
                                if (nSNPs > maxSNPs)
                                    maxSNPs = nSNPs;
                            }
                }
                maxMultiSNPs.put(isolateID,new Integer(maxSNPs));
            }
        }

        // make lists of isolates to put into each genomeset
        HashSet<String> isolatesPassed = new HashSet<String>();
        HashSet<String> isolatesFailed = new HashSet<String>();
        HashSet<String> isolatesUnknown = new HashSet<String>();

        // build table of where each isolate ends up
        String reportTSV = "isolate_name\thighest_num_snps_with_expected_16S\tlowest_num_snps_with_other_sanger_16S\tother_sanger_16S\tlowest_num_snps_with_other_genome_16S\tother_genome_16S\tnum_genome_16S\tmax_num_snps_between_genome_16S\toutput_set\n";
        for (String genomeName : genomesetMap.keySet()) {
            String set = null;
            String reason = "";
            
            int pos = genomeName.lastIndexOf(".genome");
            if (pos==-1)
                throw new Exception("Genome name '"+genomeName+"' is not in the form of isolate_name.genome");
            String isolateID = genomeName.substring(0,pos);

            // first, check that a hit is possible
            if (!isolatesWithGenome16S.contains(isolateID)) {
                set = "unknown";
                reason = "no 16S annotated in genome";
            }
            else if (!isolatesWithSanger16S.contains(isolateID)) {
                set = "unknown";
                reason = "no Sanger 16S provided";
            }

            // check hits and build report table
            reportTSV += isolateID+"\t";
            Integer ii = maxExpectedSNPs.get(isolateID);
            int expectedSNPs = -1;
            if (ii==null)
                reportTSV += "n/a\t";
            else {
                reportTSV += ii+"\t";
                expectedSNPs = ii.intValue();
                if ((expectedSNPs > maxDiff) && (set==null)) {
                    set = "fail";
                    reason = expectedSNPs+" SNPs found with Sanger sequence";
                }
            }

            ii = minOtherSangerSNPs.get(isolateID);
            int otherSNPs = -1;
            if (ii==null) {
                reportTSV += "n/a\tn/a\t";
                if ((expectedSNPs > -1) &&
                    (expectedSNPs <= maxDiff) &&
                    (set==null)) {
                    set = "pass";
                    reason = expectedSNPs+" SNPs found with Sanger sequence";
                }
            }
            else {
                reportTSV += ii+"\t"+bestOtherSanger.get(isolateID)+"\t";
                if (set==null) {
                    otherSNPs = ii.intValue();
                    if (expectedSNPs == -1)
                        set = "fail";
                    else {
                        if ((expectedSNPs <= maxDiff) &&
                            (expectedSNPs <= otherSNPs + minSigDiff)) {
                            set = "pass";
                            reason = expectedSNPs+" SNPs found with Sanger sequence";
                        }
                        else {
                            set = "fail";
                            reason = expectedSNPs+" SNPs found with Sanger sequence, "+otherSNPs+" found with "+bestOtherSanger.get(isolateID);
                        }
                    }
                }
            }

            ii = minOtherGenomeSNPs.get(isolateID);
            if (ii==null)
                reportTSV += "n/a\tn/a\t";
            else
                reportTSV += ii+"\t"+bestOtherGenome.get(isolateID)+"\t";

            ii = numGenome16S.get(isolateID);
            reportTSV += ii+"\t";

            ii = maxMultiSNPs.get(isolateID);
            if (ii==null)
                reportTSV += "n/a\t";
            else {
                reportTSV += ii+"\t";
                if (ii >= minSigDiff) {
                    set = "fail";
                    reason = "multiple divergent 16S ("+ii+" SNPs) found";
                }
            }

            if (set==null) {
                set = "unknown";
                reason = "not enough data";
            }

            reportTSV += set+"\n";
        
            if (set.equals("pass")) {
                System.out.println("Confirmed isolate "+isolateID+" "+reason);
                reportText += "Confirmed isolate "+isolateID+" "+reason+"\n";
                isolatesPassed.add(isolateID);
            }
            else if (set.equals("fail")) {
                System.out.println("Failed isolate "+isolateID+" "+reason);
                reportText += "Failed isolate "+isolateID+" "+reason+"\n";
                isolatesFailed.add(isolateID);
            }
            else {
                System.out.println("Unclear isolate "+isolateID+" "+reason);
                reportText += "Unclear isolate "+isolateID+" "+reason+"\n";
                isolatesUnknown.add(isolateID);
            }
        }

        // save the output genomesets
        List<WorkspaceObject> objects = new ArrayList<WorkspaceObject>();
        if (isolatesPassed.size() > 0) {
            objects.add(saveGenomeset(token,
                                      wsId,
                                      input.getOutputGenomesetPass(),
                                      isolatesPassed,
                                      genomesetMap,
                                      "Isolates that passed 16S checks"));
        }
        if (isolatesFailed.size() > 0) {
            objects.add(saveGenomeset(token,
                                      wsId,
                                      input.getOutputGenomesetFail(),
                                      isolatesFailed,
                                      genomesetMap,
                                      "Isolates that failed 16S checks"));
        }
        if (isolatesUnknown.size() > 0) {
            objects.add(saveGenomeset(token,
                                      wsId,
                                      input.getOutputGenomesetUnknown(),
                                      isolatesUnknown,
                                      genomesetMap,
                                      "Isolates that were unclear in 16S checks"));
        }

        // save the TSV report to a file
        java.io.File tsvFile = new java.io.File(baseDir.getPath()+"/report.tsv");
        BufferedWriter outfile = new BufferedWriter(new FileWriter(tsvFile));
        outfile.write(reportTSV);
        outfile.close();

        // save files in report
        ArrayList<kbasereport.File> files = new ArrayList<kbasereport.File>();
        files.add(new kbasereport.File()
                  .withPath(tsvFile.getPath())
                  .withDescription("TSV report")
                  .withLabel("TSV report")
                  .withName(tsvFile.getName()));

        files.add(new kbasereport.File()
                  .withPath(fileGenome16S.getPath())
                  .withDescription("Genome 16S sequences")
                  .withLabel("Genome 16S sequences")
                  .withName(fileGenome16S.getName()));

        files.add(new kbasereport.File()
                  .withPath(blastOutput.getPath())
                  .withDescription("BLAST output")
                  .withLabel("BLAST output")
                  .withName(blastOutput.getName()));

        if (blastOutputMulti != null) {
            files.add(new kbasereport.File()
                      .withPath(blastOutputMulti.getPath())
                      .withDescription("BLAST output for genomes with multiple 16S sequences")
                      .withLabel("BLAST output for genomes with multiple 16S sequences")
                      .withName(blastOutputMulti.getName()));
        }
        
        ReportInfo ri = makeReport(token,
                                   wsName,
                                   objects,
                                   files,
                                   reportText);
        
        ReportResults rv = new ReportResults()
            .withReportName(ri.getName())
            .withReportRef(ri.getRef());
        
        return rv;
    }
}
