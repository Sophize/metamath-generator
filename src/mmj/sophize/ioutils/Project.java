package mmj.sophize.ioutils;

import com.fasterxml.jackson.annotation.*;

public class Project extends Resource {
    private String abstractText;
    private String description;

    @JsonProperty("abstractText")
    public String getAbstractText() { return abstractText; }
    @JsonProperty("abstractText")
    public void setAbstractText(String value) { this.abstractText = value; }

    @JsonProperty("description")
    public String getDescription() { return description; }
    @JsonProperty("description")
    public void setDescription(String value) { this.description = value; }
}
