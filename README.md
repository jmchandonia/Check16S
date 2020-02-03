# Check16S

---

This checks that the 16S sequences obtained by Sanger sequencing
for a bunch of isolates actually match the 16S sequences in the final
assembled and annotated genomes.  It checks that there are no duplicate
16S sequences within one isolate (possibly indicating contamination),
and that there are no matches to unexpected 16S sequences from the
same batch of isolates (possibly indicating a mixup on the plate).

This uses BLASTN to compare the file of expected (i.e., Sanger-derived)
16S sequences to annotated 16S sequences (including partial ones)
in all of the genomes in the genome set.

The names of the isolates must be encoded in the files: for the 16S
sequences (uploaded to KBase as an Assembly object), each sequence
must have only the isolate name in the FASTA header.  Each genome in
the genome set of all isolates must be named "isolate_name.genome".

Recommended protocol from Morgan Price:

Verify that the genome includes a nearly-identical match to the 16S
sequence (say, above 99% across >500 nt)

Verify that the genome does NOT contain any annotated 16S sequences
that are divergent (say, under 99% identity across >200 nt)

This will still leave some grey areas if a sequence is not-quite identical.
This can be legitimate if there are errors in the original Sanger
sequence. In this case -- the erroneous Sanger sequence should not be more
similar to the 16S in other genomes from this batch.  A related issue is if
there are Ns in the Sanger sequence, these will usually be scored as
mismatches, which is not useful here.
