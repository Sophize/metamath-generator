package mmj.sophize.ioutils;

import java.util.*;
import com.fasterxml.jackson.annotation.*;

public class User {
    private String handle;
    private String userEmail;
    private String userLink;
    private String userName;
    private String userPic;

    @JsonProperty("handle")
    public String getHandle() { return handle; }
    @JsonProperty("handle")
    public void setHandle(String value) { this.handle = value; }

    @JsonProperty("userEmail")
    public String getUserEmail() { return userEmail; }
    @JsonProperty("userEmail")
    public void setUserEmail(String value) { this.userEmail = value; }

    @JsonProperty("userLink")
    public String getUserLink() { return userLink; }
    @JsonProperty("userLink")
    public void setUserLink(String value) { this.userLink = value; }

    @JsonProperty("userName")
    public String getUserName() { return userName; }
    @JsonProperty("userName")
    public void setUserName(String value) { this.userName = value; }

    @JsonProperty("userPic")
    public String getUserPic() { return userPic; }
    @JsonProperty("userPic")
    public void setUserPic(String value) { this.userPic = value; }
}
