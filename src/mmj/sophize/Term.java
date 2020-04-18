package mmj.sophize;

import java.util.*;
import com.fasterxml.jackson.annotation.*;

public class Term extends Resource {
    private String[] alternatePhrases;
    private String definition;
    private Language language;
    private String[] lookupTerms;
    private MetaLanguage metaLanguage;
    private String phrase;
    private Boolean primitive;
    private String remarks;

    @JsonProperty("alternatePhrases")
    public String[] getAlternatePhrases() { return alternatePhrases; }
    @JsonProperty("alternatePhrases")
    public void setAlternatePhrases(String[] value) { this.alternatePhrases = value; }

    @JsonProperty("definition")
    public String getDefinition() { return definition; }
    @JsonProperty("definition")
    public void setDefinition(String value) { this.definition = value; }

    @JsonProperty("language")
    public Language getLanguage() { return language; }
    @JsonProperty("language")
    public void setLanguage(Language value) { this.language = value; }

    @JsonProperty("lookupTerms")
    public String[] getLookupTerms() { return lookupTerms; }
    @JsonProperty("lookupTerms")
    public void setLookupTerms(String[] value) { this.lookupTerms = value; }

    @JsonProperty("metaLanguage")
    public MetaLanguage getMetaLanguage() { return metaLanguage; }
    @JsonProperty("metaLanguage")
    public void setMetaLanguage(MetaLanguage value) { this.metaLanguage = value; }

    @JsonProperty("phrase")
    public String getPhrase() { return phrase; }
    @JsonProperty("phrase")
    public void setPhrase(String value) { this.phrase = value; }

    @JsonProperty("primitive")
    public Boolean getPrimitive() { return primitive; }
    @JsonProperty("primitive")
    public void setPrimitive(Boolean value) { this.primitive = value; }

    @JsonProperty("remarks")
    public String getRemarks() { return remarks; }
    @JsonProperty("remarks")
    public void setRemarks(String value) { this.remarks = value; }
}
