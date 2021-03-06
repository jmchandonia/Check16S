
package genomefileutil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * <p>Original spec-file type: GenomeFeaturesToFastaParams</p>
 * <pre>
 * Produce a FASTA file with the nucleotide sequences of features in a genome.
 * string genome_ref: reference to a genome object
 * list<string> feature_lists: Optional, which features lists (features, mrnas, cdss, non_coding_features) to provide sequences. Defaults to "features".
 * list<string> filter_ids: Optional, if provided only return sequences for matching features.
 * boolean include_functions: Optional, add function to header line. Defaults to True.
 * boolean include_aliases: Optional, add aliases to header line. Defaults to True.
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "genome_ref",
    "feature_lists",
    "filter_ids",
    "include_functions",
    "include_aliases"
})
public class GenomeFeaturesToFastaParams {

    @JsonProperty("genome_ref")
    private java.lang.String genomeRef;
    @JsonProperty("feature_lists")
    private List<String> featureLists;
    @JsonProperty("filter_ids")
    private List<String> filterIds;
    @JsonProperty("include_functions")
    private Long includeFunctions;
    @JsonProperty("include_aliases")
    private Long includeAliases;
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();

    @JsonProperty("genome_ref")
    public java.lang.String getGenomeRef() {
        return genomeRef;
    }

    @JsonProperty("genome_ref")
    public void setGenomeRef(java.lang.String genomeRef) {
        this.genomeRef = genomeRef;
    }

    public GenomeFeaturesToFastaParams withGenomeRef(java.lang.String genomeRef) {
        this.genomeRef = genomeRef;
        return this;
    }

    @JsonProperty("feature_lists")
    public List<String> getFeatureLists() {
        return featureLists;
    }

    @JsonProperty("feature_lists")
    public void setFeatureLists(List<String> featureLists) {
        this.featureLists = featureLists;
    }

    public GenomeFeaturesToFastaParams withFeatureLists(List<String> featureLists) {
        this.featureLists = featureLists;
        return this;
    }

    @JsonProperty("filter_ids")
    public List<String> getFilterIds() {
        return filterIds;
    }

    @JsonProperty("filter_ids")
    public void setFilterIds(List<String> filterIds) {
        this.filterIds = filterIds;
    }

    public GenomeFeaturesToFastaParams withFilterIds(List<String> filterIds) {
        this.filterIds = filterIds;
        return this;
    }

    @JsonProperty("include_functions")
    public Long getIncludeFunctions() {
        return includeFunctions;
    }

    @JsonProperty("include_functions")
    public void setIncludeFunctions(Long includeFunctions) {
        this.includeFunctions = includeFunctions;
    }

    public GenomeFeaturesToFastaParams withIncludeFunctions(Long includeFunctions) {
        this.includeFunctions = includeFunctions;
        return this;
    }

    @JsonProperty("include_aliases")
    public Long getIncludeAliases() {
        return includeAliases;
    }

    @JsonProperty("include_aliases")
    public void setIncludeAliases(Long includeAliases) {
        this.includeAliases = includeAliases;
    }

    public GenomeFeaturesToFastaParams withIncludeAliases(Long includeAliases) {
        this.includeAliases = includeAliases;
        return this;
    }

    @JsonAnyGetter
    public Map<java.lang.String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(java.lang.String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public java.lang.String toString() {
        return ((((((((((((("GenomeFeaturesToFastaParams"+" [genomeRef=")+ genomeRef)+", featureLists=")+ featureLists)+", filterIds=")+ filterIds)+", includeFunctions=")+ includeFunctions)+", includeAliases=")+ includeAliases)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
