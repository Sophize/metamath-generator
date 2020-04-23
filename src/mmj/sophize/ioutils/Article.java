package mmj.sophize.ioutils;

import com.fasterxml.jackson.annotation.*;

public class Article extends Resource {
    private String abstractText;
    private Author[] authors;
    private String beliefset;
    private String content;
    private String title;

    @JsonProperty("abstractText")
    public String getAbstractText() { return abstractText; }
    @JsonProperty("abstractText")
    public void setAbstractText(String value) { this.abstractText = value; }

    @JsonProperty("authors")
    public Author[] getAuthors() { return authors; }
    @JsonProperty("authors")
    public void setAuthors(Author[] value) { this.authors = value; }

    @JsonProperty("beliefset")
    public String getBeliefset() { return beliefset; }
    @JsonProperty("beliefset")
    public void setBeliefset(String value) { this.beliefset = value; }

    @JsonProperty("content")
    public String getContent() { return content; }
    @JsonProperty("content")
    public void setContent(String value) { this.content = value; }

    @JsonProperty("title")
    public String getTitle() { return title; }
    @JsonProperty("title")
    public void setTitle(String value) { this.title = value; }
}
