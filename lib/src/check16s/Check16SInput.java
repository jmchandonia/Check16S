
package check16s;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * <p>Original spec-file type: Check16SInput</p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "genomeset_ref",
    "assembly_ref",
    "ws_name",
    "ws_id",
    "max_differences",
    "min_significant_differences",
    "output_genomeset_pass",
    "output_genomeset_fail",
    "output_genomeset_unknown"
})
public class Check16SInput {

    @JsonProperty("genomeset_ref")
    private String genomesetRef;
    @JsonProperty("assembly_ref")
    private String assemblyRef;
    @JsonProperty("ws_name")
    private String wsName;
    @JsonProperty("ws_id")
    private Long wsId;
    @JsonProperty("max_differences")
    private Long maxDifferences;
    @JsonProperty("min_significant_differences")
    private Long minSignificantDifferences;
    @JsonProperty("output_genomeset_pass")
    private String outputGenomesetPass;
    @JsonProperty("output_genomeset_fail")
    private String outputGenomesetFail;
    @JsonProperty("output_genomeset_unknown")
    private String outputGenomesetUnknown;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("genomeset_ref")
    public String getGenomesetRef() {
        return genomesetRef;
    }

    @JsonProperty("genomeset_ref")
    public void setGenomesetRef(String genomesetRef) {
        this.genomesetRef = genomesetRef;
    }

    public Check16SInput withGenomesetRef(String genomesetRef) {
        this.genomesetRef = genomesetRef;
        return this;
    }

    @JsonProperty("assembly_ref")
    public String getAssemblyRef() {
        return assemblyRef;
    }

    @JsonProperty("assembly_ref")
    public void setAssemblyRef(String assemblyRef) {
        this.assemblyRef = assemblyRef;
    }

    public Check16SInput withAssemblyRef(String assemblyRef) {
        this.assemblyRef = assemblyRef;
        return this;
    }

    @JsonProperty("ws_name")
    public String getWsName() {
        return wsName;
    }

    @JsonProperty("ws_name")
    public void setWsName(String wsName) {
        this.wsName = wsName;
    }

    public Check16SInput withWsName(String wsName) {
        this.wsName = wsName;
        return this;
    }

    @JsonProperty("ws_id")
    public Long getWsId() {
        return wsId;
    }

    @JsonProperty("ws_id")
    public void setWsId(Long wsId) {
        this.wsId = wsId;
    }

    public Check16SInput withWsId(Long wsId) {
        this.wsId = wsId;
        return this;
    }

    @JsonProperty("max_differences")
    public Long getMaxDifferences() {
        return maxDifferences;
    }

    @JsonProperty("max_differences")
    public void setMaxDifferences(Long maxDifferences) {
        this.maxDifferences = maxDifferences;
    }

    public Check16SInput withMaxDifferences(Long maxDifferences) {
        this.maxDifferences = maxDifferences;
        return this;
    }

    @JsonProperty("min_significant_differences")
    public Long getMinSignificantDifferences() {
        return minSignificantDifferences;
    }

    @JsonProperty("min_significant_differences")
    public void setMinSignificantDifferences(Long minSignificantDifferences) {
        this.minSignificantDifferences = minSignificantDifferences;
    }

    public Check16SInput withMinSignificantDifferences(Long minSignificantDifferences) {
        this.minSignificantDifferences = minSignificantDifferences;
        return this;
    }

    @JsonProperty("output_genomeset_pass")
    public String getOutputGenomesetPass() {
        return outputGenomesetPass;
    }

    @JsonProperty("output_genomeset_pass")
    public void setOutputGenomesetPass(String outputGenomesetPass) {
        this.outputGenomesetPass = outputGenomesetPass;
    }

    public Check16SInput withOutputGenomesetPass(String outputGenomesetPass) {
        this.outputGenomesetPass = outputGenomesetPass;
        return this;
    }

    @JsonProperty("output_genomeset_fail")
    public String getOutputGenomesetFail() {
        return outputGenomesetFail;
    }

    @JsonProperty("output_genomeset_fail")
    public void setOutputGenomesetFail(String outputGenomesetFail) {
        this.outputGenomesetFail = outputGenomesetFail;
    }

    public Check16SInput withOutputGenomesetFail(String outputGenomesetFail) {
        this.outputGenomesetFail = outputGenomesetFail;
        return this;
    }

    @JsonProperty("output_genomeset_unknown")
    public String getOutputGenomesetUnknown() {
        return outputGenomesetUnknown;
    }

    @JsonProperty("output_genomeset_unknown")
    public void setOutputGenomesetUnknown(String outputGenomesetUnknown) {
        this.outputGenomesetUnknown = outputGenomesetUnknown;
    }

    public Check16SInput withOutputGenomesetUnknown(String outputGenomesetUnknown) {
        this.outputGenomesetUnknown = outputGenomesetUnknown;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public String toString() {
        return ((((((((((((((((((((("Check16SInput"+" [genomesetRef=")+ genomesetRef)+", assemblyRef=")+ assemblyRef)+", wsName=")+ wsName)+", wsId=")+ wsId)+", maxDifferences=")+ maxDifferences)+", minSignificantDifferences=")+ minSignificantDifferences)+", outputGenomesetPass=")+ outputGenomesetPass)+", outputGenomesetFail=")+ outputGenomesetFail)+", outputGenomesetUnknown=")+ outputGenomesetUnknown)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
