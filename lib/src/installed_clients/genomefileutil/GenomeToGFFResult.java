
package genomefileutil;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * <p>Original spec-file type: GenomeToGFFResult</p>
 * <pre>
 * from_cache is 1 if the file already exists and was just returned, 0 if
 * the file was generated during this call.
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "file_path",
    "from_cache"
})
public class GenomeToGFFResult {

    @JsonProperty("file_path")
    private String filePath;
    @JsonProperty("from_cache")
    private Long fromCache;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("file_path")
    public String getFilePath() {
        return filePath;
    }

    @JsonProperty("file_path")
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public GenomeToGFFResult withFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    @JsonProperty("from_cache")
    public Long getFromCache() {
        return fromCache;
    }

    @JsonProperty("from_cache")
    public void setFromCache(Long fromCache) {
        this.fromCache = fromCache;
    }

    public GenomeToGFFResult withFromCache(Long fromCache) {
        this.fromCache = fromCache;
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
        return ((((((("GenomeToGFFResult"+" [filePath=")+ filePath)+", fromCache=")+ fromCache)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
