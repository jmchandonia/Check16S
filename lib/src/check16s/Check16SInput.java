
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
    "ws"
})
public class Check16SInput {

    @JsonProperty("genomeset_ref")
    private String genomesetRef;
    @JsonProperty("assembly_ref")
    private String assemblyRef;
    @JsonProperty("ws")
    private String ws;
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

    @JsonProperty("ws")
    public String getWs() {
        return ws;
    }

    @JsonProperty("ws")
    public void setWs(String ws) {
        this.ws = ws;
    }

    public Check16SInput withWs(String ws) {
        this.ws = ws;
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
        return ((((((((("Check16SInput"+" [genomesetRef=")+ genomesetRef)+", assemblyRef=")+ assemblyRef)+", ws=")+ ws)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
