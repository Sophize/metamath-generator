package mmj.sophize.ioutils;

import com.fasterxml.jackson.annotation.*;

public class Author {
    private User user;

    @JsonProperty("user")
    public User getUser() { return user; }
    @JsonProperty("user")
    public void setUser(User value) { this.user = value; }
}
