package mmj.sophize.ioutils;

import com.fasterxml.jackson.annotation.*;

public class Resource {
    private Citation[] citations;
    private User contributor;
    private Boolean indexable;
    private String[] names;
    private String notPermanentPtr;
    private String permanentPtr;
    private String[] tags;

    @JsonProperty("citations")
    public Citation[] getCitations() { return citations; }
    @JsonProperty("citations")
    public void setCitations(Citation[] value) { this.citations = value; }

    @JsonProperty("contributor")
    public User getContributor() { return contributor; }
    @JsonProperty("contributor")
    public void setContributor(User value) { this.contributor = value; }

    @JsonProperty("indexable")
    public Boolean getIndexable() { return indexable; }
    @JsonProperty("indexable")
    public void setIndexable(Boolean value) { this.indexable = value; }

    @JsonProperty("names")
    public String[] getNames() { return names; }
    @JsonProperty("names")
    public void setNames(String[] value) { this.names = value; }

    @JsonProperty("notPermanentPtr")
    public String getNotPermanentPtr() { return notPermanentPtr; }
    @JsonProperty("notPermanentPtr")
    public void setNotPermanentPtr(String value) { this.notPermanentPtr = value; }

    @JsonProperty("permanentPtr")
    public String getPermanentPtr() { return permanentPtr; }
    @JsonProperty("permanentPtr")
    public void setPermanentPtr(String value) { this.permanentPtr = value; }

    @JsonProperty("tags")
    public String[] getTags() { return tags; }
    @JsonProperty("tags")
    public void setTags(String[] value) { this.tags = value; }
}
