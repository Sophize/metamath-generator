package mmj.sophize;

import java.util.*;
import com.fasterxml.jackson.annotation.*;

public class Proposition extends Resource {
    private Language language;
    private String[] lookupTerms;
    private MetaLanguage metaLanguage;
    private String negativeStatement;
    private String remarks;
    private String statement;

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

    @JsonProperty("negativeStatement")
    public String getNegativeStatement() { return negativeStatement; }
    @JsonProperty("negativeStatement")
    public void setNegativeStatement(String value) { this.negativeStatement = value; }

    @JsonProperty("remarks")
    public String getRemarks() { return remarks; }
    @JsonProperty("remarks")
    public void setRemarks(String value) { this.remarks = value; }

    @JsonProperty("statement")
    public String getStatement() { return statement; }
    @JsonProperty("statement")
    public void setStatement(String value) { this.statement = value; }
}
